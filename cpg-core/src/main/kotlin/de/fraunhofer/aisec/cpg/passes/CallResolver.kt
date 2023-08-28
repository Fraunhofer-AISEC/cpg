/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDecl.TemplateInitialization
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.inference.inferFunction
import de.fraunhofer.aisec.cpg.passes.inference.inferMethod
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.*
import java.util.regex.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Resolves [CallExpr] and [NewExpr] targets.
 *
 * A [CallExpr] specifies the method that wants to be called via [CallExpr.name]. The call target is
 * a method of the same class the caller belongs to, so the name is resolved to the appropriate
 * [MethodDecl]. This pass also takes into consideration that a method might not be present in the
 * current class, but rather has its implementation in a superclass, and sets the pointer
 * accordingly.
 *
 * Constructor calls with [ConstructExpr] are resolved in such a way that their
 * [ConstructExpr.instantiates] points to the correct [RecordDecl]. Additionally, the
 * [ConstructExpr.constructor] is set to the according [ConstructorDecl].
 *
 * This pass should NOT use any DFG edges because they are computed / adjusted in a later stage.
 */
@DependsOn(VariableUsageResolver::class)
open class CallResolver(ctx: TranslationContext) : SymbolResolverPass(ctx) {
    /**
     * This seems to be a map between function declarations (more likely method declarations) and
     * their parent record (more accurately their type). Seems to be only used by
     * [getOverridingCandidates] and should probably be replaced through a scope manager call.
     */
    protected val containingType = mutableMapOf<FunctionDecl, Type>()

    override fun cleanup() {
        containingType.clear()
    }

    override fun accept(component: Component) {
        walker = ScopedWalker(scopeManager)
        walker.registerHandler { _, _, currNode -> walker.collectDeclarations(currNode) }
        walker.registerHandler { node, _ -> findRecords(node) }
        walker.registerHandler { node, _ -> findTemplates(node) }
        walker.registerHandler { currentClass, _, currentNode ->
            registerMethods(currentClass, currentNode)
        }

        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
        walker.clearCallbacks()
        walker.registerHandler { node, _ -> fixInitializers(node) }
        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
        walker.clearCallbacks()
        walker.registerHandler { node, _ -> handleNode(node) }
        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
    }

    protected fun registerMethods(currentClass: RecordDecl?, currentNode: Node?) {
        if (currentNode is MethodDecl && currentClass != null) {
            containingType[currentNode] = currentNode.objectType(currentClass.name)
        }
    }

    protected fun fixInitializers(node: Node?) {
        if (node is VariableDecl) {
            // check if we have the corresponding class for this type
            val typeString = node.type.root.name
            if (typeString in recordMap) {
                val currInitializer = node.initializer
                if (currInitializer == null && node.isImplicitInitializerAllowed) {
                    val initializer = node.newConstructExpr(typeString, "$typeString()")
                    initializer.type = node.type
                    initializer.isImplicit = true
                    node.initializer = initializer
                    node.templateParameters?.let {
                        addImplicitTemplateParametersToCall(it, initializer)
                    }
                } else if (
                    currInitializer !is ConstructExpr &&
                        currInitializer is CallExpr &&
                        currInitializer.name.localName == node.type.root.name.localName
                ) {
                    // This should actually be a construct expression, not a call!
                    val arguments = currInitializer.arguments
                    val signature = arguments.map(Node::code).joinToString(", ")
                    val initializer = node.newConstructExpr(typeString, "$typeString($signature)")
                    initializer.type = node.type
                    initializer.arguments = mutableListOf(*arguments.toTypedArray())
                    initializer.isImplicit = true
                    node.initializer = initializer
                    currInitializer.disconnectFromGraph()
                }
            }
        }
    }

