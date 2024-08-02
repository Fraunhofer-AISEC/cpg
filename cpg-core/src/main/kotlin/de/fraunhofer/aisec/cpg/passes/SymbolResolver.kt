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
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.inference.Inference.TypeInferenceObserver
import de.fraunhofer.aisec.cpg.passes.inference.inferFunction
import de.fraunhofer.aisec.cpg.passes.inference.inferMethod
import de.fraunhofer.aisec.cpg.passes.inference.startInference
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
    lateinit var currentTU: TranslationUnitDeclaration

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

        for (tu in component.translationUnits) {
            currentTU = tu
            // Gather all resolution EOG starters; and make sure they really do not have a
            // predecessor, otherwise we might analyze a node multiple times
            val nodes = tu.allEOGStarters.filter { it.prevEOG.isEmpty() }

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
            var (scope, _) = scopeManager.extractScope(reference)
            if (scope !is NameScope) {
                scope = null
            }

            target =
                (scope?.astNode ?: currentTU)
                    .startInference(ctx)
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

    protected fun handleReference(currentClass: RecordDeclaration?, current: Node?) {
        val language = current?.language

        if (current !is Reference || current is MemberExpression) return

        // Ignore references to anonymous identifiers, if the language supports it (e.g., the _
        // identifier in Go)
        if (
            language is HasAnonymousIdentifier &&
                current.name.localName == language.anonymousIdentifier
        ) {
            return
        }

        // Find a list of candidate symbols. Currently, this is only used the in the "next-gen" call
        // resolution, but in future this will also be used in resolving regular references.
        current.candidates = scopeManager.findSymbols(current.name, current.location).toSet()

        // For now, we need to ignore reference expressions that are directly embedded into call
        // expressions, because they are the "callee" property. In the future, we will use this
        // property to actually resolve the function call. However, there is a special case that
        // we want to catch already, that is if we are "calling" a reference to a variable. This
        // can be done in several languages, e.g., in C/C++ as function pointers or in Go as
        // function references. In this case, we want to resolve the declared reference expression
        // of this call expression back to its original variable declaration. In the future, we want
        // to extend this particular code to resolve all callee references to their declarations,
        // i.e., their function definitions and get rid of the separate CallResolver.
        var wouldResolveTo: Declaration? = null
        if (current.resolutionHelper is CallExpression) {
            // Peek into the declaration, and if it is only one declaration and a variable, we can
            // proceed normally, as we are running into the special case explained above. Otherwise,
            // we abort here (for now).
            wouldResolveTo = current.candidates.singleOrNull()
            if (wouldResolveTo !is VariableDeclaration && wouldResolveTo !is ParameterDeclaration) {
                return
            }
        }

        // Only consider resolving, if the language frontend did not specify a resolution. If we
        // already have populated the wouldResolveTo variable, we can re-use this instead of
        // resolving again
        var refersTo = current.refersTo ?: wouldResolveTo ?: scopeManager.resolveReference(current)

        var recordDeclType: Type? = null
        if (currentClass != null) {
            recordDeclType = currentClass.toType()
        }

        val helperType = current.resolutionHelper?.type
        if (helperType is FunctionPointerType && refersTo == null) {
            refersTo = resolveMethodFunctionPointer(current, helperType)
        }

        // only add new nodes for non-static unknown
        if (
            refersTo == null &&
                !current.isStaticAccess &&
                recordDeclType != null &&
                recordDeclType.recordDeclaration != null
        ) {
            // Maybe we are referring to a field instead of a local var
            val field = resolveMember(recordDeclType, current)
            if (field != null) {
                refersTo = field
            }
        }

        // TODO: we need to do proper scoping (and merge it with the code above), but for now
        //  this just enables CXX static fields
        if (refersTo == null && language != null && current.name.isQualified()) {
            recordDeclType = getEnclosingTypeOf(current)
            val field = resolveMember(recordDeclType, current)
            if (field != null) {
                refersTo = field
            }
        }

        if (refersTo == null) {
            // We can try to infer a possible global variable, if the language supports this
            refersTo = tryGlobalVariableInference(current)
        }

        if (refersTo != null) {
            current.refersTo = refersTo
        } else {
            Util.warnWithFileLocation(
                current,
                log,
                "Did not find a declaration for ${current.name}"
            )
        }
    }

    /**
     * Tries to infer a global variable from an unresolved [Reference]. This will return `null`, if
     * inference was not possible, or if it was turned off in the [InferenceConfiguration].
     */
    private fun tryGlobalVariableInference(ref: Reference): Declaration? {
        if (ref.language !is HasGlobalVariables) {
            return null
        }

        // For now, we only infer globals at the top-most global level, i.e., no globals in
        // namespaces
        if (ref.name.isQualified()) {
            return null
        }

        // Forward this to our inference system. This will also check whether and how inference is
        // configured.
        return scopeManager.globalScope?.astNode?.startInference(ctx)?.inferVariableDeclaration(ref)
    }

    /**
     * We get the type of the "scope" this node is in. (e.g. for a field, we drop the field's name
     * and have the class)
     */
    protected fun getEnclosingTypeOf(current: Node): Type {
        val language = current.language

        return if (language != null && language.namespaceDelimiter.isNotEmpty()) {
            val parentName = (current.name.parent ?: current.name).toString()
            var type = current.objectType(parentName)
            TypeResolver.resolveType(type)
            type
        } else {
            current.unknownType()
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
        current.refersTo = resolveMember(baseType, current)
    }

    protected fun resolveMember(containingClass: Type, reference: Reference): ValueDeclaration? {
        if (isSuperclassReference(reference)) {
            // if we have a "super" on the member side, this is a member call. We need to resolve
            // this in the call resolver instead
            return null
        }
        var member: ValueDeclaration? = null
        val record = containingClass.recordDeclaration
        if (record != null) {
            member =
                record.fields
                    .filter { it.name.lastPartsMatch(reference.name) }
                    .map { it.definition }
                    .firstOrNull()
        }
        if (member == null) {
            member =
                (containingClass.recordDeclaration?.superTypeDeclarations?.flatMap { it.fields }
                        ?: listOf())
                    .filter { it.name.localName == reference.name.localName }
                    .map { it.definition }
                    .firstOrNull()
        }
        if (member == null && record is EnumDeclaration) {
            member = record.entries[reference.name.localName]
        }

        if (member != null) {
            return member
        }

        // This is a little bit of a workaround, but at least this makes sure we are not inferring a
        // record, where a namespace already exist
        val (scope, _) = scopeManager.extractScope(reference, null)
        return if (scope == null) {
            handleUnknownField(containingClass, reference)
        } else {
            // Workaround needed for Java. If we already have a record scope, use the "old"
            // inference function
            when (scope) {
                is RecordScope -> handleUnknownField(containingClass, reference)
                is NameScope -> {
                    log.warn(
                        "We should infer a namespace variable ${reference.name} at this point, but this is not yet implemented."
                    )
                    null
                }
                else -> {
                    log.warn(
                        "We should infer a variable ${reference.name} in ${scope}, but this is not yet implemented."
                    )
                    null
                }
            }
        }
    }

    // TODO(oxisto): Move to inference class
    protected fun handleUnknownField(base: Type, ref: Reference): FieldDeclaration? {
        val name = ref.name

        // unwrap a potential pointer-type
        if (base is PointerType) {
            return handleUnknownField(base.elementType, ref)
        }

        var record = base.recordDeclaration
        if (record == null) {
            // We access an unknown field of an unknown record. so we need to handle that along the
            // way as well
            record = ctx.tryRecordInference(base, locationHint = ref)
        }

        if (record == null) {
            log.error(
                "There is no matching record in the record map. Can't identify which field is used."
            )
            return null
        }

        val target = record.fields.firstOrNull { it.name.lastPartsMatch(name) }

        return if (target != null) {
            target
        } else {
            val declaration =
                newFieldDeclaration(
                    name.localName,
                    // we will set the type later through the type inference observer
                    record.unknownType(),
                    listOf(),
                    null,
                    false,
                )
            record.addField(declaration)
            declaration.language = record.language
            declaration.isInferred = true

            // We might be able to resolve the type later (or better), if a type is
            // assigned to our reference later
            ref.registerTypeObserver(TypeInferenceObserver(declaration))

            declaration
        }
    }

    protected fun handle(node: Node?, currClass: RecordDeclaration?) {
        when (node) {
            is MemberExpression -> handleMemberExpression(currClass, node)
            is Reference -> handleReference(currClass, node)
            is ConstructExpression -> handleConstructExpression(node)
            is CallExpression -> handleCallExpression(node)
        }
    }

    protected fun handleCallExpression(call: CallExpression) {
        // Some local variables for easier smart casting
        val callee = call.callee
        val language = call.language

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
                    currentTU,
                    false
                )
            if (ok) {
                call.invokes = candidates
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
                call.invokes = result.bestViable.toList()
            }
            AMBIGUOUS -> {
                log.warn(
                    "Resolution of ${call.name} returned an ambiguous result and we cannot decide correctly, the invokes edge will contain the the ambiguous functions"
                )
                call.invokes = result.bestViable.toList()
            }
            SUCCESSFUL -> {
                call.invokes = result.bestViable.toList()
            }
            UNRESOLVED -> {
                call.invokes = tryFunctionInference(call, result)
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
        val (scope, _) = ctx.scopeManager.extractScope(source, source.scope)
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
            candidates.addAll(ctx.scopeManager.findSymbols(record.name.fqn(symbol)))
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

    /**
     * Creates an inferred element for each RecordDeclaration
     *
     * @param possibleContainingTypes
     * @param call
     */
    protected fun createMethodDummies(
        possibleContainingTypes: Set<Type>,
        bestGuess: Type?,
        call: CallExpression
    ): List<FunctionDeclaration> {
        var records =
            possibleContainingTypes.mapNotNull {
                val root = it.root as? ObjectType
                root?.recordDeclaration
            }

        // We access an unknown method of an unknown record. so we need to handle that
        // along the way as well. We prefer the base type
        if (records.isEmpty()) {
            records =
                listOfNotNull(
                    ctx.tryRecordInference(bestGuess?.root ?: unknownType(), locationHint = call)
                )
        }
        records = records.distinct()

        return records.mapNotNull { record -> record.inferMethod(call, ctx = ctx) }
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
                    (constructExpression.templateParameters.size <= template.parameters.size)
            ) {
                val defaultDifference =
                    template.parameters.size - constructExpression.templateParameters.size
                if (defaultDifference <= template.parameterDefaults.size) {
                    // Check if predefined template value is used as default in next value
                    addRecursiveDefaultTemplateArgs(constructExpression, template, scopeManager)

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

    /**
     * Returns a set of types in which the callee of our [call] could reside in. More concretely, it
     * returns a [Pair], where the first element is the set of types and the second is our best
     * guess.
     */
    protected fun getPossibleContainingTypes(call: CallExpression): Pair<Set<Type>, Type?> {
        val possibleTypes = mutableSetOf<Type>()
        var bestGuess: Type? = null
        if (call is MemberCallExpression) {
            call.base?.let { base ->
                bestGuess = base.type
                possibleTypes.add(base.type)
                possibleTypes.addAll(base.assignedTypes)
            }
        } else {
            // This could be a C++ member call with an implicit this (which we do not create), so
            // let's add the current class to the possible list
            scopeManager.currentRecord?.toType()?.let {
                bestGuess = it
                possibleTypes.add(it)
            }
        }

        return Pair(possibleTypes, bestGuess)
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
                possibleTypes.map { scopeManager.findSymbols(it.name.fqn(name)) }.flatten()

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

    fun tryFunctionInference(
        call: CallExpression,
        result: CallResolutionResult,
    ): List<FunctionDeclaration> {
        // We need to see, whether we have any suitable base (e.g. a class) or not; There are two
        // main cases
        // a) we have a member expression -> easy
        // b) we have a call expression -> not so easy. This could be a member call with an implicit
        //    this (in which case we want to explore the base type). But that is only possible if
        //    the callee is not qualified, because otherwise we are in a static call like
        //    MyClass::doSomething() or in a namespace call (in case we do not want to explore the
        //    base type here yet). This will change in a future PR.
        val (suitableBases, bestGuess) =
            if (call.callee is MemberExpression || !call.callee.name.isQualified()) {
                getPossibleContainingTypes(call)
            } else {
                Pair(setOf(), null)
            }

        return if (suitableBases.isEmpty()) {
            // Resolution has provided no result, we can forward this to the inference system,
            // if we want. While this is definitely a function, it could still be a function
            // inside a namespace. We therefore have two possible start points, a namespace
            // declaration or a translation unit. Nothing else is allowed (fow now). We can
            // re-use the information in the ResolutionResult, since this already contains the
            // actual start scope (e.g. in case the callee has an FQN).
            var scope = result.actualStartScope
            if (scope !is NameScope) {
                scope = scopeManager.globalScope
            }
            val func =
                when (val start = scope?.astNode) {
                    is TranslationUnitDeclaration -> start.inferFunction(call, ctx = ctx)
                    is NamespaceDeclaration -> start.inferFunction(call, ctx = ctx)
                    else -> null
                }
            listOfNotNull(func)
        } else {
            createMethodDummies(suitableBases, bestGuess, call)
        }
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

fun TranslationContext.tryNamespaceInference(
    name: Name,
    locationHint: Node?
): NamespaceDeclaration? {
    return scopeManager.globalScope
        ?.astNode
        ?.startInference(this)
        ?.inferNamespaceDeclaration(name, null, locationHint)
}

/**
 * Tries to infer a [RecordDeclaration] from an unresolved [Type]. This will return `null`, if
 * inference was not possible, or if it was turned off in the [InferenceConfiguration].
 */
fun TranslationContext.tryRecordInference(
    type: Type,
    locationHint: Node? = null
): RecordDeclaration? {
    val kind =
        if (type.language is HasStructs) {
            "struct"
        } else {
            "class"
        }
    // Determine the scope where we want to start our inference
    var (scope, _) = scopeManager.extractScope(type)

    if (scope !is NameScope) {
        scope = null
    }

    var holder = scope?.astNode

    // If we could not find a scope, but we have an FQN, we can try to infer a namespace (or a
    // parent record)
    var parentName = type.name.parent
    if (scope == null && parentName != null) {
        // At this point, we need to check whether we have any type reference to our parent
        // name. If we have (e.g. it is used in a function parameter, variable, etc.), then we
        // have a high chance that this is actually a parent record and not a namespace
        var parentType = typeManager.typeExists(parentName)
        holder =
            if (parentType != null) {
                tryRecordInference(parentType, locationHint = locationHint)
            } else {
                tryNamespaceInference(parentName, locationHint = locationHint)
            }
    }

    val record =
        (holder ?: this.scopeManager.globalScope?.astNode)
            ?.startInference(this)
            ?.inferRecordDeclaration(type, kind, locationHint)

    // update the type's record. Because types are only unique per scope, we potentially need to
    // update multiple type nodes, i.e., all type nodes whose FQN match the inferred record
    if (record != null) {
        typeManager.firstOrderTypes
            .filter { it.name == record.name }
            .forEach { it.recordDeclaration = record }
    }

    return record
}
