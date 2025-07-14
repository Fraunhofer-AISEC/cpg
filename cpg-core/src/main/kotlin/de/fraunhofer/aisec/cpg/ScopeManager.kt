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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.DeclaresType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
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
 * Afterward the currently valid "stack" of scopes within the tree can be accessed.
 *
 * If a language frontend encounters a [Declaration] node, it MUST call [addDeclaration], rather
 * than adding the declaration to the node itself. This ensures that all declarations are properly
 * registered in the scope map and can be resolved later.
 */
class ScopeManager(override var ctx: TranslationContext) : ScopeProvider, ContextProvider {

    /**
     * The top-most scope in the scope tree. This is the root of the tree and is not associated with
     * any particular AST node.
     */
    val globalScope: GlobalScope = GlobalScope()

    /**
     * A map associating each CPG node with its scope. The key type is intentionally a nullable
     * [Node] because the [GlobalScope] is not associated to a CPG node.
     */
    private val scopeMap: MutableMap<Node?, Scope> =
        IdentityHashMap<Node?, Scope>().also { it[null] = globalScope }

    /**
     * A lookup map for each [NameScope] and its associated FQN (as a [Name]). This is mainly needed
     * in the [lookupScope] method, which is extensively used by [extractScope] as a very fast
     * lookup from a [Name] to its [NameScope]. Otherwise, we would need to iterate through all
     * scopes, which can be quite slow.
     */
    @JsonIgnore private val nameScopeMap: MutableMap<Name, NameScope> = mutableMapOf()

    /** True, if the scope manager is currently in a [FunctionScope]. */
    val isInFunction: Boolean
        get() = this.firstScopeOrNull { it is FunctionScope } != null

    /** True, if the scope manager is currently in a [RecordScope], e.g. a class. */
    val isInRecord: Boolean
        get() = this.firstScopeOrNull { it is RecordScope } != null

    /**
     * The currently active scope. When the [ScopeManager] is initialized, this is set to the global
     * scope
     */
    var currentScope: Scope = globalScope
        private set

    /** The current function, according to the scope that is currently active. */
    val currentFunction: FunctionDeclaration?
        get() = this.firstScopeIsInstanceOrNull<FunctionScope>()?.astNode as? FunctionDeclaration

    /** The current block, according to the scope that is currently active. */
    val currentBlock: Block?
        get() = currentScope.astNode as? Block ?: currentScope.astNode?.firstParentOrNull<Block>()

    /**
     * The current method in the active scope tree, this ensures that 'this' keywords are mapped
     * correctly if a method contains a lambda or other types of function declarations
     */
    val currentMethod: MethodDeclaration?
        get() =
            this.firstScopeOrNull { scope: Scope? -> scope?.astNode is MethodDeclaration }?.astNode
                as? MethodDeclaration

    /** The current record, according to the scope that is currently active. */
    val currentRecord: RecordDeclaration?
        get() = this.firstScopeIsInstanceOrNull<RecordScope>()?.astNode as? RecordDeclaration

    val currentNamespace: Name?
        get() {
            val namedScope = this.firstScopeIsInstanceOrNull<NameScope>()
            return if (namedScope is NameScope) namedScope.name else null
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
        val globalScopes = toMerge.map { it.globalScope }
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
            for (entry in manager.nameScopeMap.entries) {
                val existing = nameScopeMap[entry.key]
                if (existing != null) {
                    // merge symbols
                    existing.symbols.mergeFrom(entry.value.symbols)

                    // copy over the typedefs as well just to be sure
                    existing.typedefs.putAll(entry.value.typedefs)

                    // also update the AST node of the existing scope to the "latest" we have seen
                    existing.astNode = entry.value.astNode

                    // now it gets more tricky. we also need to "redirect" the AST nodes in the sub
                    // scope manager to our existing NameScope (currently, they point to their own,
                    // invalid copy of the NameScope).
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
                    nameScopeMap[entry.key] = entry.value
                }
            }

            // Update global scope for all

            // We need to make sure that we do not put the "null" key (aka the global scope) of the
            // individual scope manager into our map, otherwise we would overwrite our merged global
            // scope.
            scopeMap.putAll(manager.scopeMap.filter { it.key != null })

            // free the maps, just to clear up some things. this scope manager will not be used
            // anymore
            manager.nameScopeMap.clear()
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
            val name = scope.astNode?.name
            if (name != null) {
                nameScopeMap[name] = scope
            }
        }