    protected fun handleNode(node: Node?) {
        when (node) {
            is TranslationUnitDecl -> {
                currentTU = node
            }
            is ConstructorCallExpr -> {
                handleExplicitConstructorInvocation(node)
            }
            is ConstructExpr -> {
                // We might have call expressions inside our arguments, so in order to correctly
                // resolve this call's signature, we need to make sure any call expression arguments
                // are fully resolved
                handleArguments(node)
                handleConstructExpression(node)
            }
            is CallExpr -> {
                // We might have call expressions inside our arguments, so in order to correctly
                // resolve this call's signature, we need to make sure any call expression arguments
                // are fully resolved
                handleArguments(node)
                handleCallExpression(scopeManager.currentRecord, node)
            }
        }
    }

    protected fun handleCallExpression(curClass: RecordDecl?, call: CallExpr) {
        // Function pointers are handled by extra pass, so we are not resolving them here
        if (call.callee?.type is FunctionPointerType) {
            return
        }

        // At this point, we decide what to do based on the callee property
        val callee = call.callee

        // With one exception. If the language supports templates and if this is a template call, we
        // delegate it to the language. In the future, we definitely want to do this in a smarter
        // way
        var candidates =
            if (call.instantiatesTemplate() && call.language is HasTemplates) {
                val (_, candidates) =
                    (call.language as HasTemplates).handleTemplateFunctionCalls(
                        curClass,
                        call,
                        true,
                        ctx,
                        currentTU
                    )

                candidates
            } else {
                resolveCallee(callee, curClass, call) ?: return
            }

        // If we do not have any candidates at this point, we will infer one.
        if (candidates.isEmpty()) {
            // We need to see, whether we have any suitable base (e.g. a class) or not
            val suitableBases = getPossibleContainingTypes(call)
            candidates =
                if (suitableBases.isEmpty()) {
                    // This is not really the most ideal place, but for now this will do. While this
                    // is definitely a function, it could still be a function inside a namespace. In
                    // this case, we want to start inference in that particular namespace and not in
                    // the TU. It is also a little bit redundant, since ScopeManager.resolveFunction
                    // (which gets called before) already extracts the scope, but this information
                    // gets lost.
                    val scope = scopeManager.extractScope(call, scopeManager.globalScope)

                    // We have two possible start points, a namespace declaration or a translation
                    // unit. Nothing else is allowed (fow now)
                    val func =
                        when (val start = scope?.astNode) {
                            is TranslationUnitDecl -> start.inferFunction(call, ctx = ctx)
                            is NamespaceDecl -> start.inferFunction(call, ctx = ctx)
                            else -> null
                        }

                    listOfNotNull(func)
                } else {
                    createMethodDummies(suitableBases, call)
                }
        }

        // Set the INVOKES edge to our candidates
        call.invokes = candidates

        // Additionally, also set the REFERS_TO of the callee. In the future, we might make more
        // resolution decisions based on the callee itself. Unfortunately we can only set one here,
        // so we will take the first one
        if (callee is Reference) {
            callee.refersTo = candidates.firstOrNull()
        }
    }

    /**
     * Resolves [call] to a list of [FunctionDecl] nodes, based on the [CallExpr.callee] property.
     *
     * In case a resolution is not possible, `null` can be returned.
     */
    protected fun resolveCallee(
        callee: Expression?,
        curClass: RecordDecl?,
        call: CallExpr
    ): List<FunctionDecl>? {
        return when (callee) {
            is MemberExpr -> resolveMemberCallee(callee, curClass, call)
            is Reference -> resolveReferenceCallee(callee, curClass, call)
            null -> {
                Util.warnWithFileLocation(
                    call,
                    log,
                    "Call expression without callee, maybe because of a parsing error"
                )
                null
            }
            else -> {
                Util.errorWithFileLocation(
                    call,
                    log,
                    "Could not resolve callee of unsupported type ${callee.javaClass}"
                )
                null
            }
        }
    }

    protected fun handleArguments(call: CallExpr) {
        val worklist: Deque<Node> = ArrayDeque()
        call.arguments.forEach { worklist.push(it) }
        while (worklist.isNotEmpty()) {
            val curr = worklist.pop()
            if (curr is CallExpr) {
                handleNode(curr)
            } else {
                val it = Strategy.AST_FORWARD(curr)
                while (it.hasNext()) {
                    val astChild = it.next()
                    if (astChild !is RecordDecl) {
                        worklist.push(astChild)
                    }
                }
            }
        }
    }

