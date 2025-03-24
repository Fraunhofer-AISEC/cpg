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
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
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
import de.fraunhofer.aisec.cpg.passes.inference.tryFunctionInferenceFromFunctionPointer
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

    /** Configuration for the [SymbolResolver]. */
    class Configuration(
        /** If set to true, the resolver will skip unreachable EOG edges. */
        val skipUnreachableEOG: Boolean = false,

        /**
         * If set to true, the resolver will ignore [Declaration] nodes that are on EOG paths that
         * are [EvaluationOrder.unreachable].
         */
        val ignoreUnreachableDeclarations: Boolean = false,
    ) : PassConfiguration()

    protected lateinit var walker: ScopedWalker

    protected val templateList = mutableListOf<TemplateDeclaration>()

    /** Our configuration. */
    var passConfig = passConfig<Configuration>()

    /**
     * If [Configuration.ignoreUnreachableDeclarations] is enabled, this predicate will filter
     * candidates whether they are [EvaluationOrder.unreachable]. If the declaration has ONLY
     * unreachable incoming EOG edges, we ignore them.
     */
    private val eogPredicate: ((Declaration) -> Boolean)? =
        if (passConfig?.ignoreUnreachableDeclarations == true) {
            { declaration ->
                if (declaration is FunctionDeclaration) {
                        declaration.astParent
                    } else {
                        declaration
                    }
                    ?.prevEOGEdges
                    ?.all { edge -> !edge.unreachable } == true
            }
        } else {
            null
        }

    override fun accept(component: Component) {
        ctx.currentComponent = component
        walker = ScopedWalker(scopeManager)

        cacheTemplates(component)

        walker.strategy =
            if (passConfig?.skipUnreachableEOG == true) {
                Strategy::REACHABLE_EOG_FORWARD
            } else {
                Strategy::EOG_FORWARD
            }
        walker.clearCallbacks()
        walker.registerHandler(this::handle)

        // Resolve symbols in our translation units in the order depending on their import
        // dependencies
        for (tu in (Strategy::TRANSLATION_UNITS_LEAST_IMPORTS)(component)) {
            log.debug("Resolving symbols of translation unit {}", tu.name)

            // Gather all resolution EOG starters; and make sure they really do not have a
            // predecessor, otherwise we might analyze a node multiple times
            val nodes = tu.allEOGStarters.filter { it.prevEOGEdges.isEmpty() }

            for (node in nodes) {
                walker.iterate(node)
            }
        }
    }

    override fun cleanup() {
        templateList.clear()
    }

    /** This function caches all [TemplateDeclaration]s into [templateList]. */
    private fun cacheTemplates(component: Component) {
        walker.registerHandler { node ->
            if (node is TemplateDeclaration) {
                templateList.add(node)
            }
        }
        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
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
    protected open fun handleReference(ref: Reference) {
        val language = ref.language
        val helperType = ref.resolutionHelper?.type
        val record = scopeManager.currentRecord

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

        // If our resolution helper indicates that this reference is the target of a variable with a
        // function pointer, we need to take the (return) type arguments of the function pointer
        // into consideration
        val predicate: ((Declaration) -> Boolean)? =
            if (helperType is FunctionPointerType) {
                { declaration ->
                    if (declaration is FunctionDeclaration) {
                        declaration.returnTypes == listOf(helperType.returnType) &&
                            declaration.matchesSignature(helperType.parameters) !=
                                IncompatibleSignature
                    } else {
                        false
                    } && eogPredicate?.invoke(declaration) != false
                }
            } else {
                eogPredicate
            }

        // Find a list of candidate symbols. In most cases, we can just perform a lookup by name
        // which either performs an unqualified lookup beginning from the current scope "up-wards",
        // or a qualified lookup starting from the scope specified in the name.
        var candidates = scopeManager.lookupSymbolByNodeName(ref, predicate = predicate).toSet()

        // But we have to consider one special case: For languages, that support implicit receivers,
        // this reference might be a member access of either the current class or a parent class.
        // While a regular lookup would only consider the current scope, we have to consider the
        // parent classes as well, which is exactly what resolveMemberByName does. We could probably
        // get around this if we would include the symbols of the parent class somehow in the child
        // class as a sort of "sibling" scope, but we do not have that (yet).
        if (
            language is HasImplicitReceiver &&
                candidates.isEmpty() &&
                !ref.name.isQualified() &&
                record != null
        ) {
            candidates = resolveMemberByName(ref.name.localName, setOf(record.toType())).toSet()
        }

        // Store the candidates in the reference
        ref.candidates = candidates

        // We need to choose the best viable candidate out of the ones we have for our reference.
        // Hopefully we have only one, but there might be instances where more than one is a valid
        // candidate. We let the language have a chance at overriding the default behaviour (which
        // takes only a single one).
        val wouldResolveTo = language.bestViableReferenceCandidate(ref)

        // For now, we still separate the resolving of simple variable references from call
        // resolving. Therefore, we need to stop here if we are the callee of a call and continue in
        // handleCallExpression.
        //
        // However, there is a special case that we want to catch, that is if we are "calling" a
        // reference to a variable (or parameter). This can be done in several languages, e.g., in
        // C/C++ as function pointers or in Go as function references. In this case, we want to
        // resolve the reference of this call expression back to its original declaration, and then
        // we later continue in the DynamicInvokeResolver, which sets the invokes edge.
        if (
            ref.resolutionHelper is CallExpression &&
                (wouldResolveTo !is VariableDeclaration && wouldResolveTo !is ParameterDeclaration)
        ) {
            return
        }

        // Only consider resolving, if the language frontend did not specify a resolution. If we
        // already have populated the wouldResolveTo variable, we can re-use this instead of
        // resolving again
        var refersTo = ref.refersTo ?: wouldResolveTo

        // If we did not resolve the reference up to this point, we can try to infer the declaration
        if (refersTo == null) {
            // If it's a function pointer, we can try to infer a function
            refersTo =
                if (helperType is FunctionPointerType) {
                    tryFunctionInferenceFromFunctionPointer(ref, helperType)
                } else {
                    // Otherwise, we can try to infer a variable
                    tryVariableInference(ref)
                }
        }

        if (refersTo != null) {
            ref.refersTo = refersTo
        } else {
            Util.warnWithFileLocation(ref, log, "Did not find a declaration for ${ref.name}")
        }
    }

    /**
     * This function handles resolving of a [MemberExpression] in the [ScopeManager.currentRecord].
     * This works similar to [handleReference]. First, we set the [MemberExpression.candidates]
     * based on [resolveMemberByName], which internally calls [ScopeManager.lookupSymbolByName]
     * based on the current class and its parent classes. Then, if we resolve a
     * [MemberCallExpression], we abort (and later pick up resolving in [handleCallExpression]). In
     * case of a field access, we set the [MemberExpression.refersTo] based on
     * [Language.bestViableReferenceCandidate].
     */
    protected open fun handleMemberExpression(current: MemberExpression) {
        // Some locals for easier smart casting
        val base = current.base
        val language = current.language
        val record = scopeManager.currentRecord

        // We need to adjust certain types of the base in case of a "super" expression, and we
        // delegate this to the language. If that is successful, we can continue with regular
        // resolving.
        if (
            language is HasSuperClasses &&
                record != null &&
                base is Reference &&
                base.name.localName == language.superClassKeyword
        ) {
            with(language) { handleSuperExpression(current, record) }
        }

        // Handle a possible overloaded operator->. If we find an overloaded operator, this inserts
        // an additional operator expression in-between the existing member expression and the base
        // and also affects the base type.
        val baseType = resolveOverloadedArrowOperator(current) ?: base.type.root

        // Find candidates based on possible base types
        val (possibleTypes, _) = getPossibleContainingTypes(current)
        current.candidates = resolveMemberByName(current.name.localName, possibleTypes).toSet()

        // For legacy reasons, resolving of simple variable references (including fields) is
        // separated from call resolving. Therefore, we need to stop here if we are the callee of a
        // member call and continue in handleCallExpression. But we can already make
        // handleCallExpression a bit cleaner, if we set the candidates here, similar to what we do
        // in handleReference.
        val helper = current.resolutionHelper
        if (helper is MemberCallExpression) {
            return
        }

        // We need to choose the best viable candidate out of the ones we have for our reference.
        // Hopefully we have only one, but there might be instances where more than one is a valid
        // candidate. We let the language have a chance at overriding the default behaviour (which
        // takes only a single one).
        val wouldResolveTo = language.bestViableReferenceCandidate(current)

        var refersTo = current.refersTo ?: wouldResolveTo

        if (refersTo == null && baseType is ObjectType) {
            refersTo = tryFieldInference(current, baseType)
        }

        current.refersTo = refersTo
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

    /**
     * The central entry-point for all symbol-resolving. It dispatches the handling of the node to
     * the appropriate function based on the node type.
     */
    protected open fun handle(node: Node?) {
        when (node) {
            is MemberExpression -> handleMemberExpression(node)
            is Reference -> handleReference(node)
            is ConstructExpression -> handleConstructExpression(node)
            is CallExpression -> handleCallExpression(node)
            is HasOverloadedOperation -> handleOverloadedOperator(node)
        }
    }

    /**
     * This function handles the resolution of a [CallExpression] based on a list of candidates. The
     * candidates are taken from [CallExpression.callee] which are set either in [handleReference]
     * or [handleMemberExpression], depending on the type.
     *
     * In any case, the candidates are then resolved with the arguments of the call expression using
     * [resolveWithArguments]. The result of this resolution is stored in [CallExpression.invokes]
     * and depending on [CallResolutionResult.SuccessKind] are warning is emitted if resolution was
     * erroneous or ambiguous. Furthermore, the [CallExpression.callee]'s [Reference.refersTo] is
     * also set.
     *
     * If the resolution was unsuccessful, we try to infer the function based on the information
     * provided in the [CallResolutionResult] and the [CallExpression]. This is done in
     * [tryFunctionInference].
     *
     * @param call The [CallExpression] to resolve.
     */
    protected open fun handleCallExpression(call: CallExpression) {
        // Some local variables for easier smart casting
        val callee = call.callee
        val language = call.language

        // If the base type is unknown, we cannot resolve the call
        if (callee is MemberExpression && callee.base.type is UnknownType) {
            Util.warnWithFileLocation(
                call,
                log,
                "Cannot resolve call to ${callee.name} because the base type is unknown",
            )
            return
        }

        // Dynamic function invokes (such as function pointers) are handled by an extra pass, so we
        // are
        // not resolving them here.
        //
        // We have a dynamic invoke in two cases:
        // a) our callee is not a reference
        // b) our reference already refers to a variable rather than a function
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
                    false,
                )
            if (ok) {
                call.invokes = candidates.toMutableList()
                return
            }
        }

        // Try to resolve the best viable function based on the candidates and the arguments
        val result = resolveWithArguments(callee.candidates, call.arguments, call)
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
                UNRESOLVED,
                source.scope,
            )
        val language = source.language

        // Set the start scope. This can either be the call's scope or a scope specified in an FQN.
        // If our base is a dynamic or unknown type, we can skip the scope extraction because it
        // will always fail
        val extractedScope =
            if (
                source is MemberCallExpression &&
                    (source.base?.type is DynamicType ||
                        source.base?.type is UnknownType ||
                        source.base?.type is AutoType)
            ) {
                ScopeManager.ScopeExtraction(null, Name(""))
            } else {
                ctx.scopeManager.extractScope(source, language, source.scope)
            }

        // If we could not extract the scope (even though one was specified), we can only return an
        // empty result
        if (extractedScope == null) {
            return result
        }

        val scope = extractedScope.scope
        result.actualStartScope = scope ?: source.scope

        // If there are no candidates, we can stop here
        if (candidates.isEmpty()) {
            return result
        }

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
                        ),
                    )
                }
                .filter { it.second is SignatureMatches }
                .associate { it }
        result.viableFunctions = result.signatureResults.keys

        // If we have a "problematic" result, we can stop here. In this case we cannot really
        // determine anything more.
        if (result.success == PROBLEMATIC) {
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

    private fun resolveMemberByName(
        symbol: String,
        possibleContainingTypes: Set<Type>,
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

    protected open fun handleConstructExpression(constructExpression: ConstructExpression) {
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
                    addRecursiveDefaultTemplateArgs(constructExpression, template)

                    // Add missing defaults
                    val missingNewParams: List<Node?> =
                        template.parameterDefaults.subList(
                            constructExpression.templateArguments.size,
                            template.parameterDefaults.size,
                        )
                    for (missingParam in missingNewParams) {
                        if (missingParam != null) {
                            constructExpression.addTemplateParameter(
                                missingParam,
                                TemplateDeclaration.TemplateInitialization.DEFAULT,
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

    /**
     * This function handles all nodes that have the [HasOverloadedOperation] trait. It tries to
     * resolve the overloaded operator and replace the node with the resolved operator expression.
     *
     * Which overloads are possible, is depending on whether the language implements
     * [HasOperatorOverloading] and can be specified in
     * [HasOperatorOverloading.overloadedOperatorNames].
     *
     * Internally, it takes the result of [resolveOperator] and if successful, replaces the node
     * with the resolved [OperatorCallExpression].
     */
    protected open fun handleOverloadedOperator(op: HasOverloadedOperation) {
        val result = resolveOperator(op)
        val decl = result?.bestViable?.singleOrNull() ?: return

        // If the result was successful, we can replace the node
        if (result.success == SUCCESSFUL && decl is OperatorDeclaration && op is Expression) {
            val call = operatorCallFromDeclaration(decl, op)
            walker.replace(op.astParent, op, call)
        }
    }

    /**
     * This function tries to resolve an overloaded operator based on the
     * [HasOverloadedOperation.operatorCode] of the [op] (if the [HasOverloadedOperation.language]
     * allows it). It first lookups the corresponding symbol in the
     * [HasOperatorOverloading.overloadedOperatorNames] of the language, for example `add` for a `+`
     * operator. In then tries to find the matching method candidates in the base class of the [op]
     * (using [resolveMemberByName]) and returns the result of the resolution. The base depends on
     * the individual operator / expression and is specified in
     * [HasOverloadedOperation.operatorBase].
     *
     * Finally, the candidates are resolved with the arguments of the operator expression using
     * [resolveWithArguments].
     */
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

    private fun getInvocationCandidatesFromParents(
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
                workingPossibleTypes.flatMap {
                    getInvocationCandidatesFromParents(
                        name,
                        it.superTypeDeclarations.filter { it !in possibleTypes }.toSet(),
                    )
                }
            }
        }
    }

    protected val Language<*>?.isCPP: Boolean
        get() {
            return this != null && this::class.simpleName == "CPPLanguage"
        }

    private fun getOverridingCandidates(
        possibleSubTypes: Set<Type>,
        declaration: FunctionDeclaration,
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
    private fun getConstructorDeclaration(
        constructExpression: ConstructExpression,
        recordDeclaration: RecordDeclaration,
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
            constructExpression: ConstructExpression,
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
 * Returns a set of types in which the [CallExpression.callee] (which is a [Reference]) could reside
 * in. More concretely, it returns a [Pair], where the first element is the set of types and the
 * second is our best guess.
 */
internal fun Pass<*>.getPossibleContainingTypes(ref: Reference): Pair<Set<Type>, Type?> {
    val possibleTypes = mutableSetOf<Type>()
    var bestGuess: Type? = null
    if (ref is MemberExpression) {
        bestGuess = ref.base.type
        possibleTypes.add(ref.base.type)
        possibleTypes.addAll(ref.base.assignedTypes)
    } else if (ref.language is HasImplicitReceiver) {
        // This could be a member call with an implicit receiver, so let's add the current class
        // to the possible list
        scopeManager.currentRecord?.toType()?.let {
            bestGuess = it
            possibleTypes.add(it)
        }
    }

    return Pair(possibleTypes, bestGuess)
}
