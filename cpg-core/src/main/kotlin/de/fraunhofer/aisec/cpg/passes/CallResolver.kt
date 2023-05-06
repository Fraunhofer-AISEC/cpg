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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.HasComplexCallResolution
import de.fraunhofer.aisec.cpg.frontends.HasDefaultArguments
import de.fraunhofer.aisec.cpg.frontends.HasSuperClasses
import de.fraunhofer.aisec.cpg.frontends.HasTemplates
import de.fraunhofer.aisec.cpg.frontends.cpp.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration.TemplateInitialization
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
 * Resolves [CallExpression] and [NewExpression] targets.
 *
 * A [CallExpression] specifies the method that wants to be called via [CallExpression.name]. The
 * call target is a method of the same class the caller belongs to, so the name is resolved to the
 * appropriate [MethodDeclaration]. This pass also takes into consideration that a method might not
 * be present in the current class, but rather has its implementation in a superclass, and sets the
 * pointer accordingly.
 *
 * Constructor calls with [ConstructExpression] are resolved in such a way that their
 * [ConstructExpression.instantiates] points to the correct [RecordDeclaration]. Additionally, the
 * [ConstructExpression.constructor] is set to the according [ConstructorDeclaration].
 *
 * This pass should NOT use any DFG edges because they are computed / adjusted in a later stage.
 */
@DependsOn(VariableUsageResolver::class)
open class CallResolver : SymbolResolverPass() {
    /**
     * This seems to be a map between function declarations (more likely method declarations) and
     * their parent record (more accurately their type). Seems to be only used by
     * [getOverridingCandidates] and should probably be replaced through a scope manager call.
     */
    private val containingType = mutableMapOf<FunctionDeclaration, Type>()

    override fun cleanup() {
        containingType.clear()
    }

    override fun accept(translationResult: TranslationResult) {
        scopeManager = translationResult.scopeManager
        config = translationResult.config

        walker = ScopedWalker(scopeManager)
        walker.registerHandler { _, _, currNode -> walker.collectDeclarations(currNode) }
        walker.registerHandler { node, _ -> findRecords(node) }
        walker.registerHandler { node, _ -> findTemplates(node) }
        walker.registerHandler { currentClass, _, currentNode ->
            registerMethods(currentClass, currentNode)
        }

        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
        walker.clearCallbacks()
        walker.registerHandler { node, _ -> fixInitializers(node) }
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
        walker.clearCallbacks()
        walker.registerHandler { node, _ -> handleNode(node) }
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
    }

    private fun registerMethods(currentClass: RecordDeclaration?, currentNode: Node?) {
        if (currentNode is MethodDeclaration && currentClass != null) {
            containingType[currentNode] =
                TypeParser.createFrom(currentClass.name, currentClass.language)
        }
    }