    /**
     * Resolves a [CallExpr.callee] of type [Reference] to a possible list of [FunctionDecl] nodes.
     */
    protected fun resolveReferenceCallee(
        callee: Reference,
        curClass: RecordDecl?,
        call: CallExpr
    ): List<FunctionDecl> {
        val language = call.language

        return if (curClass == null) {
            // Handle function (not method) calls. C++ allows function overloading. Make sure we
            // have at least the same number of arguments
            val candidates =
                if (language is HasComplexCallResolution) {
                    // Handle CXX normal call resolution externally, otherwise it leads to increased
                    // complexity
                    language.refineNormalCallResolution(call, ctx, currentTU)
                } else {
                    scopeManager.resolveFunction(call).toMutableList()
                }

            candidates
        } else {
            resolveMemberCallee(callee, curClass, call)
        }
    }

    /**
     * Resolves a [CallExpr.callee] of type [MemberExpr] to a possible list of [FunctionDecl] nodes.
     *
     * TODO: Change callee to MemberExpression, but we can't since resolveReferenceCallee somehow
     *   delegates resolving of regular function calls within classes to this function (meh!)
     */
    fun resolveMemberCallee(
        callee: Reference,
        curClass: RecordDecl?,
        call: CallExpr
    ): List<FunctionDecl> {
        // We need to adjust certain types of the base in case of a super call and we delegate this.
        // If that is successful, we can continue with regular resolving
        if (
            curClass != null &&
                callee is MemberExpr &&
                callee.base is Reference &&
                isSuperclassReference(callee.base as Reference)
        ) {
            (callee.language as? HasSuperClasses)?.handleSuperCall(
                callee,
                curClass,
                scopeManager,
                recordMap
            )
        }

        val possibleContainingTypes = getPossibleContainingTypes(call)

        // Find function targets
        var invocationCandidates =
            retrieveInvocationCandidatesFromCall(call, curClass, possibleContainingTypes)

        // Find invokes by supertypes
        if (
            invocationCandidates.isEmpty() &&
                callee.name.localName.isNotEmpty() &&
                (!callee.language.isCPP || shouldSearchForInvokesInParent(call))
        ) {
            val records = possibleContainingTypes.mapNotNull { recordMap[it.root.name] }.toSet()
            invocationCandidates =
                getInvocationCandidatesFromParents(callee.name.localName, call, records)
                    .toMutableList()
        }

        // Add overridden invokes
        invocationCandidates.addAll(
            invocationCandidates
                .map { getOverridingCandidates(possibleContainingTypes, it) }
                .flatten()
                .toMutableList()
        )

        return invocationCandidates
    }

    protected fun retrieveInvocationCandidatesFromCall(
        call: CallExpr,
        curClass: RecordDecl?,
        possibleContainingTypes: Set<Type>
    ): MutableList<FunctionDecl> {
        return if (call.language is HasComplexCallResolution) {
            (call.language as HasComplexCallResolution)
                .refineMethodCallResolution(
                    curClass,
                    possibleContainingTypes,
                    call,
                    ctx,
                    currentTU,
                    this
                )
                .toMutableList()
        } else {
            scopeManager.resolveFunction(call).toMutableList()
        }
    }

    /**
     * Creates an inferred element for each RecordDeclaration
     *
     * @param possibleContainingTypes
     * @param call
     */
    protected fun createMethodDummies(
        possibleContainingTypes: Set<Type>,
        call: CallExpr
    ): List<FunctionDecl> {
        return possibleContainingTypes
            .mapNotNull {
                var record = recordMap[it.root.name]
                if (record == null && config.inferenceConfiguration.inferRecords == true) {
                    record = it.startInference(ctx).inferRecordDeclaration(it, currentTU)
                    // update the record map
                    if (record != null) it.root.name.let { name -> recordMap[name] = record }
                }
                record
            }
            .map { record -> record.inferMethod(call, ctx = ctx) }
    }

