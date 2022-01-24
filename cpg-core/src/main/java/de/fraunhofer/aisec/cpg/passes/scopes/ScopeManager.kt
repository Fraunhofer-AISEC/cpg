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
package de.fraunhofer.aisec.cpg.passes.scopes

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.*
import java.util.function.Predicate
import org.slf4j.LoggerFactory

/**
 * The scope manager builds a multi-tree structure of nodes associated to a scope. These scopes
 * capture the validity of certain (Variable-, Field-, Record-)declarations but are also used to
 * identify outer scopes that should be the target of a jump (continue, break, throw).
 *
 * Language frontends MUST call [enterScope] and [leaveScope] when they encounter nodes that modify
 * the scope and [resetToGlobal] when they first handle a new [TranslationUnitDeclaration].
 * Afterwards the currently valid "stack" of scopes within the tree can be accessed.
 *
 * If a language frontend encounters a [Declaration] node, it MUST call [addDeclaration], rather
 * then adding the declaration to the node itself. This ensures that all declarations are properly
 * registred in the scope map and can be resolved later.
 */
class ScopeManager {
    /**
     * A map associating each CPG node with its scope. The key type is intentionally a nullable
     * [Node] because the [GlobalScope] is not associated to a CPG node when it is first created. It
     * is later associated using the [resetToGlobal] function.
     */
    private val scopeMap: MutableMap<Node?, Scope> = IdentityHashMap()

    /** A lookup map for each scope and its associated FQN. */
    private val fqnScopeMap: MutableMap<String, NameScope> = IdentityHashMap()

    /** The currently active scope. */
    var currentScope: Scope? = null
        private set

    /**
     * The language frontend tied to the scope manager. Can be used to implement language specifics
     * scope resolution or lookup.
     */
    var lang: LanguageFrontend? = null

    /** True, if the scope manager is currently in a [BlockScope]. */
    val isInBlock: Boolean
        get() = this.firstScopeOrNull { it is BlockScope } != null
    /** True, if the scope manager is currently in a [FunctionScope]. */
    val isInFunction: Boolean
        get() = this.firstScopeOrNull { it is FunctionScope } != null
    /** True, if the scope manager is currently in a [RecordScope], e.g. a class. */
    val isInRecord: Boolean
        get() = this.firstScopeOrNull { it is RecordScope } != null

    val globalScope: GlobalScope?
        get() = this.firstScopeIsInstanceOrNull()

    /** The current block, according to the scope that is currently active. */
    val currentBlock: CompoundStatement?
        get() = this.firstScopeIsInstanceOrNull<BlockScope>()?.astNode as? CompoundStatement
    /** The current function, according to the scope that is currently active. */
    val currentFunction: FunctionDeclaration?
        get() = this.firstScopeIsInstanceOrNull<FunctionScope>()?.astNode as? FunctionDeclaration
    /** The current record, according to the scope that is currently active. */
    val currentRecord: RecordDeclaration?
        get() = this.firstScopeIsInstanceOrNull<RecordScope>()?.astNode as? RecordDeclaration

    val currentTypedefs: Collection<TypedefDeclaration>
        get() = this.getCurrentTypedefs(currentScope)

    val currentNamePrefix: String
        get() {
            val namedScope = this.firstScopeIsInstanceOrNull<NameScope>()
            return if (namedScope is NameScope) namedScope.namePrefix else ""
        }
    val currentNamePrefixWithDelimiter: String
        get() {
            var namePrefix = currentNamePrefix
            if (namePrefix.isNotEmpty()) {
                namePrefix += lang!!.namespaceDelimiter
            }
            return namePrefix
        }

