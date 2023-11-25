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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.HasFirstClassFunctions
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ReferenceTag
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.IncompleteType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
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
 * than adding the declaration to the node itself. This ensures that all declarations are properly
 * registered in the scope map and can be resolved later.
 */
class ScopeManager : ScopeProvider {
    /**
     * A map associating each CPG node with its scope. The key type is intentionally a nullable
     * [Node] because the [GlobalScope] is not associated to a CPG node when it is first created. It
     * is later associated using the [resetToGlobal] function.
     */
    private val scopeMap: MutableMap<Node?, Scope> = IdentityHashMap()

    /** A lookup map for each scope and its associated FQN. */
    private val fqnScopeMap: MutableMap<String, NameScope> = mutableMapOf()

    /** The currently active scope. */
    var currentScope: Scope? = null
        private set

    /** Represents an alias with the name [to] for the particular name [from]. */
    data class Alias(var from: Name, var to: Name)

    /**
     * A cache map of reference tags (computed with [Reference.referenceTag]) and their respective
     * pair of original [Reference] and resolved [ValueDeclaration]. This is used by
     * [resolveReference] as a caching mechanism.
     */
    private val symbolTable = mutableMapOf<ReferenceTag, Pair<Reference, ValueDeclaration>>()

    /**
     * In some languages, we can define aliases for names. An example is renaming package imports in
     * Go, e.g., to avoid name conflicts.
     *
     * In reality, they can probably be defined at different scopes for other languages, but for now
     * we only allow it for the current file.
     *
     * This can potentially be used to replace [addTypedef] at some point, which still relies on the
     * existence of a [LanguageFrontend].
     */
    private val aliases = mutableMapOf<PhysicalLocation.ArtifactLocation, MutableSet<Alias>>()

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
        get() = scopeMap[null] as? GlobalScope

    /** The current block, according to the scope that is currently active. */
    val currentBlock: Block?
        get() = this.firstScopeIsInstanceOrNull<BlockScope>()?.astNode as? Block
    /** The current function, according to the scope that is currently active. */
    val currentFunction: FunctionDeclaration?
        get() = this.firstScopeIsInstanceOrNull<FunctionScope>()?.astNode as? FunctionDeclaration
    /** The current record, according to the scope that is currently active. */
    val currentRecord: RecordDeclaration?
        get() = this.firstScopeIsInstanceOrNull<RecordScope>()?.astNode as? RecordDeclaration

    val currentTypedefs: Collection<TypedefDeclaration>
        get() = this.getCurrentTypedefs(currentScope)