    /**
     * In C++ search we don't search in the parent if there is a potential candidate with matching
     * name
     *
     * @param call
     * @return true if we should stop searching parent, false otherwise
     */
    protected fun shouldSearchForInvokesInParent(call: CallExpr): Boolean {
        return scopeManager.resolveFunctionStopScopeTraversalOnDefinition(call).isEmpty()
    }

    protected fun handleConstructExpression(constructExpr: ConstructExpr) {
        if (constructExpr.instantiates != null && constructExpr.constructor != null) return
        val typeName = constructExpr.type.name
        val recordDeclaration = recordMap[typeName]
        constructExpr.instantiates = recordDeclaration
        for (template in templateList) {
            if (
                template is ClassTemplateDecl &&
                    recordDeclaration != null &&
                    recordDeclaration in template.realizations &&
                    (constructExpr.templateParameters.size <= template.parameters.size)
            ) {
                val defaultDifference =
                    template.parameters.size - constructExpr.templateParameters.size
                if (defaultDifference <= template.parameterDefaults.size) {
                    // Check if predefined template value is used as default in next value
                    addRecursiveDefaultTemplateArgs(constructExpr, template)

                    // Add missing defaults
                    val missingNewParams: List<Node?> =
                        template.parameterDefaults.subList(
                            constructExpr.templateParameters.size,
                            template.parameterDefaults.size
                        )
                    for (missingParam in missingNewParams) {
                        if (missingParam != null) {
                            constructExpr.addTemplateParameter(
                                missingParam,
                                TemplateInitialization.DEFAULT
                            )
                        }
                    }
                    constructExpr.templateInstantiation = template
                    break
                }
            }
        }
        if (recordDeclaration != null) {
            val constructor = getConstructorDeclaration(constructExpr, recordDeclaration)
            constructExpr.constructor = constructor
        }
    }

    protected fun handleExplicitConstructorInvocation(eci: ConstructorCallExpr) {
        eci.containingClass?.let { containingClass ->
            val recordDeclaration = recordMap[eci.parseName(containingClass)]
            val signature = eci.arguments.map { it.type }
            if (recordDeclaration != null) {
                val constructor =
                    getConstructorDeclarationForExplicitInvocation(signature, recordDeclaration)
                val invokes = mutableListOf<FunctionDecl>()
                invokes.add(constructor)
                eci.invokes = invokes
            }
        }
    }

    protected fun getPossibleContainingTypes(node: Node?): Set<Type> {
        val possibleTypes = mutableSetOf<Type>()
        if (node is MemberCallExpr) {
            node.base?.let { base ->
                possibleTypes.add(base.type)
                possibleTypes.addAll(base.assignedTypes)
            }
        } else {
            // This could be a C++ member call with an implicit this (which we do not create), so
            // let's add the current class to the possible list
            scopeManager.currentRecord?.toType()?.let { possibleTypes.add(it) }
        }

        return possibleTypes
    }

    fun getInvocationCandidatesFromRecord(
        recordDecl: RecordDecl?,
        name: String?,
        call: CallExpr
    ): List<FunctionDecl> {
        if (recordDecl == null) return listOf()

        val namePattern =
            Pattern.compile(
                "(" +
                    Pattern.quote(recordDecl.name.toString()) +
                    Regex.escape(recordDecl.language?.namespaceDelimiter ?: "") +
                    ")?" +
                    Pattern.quote(name)
            )
        return if (call.language is HasComplexCallResolution) {
            (call.language as HasComplexCallResolution).refineInvocationCandidatesFromRecord(
                recordDecl,
                call,
                namePattern,
                ctx
            )
        } else {
            recordDecl.methods.filter {
                namePattern.matcher(it.name).matches() && it.hasSignature(call.signature)
            }
        }
    }