    init {
        pushScope(GlobalScope())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ScopeManager::class.java)
    }

    /**
     * Combines the state of several scope managers into this one. Primarily used in combination
     * with concurrent frontends.
     *
     * @param toMerge The scope managers to merge into this one
     */
    fun mergeFrom(toMerge: Collection<ScopeManager>) {
        val globalScopes =
            toMerge
                .map { s: ScopeManager -> s.scopeMap[null] }
                .filter { obj: Scope? -> GlobalScope::class.java.isInstance(obj) }
                .map { obj: Scope? -> GlobalScope::class.java.cast(obj) }
        val currGlobalScope = scopeMap[null]
        if (currGlobalScope !is GlobalScope) {
            LOGGER.error("Scope for null node is not a GlobalScope or is null")
        } else {
            currGlobalScope.mergeFrom(globalScopes)
            scopeMap[null] = currGlobalScope
        }
        for (manager in toMerge) {
            // loop through all scopes in the FQN map to check for potential duplicates we need to
            // merge
            for (entry in manager.fqnScopeMap.entries) {
                val existing = fqnScopeMap[entry.key]
                if (existing != null) {
                    // a name scope with an identical FQN already exist. we transfer all
                    // declarations over to it. We are NOT using [addValueDeclaration] because this
                    // will add it to the underlying AST node as well. This was already done by the
                    // respective sub-scope manager. We add it directly to the declarations array
                    // instead.
                    existing.valueDeclarations.addAll(entry.value.valueDeclarations)
                    existing.structureDeclarations.addAll(entry.value.structureDeclarations)

                    // copy over the typedefs as well just to be sure
                    existing.typedefs.addAll(entry.value.typedefs)

                    // also update the AST node of the existing scope to the "latest" we have seen
                    existing.astNode = entry.value.astNode

                    // now it gets more tricky. we also need to "redirect" the AST nodes in the sub
                    // scope manager to our
                    // existing NameScope (currently, they point to their own, invalid copy of the
                    // NameScope).
                    //
                    // The only way to do this, is to filter for the particular
                    // scope (the value of the map) and return the keys (the nodes)
                    val keys =
                        manager.scopeMap.filter { it.value.astNode == entry.value.astNode }.map {
                            it.key
                        }

                    // now, we redirect it to the existing scope
                    keys.forEach { manager.scopeMap[it] = existing }
                } else {
                    // this is the first we see for this particular FQN, so we add it to our map
                    fqnScopeMap[entry.key] = entry.value
                }
            }

            scopeMap.putAll(manager.scopeMap)

            // free the maps, just to clear up some things. this scope manager will not be used
            // anymore
            manager.fqnScopeMap.clear()
            manager.scopeMap.clear()
        }
    }

    /**
     * Pushes the scope on the current scope stack. Used internally by [enterScope].
     *
     * @param scope the scope
     */
    private fun pushScope(scope: Scope) {
        if (scopeMap.containsKey(scope.astNode)) {
            LOGGER.error(
                "Node cannot be scoped twice. A node must be at most one associated scope apart from the parent scopes."
            )
            return
        }
        scopeMap[scope.astNode] = scope
        if (scope is NameScope) {
            // for this to work, it is essential that RecordDeclaration and NamespaceDeclaration
            // nodes have a FQN as their name.
            fqnScopeMap[scope.astNode.name] = scope
        }
        currentScope?.let {
            it.getChildren().add(scope)
            scope.setParent(it)
        }
        currentScope = scope
    }

    /**
     * This function, in combination with [leaveScope] is the main interaction point with the scope
     * manager for language frontends. Every time a language frontend handles a node that begins a
     * new scope, this function needs to be called. Appropriate scopes will then be created
     * on-the-fly, if they do not exist.
     *
     * The scope manager has an internal association between the type of scope, e.g. a [BlockScope]
     * and the CPG node it represents, e.g. a [CompoundStatement].
     *
     * Afterwards, all calls to [addDeclaration] will be distributed to the
     * [de.fraunhofer.aisec.cpg.graph.DeclarationHolder] that is currently in-scope.
     */
    fun enterScope(nodeToScope: Node) {
        var newScope: Scope? = null

        // check, if the node does not have an entry in the scope map
        if (!scopeMap.containsKey(nodeToScope)) {
            newScope =
                when (nodeToScope) {
                    is CompoundStatement -> BlockScope(nodeToScope)
                    is WhileStatement, is DoStatement, is AssertStatement ->
                        LoopScope(nodeToScope as Statement)
                    is ForStatement, is ForEachStatement -> LoopScope(nodeToScope as Statement)
                    is SwitchStatement -> SwitchScope(nodeToScope)
                    is FunctionDeclaration -> FunctionScope(nodeToScope)
                    is IfStatement -> ValueDeclarationScope(nodeToScope)
                    is CatchClause -> ValueDeclarationScope(nodeToScope)
                    is RecordDeclaration ->
                        RecordScope(nodeToScope, currentNamePrefix, lang!!.namespaceDelimiter)
                    is TemplateDeclaration ->
                        TemplateScope(nodeToScope, currentNamePrefix, lang!!.namespaceDelimiter)
                    is TryStatement -> TryScope(nodeToScope)
                    is NamespaceDeclaration -> newNameScopeIfNecessary(nodeToScope)
                    else -> {
                        LOGGER.error(
                            "No known scope for AST node of type {}",
                            nodeToScope.javaClass
                        )
                        return
                    }
                }
        }

        // push the new scope
        if (newScope != null) {
            pushScope(newScope)
            newScope.setScopedName(currentNamePrefix)
        } else {
            currentScope = scopeMap[nodeToScope]
        }
    }

    /**
     * A small internal helper function used by [enterScope] to create a [NameScope].
     *
     * The issue with name scopes, such as a namespace, is that it can exist across several files,
     * i.e. translation units, represented by different [NamespaceDeclaration] nodes. But, in order
     * to make namespace resolution work across files, only one [NameScope] must exist that holds
     * all declarations, such as classes, independently of the translation units. Therefore, we need
     * to check, whether such as node already exists. If it does already exist:
     * - we update the scope map so that the current [NamespaceDeclaration] points to the existing
     * [NameScope]
     * - we return null, indicating to [enterScope], that no new scope needs to be pushed by
     * [enterScope].
     *
     * Otherwise, we return a new name scope.
     */
    private fun newNameScopeIfNecessary(nodeToScope: NamespaceDeclaration): NameScope? {
        val existingScope =
            currentScope?.children?.firstOrNull {
                it is NameScope && it.scopedName == nodeToScope.name
            }

        return if (existingScope != null) {
            // update the AST node to this namespace declaration
            existingScope.astNode = nodeToScope

            // make it also available in the scope map. Otherwise, we cannot leave the
            // scope
            scopeMap[nodeToScope] = existingScope

            // do NOT return a new name scope, but rather return null, so enterScope knows that it
            // does not need to push a new scope
            null
        } else {
            NameScope(nodeToScope, currentNamePrefix, lang!!.namespaceDelimiter)
        }
    }

    /**
     * Similar to [enterScope], but does so in a "read-only" mode, e.g. it does not modify the scope
     * tree and does not create new scopes on the fly, as [enterScope] does.
     */
    fun enterScopeIfExists(nodeToScope: Node?) {
        if (scopeMap.containsKey(nodeToScope)) {
            val scope = scopeMap[nodeToScope]

            // we need a special handling of name spaces, because
            // they are associated to more than one AST node
            if (scope is NameScope) {
                // update AST (see enterScope for an explanation)
                scope.astNode = nodeToScope
            }
            currentScope = scope
        }
    }

    /**
     * The counter-part of [enterScope]. Language frontends need to call this function, when the
     * scope of the currently processed AST node ends. There MUST have been a corresponding
     * [enterScope] call with the same [nodeToLeave], otherwise the scope-tree might be corrupted.
     *
     * @param nodeToLeave the AST node
     *
     * @return the scope that was just left
     */
    fun leaveScope(nodeToLeave: Node): Scope? {
        // Check to return as soon as we know that there is no associated scope. This check could be
        // omitted but will increase runtime if leaving a node without scope will happen often.
        if (!scopeMap.containsKey(nodeToLeave)) {
            return null
        }

        val leaveScope = firstScopeOrNull { it.astNode == nodeToLeave }
        if (leaveScope == null) {
            if (scopeMap.containsKey(nodeToLeave)) {
                Util.errorWithFileLocation(
                    nodeToLeave,
                    LOGGER,
                    "Node of type {} has a scope but is not active in the moment.",
                    nodeToLeave.javaClass
                )
            } else {
                Util.errorWithFileLocation(
                    nodeToLeave,
                    LOGGER,
                    "Node of type {} is not associated with a scope.",
                    nodeToLeave.javaClass
                )
            }

            return null
        }

        // go back to the parent of the scope we just left
        currentScope = leaveScope.parent
        return leaveScope
    }

    /**
     * This function MUST be called when a language frontend first handles a [Declaration]. It adds
     * a declaration to the scope manager, taking into account the currently active scope.
     * Furthermore it adds the declaration to the [de.fraunhofer.aisec.cpg.graph.DeclarationHolder]
     * that is associated with the current scope through [ValueDeclarationScope.addValueDeclaration]
     * and [StructureDeclarationScope.addStructureDeclaration].
     *
     * @param declaration
     */
    fun addDeclaration(declaration: Declaration?) {
        when (declaration) {
            is ProblemDeclaration, is IncludeDeclaration -> {
                // directly add problems and includes to the global scope
                this.globalScope?.addDeclaration(declaration)
            }
            is ValueDeclaration -> {
                val scope = this.firstScopeIsInstanceOrNull<ValueDeclarationScope>()
                scope?.addValueDeclaration(declaration)
            }
            is RecordDeclaration,
            is NamespaceDeclaration,
            is EnumDeclaration,
            is TemplateDeclaration -> {
                val scope = this.firstScopeIsInstanceOrNull<StructureDeclarationScope>()
                scope?.addDeclaration(declaration)
            }
        }
    }

    /**
     * This function tries to find the first scope that satisfies the condition specified in
     * [predicate]. It starts searching in the [searchScope], moving up-wards using the
     * [Scope.parent] attribute.
     *
     * @param searchScope the scope to start the search in
     * @param predicate the search predicate
     */
    @JvmOverloads
    fun firstScopeOrNull(searchScope: Scope? = currentScope, predicate: Predicate<Scope>): Scope? {
        // start at searchScope
        var scope = searchScope

        while (scope != null) {
            if (predicate.test(scope)) {
                return scope
            }

            // go up-wards in the scope tree
            scope = scope.parent
        }

        return null
    }

    /**
     * Tries to find the first scope that is an instance of the scope type [T]. Calls
     * [firstScopeOrNull] internally.
     *
     * @param searchScope the scope to start the search in
     */
    inline fun <reified T : Scope> firstScopeIsInstanceOrNull(
        searchScope: Scope? = currentScope
    ): T? {
        return this.firstScopeOrNull(searchScope) { it is T } as? T
    }

    /**
     * Retrieves all unique scopes that satisfy the condition specified in [predicate],
     * independently of their hierarchy.
     *
     * @param predicate the search predicate
     */
    fun filterScopes(predicate: (Scope) -> Boolean): List<Scope> {
        return scopeMap.values.filter(predicate).distinct()
    }

    /**
     * This function filters scopes according to [predicate] and makes them unique according to
     * [uniqueProperty].
     *
     * @param predicate the search predicate
     * @param uniqueProperty the unique property to run a distinct filter on
     */
    fun <T> filterScopesDistinctBy(
        predicate: (Scope) -> Boolean,
        uniqueProperty: (Scope) -> T
    ): List<Scope> {
        return scopeMap.values.filter(predicate).distinctBy(uniqueProperty)
    }

    /** This function returns the [Scope] associated with a node. */
    fun lookupScope(node: Node): Scope? {
        return scopeMap[node]
    }

    /** This function looks up scope by its FQN. This only works for [NameScope]s */
    fun lookupScope(fqn: String): NameScope? {
        return this.fqnScopeMap[fqn]
    }

    /**
     * This function SHOULD only be used by the
     * [de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass] while building up the EOG. It adds
     * a [BreakStatement] to the list of break statements of the current "breakable" scope.
     */
    fun addBreakStatement(breakStatement: BreakStatement) {
        if (breakStatement.label == null) {
            val scope = firstScopeOrNull { scope: Scope? -> scope?.isBreakable() == true }
            if (scope == null) {
                LOGGER.error(
                    "Break inside of unbreakable scope. The break will be ignored, but may lead " +
                        "to an incorrect graph. The source code is not valid or incomplete."
                )
                return
            }
            (scope as Breakable).addBreakStatement(breakStatement)
        } else {
            val labelStatement = getLabelStatement(breakStatement.label)
            if (labelStatement != null) {
                val scope = lookupScope(labelStatement.subStatement)
                (scope as Breakable?)!!.addBreakStatement(breakStatement)
            }
        }
    }

    /**
     * This function SHOULD only be used by the
     * [de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass] while building up the EOG. It adds
     * a [ContinueStatement] to the list of continue statements of the current "continuable" scope.
     */
    fun addContinueStatement(continueStatement: ContinueStatement) {
        if (continueStatement.label == null) {
            val scope = firstScopeOrNull { scope: Scope? -> scope?.isContinuable() == true }
            if (scope == null) {
                LOGGER.error(
                    "Continue inside of not continuable scope. The continue will be ignored, but may lead " +
                        "to an incorrect graph. The source code is not valid or incomplete."
                )
                return
            }
            (scope as Continuable).addContinueStatement(continueStatement)
        } else {
            val labelStatement = getLabelStatement(continueStatement.label)
            if (labelStatement != null) {
                val scope = lookupScope(labelStatement.subStatement)
                (scope as Continuable?)!!.addContinueStatement(continueStatement)
            }
        }
    }

    /**
     * This function SHOULD only be used by the
     * [de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass] while building up the EOG. It adds
     * a [LabelStatement] to the list of label statements of the current scope.
     */
    fun addLabelStatement(labelStatement: LabelStatement) {
        currentScope?.addLabelStatement(labelStatement)
    }

    /**
     * This function is internal to the scoep manager and primarily used by [addBreakStatement] and
     * [addContinueStatement]. It retrieves the [LabelStatement] associated with the [labelString].
     */
    private fun getLabelStatement(labelString: String): LabelStatement? {
        var labelStatement: LabelStatement?
        var searchScope = currentScope
        while (searchScope != null) {
            labelStatement = searchScope.getLabelStatements()[labelString]
            if (labelStatement != null) {
                return labelStatement
            }
            searchScope = searchScope.parent
        }
        return null
    }

    /**
     * This function MUST be called when a language frontend first enters a translation unit. It
     * sets the [GlobalScope] to the current translation unit specified in [declaration].
     */
    fun resetToGlobal(declaration: TranslationUnitDeclaration?) {
        val global = this.globalScope
        if (global != null) {
            // update the AST node to this translation unit declaration
            global.astNode = declaration
            currentScope = global
        }
    }

    /**
     * Soley used by the [de.fraunhofer.aisec.cpg.graph.TypeManager], adds typedefs to the current
     * [ValueDeclarationScope].
     */
    fun addTypedef(typedef: TypedefDeclaration?) {
        val scope = this.firstScopeIsInstanceOrNull<ValueDeclarationScope>()
        if (scope == null) {
            LOGGER.error("Cannot add typedef. Not in declaration scope.")
            return
        }

        scope.addTypedef(typedef)

        if (scope.astNode == null) {
            lang!!.currentTU.addTypedef(typedef!!)
        } else {
            scope.astNode.addTypedef(typedef!!)
        }
    }

    private fun getCurrentTypedefs(scope: Scope?): Collection<TypedefDeclaration> {
        val typedefs = mutableMapOf<Type, TypedefDeclaration>()

        var current = scope

        while (current != null) {
            if (scope is ValueDeclarationScope) {
                scope.typedefs.forEach { typedefs.putIfAbsent(it.alias, it) }
            }

            current = current.parent
        }

        return typedefs.values
    }

    /**
     * Resolves only references to Values in the current scope, static references to other visible
     * records are not resolved over the ScopeManager.
     *
     * TODO: We should merge this function with [.resolveFunction]
     *
     * @param scope
     * @param ref
     * @return
     */
    @JvmOverloads
    fun resolveReference(
        ref: DeclaredReferenceExpression,
        scope: Scope? = currentScope
    ): ValueDeclaration? {
        return resolve<ValueDeclaration>(scope) {
                if (it.name == ref.name) {
                    // If the reference seems to point to a function the entire signature is checked
                    // for equality
                    if (ref.type is FunctionPointerType && it is FunctionDeclaration) {
                        val fptrType = (ref as HasType).type as FunctionPointerType
                        val d = it
                        if (d.type == fptrType.returnType && d.hasSignature(fptrType.parameters)) {
                            return@resolve true
                        }
                    } else {
                        return@resolve true
                    }
                }

                return@resolve false
            }
            .firstOrNull()
    }

    /**
     * Tries to resolve a function in a call expression.
     *
     * @param call the call expression
     * @return a list of possible functions
     */
    @JvmOverloads
    fun resolveFunction(
        call: CallExpression,
        scope: Scope? = currentScope
    ): List<FunctionDeclaration> {
        var s = scope

        // First, we need to check, whether we have some kind of scoping.
        if (lang != null && call.fqn != null && call.fqn.contains(lang!!.namespaceDelimiter)) {
            // extract the scope name, it is usually a name space, but could probably be something
            // else as well in other languages
            val scopeName = call.fqn.substring(0, call.fqn.lastIndexOf(lang!!.namespaceDelimiter))

            // TODO: proper scope selection

            // this is a scoped call. we need to explicitly jump to that particular scope
            val scopes = filterScopes { (it is NameScope && it.scopedName == scopeName) }
            s =
                if (scopes.isEmpty()) {
                    LOGGER.error(
                        "Could not find the scope {} needed to resolve the call {}. Falling back to the current scope",
                        scopeName,
                        call.fqn
                    )
                    currentScope
                } else {
                    scopes[0]
                }
        }

        return resolve(s) { it.name == call.name && it.hasSignature(call.signature) }
    }

    fun resolveFunctionStopScopeTraversalOnDefinition(
        call: CallExpression
    ): List<FunctionDeclaration> {
        return resolve(currentScope, true) { f: FunctionDeclaration -> f.name == call.name }
    }

    /**
     * Traverses the scope up-wards and looks for declarations of type [T] which matches the
     * condition [predicate].
     *
     * It returns a list of all declarations that match the predicate, ordered by reachability in
     * the scope stack. This means that "local" declarations will be in the list first, global items
     * will be last.
     *
     * @param searchScope the scope to start the search in
     * @param predicate predicate the element must match to
     * @param <T>
     */
    inline fun <reified T : Declaration> resolve(
        searchScope: Scope?,
        stopIfFound: Boolean = false,
        predicate: (T) -> Boolean
    ): List<T> {
        var scope = searchScope
        val declarations = mutableListOf<T>()

        while (scope != null) {
            if (scope is ValueDeclarationScope) {
                declarations.addAll(scope.valueDeclarations.filterIsInstance<T>().filter(predicate))
            }

            if (scope is StructureDeclarationScope) {
                var list = scope.structureDeclarations.filterIsInstance<T>().filter(predicate)

                // this was taken over from the old resolveStructureDeclaration.
                // TODO(oxisto): why is this only when the list is empty?
                if (list.isEmpty()) {
                    for (declaration in scope.structureDeclarations) {
                        if (declaration is RecordDeclaration) {
                            list = declaration.templates.filterIsInstance<T>().filter(predicate)
                        }
                    }
                }

                declarations.addAll(list)
            }

            // some (all?) languages require us to stop immediately if we found something on this
            // scope. This is the case where function overloading is allowed, but only within the
            // same scope
            if (stopIfFound && declarations.isNotEmpty()) {
                return declarations
            }

            // go up-wards in the scope tree
            scope = scope.getParent()
        }

        return declarations
    }

    /**
     * Resolves function templates of the given [CallExpression].
     *
     * @param scope where we are searching for the FunctionTemplateDeclarations
     * @param call CallExpression we want to resolve an invocation target for
     * @return List of FunctionTemplateDeclaration that match the name provided in the
     * CallExpression and therefore are invocation candidates
     */
    @JvmOverloads
    fun resolveFunctionTemplateDeclaration(
        call: CallExpression,
        scope: Scope? = currentScope
    ): List<FunctionTemplateDeclaration> {
        return resolve(scope, true) { c: FunctionTemplateDeclaration -> c.name == call.name }
    }

    /**
     * Retrieves the [RecordDeclaration] for the given name in the given scope.
     *
     * @param scope the scope
     * @param name the name
     * @return the declaration, or null if it does not exist
     */
    fun getRecordForName(scope: Scope, name: String): RecordDeclaration? {
        return resolve<RecordDeclaration>(scope, true) { it.name == name }.firstOrNull()
    }
}
