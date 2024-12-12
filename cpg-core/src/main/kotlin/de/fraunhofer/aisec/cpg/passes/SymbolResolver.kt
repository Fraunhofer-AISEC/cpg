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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.CallResolutionResult.SuccessKind.*
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.helpers.replace
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.inference.tryFieldInference
import de.fraunhofer.aisec.cpg.passes.inference.tryFunctionInference
import de.fraunhofer.aisec.cpg.passes.inference.tryVariableInference
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Creates new connections between the place where a variable is declared and where it is used.
 *
 * A field access is modeled with a [MemberExpression]. After AST building, its base and member
 * references are set to [Reference] stubs. This pass resolves those references and makes the member
 * point to the appropriate [FieldDeclaration] and the base to the "this" [FieldDeclaration] of the
 * containing class. It is also capable of resolving references to fields that are inherited from a
 * superclass and thus not declared in the actual base class. When base or member declarations are
 * not found in the graph, a new "inferred" [FieldDeclaration] is being created that is then used to
 * collect all usages to the same unknown declaration. [Reference] stubs are removed from the graph
 * after being resolved.
 *
 * Accessing a local variable is modeled directly with a [Reference]. This step of the pass doesn't
 * remove the [Reference] nodes like in the field usage case but rather makes their "refersTo" point
 * to the appropriate [ValueDeclaration].
 *
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
@DependsOn(TypeResolver::class)
@DependsOn(TypeHierarchyResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(ImportResolver::class)
open class SymbolResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    protected lateinit var walker: ScopedWalker

    protected val templateList = mutableListOf<TemplateDeclaration>()

    override fun accept(component: Component) {
        ctx.currentComponent = component
        walker = ScopedWalker(scopeManager)

        walker.registerHandler(::findTemplates)
        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }

        walker.strategy = Strategy::EOG_FORWARD
        walker.clearCallbacks()
        walker.registerHandler(this::handle)

        // Resolve symbols in our translation units in the order depending on their import
        // dependencies
        component.importDependencies.sortedTranslationUnits.forEach {
            log.debug("Resolving symbols of translation unit {}", it.name)

            // Gather all resolution EOG starters; and make sure they really do not have a
            // predecessor, otherwise we might analyze a node multiple times
            val nodes = it.allEOGStarters.filter { it.prevEOGEdges.isEmpty() }

            for (node in nodes) {
                walker.iterate(node)
            }
        }
    }

    override fun cleanup() {
        templateList.clear()
    }

    /** Caches all TemplateDeclarations in [templateList] */
    protected fun findTemplates(node: Node?) {
        if (node is TemplateDeclaration) {
            templateList.add(node)
        }
    }

    /**
     * Determines if the [reference] refers to the super class and we have to start searching there.
     */
    protected fun isSuperclassReference(reference: Reference): Boolean {
        val language = reference.language
        return language is HasSuperClasses && reference.name.endsWith(language.superClassKeyword)
    }

    /** This function seems to resolve function pointers pointing to a [MethodDeclaration]. */
    protected fun resolveMethodFunctionPointer(
        reference: Reference,
        type: FunctionPointerType
    ): ValueDeclaration? {
        var target = scopeManager.resolveReference(reference)

        // If we didn't find anything, we create a new function or method declaration
        if (target == null) {
            // Determine the scope where we want to start our inference
            val extractedScope = scopeManager.extractScope(reference)
            var scope = extractedScope?.scope
            if (scope !is NameScope) {
                scope = null
            }

            target =
                (scope?.astNode ?: reference.translationUnit)
                    ?.startInference(ctx)
                    ?.inferFunctionDeclaration(
                        reference.name,
                        null,
                        false,
                        type.parameters,
                        type.returnType
                    )
        }

        return target
    }

    /**
     * This function handles symbol resolving for a [Reference]. After a successful lookup of the
     * symbol contained in [Reference.name], the property [Reference.refersTo] is set to the best
     * (or only) candidate.
     *
     * On a high-level, it performs the following steps:
     * - Use [ScopeManager.lookupSymbolByName] to retrieve [Declaration] candidates based on the
     *   [Reference.name]. This can either result in an "unqualified" or "qualified" lookup,
     *   depending on the name.
     * - The results of the lookup are stored in [Reference.candidates]. The purpose of this is
     *   two-fold. First, it is a good way to debug potential symbol resolution errors. Second, it
     *   is used by other functions, for example [handleCallExpression], which then picks the best
     *   viable option out of the candidates (if the reference is part of the
     *   [CallExpression.callee]).
     * - In the next step, we need to decide whether we are resolving a standalone reference (which
     *   most likely points to a [VariableDeclaration]) or if we are part of a
     *   [CallExpression.callee]. In the first case, we can directly assign [Reference.refersTo]
     *   based on the candidates (at the moment we only assign it if we have exactly one candidate).
     *   In the second case, we are finished and let [handleCallExpression] take care of the rest
     *   once the EOG reaches the appropriate [CallExpression] (which should actually be just be the
     *   next EOG node).
     */
    protected fun handleReference(currentClass: RecordDeclaration?, ref: Reference) {
        val language = ref.language

        // Ignore references to anonymous identifiers, if the language supports it (e.g., the _
        // identifier in Go)
        if (
            language is HasAnonymousIdentifier && ref.name.localName == language.anonymousIdentifier
        ) {
            return
        }

        // Ignore references to "super" if the language has super expressions, because they will be
        // handled separately in handleMemberExpression
        if (language is HasSuperClasses && ref.name.localName == language.superClassKeyword) {
            return
        }

        // Find a list of candidate symbols. Currently, this is only used the in the "next-gen" call
        // resolution, but in future this will also be used in resolving regular references.
        ref.candidates = scopeManager.lookupSymbolByNameOfNode(ref).toSet()

        // Preparation for a future without legacy call resolving. Taking the first candidate is not
        // ideal since we are running into an issue with function pointers here (see workaround
        // below).
        var wouldResolveTo = ref.candidates.singleOrNull()

        // For now, we need to ignore reference expressions that are directly embedded into call
        // expressions, because they are the "callee" property. In the future, we will use this
        // property to actually resolve the function call. However, there is a special case that
        // we want to catch already, that is if we are "calling" a reference to a variable. This
        // can be done in several languages, e.g., in C/C++ as function pointers or in Go as
        // function references. In this case, we want to resolve the declared reference expression
        // of this call expression back to its original variable declaration. In the future, we want
        // to extend this particular code to resolve all callee references to their declarations,
        // i.e., their function definitions and get rid of the separate CallResolver.
        if (ref.resolutionHelper is CallExpression) {
            // Peek into the declaration, and if it is only one declaration and a variable, we can
            // proceed normally, as we are running into the special case explained above. Otherwise,
            // we abort here (for now).
            if (wouldResolveTo !is VariableDeclaration && wouldResolveTo !is ParameterDeclaration) {
                return
            }
        }

        // Some stupid C++ workaround to use the legacy call resolver when we try to resolve targets
        // for function pointers. At least we are only invoking the legacy resolver for a very small
        // percentage of references now.
        if (wouldResolveTo is FunctionDeclaration) {
            // We need to invoke the legacy resolver, just to be sure
            var legacy = scopeManager.resolveReference(ref)

            // This is just for us to catch these differences in symbol resolving in the future. The
            // difference is pretty much only that the legacy system takes parameters of the
            // function-pointer-type into account and the new system does not (yet), because it just
            // takes the first match. This will be needed to solve in the future.
            if (legacy != wouldResolveTo) {
                log.warn(
                    "The legacy symbol resolution and the new system produced different results here. This needs to be investigated in the future. For now, we take the legacy result."
                )
                wouldResolveTo = legacy
            }
        }

        // Only consider resolving, if the language frontend did not specify a resolution. If we
        // already have populated the wouldResolveTo variable, we can re-use this instead of
        // resolving again
        var refersTo = ref.refersTo ?: wouldResolveTo

        var recordDeclType: Type? = null
        if (currentClass != null) {
            recordDeclType = currentClass.toType()
        }

        val helperType = ref.resolutionHelper?.type
        if (helperType is FunctionPointerType && refersTo == null) {
            refersTo = resolveMethodFunctionPointer(ref, helperType)
        }

        // If we did not resolve the reference up to this point, we can try to infer the declaration
        if (refersTo == null) {
            refersTo = tryVariableInference(ref)
        }

        if (refersTo != null) {
            ref.refersTo = refersTo
        } else {
            Util.warnWithFileLocation(ref, log, "Did not find a declaration for ${ref.name}")
        }
    }

    protected fun handleMemberExpression(curClass: RecordDeclaration?, current: MemberExpression) {
        // Some locals for easier smart casting
        var base = current.base
        var language = current.language

        // We need to adjust certain types of the base in case of a "super" expression, and we
        // delegate this to the language. If that is successful, we can continue with regular
        // resolving.
        if (
            language is HasSuperClasses &&
                curClass != null &&
                base is Reference &&
                base.name.endsWith(language.superClassKeyword)
        ) {
            language.handleSuperExpression(
                current,
                curClass,
                scopeManager,
            )
        }

        // For legacy reasons, method and field resolving is split between the VariableUsageResolver
        // and the CallResolver. Since we are trying to merge these two, the first step was to have
        // the callee/member field of a MemberCallExpression set to a MemberExpression. This means
        // however, that these will show up in this callback function. To not mess with legacy code
        // (yet), we are ignoring all MemberExpressions whose parents are MemberCallExpressions in
        // this function for now.
        if (current.resolutionHelper is MemberCallExpression) {
            return
        }

        if (base is Reference) {
            // The base has been resolved by now. Maybe we have some other clue about
            // this base from the type system, so we can set the declaration accordingly.
            // TODO(oxisto): It is actually not really a good approach, but it is currently
            //  needed to make the java frontend happy, but this needs to be removed at some point
            if (base.refersTo == null) {
                base.refersTo = base.type.recordDeclaration
            }
        }

        val baseType = base.type.root
        if (baseType is ObjectType) {
            current.refersTo = resolveMember(baseType, current)
        }
    }

    /**
     * This function resolves a possible overloaded -> (arrow) operator, for languages which support
     * operator overloading. The implicit call to the overloaded operator function is inserted as
     * base for the MemberExpression. This can be the case for a [MemberExpression] or
     * [MemberCallExpression]
     */
    private fun resolveOverloadedArrowOperator(ex: Expression): Type? {
        var type: Type? = null
        if (
            ex.language is HasOperatorOverloading &&
                ex is MemberExpression &&
                ex.operatorCode == "->" &&
                ex.base.type !is PointerType
        ) {
            val result = resolveOperator(ex)
            val op = result?.bestViable?.singleOrNull()
            if (result?.success == SUCCESSFUL && op is OperatorDeclaration) {
                type = op.returnTypes.singleOrNull()?.root ?: unknownType()

                // We need to insert a new operator call expression in between
                val call = operatorCallFromDeclaration(op, ex)

                // Make the call our new base
                ex.base = call
            }
        }
        return type
    }

    protected fun resolveMember(
        containingClass: ObjectType,
        reference: Reference
    ): ValueDeclaration? {
        if (isSuperclassReference(reference)) {
            // if we have a "super" on the member side, this is a member call. We need to resolve
            // this in the call resolver instead
            return null
        }
        var member: ValueDeclaration? = null
        var type: Type = containingClass

        // Handle a possible overloaded operator->
        type = resolveOverloadedArrowOperator(reference) ?: type

        val record = type.recordDeclaration
        if (record != null) {
            // TODO(oxisto): This should use symbols rather than the AST fields
            member =
                record.fields
                    .filter { it.name.lastPartsMatch(reference.name) }
                    .map { it.definition }
                    .firstOrNull()
        }
        if (member == null) {
            member =
                type.superTypes
                    .flatMap { it.recordDeclaration?.fields ?: listOf() }
                    .filter { it.name.localName == reference.name.localName }
                    .map { it.definition }
                    .firstOrNull()
        }

        if (member == null && record is EnumDeclaration) {
            member = record.entries[reference.name.localName]
        }

        if (member == null) {
            member = tryFieldInference(reference, containingClass)
        }

        return member
    }

    protected fun handle(node: Node?, currClass: RecordDeclaration?) {
        when (node) {
            is MemberExpression -> handleMemberExpression(currClass, node)
            is Reference -> handleReference(currClass, node)
            is ConstructExpression -> handleConstructExpression(node)
            is CallExpression -> handleCallExpression(node)
            is HasOverloadedOperation -> handleOverloadedOperator(node)
        }
    }

    protected fun handleCallExpression(call: CallExpression) {
        // Some local variables for easier smart casting
        val callee = call.callee
        val language = call.language

        // Handle a possible overloaded operator->
        resolveOverloadedArrowOperator(callee)

        // Dynamic function invokes (such as function pointers) are handled by extra pass, so we are
        // not resolving them here.
        //
        // We have a dynamic invoke in two cases:
        // a) our calleee is not a reference
        // b) our reference refers to a variable rather than a function
        if (
            callee !is Reference ||
                callee.refersTo is VariableDeclaration ||
                callee.refersTo is ParameterDeclaration
        ) {
            return
        }

        // If this is a template call and our language supports templates, we need to directly
        // handle this with the template system. This will also take care of inference and
        // everything. This will stay in this way until we completely redesign the template system.
        if (call.instantiatesTemplate() && language is HasTemplates) {
            val (ok, candidates) =
                language.handleTemplateFunctionCalls(
                    scopeManager.currentRecord,
                    call,
                    true,
                    ctx,
                    call.translationUnit,
                    false
                )
            if (ok) {
                call.invokes = candidates.toMutableList()
                return
            }
        }

        // We are moving towards a new approach to call resolution. However, we cannot use this for
        // all nodes yet, so we need to use legacy resolution for some
        val isSpecialCXXCase =
            call.language.isCPP && scopeManager.currentRecord != null && !callee.name.isQualified()
        val useLegacyResolution =
            when {
                isSpecialCXXCase -> true
                call is MemberCallExpression -> true
                else -> {
                    false
                }
            }

        // Retrieve a list of candidates; either from the "legacy" system or directly from our
        // callee
        var candidates =
            if (useLegacyResolution) {
                val (possibleTypes, _) = getPossibleContainingTypes(call)
                resolveMemberByName(callee.name.localName, possibleTypes).toSet()
            } else {
                callee.candidates
            }

        // There seems to be one more special case and that is a regular function within a record.
        // This could either be a member call with an omitted "this" or a regular call. The problem
        // is that the legacy system can now only resolve member calls but not regular calls
        // (anymore). So if we have this special case and the legacy system does not return any
        // candidates, we need to switch to the new system.
        if (isSpecialCXXCase && candidates.isEmpty()) {
            candidates = callee.candidates
        }

        val result = resolveWithArguments(candidates, call.arguments, call)
        when (result.success) {
            PROBLEMATIC -> {
                log.error(
                    "Resolution of ${call.name} returned an problematic result and we cannot decide correctly, the invokes edge will contain all possible viable functions"
                )
                call.invokes = result.bestViable.toMutableList()
            }
            AMBIGUOUS -> {
                log.warn(
                    "Resolution of ${call.name} returned an ambiguous result and we cannot decide correctly, the invokes edge will contain the the ambiguous functions"
                )
                call.invokes = result.bestViable.toMutableList()
            }
            SUCCESSFUL -> {
                call.invokes = result.bestViable.toMutableList()
            }
            UNRESOLVED -> {
                call.invokes = tryFunctionInference(call, result).toMutableList()
            }
        }

        // We also set the callee's refersTo
        callee.refersTo = call.invokes.firstOrNull()
    }

    /**
     * This function tries to resolve a set of [candidates] (e.g. coming from a
     * [CallExpression.callee]) into the best matching [FunctionDeclaration] (or multiple functions,
     * if applicable) based on the supplied [arguments]. The result is returned in the form of a
     * [CallResolutionResult] which holds detail information about intermediate results as well as
     * the kind of success the resolution had.
     *
     * The [source] expression specifies the node in the graph that triggered this resolution. This
     * is most likely a [CallExpression], but could be other node as well. It is also the source of
     * the scope and language used in the resolution.
     */
    private fun resolveWithArguments(
        candidates: Set<Declaration>,
        arguments: List<Expression>,
        source: Expression,
    ): CallResolutionResult {
        val result =
            CallResolutionResult(
                source,
                arguments,
                candidates.filterIsInstance<FunctionDeclaration>().toSet(),
                setOf(),
                mapOf(),
                setOf(),
                CallResolutionResult.SuccessKind.UNRESOLVED,
                source.scope,
            )
        val language = source.language

        if (language == null) {
            result.success = CallResolutionResult.SuccessKind.PROBLEMATIC
            return result
        }

        // Set the start scope. This can either be the call's scope or a scope specified in an FQN
        val extractedScope = ctx.scopeManager.extractScope(source, source.scope)
        val scope = extractedScope?.scope
        result.actualStartScope = scope ?: source.scope

        // If the function does not allow function overloading, and we have multiple candidate
        // symbols, the result is "problematic"
        if (source.language !is HasFunctionOverloading && result.candidateFunctions.size > 1) {
            result.success = PROBLEMATIC
        }

        // Filter functions that match the signature of our call, either directly or with casts;
        // those functions are "viable". Take default arguments into account if the language has
        // them.
        result.signatureResults =
            result.candidateFunctions
                .map {
                    Pair(
                        it,
                        it.matchesSignature(
                            arguments.map(Expression::type),
                            arguments,
                            source.language is HasDefaultArguments,
                        )
                    )
                }
                .filter { it.second is SignatureMatches }
                .associate { it }
        result.viableFunctions = result.signatureResults.keys

        // If we have a "problematic" result, we can stop here. In this case we cannot really
        // determine anything more.
        if (result.success == CallResolutionResult.SuccessKind.PROBLEMATIC) {
            result.bestViable = result.viableFunctions
            return result
        }

        // Otherwise, give the language a chance to narrow down the result (ideally to one) and set
        // the success kind.
        val pair = language.bestViableResolution(result)
        result.bestViable = pair.first
        result.success = pair.second

        return result
    }

    protected fun resolveMemberByName(
        symbol: String,
        possibleContainingTypes: Set<Type>
    ): Set<Declaration> {
        var candidates = mutableSetOf<Declaration>()
        val records = possibleContainingTypes.mapNotNull { it.root.recordDeclaration }.toSet()
        for (record in records) {
            candidates.addAll(
                ctx.scopeManager.lookupSymbolByName(record.name.fqn(symbol), record.language)
            )
        }

        // Find invokes by supertypes
        if (candidates.isEmpty() && symbol.isNotEmpty()) {
            val records = possibleContainingTypes.mapNotNull { it.root.recordDeclaration }.toSet()
            candidates = getInvocationCandidatesFromParents(symbol, records).toMutableSet()
        }

        // Add overridden invokes
        candidates.addAll(
            candidates
                .filterIsInstance<FunctionDeclaration>()
                .map { getOverridingCandidates(possibleContainingTypes, it) }
                .flatten()
        )

        return candidates
    }

    protected fun handleConstructExpression(constructExpression: ConstructExpression) {
        if (constructExpression.instantiates != null && constructExpression.constructor != null)
            return
        val recordDeclaration = constructExpression.type.root.recordDeclaration
        constructExpression.instantiates = recordDeclaration
        for (template in templateList) {
            if (
                template is RecordTemplateDeclaration &&
                    recordDeclaration != null &&
                    recordDeclaration in template.realizations &&
                    (constructExpression.templateArguments.size <= template.parameters.size)
            ) {
                val defaultDifference =
                    template.parameters.size - constructExpression.templateArguments.size
                if (defaultDifference <= template.parameterDefaults.size) {
                    // Check if predefined template value is used as default in next value
                    addRecursiveDefaultTemplateArgs(constructExpression, template, scopeManager)

                    // Add missing defaults
                    val missingNewParams: List<Node?> =
                        template.parameterDefaults.subList(
                            constructExpression.templateArguments.size,
                            template.parameterDefaults.size
                        )
                    for (missingParam in missingNewParams) {
                        if (missingParam != null) {
                            constructExpression.addTemplateParameter(
                                missingParam,
                                TemplateDeclaration.TemplateInitialization.DEFAULT
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

    private fun handleOverloadedOperator(op: HasOverloadedOperation) {
        val result = resolveOperator(op)
        val decl = result?.bestViable?.singleOrNull() ?: return

        // If the result was successful, we can replace the node
        if (result.success == SUCCESSFUL && decl is OperatorDeclaration && op is Expression) {
            val call = operatorCallFromDeclaration(decl, op)
            walker.replace(op.astParent, op, call)
        }
    }

    private fun resolveOperator(op: HasOverloadedOperation): CallResolutionResult? {
        val language = op.language
        val base = op.operatorBase
        if (language !is HasOperatorOverloading || language.isPrimitive(base.type)) {
            return null
        }

        val symbol = language.overloadedOperatorNames[Pair(op::class, op.operatorCode)]
        if (symbol == null) {
            log.warn(
                "Could not resolve operator overloading for unknown operatorCode ${op.operatorCode}"
            )
            return null
        }

        val possibleTypes = mutableSetOf<Type>()
        possibleTypes.add(op.operatorBase.type)
        possibleTypes.addAll(op.operatorBase.assignedTypes)

        val candidates =
            resolveMemberByName(symbol, possibleTypes)
                .filterIsInstance<OperatorDeclaration>()
                .toSet()

        return resolveWithArguments(candidates, op.operatorArguments, op as Expression)
    }

    protected fun getInvocationCandidatesFromParents(
        name: Symbol,
        possibleTypes: Set<RecordDeclaration>,
    ): List<Declaration> {
        val workingPossibleTypes = mutableSetOf(*possibleTypes.toTypedArray())
        return if (possibleTypes.isEmpty()) {
            listOf()
        } else {
            val firstLevelCandidates =
                possibleTypes
                    .map { record ->
                        scopeManager.lookupSymbolByName(record.name.fqn(name), record.language)
                    }
                    .flatten()

            // C++ does not allow overloading at different hierarchy levels. If we find a
            // FunctionDeclaration with the same name as the function in the CallExpression we have
            // to stop the search in the parent even if the FunctionDeclaration does not match with
            // the signature of the CallExpression
            // TODO: move this to refineMethodResolution of CXXLanguage
            if (possibleTypes.firstOrNull()?.language.isCPP) { // TODO: Needs a special trait?
                workingPossibleTypes.removeIf { recordDeclaration ->
                    !shouldContinueSearchInParent(recordDeclaration, name)
                }
            }
            firstLevelCandidates.ifEmpty {
                workingPossibleTypes
                    .map { it.superTypeDeclarations }
                    .map { getInvocationCandidatesFromParents(name, it) }
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
        declaration: FunctionDeclaration
    ): Set<FunctionDeclaration> {
        return declaration.overriddenBy
            .filter { f ->
                f is MethodDeclaration && f.recordDeclaration?.toType() in possibleSubTypes
            }
            .toSet()
    }

    /**
     * @param constructExpression we want to find an invocation target for
     * @param recordDeclaration associated with the Object the ConstructExpression constructs
     * @return a ConstructDeclaration that is an invocation of the given ConstructExpression. If
     *   there is no valid ConstructDeclaration we will create an implicit ConstructDeclaration that
     *   matches the ConstructExpression.
     */
    protected fun getConstructorDeclaration(
        constructExpression: ConstructExpression,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration? {
        val signature = constructExpression.signature
        val constructorCandidate =
            recordDeclaration.constructors.firstOrNull {
                it.matchesSignature(
                    signature,
                    constructExpression.arguments,
                    constructExpression.language is HasDefaultArguments,
                ) != IncompatibleSignature
            }

        return constructorCandidate
            ?: recordDeclaration
                .startInference(ctx)
                ?.createInferredConstructor(constructExpression.signature)
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(SymbolResolver::class.java)

        /**
         * Adds implicit duplicates of the TemplateParams to the implicit ConstructExpression
         *
         * @param templateParams of the [VariableDeclaration]/[NewExpression]
         * @param constructExpression duplicate TemplateParameters (implicit) to preserve AST, as
         *   [ConstructExpression] uses AST as well as the [VariableDeclaration]/[NewExpression]
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

/**
 * Returns a set of types in which the callee of our [call] could reside in. More concretely, it
 * returns a [Pair], where the first element is the set of types and the second is our best guess.
 */
internal fun Pass<*>.getPossibleContainingTypes(call: CallExpression): Pair<Set<Type>, Type?> {
    val possibleTypes = mutableSetOf<Type>()
    var bestGuess: Type? = null
    if (call is MemberCallExpression) {
        call.base?.let { base ->
            bestGuess = base.type
            possibleTypes.add(base.type)
            possibleTypes.addAll(base.assignedTypes)
        }
    } else if (call.language is HasImplicitReceiver) {
        // This could be a member call with an implicit receiver, so let's add the current class
        // to the possible list
        scopeManager.currentRecord?.toType()?.let {
            bestGuess = it
            possibleTypes.add(it)
        }
    }

    return Pair(possibleTypes, bestGuess)
}