        currentScope.children.add(scope)
        scope.parent = currentScope

        currentScope = scope
    }

    /**
     * This function, in combination with [leaveScope] is the main interaction point with the scope
     * manager for language frontends. Every time a language frontend handles a node that begins a
     * new scope, this function needs to be called. Appropriate scopes will then be created
     * on-the-fly, if they do not exist.
     *
     * The scope manager has an internal association between the type of scope, e.g. a [LocalScope]
     * and the CPG node it represents, e.g. a [Block].
     *
     * Afterward, all calls to [addDeclaration] will be distributed to the [DeclarationHolder] that
     * is currently in-scope.
     */
    fun enterScope(nodeToScope: Node) {
        // check, if the node does not have an entry in the scope map
        if (!scopeMap.containsKey(nodeToScope)) {
            val newScope =
                when (nodeToScope) {
                    is WhileStatement,
                    is DoStatement,
                    is AssertStatement,
                    is ForStatement,
                    is ForEachStatement,
                    is SwitchStatement,
                    is TryStatement,
                    is IfStatement,
                    is CatchClause,
                    is CollectionComprehension,
                    is Block -> LocalScope(nodeToScope)
                    is FunctionDeclaration -> FunctionScope(nodeToScope)
                    is RecordDeclaration -> RecordScope(nodeToScope)
                    is TemplateDeclaration -> TemplateScope(nodeToScope)
                    is TranslationUnitDeclaration -> FileScope(nodeToScope)
                    is NamespaceDeclaration -> newNamespaceIfNecessary(nodeToScope)
                    else -> {
                        LOGGER.error(
                            "No known scope for AST node of type {}",
                            nodeToScope.javaClass,
                        )
                        return
                    }
                }

            if (newScope != null) {
                // push the new scope
                pushScope(newScope)
                newScope.scopedName = currentNamespace?.toString()
            }
        }

        // We need to check again because of the newNamespaceIfNecessary function
        var existing = scopeMap[nodeToScope]
        if (existing != null) {
            currentScope = existing
        }
    }

    /**
     * A small internal helper function used by [enterScope] to create a [NamespaceScope].
     *
     * The issue with name scopes, such as a namespace, is that it can exist across several files,
     * i.e. translation units, represented by different [NamespaceDeclaration] nodes. But, in order
     * to make namespace resolution work across files, only one [NameScope] must exist that holds
     * all declarations, such as classes, independently of the translation units. Therefore, we need
     * to check, whether such as node already exists. If it does already exist:
     * - we update the scope map so that the current [NamespaceDeclaration] points to the existing
     *   [NamespaceScope]
     * - we return null, indicating to [enterScope], that no new scope needs to be pushed by
     *   [enterScope].
     *
     * Otherwise, we return a new namespace scope.
     */
    private fun newNamespaceIfNecessary(nodeToScope: NamespaceDeclaration): NamespaceScope? {
        val existingScope =
            filterScopes { it is NamespaceScope && it.name == nodeToScope.name }.firstOrNull()

        return if (existingScope != null) {
            // update the AST node to this namespace declaration
            existingScope.astNode = nodeToScope

            // make it also available in the scope map. Otherwise, we cannot leave the
            // scope
            scopeMap[nodeToScope] = existingScope

            null
        } else {
            NamespaceScope(nodeToScope)
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
                    nodeToLeave.javaClass,
                )
            } else {
                Util.errorWithFileLocation(
                    nodeToLeave,
                    LOGGER,
                    "Node of type {} is not associated with a scope.",
                    nodeToLeave.javaClass,
                )
            }

            return null
        }

        // go back to the parent of the scope we just left
        currentScope = leaveScope.parent ?: globalScope
        return leaveScope
    }

    /**
     * This function MUST be called when a language frontend first handles a [Declaration]. It adds
     * a declaration to the scope manager, taking into account the currently active scope.
     *
     * @param declaration the declaration to add
     */
    fun addDeclaration(declaration: Declaration) {
        currentScope.addSymbol(declaration.symbol, declaration)
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
    fun firstScopeOrNull(searchScope: Scope = currentScope, predicate: Predicate<Scope>): Scope? {
        // start at searchScope
        var scope: Scope? = searchScope

        while (scope != null) {
            if (predicate.test(scope)) {
                return scope
            }

            // go upwards in the scope tree
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
        searchScope: Scope = currentScope
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

    /**
     * This function looks up scope by its FQN. This only works for [NameScope]s.
     *
     * Note: Beware that this only does a very simple lookup in the scope table and DOES NOT take
     * into account any eventual aliases that might be active in the current scope.
     */
    fun lookupScope(fqn: Name): NameScope? {
        return this.nameScopeMap[fqn]
    }

    /**
     * This function retrieves the [LabelStatement] associated with the [labelString]. This depicts
     * the feature of some languages to attach a label to a point in the source code and use it as
     * the target for control flow manipulation, e.g. [BreakStatement], [GotoStatement].
     */
    fun getLabelStatement(labelString: String?): LabelStatement? {
        if (labelString == null) return null
        var labelStatement: LabelStatement?
        var searchScope: Scope? = currentScope
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
        // update the AST node to this translation unit declaration
        global.astNode = declaration
        currentScope = global
    }

    /**
     * Adds typedefs to a [Scope]. The language frontend needs to decide on the scope of the
     * typedef. Most likely, typedefs are global. Therefore, the [GlobalScope] is set as default.
     */
    fun addTypedef(typedef: TypedefDeclaration, scope: Scope = globalScope) {
        scope.addTypedef(typedef)
    }

    /**
     * This class represents the result of the [extractScope] operation. It contains a [scope]
     * object, if a scope was found and the [adjustedName] that is normalized if any aliases were
     * found during scope extraction.
     */
    data class ScopeExtraction(val scope: Scope?, val adjustedName: Name)

    /**
     * This function extracts a scope for the [Name], e.g. if the name is fully qualified (wrapped
     * in a [ScopeExtraction] object. `null` is returned if a scope was specified, but does not
     * exist as a [Scope] object.
     *
     * The returned object contains the extracted scope and a name that is adjusted by possible
     * import aliases. The extracted scope is "responsible" for the name (e.g. declares the parent
     * namespace) and the returned name only differs from the provided name if aliasing was involved
     * at the node location (e.g. because of imports).
     *
     * Note: Currently only *fully* qualified names are properly resolved. This function will
     * probably return imprecise results for partially qualified names, e.g. if a name `A` inside
     * `B` points to `A::B`, rather than to `A`.
     *
     * @param node the nodes name references a namespace constituted by a scope
     * @param scope the current scope relevant for the name resolution, e.g. parent of node
     * @return a [ScopeExtraction] object with the scope of node.name and the alias-adjusted name
     */
    fun extractScope(
        node: HasNameAndLocation,
        language: Language<*> = node.language,
        scope: Scope? = currentScope,
    ): ScopeExtraction? {
        return extractScope(node.name, language, node.location, scope)
    }

    /**
     * This function extracts a scope for the [Name], e.g. if the name is fully qualified. `null` is
     * returned, if no scope can be extracted.
     *
     * The pair returns the extracted scope and a name that is adjusted by possible import aliases.
     * The extracted scope is "responsible" for the name (e.g. declares the parent namespace) and
     * the returned name only differs from the provided name if aliasing was involved at the node
     * location (e.g. because of imports).
     *
     * Note: Currently only *fully* qualified names are properly resolved. This function will
     * probably return imprecise results for partially qualified names, e.g. if a name `A` inside
     * `B` points to `A::B`, rather than to `A`.
     *
     * @param name the name
     * @param scope the current scope relevant for the name resolution, e.g. parent of node
     * @return a pair with the scope of node.name and the alias-adjusted name
     */
    fun extractScope(
        name: Name,
        language: Language<*>,
        location: PhysicalLocation? = null,
        scope: Scope? = currentScope,
    ): ScopeExtraction? {
        var n = name
        var s: Scope? = null
        val scopeName = n.parent

        // First, we need to check, whether we have some kind of scoping.
        if (scopeName != null) {
            // This is a rather ugly hack, but we need to check whether the scopeName is the same as
            // the current scope. This is unfortunately necessary since "toType()" returns a type
            // with an FQN that is scoped to the current scope and we need to change that :(
            if (scopeName == scope?.name) {
                return ScopeExtraction(scope, name)
            }

            // We need to check, whether we have an alias for the name's parent in this file
            val scope = lookupScopeByName(scopeName, language, scope)

            if (scope == null) {
                Util.warnWithFileLocation(
                    location,
                    LOGGER,
                    "Could not find the scope $scopeName needed to resolve $n",
                )
                return null
            }
            s = scope
            n = adjustNameIfNecessary(scope.name, n.parent, n)
        }

        return ScopeExtraction(s, n)
    }

    /**
     * This function looks up a [Scope] by its [name] relative to [startScope]. The reason why this
     * is necessary is that the [name] could potentially include aliases set by an
     * [ImportDeclaration] and therefore can not directly be found in the [nameScopeMap].
     *
     * It works by splitting the name into its parts and then iteratively looking up the scope for
     * each part, starting at the "beginning". For example if we have a name `A::B::C`, we first
     * look up the scope for `A`, then the scope for `B` in the scope of `A`, and finally the scope
     * for `C`.
     *
     * If no scope is found at any point in the chain, `null` is returned.
     *
     * @param name the name to look up
     * @param startScope the scope to start the lookup in
     */
    fun lookupScopeByName(name: Name, language: Language<*>?, startScope: Scope?): Scope? {
        val parts = name.splitTo(mutableListOf())
        var part: Name? = name
        var scope = startScope

        while (parts.isNotEmpty() && scope != null) {
            // Take the "last" entry in the list as this is the less specific one. The order of the
            // split list of the name "A::B::C" is ["A::B::C", "A::B", "A"]. We want to process them
            // in reverse order ("A", "A::B", "A::B::C").
            part = parts.removeLast()

            // We need to look for everything that declares some sort of type or introduces a type
            // alias. We need to map the declaration to a scope and then hope that its unique. We
            // need to do it in this order because there can be multiple declarations of the same
            // namespace (in different files), but they all (should) point to the same scope.
            scope =
                scope
                    .lookupSymbol(part.localName, languageOnly = language) {
                        it is NamespaceDeclaration ||
                            it is RecordDeclaration ||
                            it is TypedefDeclaration
                    }
                    .map {
                        // If it is a typedef, we need to use the type's name instead of the
                        // declaration's name. Otherwise, we just take the name of the declaration
                        // to look up the corresponding scope.
                        nameScopeMap[
                            if (it is TypedefDeclaration) {
                                it.type.name
                            } else {
                                it.name
                            }]
                    }
                    .toSet()
                    .singleOrNull()
        }

        return scope
    }

    private fun adjustNameIfNecessary(
        newParentName: Name?,
        oldParentName: Name?,
        name: Name,
    ): Name =
        if (newParentName != oldParentName) {
            Name(name.localName, newParentName, delimiter = name.delimiter)
        } else {
            name
        }

    /**
     * Directly jumps to a given scope. Returns the previous scope. Do not forget to set the scope
     * back to the old scope after performing the actions inside this scope.
     *
     * Handle with care, here be dragons. Should not be exposed outside the cpg-core module.
     */
    @PleaseBeCareful
    internal fun jumpTo(scope: Scope?): Scope? {
        return if (scope != null) {
            val oldScope = currentScope
            currentScope = scope
            return oldScope
        } else {
            null
        }
    }

    /**
     * This function can be used to execute multiple statements contained in [init] in the scope of
     * [scope]. The specified scope will be selected using [jumpTo]. The last expression in [init]
     * will also be used as a return value of this function. This can be useful, if you create
     * objects, such as a [Node] inside this scope and want to return it to the calling function.
     */
    fun <T : Any> withScope(scope: Scope?, init: (scope: Scope?) -> T): T {
        val oldScope = jumpTo(scope)
        val ret = init(scope)
        jumpTo(oldScope)

        return ret
    }

    /**
     * Retrieves the [RecordDeclaration] for the given name in the given scope.
     *
     * @param name the name
     * * @param scope the scope. Default is [currentScope]
     *
     * @return the declaration, or null if it does not exist
     */
    fun getRecordForName(name: Name, language: Language<*>): RecordDeclaration? {
        return lookupSymbolByName(name, language)
            .filterIsInstance<RecordDeclaration>()
            .singleOrNull()
    }

    fun typedefFor(
        alias: Name,
        scope: Scope? = currentScope,
        prefix: Name? = currentNamespace,
    ): Type? {
        var current: Scope? = scope

        // We need to build a path from the current scope to the top most one. This ensures us that
        // a local definition overwrites / shadows one that was there on a higher scope.
        while (current != null) {
            // This is a little bit of a hack to support partial FQN resolution at least with
            // typedefs, but it's not really ideal.
            // And this also should be merged with the scope manager logic when resolving names.
            //
            // The better approach would be to harmonize the FQN of all types in one pass before
            // all this happens.
            //
            // This process has several steps:

            // First, try to look up the name either by its FQN (if it is qualified) or make it
            // qualified based on the current namespace
            val key =
                current.typedefs.keys.firstOrNull {
                    var lookupName = alias

                    // If the lookup name is already a FQN, we can use the name directly
                    lookupName =
                        if (lookupName.isQualified()) {
                            lookupName
                        } else {
                            // Otherwise, we want to make an FQN out of it using the current
                            // namespace
                            prefix?.fqn(lookupName.localName) ?: lookupName
                        }

                    it.lastPartsMatch(lookupName)
                }
            if (key != null) {
                return current.typedefs[key]?.type
            }

            // Next, do a local lookup, to see if we have a typedef our current scope
            // (only do this if the name is not qualified)
            if (!alias.isQualified()) {
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
    override val scope: Scope
        get() = currentScope

    /**
     * A convenience function to call [lookupSymbolByName] with the properties of [node]. The
     * arguments [scope] and [predicate] are forwarded.
     */
    fun lookupSymbolByNodeName(
        node: Node,
        scope: Scope = node.scope ?: currentScope,
        replaceImports: Boolean = true,
        predicate: ((Declaration) -> Boolean)? = null,
    ): List<Declaration> {
        return lookupSymbolByName(
            node.name,
            node.language,
            node.location,
            scope,
            replaceImports = replaceImports,
            predicate = predicate,
        )
    }

    /**
     * A convenience function to call [lookupSymbolByName] with the properties of [node].
     * Additionally, it adds a predicate to the search that the declaration must be of type [T].
     */
    inline fun <reified T : Declaration> lookupSymbolByNodeNameOfType(
        node: Node,
        scope: Scope = node.scope ?: currentScope,
        replaceImports: Boolean = true,
    ): List<T> {
        return lookupSymbolByName(node.name, node.language, node.location, scope, replaceImports) {
                it is T
            }
            .filterIsInstance<T>()
    }

    /**
     * This function tries to convert a [Node.name] into a [Symbol] and then performs a lookup of
     * this symbol. This can either be an "unqualified lookup" if [name] is not qualified or a
     * "qualified lookup" if [Name.isQualified] is true. In the unqualified case the lookup starts
     * in [startScope], in the qualified case we use [extractScope] to find the appropriate scope
     * and need to restrict our search to this particular scope.
     *
     * This function can return a list of multiple declarations in order to check for things like
     * function overloading. But it will only return list of declarations within the same scope; the
     * list cannot be spread across different scopes.
     *
     * This means that as soon one or more declarations (of the matching [language]) for the symbol
     * are found in a "local" scope, these shadow all other occurrences of the same / symbol in a
     * "higher" scope and only the ones from the lower ones will be returned.
     */
    fun lookupSymbolByName(
        name: Name,
        language: Language<*>,
        location: PhysicalLocation? = null,
        startScope: Scope? = currentScope,
        replaceImports: Boolean = true,
        predicate: ((Declaration) -> Boolean)? = null,
    ): List<Declaration> {
        val extractedScope = extractScope(name, language, location, startScope)
        val scope: Scope?
        val n: Name
        if (extractedScope == null) {
            // the scope does not exist at all
            return listOf()
        } else {
            scope = extractedScope.scope
            n = extractedScope.adjustedName
        }

        // We need to differentiate between a qualified and unqualified lookup. We have a qualified
        // lookup, if the scope is not null. In this case we need to stay within the specified scope
        val list =
            when {
                scope != null -> {
                    scope
                        .lookupSymbol(
                            n.localName,
                            languageOnly = language,
                            qualifiedLookup = true,
                            replaceImports = replaceImports,
                            predicate = predicate,
                        )
                        .toMutableList()
                }
                else -> {
                    // Otherwise, we can look up the symbol alone (without any FQN) starting from
                    // the startScope
                    startScope
                        ?.lookupSymbol(
                            n.localName,
                            languageOnly = language,
                            replaceImports = replaceImports,
                            predicate = predicate,
                        )
                        ?.toMutableList() ?: mutableListOf()
                }
            }

        // If we have both the definition and the declaration of a function declaration in our list,
        // we chose only the definition
        val it = list.iterator()
        while (it.hasNext()) {
            val decl = it.next()
            if (decl is FunctionDeclaration) {
                val definition = decl.definition
                if (!decl.isDefinition && definition != null && definition in list) {
                    it.remove()
                }
            }
        }

        return list
    }

    /**
     * This function tries to look up the symbol contained in [name] (using [lookupSymbolByName])
     * and returns a [DeclaresType] node, if this name resolved to something which declares a type.
     *
     * In case that the lookup returns more than one symbol, this function will emit a warning and
     * return the first symbol found.
     */
    fun lookupTypeSymbolByName(
        name: Name,
        language: Language<*>,
        startScope: Scope?,
    ): DeclaresType? {
        var symbols =
            lookupSymbolByName(name = name, language = language, startScope = startScope) {
                    it is DeclaresType
                }
                .filterIsInstance<DeclaresType>()

        // We need to have a single match, otherwise we have an ambiguous type. We emit a warning
        // here, but we still have to take one of the symbols, because otherwise the type system
        // things the type does not exist and tries to infer one.
        if (symbols.size > 1) {
            LOGGER.warn(
                "Lookup of type {} returned more than one symbol which declares a type, this is an ambiguity and the following analysis might not be correct.",
                name,
            )
            return symbols.firstOrNull()
        }

        return symbols.singleOrNull()
    }

    /**
     * Returns the [TranslationUnitDeclaration] that should be used for inference, especially for
     * global declarations.
     *
     * @param TypeToInfer the type of the node that should be inferred
     * @param source the source that was responsible for the inference
     */
    fun <TypeToInfer : Node> translationUnitForInference(source: Node): TranslationUnitDeclaration {
        return source.language.translationUnitForInference<TypeToInfer>(source)
    }
}

/**
 * [SignatureResult] will be the result of the function [FunctionDeclaration.matchesSignature] which
 * calculates whether the provided [CallExpression] will match the signature of the current
 * [FunctionDeclaration].
 */
sealed class SignatureResult(open val casts: List<CastResult>? = null) {
    val ranking: Int
        get() {
            var i = 0
            for (cast in this.casts ?: listOf()) {
                i += cast.depthDistance
            }
            return i
        }

    val isDirectMatch: Boolean
        get() {
            return this.casts?.all { it is DirectMatch } == true
        }
}

data object IncompatibleSignature : SignatureResult()

data class SignatureMatches(override val casts: List<CastResult>) : SignatureResult(casts)

fun FunctionDeclaration.matchesSignature(
    signature: List<Type>,
    arguments: List<Expression>? = null,
    useDefaultArguments: Boolean = false,
): SignatureResult {
    val casts = mutableListOf<CastResult>()

    var remainingArguments = signature.size

    // Loop through all parameters of this function
    for ((i, param) in this.parameters.withIndex()) {
        // Once we are in variadic mode, all arguments match
        if (param.isVariadic) {
            remainingArguments = 0
            break
        }

        // Try to find a matching call argument by index
        val type = signature.getOrNull(i)

        // Yay, we still have arguments/types left
        if (type != null) {
            // Check, if we can cast the arg into our target type; and if, yes, what is
            // the "distance" to the base type. We need this to narrow down the type during
            // resolving
            val match = type.tryCast(param.type, arguments?.getOrNull(i), param)
            if (match == CastNotPossible) {
                return IncompatibleSignature
            }

            casts += match
            remainingArguments--
        } else {
            // If the type (argument) is null, this might signal that we have less arguments than
            // our function signature, so we are likely not a match. But, the function could still
            // have a default argument (if the language supports it).
            if (useDefaultArguments) {
                val defaultParam = this.defaultParameters[i]
                if (defaultParam != null) {
                    casts += DirectMatch

                    // We have a matching default parameter, let's decrement the remaining arguments
                    // and continue matching
                    remainingArguments--
                    continue
                }
            }

            // We did not have a matching default parameter, or we don't have/support default
            // parameters, so our matching is done here
            return IncompatibleSignature
        }
    }

    // TODO(oxisto): In some languages, we can also have named parameters, but this is not yet
    //  supported

    // If we still have remaining arguments at the end of the matching check, the signature is
    // incompatible
    return if (remainingArguments > 0) {
        IncompatibleSignature
    } else {
        // Otherwise, return the matching cast results
        SignatureMatches(casts)
    }
}

/**
 * This is the result of [SymbolResolver.resolveWithArguments]. It holds all necessary intermediate
 * results (such as [candidateFunctions], [viableFunctions]) as well as the final result (see
 * [bestViable]) of the call resolution.
 */
data class CallResolutionResult(
    /** The original expression that triggered the resolution. Most likely a [CallExpression]. */
    val source: Expression,

    /** The arguments that were supplied to the expression. */
    val arguments: List<Expression>,

    /**
     * A set of candidate symbols we discovered based on the [CallExpression.callee] (using
     * [ScopeManager.lookupSymbolByName]), more specifically a list of [FunctionDeclaration] nodes.
     */
    var candidateFunctions: Set<FunctionDeclaration>,

    /**
     * A set of functions, that restrict the [candidateFunctions] to those whose signature match.
     */
    var viableFunctions: Set<FunctionDeclaration>,

    /**
     * A helper map to store the [SignatureResult] of each call to
     * [FunctionDeclaration.matchesSignature] for each function in [viableFunctions].
     */
    var signatureResults: Map<FunctionDeclaration, SignatureResult>,

    /**
     * This set contains the best viable function(s) of the [viableFunctions]. Ideally this is only
     * one, but because of ambiguities or other factors, this can contain multiple functions.
     */
    var bestViable: Set<FunctionDeclaration>,

    /** The kind of success this resolution had. */
    var success: SuccessKind,

    /**
     * The actual start scope of the resolution, after [ScopeManager.extractScope] is called on the
     * callee. This can differ from the original start scope parameter handed to
     * [SymbolResolver.resolveWithArguments] if the callee contains an FQN.
     */
    var actualStartScope: Scope?,
) {
    /**
     * This enum holds information about the kind of success this call resolution had. For example,
     * whether it was successful without any errors or if an ambiguous result was returned.
     */
    enum class SuccessKind {
        /**
         * The call resolution was successful, and we have identified the best viable function(s).
         *
         * Ideally, we have only one function in [bestViable], but it could be that we still have
         * multiple functions in this list. The most common scenario for this is if we have a member
         * call to an interface, and we know at least partially which implemented classes could be
         * in the [MemberExpression.base]. In this case, all best viable functions of each of the
         * implemented classes are contained in [bestViable].
         */
        SUCCESSFUL,

        /**
         * The call resolution was problematic, i.e., some error occurred, or we were running into
         * an unexpected state. An example would be that we arrive at multiple [candidateFunctions]
         * for a language that does not have [HasFunctionOverloading].
         *
         * We try to store the most accurate result(s) possible in [bestViable].
         */
        PROBLEMATIC,

        /**
         * The call resolution was ambiguous in a way that we cannot decide between one or more
         * [viableFunctions]. This can happen if we have multiple functions that have the same
         * [SignatureResult.ranking]. A real compiler could not differentiate between those two
         * functions and would throw a compile error.
         *
         * We store all ambiguous functions in [bestViable].
         */
        AMBIGUOUS,

        /**
         * The call resolution was unsuccessful, we could not find a [bestViable] or even a list of
         * [viableFunctions] out of the [candidateFunctions].
         *
         * [bestViable] is empty in this case.
         */
        UNRESOLVED,
    }
}
