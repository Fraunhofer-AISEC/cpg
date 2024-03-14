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

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.StructureDeclarationScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.inference.Inference.TypeInferenceObserver
import de.fraunhofer.aisec.cpg.passes.inference.inferFunction
import de.fraunhofer.aisec.cpg.passes.inference.inferMethod
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
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
open class SymbolResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    protected lateinit var walker: ScopedWalker
    lateinit var currentTU: TranslationUnitDeclaration

    protected val templateList = mutableListOf<TemplateDeclaration>()

    override fun accept(component: Component) {
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
            // gather all resolution start holders and their start nodes
            val nodes = tu.allEOGStarters.toSet()

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
            // Peek into the declaration, and if it is a variable, we can proceed normally, as we
            // are running into the special case explained above. Otherwise, we abort here (for
            // now).
            wouldResolveTo = scopeManager.resolveReference(current)
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
            if (language != null && language.namespaceDelimiter in current.name.toString()) {
                recordDeclType = getEnclosingTypeOf(current)
            }
            val field = resolveMember(recordDeclType, current)
            if (field != null) {
                refersTo = field
            }
        }

        // TODO: we need to do proper scoping (and merge it with the code above), but for now
        //  this just enables CXX static fields
        if (
            refersTo == null &&
                language != null &&
                language.namespaceDelimiter in current.name.toString()
        ) {
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
     * Tries to infer a [RecordDeclaration] from an unresolved [Type]. This will return `null`, if
     * inference was not possible, or if it was turned off in the [InferenceConfiguration].
     */
    private fun tryRecordInference(
        type: Type,
    ): RecordDeclaration? {
        val kind =
            if (type.language is HasStructs) {
                "struct"
            } else {
                "class"
            }
        val record = type.startInference(ctx)?.inferRecordDeclaration(type, currentTU, kind)

        // update the type's record
        if (record != null) {
            type.recordDeclaration = record
        }

        return record
    }

    /**
     * We get the type of the "scope" this node is in. (e.g. for a field, we drop the field's name
     * and have the class)
     */
    protected fun getEnclosingTypeOf(current: Node): Type {
        val language = current.language

        return if (language != null && language.namespaceDelimiter.isNotEmpty()) {
            val parentName = (current.name.parent ?: current.name).toString()
            current.objectType(parentName)
        } else {
            current.unknownType()
        }
    }

    protected fun handleMemberExpression(curClass: RecordDeclaration?, current: Node?) {
        if (current !is MemberExpression) {
            return
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

        var baseTarget: Declaration? = null
        if (current.base is Reference) {
            val base = current.base as Reference
            if (
                current.language is HasSuperClasses &&
                    base.name.toString() == (current.language as HasSuperClasses).superClassKeyword
            ) {
                if (curClass != null && curClass.superClasses.isNotEmpty()) {
                    val superType = curClass.superClasses[0]
                    val superRecord = superType.recordDeclaration
                    if (superRecord == null) {
                        log.error(
                            "Could not find referring super type ${superType.typeName} for ${curClass.name} in the record map. Will set the super type to java.lang.Object"
                        )
                        // TODO: Should be more generic!
                        base.type = current.objectType(Any::class.java.name)
                    } else {
                        // We need to connect this super reference to the receiver of this
                        // method
                        val func = scopeManager.currentFunction
                        if (func is MethodDeclaration) {
                            baseTarget = func.receiver
                        }
                        if (baseTarget != null) {
                            base.refersTo = baseTarget
                            // Explicitly set the type of the call's base to the super type
                            base.type = superType
                            (base.refersTo as? HasType)?.type = superType
                            // And set the assigned subtypes, to ensure, that really only our
                            // super type is in there
                            base.assignedTypes = mutableSetOf(superType)
                            (base.refersTo as? ValueDeclaration)?.assignedTypes =
                                mutableSetOf(superType)
                        }
                    }
                } else {
                    // no explicit super type -> java.lang.Object
                    // TODO: Should be more generic
                    val objectType = current.objectType(Any::class.java.name)
                    base.type = objectType
                }
            } else {
                // The base should have been resolved by now. Maybe we have some other clue about
                // this base from the type system, so we can set the declaration accordingly.
                if (base.refersTo == null) {
                    base.refersTo = base.type.recordDeclaration
                }
            }
        }

        val baseType = current.base.type.root
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
                containingClass.superTypes
                    .flatMap { it.fields }
                    .filter { it.name.lastPartsMatch(reference.name) }
                    .map { it.definition }
                    .firstOrNull()
        }
        if (member == null && record is EnumDeclaration) {
            member = record.entries[reference.name.localName]
        }
        return member ?: handleUnknownField(containingClass, reference)
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
            record = tryRecordInference(base)
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
                    unknownType(),
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
            is CallExpression -> handleCallExpression(scopeManager.currentRecord, node)
        }
    }

    protected fun handleCallExpression(curClass: RecordDeclaration?, call: CallExpression) {
        // Dynamic function invokes (such as function pointers) are handled by extra pass, so we are
        // not resolving them here. In this case, our callee refers to a variable rather than a
        // function.
        if (
            (call.callee as? Reference)?.refersTo is VariableDeclaration ||
                (call.callee as? Reference)?.refersTo is ParameterDeclaration
        ) {
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
            // We need to see, whether we have any suitable base (e.g. a class) or not; but only if
            // the call itself is not already scoped (e.g. to a namespace)
            val suitableBases =
                if (callee is MemberExpression || callee?.name?.isQualified() == false) {
                    getPossibleContainingTypes(call)
                } else {
                    setOf()
                }

            candidates =
                if (suitableBases.isEmpty()) {
                    // This is not really the most ideal place, but for now this will do. While this
                    // is definitely a function, it could still be a function inside a namespace. In
                    // this case, we want to start inference in that particular namespace and not in
                    // the TU. It is also a little bit redundant, since ScopeManager.resolveFunction
                    // (which gets called before) already extracts the scope, but this information
                    // gets lost.
                    val (scope, _) = scopeManager.extractScope(call, scopeManager.globalScope)

                    // We have two possible start points, a namespace declaration or a translation
                    // unit. Nothing else is allowed (fow now)
                    val func =
                        when (val start = scope?.astNode) {
                            is TranslationUnitDeclaration -> start.inferFunction(call, ctx = ctx)
                            is NamespaceDeclaration -> start.inferFunction(call, ctx = ctx)
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
     * Resolves [call] to a list of [FunctionDeclaration] nodes, based on the
     * [CallExpression.callee] property.
     *
     * In case a resolution is not possible, `null` can be returned.
     */
    protected fun resolveCallee(
        callee: Expression?,
        curClass: RecordDeclaration?,
        call: CallExpression
    ): List<FunctionDeclaration>? {
        return when (callee) {
            is MemberExpression -> resolveMemberCallee(callee, curClass, call)
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

    /**
     * Resolves a [CallExpression.callee] of type [Reference] to a possible list of
     * [FunctionDeclaration] nodes.
     */
    protected fun resolveReferenceCallee(
        callee: Reference,
        curClass: RecordDeclaration?,
        call: CallExpression
    ): List<FunctionDeclaration> {
        val language = call.language

        return if (curClass == null || callee.name.isQualified()) {
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
            resolveCalleeByName(callee.name.localName, curClass, call)
        }
    }

    /**
     * Resolves a [CallExpression.callee] of type [MemberExpression] to a possible list of
     * [FunctionDeclaration] nodes.
     */
    fun resolveMemberCallee(
        callee: MemberExpression,
        curClass: RecordDeclaration?,
        call: CallExpression
    ): List<FunctionDeclaration> {
        // We need to adjust certain types of the base in case of a super call and we delegate this.
        // If that is successful, we can continue with regular resolving.
        if (
            curClass != null &&
                callee.base is Reference &&
                isSuperclassReference(callee.base as Reference)
        ) {
            (callee.language as? HasSuperClasses)?.handleSuperCall(
                callee,
                curClass,
                scopeManager,
            )
        }
        return resolveCalleeByName(callee.name.localName, curClass, call)
    }

    protected fun resolveCalleeByName(
        localName: String,
        curClass: RecordDeclaration?,
        call: CallExpression
    ): List<FunctionDeclaration> {

        val possibleContainingTypes = getPossibleContainingTypes(call)

        // Find function targets. If our languages has a complex call resolution, we need to take
        // this into account
        var invocationCandidates =
            if (call.language is HasComplexCallResolution) {
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

        // Find invokes by supertypes
        if (
            invocationCandidates.isEmpty() &&
                localName.isNotEmpty() &&
                (!call.language.isCPP || shouldSearchForInvokesInParent(call))
        ) {
            val records = possibleContainingTypes.mapNotNull { it.root.recordDeclaration }.toSet()
            invocationCandidates =
                getInvocationCandidatesFromParents(localName, call, records).toMutableList()
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

    /**
     * Creates an inferred element for each RecordDeclaration
     *
     * @param possibleContainingTypes
     * @param call
     */
    protected fun createMethodDummies(
        possibleContainingTypes: Set<Type>,
        call: CallExpression
    ): List<FunctionDeclaration> {
        return possibleContainingTypes
            .mapNotNull {
                val root = it.root as? ObjectType
                var record = root?.recordDeclaration
                if (root != null && record == null) {
                    record =
                        it.startInference(ctx)
                            ?.inferRecordDeclaration(it, currentTU, locationHint = call)
                    // update the record declaration
                    root.recordDeclaration = record
                }
                record
            }
            .mapNotNull { record -> record.inferMethod(call, ctx = ctx) }
    }

    /**
     * In C++ search we don't search in the parent if there is a potential candidate with matching
     * name
     *
     * @param call
     * @return true if we should stop searching parent, false otherwise
     */
    protected fun shouldSearchForInvokesInParent(call: CallExpression): Boolean {
        return scopeManager.resolveFunctionStopScopeTraversalOnDefinition(call).isEmpty()
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

    protected fun getPossibleContainingTypes(node: Node?): Set<Type> {
        val possibleTypes = mutableSetOf<Type>()
        if (node is MemberCallExpression) {
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
        recordDeclaration: RecordDeclaration?,
        name: String,
        call: CallExpression
    ): List<FunctionDeclaration> {
        if (recordDeclaration == null) return listOf()

        return if (call.language is HasComplexCallResolution) {
            (call.language as HasComplexCallResolution).refineInvocationCandidatesFromRecord(
                recordDeclaration,
                call,
                name,
                ctx
            )
        } else {
            // We should not directly access the "methods" property of the record declaration,
            // because depending on the programming language, this only may hold methods that are
            // declared directly within the original type declaration, but not ones that are
            // declared "outside" (e.g, like it is possible in Go and C++). Instead, we should
            // retrieve the scope of the record and look for appropriate declarations.
            val scope = scopeManager.lookupScope(recordDeclaration) as? StructureDeclarationScope

            // Filter the value declarations for an appropriate method
            scope?.valueDeclarations?.filterIsInstance<MethodDeclaration>()?.filter {
                it.name.lastPartsMatch(name) && it.hasSignature(call)
            } ?: listOf()
        }
    }

    protected fun getInvocationCandidatesFromParents(
        name: String,
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
        var constructorCandidate =
            recordDeclaration.constructors.firstOrNull { it.hasSignature(signature) }

        if (constructorCandidate == null && constructExpression.language is HasDefaultArguments) {
            // Check for usage of default args
            constructorCandidate =
                resolveConstructorWithDefaults(constructExpression, signature, recordDeclaration)
        }
        if (constructorCandidate == null && constructExpression.language.isCPP) { // TODO: Fix this
            // If we don't find any candidate and our current language is c/c++ we check if there is
            // a candidate with an implicit cast
            constructorCandidate =
                resolveWithImplicitCast(constructExpression, recordDeclaration.constructors)
                    .firstOrNull() as ConstructorDeclaration?
        }

        return constructorCandidate
            ?: recordDeclaration
                .startInference(ctx)
                ?.createInferredConstructor(constructExpression.signature)
    }

    protected fun getConstructorDeclarationForExplicitInvocation(
        signature: List<Type>,
        recordDeclaration: RecordDeclaration
    ): ConstructorDeclaration? {
        return recordDeclaration.constructors.firstOrNull { it.hasSignature(signature) }
            ?: recordDeclaration.startInference(ctx)?.createInferredConstructor(signature)
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
