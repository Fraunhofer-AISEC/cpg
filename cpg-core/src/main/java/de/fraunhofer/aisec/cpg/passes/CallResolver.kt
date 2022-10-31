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
import de.fraunhofer.aisec.cpg.frontends.HasTemplates
import de.fraunhofer.aisec.cpg.frontends.cpp.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration.TemplateInitialization
import de.fraunhofer.aisec.cpg.graph.duplicate
import de.fraunhofer.aisec.cpg.graph.newConstructExpression
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
        walker.registerHandler { node, _ -> resolve(node) }
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
    }

    private fun registerMethods(currentClass: RecordDeclaration?, currentNode: Node) {
        if (currentNode is MethodDeclaration && currentClass != null) {
            containingType[currentNode] = TypeParser.createFrom(currentClass.name, true)
        }
    }

    private fun fixInitializers(node: Node) {
        if (node is VariableDeclaration) {
            // check if we have the corresponding class for this type
            val typeString = node.type.root.name
            if (typeString in recordMap) {
                val currInitializer = node.initializer
                if (currInitializer == null && node.isImplicitInitializerAllowed) {
                    val initializer = node.newConstructExpression("()")
                    initializer.isImplicit = true
                    node.initializer = initializer
                    node.templateParameters?.let {
                        addImplicitTemplateParametersToCall(it, initializer)
                    }
                } else if (
                    currInitializer is CallExpression && currInitializer.name == typeString
                ) {
                    // This should actually be a construct expression, not a call!
                    val arguments = currInitializer.arguments
                    val signature = arguments.map(Node::code).joinToString(", ")
                    val initializer = node.newConstructExpression("($signature)")
                    initializer.arguments = mutableListOf(*arguments.toTypedArray())
                    initializer.isImplicit = true
                    node.initializer = initializer
                    currInitializer.disconnectFromGraph()
                }
            }
        }
    }

    protected fun resolve(node: Node) {
        when (node) {
            is TranslationUnitDeclaration -> {
                currentTU = node
            }
            is ExplicitConstructorInvocation -> {
                resolveExplicitConstructorInvocation(node)
            }
            is ConstructExpression -> {
                // We might have call expressions inside our arguments, so in order to correctly
                // resolve this call's signature, we need to make sure any call expression arguments
                // are fully resolved
                resolveArguments(node)
                resolveConstructExpression(node)
            }
            is CallExpression -> {
                // We might have call expressions inside our arguments, so in order to correctly
                // resolve this call's signature, we need to make sure any call expression arguments
                // are fully resolved
                resolveArguments(node)
                handleCallExpression(scopeManager!!.currentRecord, node)
            }
        }
    }

    private fun handleCallExpression(curClass: RecordDeclaration?, call: CallExpression) {
        if (
            call.base is DeclaredReferenceExpression &&
                isSuperclassReference(call.base as DeclaredReferenceExpression)
        ) {
            handleSuperCall(curClass!!, call)
            return
        }

        if (call is MemberCallExpression) {
            val member = call.member
            if (!(member is HasType && (member as HasType).type is FunctionPointerType)) {
                // function pointers are handled by extra pass
                handleMethodCall(curClass, call)
            }
            return
        }
        if (call.instantiatesTemplate() && call.language is HasTemplates) {
            handleTemplateFunctionCalls(curClass, call, true)
            return
        }

        // we could be referring to a function pointer even though it is not a member call if the
        // usual function pointer syntax (*fp)() has been omitted: fp(). Looks like a normal call,
        // but it isn't
        val funcPointer =
            walker.getDeclarationForScope(call) { v ->
                v.type is FunctionPointerType && v.name == call.name
            }
        if (!funcPointer.isPresent) {
            // function pointers are handled by extra pass
            handleNormalCalls(curClass, call)
        }
    }

    private fun resolveArguments(call: CallExpression) {
        val worklist: Deque<Node> = ArrayDeque()
        call.arguments.forEach { worklist.push(it) }
        while (!worklist.isEmpty()) {
            val curr = worklist.pop()
            if (curr is CallExpression) {
                resolve(curr)
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

    protected open fun handleNormalCalls(curClass: RecordDeclaration?, call: CallExpression) {
        if (curClass == null) {
            // Handle function (not method) calls
            // C++ allows function overloading. Make sure we have at least the same number of
            // arguments
            if (call.language is HasComplexCallResolution) {
                // Handle CXX normal call resolution externally, otherwise it leads to increased
                // complexity
                // TODO: Move this to doBetterCallResolution()
                handleNormalCallCXX(call)
            } else {
                val invocationCandidates = scopeManager!!.resolveFunction(call).toMutableList()

                if (invocationCandidates.isEmpty()) {
                    // If we have no candidates, we create an inferred FunctionDeclaration
                    invocationCandidates.add(currentTU.inferFunction(call))
                }

                call.invokes = invocationCandidates
            }
        } else if (!handlePossibleStaticImport(call, curClass)) {
            handleMethodCall(curClass, call)
        }
    }

    fun handleMethodCall(curClass: RecordDeclaration?, call: CallExpression) {
        val possibleContainingTypes = getPossibleContainingTypes(call, curClass)

        // Find overridden invokes
        var invocationCandidates =
            call.invokes
                .map { getOverridingCandidates(possibleContainingTypes, it) }
                .flatten()
                .toMutableList()

        // Find function targets
        if (invocationCandidates.isEmpty()) {
            invocationCandidates =
                retrieveInvocationCandidatesFromCall(call, curClass, possibleContainingTypes)
        }

        // Find invokes by supertypes
        if (
            invocationCandidates.isEmpty() &&
                (call.language !is CPPLanguage || shouldSearchForInvokesInParent(call))
        ) {
            val nameParts =
                call.name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (nameParts.isNotEmpty()) {
                val records =
                    possibleContainingTypes.mapNotNull { recordMap[it.root.typeName] }.toSet()
                invocationCandidates =
                    getInvocationCandidatesFromParents(nameParts[nameParts.size - 1], call, records)
                        .toMutableList()
            }
        }
        createMethodDummies(invocationCandidates, possibleContainingTypes, call)
        call.invokes = invocationCandidates
    }

    private fun retrieveInvocationCandidatesFromCall(
        call: CallExpression,
        curClass: RecordDeclaration?,
        possibleContainingTypes: Set<Type>
    ): MutableList<FunctionDeclaration> {
        return if (call.language is HasComplexCallResolution) {
            // TODO: Move logic to the doBetterCallResolution
            handleCXXMethodCall(curClass, possibleContainingTypes, call).toMutableList()
        } else {
            scopeManager!!.resolveFunction(call).toMutableList()
        }
    }

    /**
     * Creates an inferred element for each RecordDeclaration if the invocationCandidates are empty
     *
     * @param invocationCandidates
     * @param possibleContainingTypes
     * @param call
     */
    private fun createMethodDummies(
        invocationCandidates: MutableList<FunctionDeclaration>,
        possibleContainingTypes: Set<Type>,
        call: CallExpression
    ) {
        if (invocationCandidates.isEmpty()) {
            possibleContainingTypes
                .mapNotNull {
                    var record = recordMap[it.root.typeName]
                    if (record == null && config?.inferenceConfiguration?.inferRecords == true) {
                        record = inferRecordDeclaration(it, it.root.typeName)
                    }
                    record
                }
                .map { record -> record.inferMethod(call) }
                .forEach { invocationCandidates.add(it) }
        }
    }

    /**
     * In C++ search we don't search in the parent if there is a potential candidate with matching
     * name
     *
     * @param call
     * @return true if we should stop searching parent, false otherwise
     */
    private fun shouldSearchForInvokesInParent(call: CallExpression): Boolean {
        if (scopeManager == null) {
            Util.errorWithFileLocation(
                call,
                log,
                "Could not search for invokes in parent: scopeManager is null"
            )
            return false
        }
        return scopeManager!!.resolveFunctionStopScopeTraversalOnDefinition(call).isEmpty()
    }

    private fun resolveConstructExpression(constructExpression: ConstructExpression) {
        val typeName = constructExpression.type.typeName
        val recordDeclaration = recordMap[typeName]
        constructExpression.instantiates = recordDeclaration
        for (template in templateList) {
            if (
                template is ClassTemplateDeclaration &&
                    recordDeclaration in template.realization &&
                    (constructExpression.templateParameters.size <= template.getParameters().size)
            ) {
                val defaultDifference =
                    template.getParameters().size - constructExpression.templateParameters.size
                if (defaultDifference <= template.getParameterDefaults().size) {
                    // Check if predefined template value is used as default in next value
                    addRecursiveDefaultTemplateArgs(constructExpression, template)

                    // Add missing defaults
                    val missingNewParams: List<Node> =
                        template
                            .getParameterDefaults()
                            .subList(
                                constructExpression.templateParameters.size,
                                template.getParameterDefaults().size
                            )
                    for (missingParam in missingNewParams) {
                        constructExpression.addTemplateParameter(
                            missingParam,
                            TemplateInitialization.DEFAULT
                        )
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

    private fun resolveExplicitConstructorInvocation(eci: ExplicitConstructorInvocation) {
        if (eci.containingClass != null) {
            val recordDeclaration = recordMap[eci.containingClass]
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

    private fun handlePossibleStaticImport(
        call: CallExpression,
        curClass: RecordDeclaration
    ): Boolean {
        val name = call.name.substring(call.name.lastIndexOf('.') + 1)
        val nameMatches =
            curClass.staticImports.filterIsInstance<FunctionDeclaration>().filter {
                it.name == name || it.name.endsWith(".$name")
            }
        return if (nameMatches.isEmpty()) {
            false
        } else {
            val invokes = mutableListOf<FunctionDeclaration>()
            val target = nameMatches.firstOrNull { it.hasSignature(call.signature) }
            if (target == null) {
                generateInferredStaticallyImportedMethods(call, name, invokes, curClass)
            } else {
                invokes.add(target)
            }
            call.invokes = invokes
            true
        }
    }

    private fun generateInferredStaticallyImportedMethods(
        call: CallExpression,
        name: String,
        invokes: MutableList<FunctionDeclaration>,
        curClass: RecordDeclaration?
    ) {
        // We had an import for this method name, just not the correct signature. Let's just add
        // an inferred node to any class that might be affected
        if (curClass == null) {
            LOGGER.warn("Cannot generate inferred nodes for imports of a null class: $call")
            return
        }
        val containingRecords =
            curClass.staticImportStatements
                .filter { it.endsWith(".$name") }
                .map { it.substring(0, it.lastIndexOf('.')) }
                .mapNotNull { recordMap[it] }

        for (recordDeclaration in containingRecords) {
            val inferred =
                recordDeclaration
                    .startInference()
                    .createInferredFunctionDeclaration(
                        name,
                        "",
                        true,
                        call.signature,
                        call.type // TODO: Is this correct?
                    )

            invokes.add(inferred)
        }
    }

    private fun getPossibleContainingTypes(node: Node?, curClass: RecordDeclaration?): Set<Type> {
        val possibleTypes = mutableSetOf<Type>()
        if (node is MemberCallExpression) {
            val base = node.base!!
            possibleTypes.add(base.type)
            possibleTypes.addAll(base.possibleSubTypes)
        } else if (node is StaticCallExpression) {
            if (node.targetRecord != null) {
                possibleTypes.add(TypeParser.createFrom(node.targetRecord, true))
            }
        } else if (curClass != null) {
            possibleTypes.add(TypeParser.createFrom(curClass.name, true))
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
                "(" + Pattern.quote(recordDeclaration.name) + "\\.)?" + Pattern.quote(name)
            )
        return if (
            call.language is HasComplexCallResolution
        ) { // TODO Move to doBetterCallResolution
            getInvocationCandidatesFromRecordCXX(recordDeclaration, call, namePattern)
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
     * there is no valid ConstructDeclaration we will create an implicit ConstructDeclaration that
     * matches the ConstructExpression.
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
                .startInference()
                .createInferredConstructor(constructExpression.signature)
    }

    private fun getConstructorDeclarationForExplicitInvocation(
        signature: List<Type?>,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration {
        return recordDeclaration.constructors.firstOrNull { it.hasSignature(signature) }
            ?: recordDeclaration.startInference().createInferredConstructor(signature)
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(CallResolver::class.java)

        /**
         * Adds implicit duplicates of the TemplateParams to the implicit ConstructExpression
         *
         * @param templateParams of the VariableDeclaration/NewExpression
         * @param constructExpression duplicate TemplateParameters (implicit) to preserve AST, as
         * ConstructExpression uses AST as well as the VariableDeclaration/NewExpression
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