    val currentNamespace: Name?
        get() {
            val namedScope = this.firstScopeIsInstanceOrNull<NameScope>()
            return if (namedScope is NameScope) namedScope.name else null
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
        val globalScopes = toMerge.mapNotNull { it.globalScope }
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
                    existing.typedefs.putAll(entry.value.typedefs)

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
                        manager.scopeMap
                            .filter { it.value.astNode == entry.value.astNode }
                            .map { it.key }

                    // now, we redirect it to the existing scope
                    keys.forEach { manager.scopeMap[it] = existing }
                } else {
                    // this is the first we see for this particular FQN, so we add it to our map
                    fqnScopeMap[entry.key] = entry.value
                }
            }

            // We need to make sure that we do not put the "null" key (aka the global scope) of the
            // individual scope manager into our map, otherwise we would overwrite our merged global
            // scope.
            scopeMap.putAll(manager.scopeMap.filter { it.key != null })

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
            fqnScopeMap[scope.astNode?.name.toString()] = scope
        }
        currentScope?.let {
            it.children.add(scope)
            scope.parent = it
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
     * and the CPG node it represents, e.g. a [Block].
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
                    is Block -> BlockScope(nodeToScope)
                    is WhileStatement,
                    is DoStatement,
                    is AssertStatement -> LoopScope(nodeToScope as Statement)
                    is ForStatement,
                    is ForEachStatement -> LoopScope(nodeToScope as Statement)
                    is SwitchStatement -> SwitchScope(nodeToScope)
                    is FunctionDeclaration -> FunctionScope(nodeToScope)
                    is IfStatement -> ValueDeclarationScope(nodeToScope)
                    is CatchClause -> ValueDeclarationScope(nodeToScope)
                    is RecordDeclaration -> RecordScope(nodeToScope)
                    is TemplateDeclaration -> TemplateScope(nodeToScope)
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
            newScope.scopedName = currentNamespace?.toString()
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
     *   [NameScope]
     * - we return null, indicating to [enterScope], that no new scope needs to be pushed by
     *   [enterScope].
     *
     * Otherwise, we return a new name scope.
     */
    private fun newNameScopeIfNecessary(nodeToScope: NamespaceDeclaration): NameScope? {
        val existingScope =
            currentScope?.children?.firstOrNull { it is NameScope && it.name == nodeToScope.name }

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
            NameScope(nodeToScope)
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
     * Furthermore, it adds the declaration to the [de.fraunhofer.aisec.cpg.graph.DeclarationHolder]
     * that is associated with the current scope through [ValueDeclarationScope.addValueDeclaration]
     * and [StructureDeclarationScope.addStructureDeclaration].
     *
     * Setting [Scope.astNode] to false is useful, if you want to make sure a certain declaration is
     * visible within a scope, but is not directly part of the scope's AST. An example is the way
     * C/C++ handles unscoped enum constants. They are visible in the enclosing scope, e.g., a
     * translation unit, but they are added to the AST of their enum declaration, not the
     * translation unit. The enum declaration is then added to the translation unit.
     *
     * @param declaration the declaration to add
     * @param addToAST specifies, whether the declaration also gets added to the [Scope.astNode] of
     *   the current scope (if it implements [DeclarationHolder]). Defaults to true.
     */
    @JvmOverloads
    fun addDeclaration(declaration: Declaration?, addToAST: Boolean = true) {
        when (declaration) {
            is ProblemDeclaration,
            is IncludeDeclaration -> {
                // directly add problems and includes to the global scope
                this.globalScope?.addDeclaration(declaration, addToAST)
            }
            is ValueDeclaration -> {
                val scope = this.firstScopeIsInstanceOrNull<ValueDeclarationScope>()
                scope?.addValueDeclaration(declaration, addToAST)
            }
            is RecordDeclaration,
            is NamespaceDeclaration,
            is EnumDeclaration,
            is TemplateDeclaration -> {
                val scope = this.firstScopeIsInstanceOrNull<StructureDeclarationScope>()
                scope?.addDeclaration(declaration, addToAST)
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

    /** This function returns the [Scope] associated with a node. */
    fun lookupScope(node: Node): Scope? {
        return if (node is TranslationUnitDeclaration) {
            globalScope
        } else scopeMap[node]
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
                Util.errorWithFileLocation(
                    breakStatement,
                    LOGGER,
                    "Break inside of unbreakable scope. The break will be ignored, but may lead " +
                        "to an incorrect graph. The source code is not valid or incomplete."
                )
                return
            }
            (scope as Breakable).addBreakStatement(breakStatement)
        } else {
            val labelStatement = getLabelStatement(breakStatement.label)
            labelStatement?.subStatement?.let {
                val scope = lookupScope(it)
                (scope as Breakable?)?.addBreakStatement(breakStatement)
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
            labelStatement?.subStatement?.let {
                val scope = lookupScope(it)
                (scope as Continuable?)?.addContinueStatement(continueStatement)
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
     * This function is internal to the scope manager and primarily used by [addBreakStatement] and
     * [addContinueStatement]. It retrieves the [LabelStatement] associated with the [labelString].
     */
    private fun getLabelStatement(labelString: String?): LabelStatement? {
        if (labelString == null) return null
        var labelStatement: LabelStatement?
        var searchScope = currentScope
        while (searchScope != null) {
            labelStatement = searchScope.labelStatements[labelString]
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

    /** Only used by the [TypeManager], adds typedefs to the current [ValueDeclarationScope]. */
    fun addTypedef(typedef: TypedefDeclaration) {
        val scope = this.firstScopeIsInstanceOrNull<ValueDeclarationScope>()
        if (scope == null) {
            LOGGER.error("Cannot add typedef. Not in declaration scope.")
            return
        }

        scope.addTypedef(typedef)
    }

    private fun getCurrentTypedefs(searchScope: Scope?): Collection<TypedefDeclaration> {
        val typedefs = mutableMapOf<Type, TypedefDeclaration>()

        val path = mutableListOf<ValueDeclarationScope>()
        var current = searchScope

        // We need to build a path from the current scope to the top most one
        while (current != null) {
            if (current is ValueDeclarationScope) {
                path += current
            }
            current = current.parent
        }

        // And then follow the path in reverse. This ensures us that a local definition
        // overwrites / shadows one that was there on a higher scope.
        for (scope in path.reversed()) {
            typedefs.putAll(scope.typedefs)
        }

        return typedefs.values
    }

    /**
     * Resolves only references to Values in the current scope, static references to other visible
     * records are not resolved over the ScopeManager.
     *
     * @param ref
     * @return
     *
     * TODO: We should merge this function with [.resolveFunction]
     */
    fun resolveReference(ref: Reference): ValueDeclaration? {
        val startScope = ref.scope

        // Retrieve a unique tag for the particular reference based on the current scope
        val tag = ref.referenceTag

        // If we find a match in our symbol table, we can immediately return the declaration. We
        // need to be careful about potential collisions in our tags, since they are based on the
        // hash-code of the scope. We therefore take the extra precaution to compare the scope in
        // case we get a hit. This should not take too much performance overhead.
        val pair = symbolTable[tag]
        if (pair != null && ref.scope == pair.first.scope) {
            return pair.second
        }

        val (scope, name) = extractScope(ref, startScope)

        // Try to resolve value declarations according to our criteria
        val decl =
            resolve<ValueDeclaration>(scope) {
                    if (it.name.lastPartsMatch(name)) {
                        val helper = ref.resolutionHelper
                        return@resolve when {
                            // If the reference seems to point to a function (using a function
                            // pointer) the entire signature is checked for equality
                            helper?.type is FunctionPointerType && it is FunctionDeclaration -> {
                                val fptrType = helper.type as FunctionPointerType
                                // TODO(oxisto): Support multiple return values
                                val returnType = it.returnTypes.firstOrNull() ?: IncompleteType()
                                returnType == fptrType.returnType &&
                                    it.hasSignature(fptrType.parameters)
                            }
                            // If our language has first-class functions, we can safely return them
                            // as a reference
                            ref.language is HasFirstClassFunctions -> {
                                true
                            }
                            // Otherwise, we are not looking for functions here
                            else -> {
                                it !is FunctionDeclaration
                            }
                        }
                    }

                    return@resolve false
                }
                .firstOrNull()

        // Update the symbol cache, if we found a declaration for the tag
        if (decl != null) {
            symbolTable[tag] = Pair(ref, decl)
        }

        return decl
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
        startScope: Scope? = currentScope
    ): List<FunctionDeclaration> {
        val (scope, name) = extractScope(call, startScope)

        val func =
            resolve<FunctionDeclaration>(scope) {
                it.name.lastPartsMatch(name) && it.hasSignature(call)
            }

        return func
    }

    /**
     * This function extracts a possible scope out of a [Name], e.g. if the name is fully qualified.
     * This also resolves possible name aliases (e.g. because of imports). It returns a pair of a
     * scope (if found) as well as the name, which is possibly adjusted for the aliases.
     *
     * Note: Currently only *fully* qualified names are properly resolved. This function will
     * probably return imprecise results for partially qualified names, e.g. if a name `A` inside
     * `B` points to `A::B`, rather than to `A`.
     */
    fun extractScope(node: Node, scope: Scope? = currentScope): Pair<Scope?, Name> {
        var name: Name = node.name
        var s = scope

        // First, we need to check, whether we have some kind of scoping.
        if (node.name.isQualified()) {
            // extract the scope name, it is usually a name space, but could probably be something
            // else as well in other languages
            var scopeName = node.name.parent

            // We need to check, whether we have an alias for the scope name in this file
            val list = aliases[node.location?.artifactLocation]
            val alias = list?.firstOrNull { it.to == scopeName }?.from
            if (alias != null) {
                scopeName = alias
                // Reconstruct the original name with the alias, so we can resolve declarations with
                // the namespace
                name = Name(name.localName, alias)
            }

            // this is a scoped call. we need to explicitly jump to that particular scope
            val scopes = filterScopes { (it is NameScope && it.name == scopeName) }
            s =
                if (scopes.isEmpty()) {
                    Util.errorWithFileLocation(
                        node,
                        LOGGER,
                        "Could not find the scope $scopeName needed to resolve the call ${node.name}"
                    )
                    scope
                } else {
                    scopes[0]
                }
        }

        return Pair(s, name)
    }

    /**
     * Directly jumps to a given scope. Returns the previous scope. Do not forget to set the scope
     * back to the old scope after performing the actions inside this scope.
     *
     * Handle with care, here be dragons. Should not be exposed outside the cpg-core module.
     */
    @PleaseBeCareful
    internal fun jumpTo(scope: Scope?): Scope? {
        val oldScope = currentScope
        currentScope = scope
        return oldScope
    }

    /**
     * This function can be used to execute multiple statements contained in [init] in the scope of
     * [scope]. The specified scope will be selected using [jumpTo]. The last expression in [init]
     * will also be used as a return value of this function. This can be useful, if you create
     * objects, such as a [Node] inside this scope and want to return it to the calling function.
     */
    fun <T : Any> withScope(scope: Scope?, init: () -> T): T {
        val oldScope = jumpTo(scope)
        val ret = init()
        jumpTo(oldScope)

        return ret
    }

    fun resolveFunctionStopScopeTraversalOnDefinition(
        call: CallExpression
    ): List<FunctionDeclaration> {
        return resolve(currentScope, true) { f -> f.name.lastPartsMatch(call.name) }
    }

    /**
     * Traverses the scope upwards and looks for declarations of type [T] which matches the
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
        noinline predicate: (T) -> Boolean
    ): List<T> {
        return resolve(T::class.java, searchScope, stopIfFound, predicate)
    }

    fun <T : Declaration> resolve(
        klass: Class<T>,
        searchScope: Scope?,
        stopIfFound: Boolean = false,
        predicate: (T) -> Boolean
    ): List<T> {
        var scope = searchScope
        val declarations = mutableListOf<T>()

        while (scope != null) {
            if (scope is ValueDeclarationScope) {
                declarations.addAll(
                    scope.valueDeclarations.filterIsInstance(klass).filter(predicate)
                )
            }

            if (scope is StructureDeclarationScope) {
                var list = scope.structureDeclarations.filterIsInstance(klass).filter(predicate)

                // this was taken over from the old resolveStructureDeclaration.
                // TODO(oxisto): why is this only when the list is empty?
                if (list.isEmpty()) {
                    for (declaration in scope.structureDeclarations) {
                        if (declaration is RecordDeclaration) {
                            list = declaration.templates.filterIsInstance(klass).filter(predicate)
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

            // go upwards in the scope tree
            scope = scope.parent
        }

        return declarations
    }

    /**
     * Resolves function templates of the given [CallExpression].
     *
     * @param scope where we are searching for the FunctionTemplateDeclarations
     * @param call CallExpression we want to resolve an invocation target for
     * @return List of FunctionTemplateDeclaration that match the name provided in the
     *   CallExpression and therefore are invocation candidates
     */
    @JvmOverloads
    fun resolveFunctionTemplateDeclaration(
        call: CallExpression,
        scope: Scope? = currentScope
    ): List<FunctionTemplateDeclaration> {
        return resolve(scope, true) { c -> c.name.lastPartsMatch(call.name) }
    }

    /**
     * Retrieves the [RecordDeclaration] for the given name in the given scope.
     *
     * @param name the name
     * * @param scope the scope. Default is [currentScope]
     *
     * @return the declaration, or null if it does not exist
     */
    fun getRecordForName(name: Name, scope: Scope? = currentScope): RecordDeclaration? {
        return resolve<RecordDeclaration>(scope, true) { it.name.lastPartsMatch(name) }
            .firstOrNull()
    }

    fun addAlias(file: PhysicalLocation.ArtifactLocation, from: Name, to: Name) {
        val list = aliases.computeIfAbsent(file) { mutableSetOf() }

        list += Alias(from, to)
    }

    fun typedefFor(alias: Type): Type? {
        var current = currentScope

        // We need to build a path from the current scope to the top most one. This ensures us that
        // a local definition overwrites / shadows one that was there on a higher scope.
        while (current != null) {
            if (current is ValueDeclarationScope) {
                val decl = current.typedefs[alias]
                if (decl != null) {
                    return decl.type
                }
            }

            current = current.parent
        }

        return null
    }

    /** Returns the current scope for the [ScopeProvider] interface. */
    override val scope: Scope?
        get() = currentScope
}