    private fun fixInitializers(node: Node?) {
        if (node is VariableDeclaration) {
            // check if we have the corresponding class for this type
            val typeString = node.type.root.name
            if (typeString in recordMap) {
                val currInitializer = node.initializer
                if (currInitializer == null && node.isImplicitInitializerAllowed) {
                    val initializer = node.newConstructExpression(typeString, "$typeString()")
                    initializer.isImplicit = true
                    node.initializer = initializer
                    node.templateParameters?.let {
                        addImplicitTemplateParametersToCall(it, initializer)
                    }
                } else if (
                    currInitializer !is ConstructExpression &&
                        currInitializer is CallExpression &&
                        currInitializer.name.localName == node.type.root.name.localName
                ) {
                    // This should actually be a construct expression, not a call!
                    val arguments = currInitializer.arguments
                    val signature = arguments.map(Node::code).joinToString(", ")
                    val initializer =
                        node.newConstructExpression(typeString, "$typeString($signature)")
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
            is TranslationUnitDeclaration -> {
                currentTU = node
            }
            is ExplicitConstructorInvocation -> {
                handleExplicitConstructorInvocation(node)
            }
            is ConstructExpression -> {
                // We might have call expressions inside our arguments, so in order to correctly
                // resolve this call's signature, we need to make sure any call expression arguments
                // are fully resolved
                handleArguments(node)
                handleConstructExpression(node)
            }
            is CallExpression -> {
                // We might have call expressions inside our arguments, so in order to correctly
                // resolve this call's signature, we need to make sure any call expression arguments
                // are fully resolved
                handleArguments(node)
                handleCallExpression(scopeManager.currentRecord, node)
            }
        }
    }

    private fun handleCallExpression(curClass: RecordDeclaration?, call: CallExpression) {
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
                        scopeManager,
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
                            is TranslationUnitDeclaration ->
                                start.inferFunction(call, scopeManager = scopeManager)
                            is NamespaceDeclaration ->
                                start.inferFunction(call, scopeManager = scopeManager)
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
        if (callee is DeclaredReferenceExpression) {
            callee.refersTo = candidates.firstOrNull()
        }
    }

    /**
     * Resolves [call] to a list of [FunctionDeclaration] nodes, based on the
     * [CallExpression.callee] property.
     *
     * In case a resolution is not possible, `null` can be returned.
     */
    private fun resolveCallee(
        callee: Expression?,
        curClass: RecordDeclaration?,
        call: CallExpression
    ): List<FunctionDeclaration>? {
        return when (callee) {
            is MemberExpression -> resolveMemberCallee(callee, curClass, call)
            is DeclaredReferenceExpression -> resolveReferenceCallee(callee, curClass, call)
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

    private fun handleArguments(call: CallExpression) {
        val worklist: Deque<Node> = ArrayDeque()
        call.arguments.forEach { worklist.push(it) }
        while (!worklist.isEmpty()) {
            val curr = worklist.pop()
            if (curr is CallExpression) {
                handleNode(curr)
            } else {
                val it = Strategy.AST_FORWARD(curr)
                while (it.hasNext()) {
                    val astChild = it.next()
                    if (astChild !is RecordDeclaration) {
                        worklist.push(astChild)
                    }
                }
            }
        }
    }

    /**
     * Resolves a [CallExpression.callee] of type [DeclaredReferenceExpression] to a possible list
     * of [FunctionDeclaration] nodes.
     */
    private fun resolveReferenceCallee(
        callee: DeclaredReferenceExpression,
        curClass: RecordDeclaration?,
        call: CallExpression
    ): List<FunctionDeclaration> {
        val language = call.language

        if (curClass == null) {
            // Handle function (not method) calls. C++ allows function overloading. Make sure we
            // have at least the same number of arguments
            val candidates =
                if (language is HasComplexCallResolution) {
                    // Handle CXX normal call resolution externally, otherwise it leads to increased
                    // complexity
                    language.refineNormalCallResolution(call, scopeManager, currentTU)
                } else {
                    scopeManager.resolveFunction(call).toMutableList()
                }

            return candidates
        } else {
            return resolveMemberCallee(callee, curClass, call)
        }
    }

    /**
     * Resolves a [CallExpression.callee] of type [MemberExpression] to a possible list of
     * [FunctionDeclaration] nodes.
     *
     * TODO: Change callee to MemberExpression, but we can't since resolveReferenceCallee somehow
     *   delegates resolving of regular function calls within classes to this function (meh!)
     */
    fun resolveMemberCallee(
        callee: DeclaredReferenceExpression,
        curClass: RecordDeclaration?,
        call: CallExpression
    ): List<FunctionDeclaration> {
        // We need to adjust certain types of the base in case of a super call and we delegate this.
        // If that is successful, we can continue with regular resolving
        if (
            callee is MemberExpression &&
                callee.base is DeclaredReferenceExpression &&
                isSuperclassReference(callee.base as DeclaredReferenceExpression)
        ) {
            (callee.language as? HasSuperClasses)?.handleSuperCall(
                callee,
                curClass!!,
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
                (callee.language !is CPPLanguage || shouldSearchForInvokesInParent(call))
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

    private fun retrieveInvocationCandidatesFromCall(
        call: CallExpression,
        curClass: RecordDeclaration?,
        possibleContainingTypes: Set<Type>
    ): MutableList<FunctionDeclaration> {
        return if (call.language is HasComplexCallResolution) {
            (call.language as HasComplexCallResolution)
                .refineMethodCallResolution(
                    curClass,
                    possibleContainingTypes,
                    call,
                    scopeManager,
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
    private fun createMethodDummies(
        possibleContainingTypes: Set<Type>,
        call: CallExpression
    ): List<FunctionDeclaration> {
        return possibleContainingTypes
            .mapNotNull {
                var record = recordMap[it.root.name]
                if (record == null && config?.inferenceConfiguration?.inferRecords == true) {
                    record = it.startInference(scopeManager).inferRecordDeclaration(it, currentTU)
                    // update the record map
                    if (record != null) it.root.name.let { name -> recordMap[name] = record }
                }
                record
            }
            .map { record -> record.inferMethod(call, scopeManager = scopeManager) }
    }

    /**
     * In C++ search we don't search in the parent if there is a potential candidate with matching
     * name
     *
     * @param call
     * @return true if we should stop searching parent, false otherwise
     */
    private fun shouldSearchForInvokesInParent(call: CallExpression): Boolean {
        return scopeManager.resolveFunctionStopScopeTraversalOnDefinition(call).isEmpty()
    }

    private fun handleConstructExpression(constructExpression: ConstructExpression) {
        if (constructExpression.instantiates != null && constructExpression.constructor != null)
            return
        val typeName = constructExpression.type.name
        val recordDeclaration = recordMap[typeName]
        constructExpression.instantiates = recordDeclaration
        for (template in templateList) {
            if (
                template is ClassTemplateDeclaration &&
                    recordDeclaration != null &&
                    recordDeclaration in template.realizations &&
                    (constructExpression.templateParameters.size <= template.parameters.size)
            ) {
                val defaultDifference =
                    template.parameters.size - constructExpression.templateParameters.size
                if (defaultDifference <= template.parameterDefaults.size) {
                    // Check if predefined template value is used as default in next value
                    addRecursiveDefaultTemplateArgs(constructExpression, template)

                    // Add missing defaults
                    val missingNewParams: List<Node?> =
                        template.parameterDefaults.subList(
                            constructExpression.templateParameters.size,
                            template.parameterDefaults.size
                        )
                    for (missingParam in missingNewParams) {
                        if (missingParam != null) {
                            constructExpression.addTemplateParameter(
                                missingParam,
                                TemplateInitialization.DEFAULT
                            )
                        }
                    }
                    constructExpression.templateInstantiation = template
                    break
                }
            }
        }
        if (recordDeclaration != null) {
            val constructor = getConstructorDeclaration(constructExpression, recordDeclaration)
            constructExpression.constructor = constructor
        }
    }

    private fun handleExplicitConstructorInvocation(eci: ExplicitConstructorInvocation) {
        if (eci.containingClass != null) {
            val recordDeclaration = recordMap[eci.parseName(eci.containingClass!!)]
            val signature = eci.arguments.map { it.type }
            if (recordDeclaration != null) {
                val constructor =
                    getConstructorDeclarationForExplicitInvocation(signature, recordDeclaration)
                val invokes = mutableListOf<FunctionDeclaration>()
                invokes.add(constructor)
                eci.invokes = invokes
            }
        }
    }

    private fun getPossibleContainingTypes(node: Node?): Set<Type> {
        val possibleTypes = mutableSetOf<Type>()
        if (node is MemberCallExpression) {
            val base = node.base!!
            possibleTypes.add(base.type)
            possibleTypes.addAll(base.possibleSubTypes)
        } else {
            // This could be a C++ member call with an implicit this (which we do not create), so
            // lets add the current class to the possible list
            scopeManager.currentRecord?.toType()?.let { possibleTypes.add(it) }
        }

        return possibleTypes
    }

    fun getInvocationCandidatesFromRecord(
        recordDeclaration: RecordDeclaration?,
        name: String?,
        call: CallExpression
    ): List<FunctionDeclaration> {
        if (recordDeclaration == null) return listOf()

        val namePattern =
            Pattern.compile(
                "(" +
                    Pattern.quote(recordDeclaration.name.toString()) +
                    Regex.escape(recordDeclaration.language!!.namespaceDelimiter) +
                    ")?" +
                    Pattern.quote(name)
            )
        return if (call.language is HasComplexCallResolution) {
            (call.language as HasComplexCallResolution).refineInvocationCandidatesFromRecord(
                recordDeclaration,
                call,
                namePattern
            )
        } else {
            recordDeclaration.methods.filter {
                namePattern.matcher(it.name).matches() && it.hasSignature(call.signature)
            }
        }
    }

    private fun getInvocationCandidatesFromParents(
        name: String?,
        call: CallExpression,
        possibleTypes: Set<RecordDeclaration>
    ): List<FunctionDeclaration> {
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
            if (call.language is CPPLanguage) { // TODO: Needs a special trait?
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

    private fun getOverridingCandidates(
        possibleSubTypes: Set<Type?>,
        declaration: FunctionDeclaration
    ): Set<FunctionDeclaration> {
        return declaration.overriddenBy
            .filter { f -> containingType[f] in possibleSubTypes }
            .toSet()
    }

    /**
     * @param signature of the ConstructExpression
     * @param recordDeclaration matching the class the ConstructExpression wants to construct
     * @return ConstructorDeclaration that matches the provided signature
     */
    private fun getConstructorDeclarationDirectMatch(
        signature: List<Type?>,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration? {
        for (constructor in recordDeclaration.constructors) {
            if (constructor.hasSignature(signature)) {
                return constructor
            }
        }
        return null
    }

    /**
     * @param constructExpression we want to find an invocation target for
     * @param recordDeclaration associated with the Object the ConstructExpression constructs
     * @return a ConstructDeclaration that is an invocation of the given ConstructExpression. If
     *   there is no valid ConstructDeclaration we will create an implicit ConstructDeclaration that
     *   matches the ConstructExpression.
     */
    private fun getConstructorDeclaration(
        constructExpression: ConstructExpression,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration {
        val signature: List<Type?> = constructExpression.signature
        var constructorCandidate =
            getConstructorDeclarationDirectMatch(signature, recordDeclaration)
        if (constructorCandidate == null && constructExpression.language is HasDefaultArguments) {
            // Check for usage of default args
            constructorCandidate =
                resolveConstructorWithDefaults(constructExpression, signature, recordDeclaration)
        }
        if (
            constructorCandidate == null && constructExpression.language is CPPLanguage
        ) { // TODO: Fix this
            // If we don't find any candidate and our current language is c/c++ we check if there is
            // a candidate with an implicit cast
            constructorCandidate =
                resolveConstructorWithImplicitCast(constructExpression, recordDeclaration)
        }

        return constructorCandidate
            ?: recordDeclaration
                .startInference(scopeManager)
                .createInferredConstructor(constructExpression.signature)
    }

    private fun getConstructorDeclarationForExplicitInvocation(
        signature: List<Type?>,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration {
        return recordDeclaration.constructors.firstOrNull { it.hasSignature(signature) }
            ?: recordDeclaration.startInference(scopeManager).createInferredConstructor(signature)
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(CallResolver::class.java)

        /**
         * Adds implicit duplicates of the TemplateParams to the implicit ConstructExpression
         *
         * @param templateParams of the VariableDeclaration/NewExpression
         * @param constructExpression duplicate TemplateParameters (implicit) to preserve AST, as
         *   ConstructExpression uses AST as well as the VariableDeclaration/NewExpression
         */
        fun addImplicitTemplateParametersToCall(
            templateParams: List<Node>,
            constructExpression: ConstructExpression
        ) {
            for (node in templateParams) {
                if (node is TypeExpression) {
                    constructExpression.addTemplateParameter(node.duplicate(true))
                } else if (node is Literal<*>) {
                    constructExpression.addTemplateParameter(node.duplicate(true))
                }
            }
        }
    }
}