    protected fun getInvocationCandidatesFromParents(
        name: String?,
        call: CallExpr,
        possibleTypes: Set<RecordDecl>
    ): List<FunctionDecl> {
        val workingPossibleTypes = mutableSetOf(*possibleTypes.toTypedArray())
        return if (possibleTypes.isEmpty()) {
            listOf()
        } else {
            val firstLevelCandidates =
                possibleTypes.map { getInvocationCandidatesFromRecord(it, name, call) }.flatten()

            // C++ does not allow overloading at different hierarchy levels. If we find a
            // FunctionDeclaration with the same name as the function in the CallExpression we have
            // to stop the search in the parent even if the FunctionDeclaration does not match with
            // the signature of the CallExpression
            if (call.language.isCPP) { // TODO: Needs a special trait?
                workingPossibleTypes.removeIf { recordDeclaration ->
                    !shouldContinueSearchInParent(recordDeclaration, name)
                }
            }
            firstLevelCandidates.ifEmpty {
                workingPossibleTypes
                    .map { it.superTypeDeclarations }
                    .map { getInvocationCandidatesFromParents(name, call, it) }
                    .flatten()
            }
        }
    }

    protected val Language<*>?.isCPP: Boolean
        get() {
            return this != null && this::class.simpleName == "CPPLanguage"
        }

    protected fun getOverridingCandidates(
        possibleSubTypes: Set<Type>,
        declaration: FunctionDecl
    ): Set<FunctionDecl> {
        return declaration.overriddenBy
            .filter { f -> containingType[f] in possibleSubTypes }
            .toSet()
    }

    /**
     * @param signature of the ConstructExpression
     * @param recordDecl matching the class the ConstructExpression wants to construct
     * @return ConstructorDeclaration that matches the provided signature
     */
    protected fun getConstructorDeclarationDirectMatch(
        signature: List<Type>,
        recordDecl: RecordDecl
    ): ConstructorDecl? {
        for (constructor in recordDecl.constructors) {
            if (constructor.hasSignature(signature)) {
                return constructor
            }
        }
        return null
    }

    /**
     * @param constructExpr we want to find an invocation target for
     * @param recordDecl associated with the Object the ConstructExpression constructs
     * @return a ConstructDeclaration that is an invocation of the given ConstructExpression. If
     *   there is no valid ConstructDeclaration we will create an implicit ConstructDeclaration that
     *   matches the ConstructExpression.
     */
    protected fun getConstructorDeclaration(
        constructExpr: ConstructExpr,
        recordDecl: RecordDecl
    ): ConstructorDecl {
        val signature = constructExpr.signature
        var constructorCandidate = getConstructorDeclarationDirectMatch(signature, recordDecl)
        if (constructorCandidate == null && constructExpr.language is HasDefaultArguments) {
            // Check for usage of default args
            constructorCandidate =
                resolveConstructorWithDefaults(constructExpr, signature, recordDecl)
        }
        if (constructorCandidate == null && constructExpr.language.isCPP) { // TODO: Fix this
            // If we don't find any candidate and our current language is c/c++ we check if there is
            // a candidate with an implicit cast
            constructorCandidate = resolveConstructorWithImplicitCast(constructExpr, recordDecl)
        }

        return constructorCandidate
            ?: recordDecl.startInference(ctx).createInferredConstructor(constructExpr.signature)
    }

    protected fun getConstructorDeclarationForExplicitInvocation(
        signature: List<Type>,
        recordDecl: RecordDecl
    ): ConstructorDecl {
        return recordDecl.constructors.firstOrNull { it.hasSignature(signature) }
            ?: recordDecl.startInference(ctx).createInferredConstructor(signature)
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(CallResolver::class.java)

        /**
         * Adds implicit duplicates of the TemplateParams to the implicit ConstructExpression
         *
         * @param templateParams of the VariableDeclaration/NewExpressionp
         * @param constructExpr duplicate TemplateParameters (implicit) to preserve AST, as
         *   ConstructExpression uses AST as well as the VariableDeclaration/NewExpression
         */
        fun addImplicitTemplateParametersToCall(
            templateParams: List<Node>,
            constructExpr: ConstructExpr
        ) {
            for (node in templateParams) {
                if (node is TypeExpr) {
                    constructExpr.addTemplateParameter(node.duplicate(true))
                } else if (node is Literal<*>) {
                    constructExpr.addTemplateParameter(node.duplicate(true))
                }
            }
        }
    }
}
