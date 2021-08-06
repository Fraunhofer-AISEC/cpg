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

import de.fraunhofer.aisec.cpg.ExperimentalGolang
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguageFrontend
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
import java.util.function.Function
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.stream.Collectors
import org.slf4j.LoggerFactory

/**
 * The scope manager builds a multitree-structure of scopes associated to a scope. These Scopes
 * capture the validity of certain (Variable-, Field-, Record-)declarations but are also used to
 * identify outer scopes that should be the target of a jump (continue, break, throw).
 *
 * [enterScope] and [leaveScope] can be used to enter the tree of scopes and then sitting at a path,
 * access the currently valid "stack" of scopes.
 */
class ScopeManager {
    /**
     * A map associating each CPG node with its scope. The key type is intentionally a nullable
     * [Node] because the [GlobalScope] is not associated to a CPG node when it is first created. It
     * is later associated using the [resetToGlobal] function.
     */
    private val scopeMap: MutableMap<Node?, Scope> = IdentityHashMap()

    /** A lookup map for each scope and its associated FQN. */
    private val fqnScopeMap: MutableMap<String, Scope> = IdentityHashMap()

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
        get() = this.getFirstScopeThat { scope: Scope? -> scope is BlockScope } != null
    /** True, if the scope manager is currently in a [FunctionScope]. */
    val isInFunction: Boolean
        get() = this.getFirstScopeThat { scope: Scope? -> scope is FunctionScope } != null
    /** True, if the scope manager is currently in a [RecordScope], e.g. a class. */
    val isInRecord: Boolean
        get() = this.getFirstScopeThat { scope: Scope? -> scope is RecordScope } != null

    /** The current block, according to the scope that is currently active. */
    val currentBlock: CompoundStatement?
        get() =
            this.getFirstScopeThat { scope: Scope? -> scope is BlockScope }?.astNode as?
                CompoundStatement
    /** The current function, according to the scope that is currently active. */
    val currentFunction: FunctionDeclaration?
        get() =
            this.getFirstScopeThat { scope: Scope? -> scope is FunctionScope }?.astNode as?
                FunctionDeclaration
    /** The current record, according to the scope that is currently active. */
    val currentRecord: RecordDeclaration?
        get() =
            this.getFirstScopeThat { scope: Scope? -> scope is RecordScope }?.astNode as?
                RecordDeclaration

    /**
     * Combines the state of several scope managers into this one. Primarily used in combination
     * with concurrent frontends.
     *
     * @param toMerge The scope managers to merge into this one
     */
    fun mergeFrom(toMerge: Collection<ScopeManager>) {
        val globalScopes =
            toMerge
                .stream()
                .map { s: ScopeManager -> s.scopeMap[null] }
                .filter { obj: Scope? -> GlobalScope::class.java.isInstance(obj) }
                .map { obj: Scope? -> GlobalScope::class.java.cast(obj) }
                .collect(Collectors.toList())
        val currGlobalScope = scopeMap[null]
        if (currGlobalScope !is GlobalScope) {
            LOGGER.error("Scope for null node is not a GlobalScope or is null")
        } else {
            currGlobalScope.mergeFrom(globalScopes)
            scopeMap[null] = currGlobalScope
        }
        for (manager in toMerge) {
            scopeMap.putAll(manager.scopeMap)
            fqnScopeMap.putAll(manager.fqnScopeMap)
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
        if (scope is NameScope || scope is RecordScope) {
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
     * new scope, this function needs to be called.
     *
     * Afterwards, all calls to [addDeclaration] will be distributed to the
     * [de.fraunhofer.aisec.cpg.graph.DeclarationHolder] that is currently in-scope.
     *
     * The scope manager has an internal association between the type of scope, e.g. a [BlockScope]
     * and the CPG node it represents, e.g. a [CompoundStatement].
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
    private fun newNameScopeIfNecessary(nodeToScope: Node): NameScope? {
        val existing =
            (currentScope as? StructureDeclarationScope)?.structureDeclarations?.firstOrNull {
                x: Declaration ->
                x.name == nodeToScope.name
            }

        return if (existing != null) {
            val oldScope = scopeMap[existing]

            // might still be non-existing in some cases because this is hacky
            if (oldScope != null) {
                // update the AST node to this namespace declaration
                oldScope.astNode = nodeToScope

                // set current scope
                currentScope = oldScope

                // make it also available in the scope map, otherwise, we cannot leave the
                // scope
                scopeMap[oldScope.astNode] = oldScope
                null
            } else {
                NameScope(nodeToScope, currentNamePrefix, lang!!.namespaceDelimiter)
            }
        } else {
            NameScope(nodeToScope, currentNamePrefix, lang!!.namespaceDelimiter)
        }
    }

    fun enterScopeIfExists(nodeToScope: Node?) {
        if (scopeMap.containsKey(nodeToScope)) {
            val scope = scopeMap[nodeToScope]

            // we need a special handling of name spaces, because
            // thy are associated to more than one AST node
            if (scope is NameScope) {
                // update AST (see enterScope for an explanation)
                scope.astNode = nodeToScope
            }
            currentScope = scope
        }
    }

    /**
     * Remove all scopes above the specified one including the specified one.
     *
     * @param nodeToLeave
     * - The scope is defined by its astNode
     * @return the scope is returned for processing
     */
    fun leaveScope(nodeToLeave: Node): Scope? {
        // Check to return as soon as we know that there is no associated scope, this check could be
        // omitted
        // but will increase runtime if leaving a node without scope will happen often.
        if (!scopeMap.containsKey(nodeToLeave)) {
            return null
        }
        val leaveScope = getFirstScopeThat { scope: Scope? -> scope?.astNode == nodeToLeave }
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
        currentScope = leaveScope.parent
        return leaveScope
    }

    fun leaveScopeIfExists(nodeToLeave: Node?): Scope? {
        val leaveScope = scopeMap.get(nodeToLeave)
        if (leaveScope != null) {
            currentScope = leaveScope.parent
        }
        return leaveScope
    }

    /**
     * This function tries to find the first scope that satisfies the condition specified in
     * [predicate]. It starts searching in the [searchScope], moving up-wards using the
     * [Scope.parent] attribute.
     */
    @JvmOverloads
    fun getFirstScopeThat(searchScope: Scope? = currentScope, predicate: Predicate<Scope>): Scope? {
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

    fun getScopesThat(predicate: Predicate<Scope?>): List<Scope?> {
        val scopes: MutableList<Scope?> = ArrayList()
        for (scope in scopeMap.values) if (predicate.test(scope)) scopes.add(scope)
        return scopes
    }

    fun <T> getUniqueScopesThat(
        predicate: Predicate<Scope?>,
        uniqueProperty: Function<Scope?, T>
    ): List<Scope?> {
        val scopes: MutableList<Scope?> = ArrayList()
        val seen: MutableSet<T> = HashSet()
        for (scope in scopeMap.values) {
            if (predicate.test(scope) && seen.add(uniqueProperty.apply(scope))) {
                scopes.add(scope)
            }
        }
        return scopes
    }

    fun addBreakStatement(breakStatement: BreakStatement) {
        if (breakStatement.label == null) {
            val scope = getFirstScopeThat { scope: Scope? -> scope?.isBreakable() == true }
            if (scope == null) {
                LOGGER.error(
                    "Break inside of unbreakable scope. The break will be ignored, but may lead " +
                        "to an incorrect graph. The source code is not valid or incomplete."
                )
                return
            }
            (scope as IBreakable).addBreakStatement(breakStatement)
        } else {
            val labelStatement = getLabelStatement(breakStatement.label)
            if (labelStatement != null) {
                val scope = getScopeOfStatement(labelStatement.subStatement)
                (scope as IBreakable?)!!.addBreakStatement(breakStatement)
            }
        }
    }

    fun addContinueStatement(continueStatement: ContinueStatement) {
        if (continueStatement.label == null) {
            val scope = getFirstScopeThat { scope: Scope? -> scope?.isContinuable() == true }
            if (scope == null) {
                LOGGER.error(
                    "Continue inside of not continuable scope. The continue will be ignored, but may lead " +
                        "to an incorrect graph. The source code is not valid or incomplete."
                )
                return
            }
            (scope as IContinuable).addContinueStatement(continueStatement)
        } else {
            val labelStatement = getLabelStatement(continueStatement.label)
            if (labelStatement != null) {
                val scope = getScopeOfStatement(labelStatement.subStatement)
                (scope as IContinuable?)!!.addContinueStatement(continueStatement)
            }
        }
    }

    fun addLabelStatement(labelStatement: LabelStatement?) {
        currentScope!!.addLabelStatement(labelStatement)
    }

    fun getLabelStatement(labelString: String?): LabelStatement? {
        var labelStatement: LabelStatement?
        var searchScope = currentScope
        while (searchScope != null) {
            labelStatement = searchScope.getLabelStatements().get(labelString)
            if (labelStatement != null) {
                return labelStatement
            }
            searchScope = searchScope.parent
        }
        return null
    }

    /**
     * TO remove a valueDeclaration in the cases were the declaration gets replaced by something
     * else
     *
     * @param declaration
     */
    fun removeDeclaration(declaration: Declaration?) {
        var toIterate = currentScope
        do {
            if (toIterate is ValueDeclarationScope) {
                val declScope = toIterate
                if (declScope.valueDeclarations.contains(declaration)) {
                    declScope.valueDeclarations.remove(declaration)
                    if (declScope.getAstNode() is RecordDeclaration) {
                        val rec = declScope.getAstNode() as RecordDeclaration
                        rec.removeField(declaration as FieldDeclaration?)
                        rec.removeMethod(declaration as MethodDeclaration?)
                        rec.removeConstructor(declaration as ConstructorDeclaration?)
                        rec.removeRecord(declaration as RecordDeclaration?)
                    } else if (declScope.getAstNode() is FunctionDeclaration) {
                        (declScope.getAstNode() as FunctionDeclaration).removeParameter(
                            declaration as ParamVariableDeclaration?
                        )
                    } else if (declScope.getAstNode() is Statement) {
                        if (declaration is VariableDeclaration) {
                            (declScope.getAstNode() as Statement).removeLocal(
                                declaration as VariableDeclaration?
                            )
                        }
                    } else if (declScope.getAstNode() is EnumDeclaration) {
                        (declScope.getAstNode() as EnumDeclaration).entries.remove(declaration)
                    }
                }
            }
            toIterate = toIterate!!.getParent()
        } while (toIterate != null)
    }

    fun resetToGlobal(declaration: TranslationUnitDeclaration?) {
        val global = getFirstScopeThat { scope: Scope? -> scope is GlobalScope } as GlobalScope?
        if (global != null) {
            // update the AST node to this translation unit declaration
            global.astNode = declaration
            currentScope = global
        }
    }

    /**
     * Adds a declaration to the CPG by taking into account the currently active scope, and add the
     * Declaration to the appropriate node. This function will keep the declaration in the Scopes
     * and allows the ScopeManager by himself to resolve ValueDeclarations through
     * [ScopeManager.resolve].
     *
     * @param declaration
     */
    fun addDeclaration(declaration: Declaration?) {
        when (declaration) {
            is ProblemDeclaration, is IncludeDeclaration -> {
                // directly add problems and includes to the global scope
                val globalScope =
                    getFirstScopeThat { scope: Scope? -> scope is GlobalScope } as GlobalScope?
                globalScope!!.addDeclaration(declaration)
            }
            is ValueDeclaration -> {
                val scopeForValueDeclaration =
                    getFirstScopeThat { scope: Scope? -> scope is ValueDeclarationScope } as
                        ValueDeclarationScope?
                scopeForValueDeclaration!!.addValueDeclaration(declaration as ValueDeclaration?)
            }
            is RecordDeclaration,
            is NamespaceDeclaration,
            is EnumDeclaration,
            is TemplateDeclaration -> {
                val scopeForStructureDeclaration =
                    getFirstScopeThat { scope: Scope? -> scope is StructureDeclarationScope } as
                        StructureDeclarationScope?
                scopeForStructureDeclaration!!.addDeclaration(declaration)
            }
        }
    }

    fun addTypedef(typedef: TypedefDeclaration?) {
        val scope =
            getFirstScopeThat { obj: Scope? ->
                ValueDeclarationScope::class.java.isInstance(obj)
            } as
                ValueDeclarationScope?
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

    val currentTypedefs: List<TypedefDeclaration>
        get() = getCurrentTypedefs(currentScope)

    private fun getCurrentTypedefs(scope: Scope?): List<TypedefDeclaration> {
        val curr: MutableList<TypedefDeclaration> = ArrayList()
        if (scope is ValueDeclarationScope) {
            curr.addAll(scope.typedefs)
        }
        if (scope!!.getParent() != null) {
            for (parentTypedef in getCurrentTypedefs(scope.getParent())) {
                if (curr.stream().map { obj: TypedefDeclaration -> obj.alias }.noneMatch { o: Type?
                        ->
                        parentTypedef.alias.equals(o)
                    }
                ) {
                    curr.add(parentTypedef)
                }
            }
        }
        return curr
    }

    val currentNamePrefix: String
        get() {
            val namedScope = getFirstScopeThat { scope: Scope? ->
                scope is NameScope || scope is RecordScope
            }
            return if (namedScope is NameScope) namedScope.namePrefix
            else (namedScope as? RecordScope)?.getAstNode()?.name ?: ""
        }
    val currentNamePrefixWithDelimiter: String
        get() {
            var namePrefix = currentNamePrefix
            if (!namePrefix.isEmpty()) {
                namePrefix += lang!!.namespaceDelimiter
            }
            return namePrefix
        }

    fun resolve(ref: DeclaredReferenceExpression): ValueDeclaration? {
        return resolve(currentScope, ref)
    }

    /**
     * Tries to resolve a function in a call expression.
     *
     * @param call the call expression
     * @return a list of possible functions
     */
    @OptIn(ExperimentalGolang::class)
    @JvmOverloads
    fun resolveFunction(
        call: CallExpression,
        scope: Scope? = currentScope
    ): List<FunctionDeclaration> {
        var s = scope

        // First, we need to check, whether we have some kind of scoping. this is currently only
        // limited to the Go frontend, but should work for other languages as well - but is not
        // tested.
        if (lang is GoLanguageFrontend &&
                call.fqn != null &&
                call.fqn.contains((lang as GoLanguageFrontend).namespaceDelimiter)
        ) {
            // extract the scope name, it is usually a name space, but could probably be something
            // else as well in other languages
            val scopeName =
                call.fqn.substring(
                    0,
                    call.fqn.lastIndexOf((lang as GoLanguageFrontend).namespaceDelimiter)
                )

            // this is a scoped call. we need to explicitly jump to that particular scope
            val scopes = getScopesThat { predicate: Scope? ->
                (predicate is NameScope && predicate.scopedName == scopeName)
            }
            s =
                if (scopes == null || scopes.isEmpty()) {
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

        return resolveValueDeclaration(
            s,
            { f: FunctionDeclaration -> f.name == call.name && f.hasSignature(call.signature) },
            FunctionDeclaration::class.java
        )
    }

    fun resolveFunctionTemplateDeclaration(
        call: CallExpression
    ): List<FunctionTemplateDeclaration> {
        return resolveFunctionTemplateDeclaration(currentScope, call)
    }

    fun resolveFunctionStopScopeTraversalOnDefinition(
        call: CallExpression
    ): List<FunctionDeclaration> {
        return resolveFunctionStopScopeTraversalOnDefinition(currentScope, call)
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
    private fun resolve(scope: Scope?, ref: Node): ValueDeclaration? {
        if (scope is ValueDeclarationScope) {
            for (valDecl in scope.valueDeclarations) {
                if (valDecl.name == ref.name) {

                    // If the reference seems to point to a function the entire signature is checked
                    // for
                    // equality
                    if (ref is HasType &&
                            (ref as HasType).type is FunctionPointerType &&
                            valDecl is FunctionDeclaration
                    ) {
                        val fptrType = (ref as HasType).type as FunctionPointerType
                        val d = valDecl
                        if (d.type == fptrType.returnType && d.hasSignature(fptrType.parameters)) {
                            return valDecl
                        }
                    } else {
                        return valDecl
                    }
                }
            }
        }
        return if (scope!!.getParent() != null) resolve(scope.getParent(), ref) else null
    }

    /**
     * Traverses the scope and looks for Declarations of type c which matches f
     *
     * @param scope
     * @param p predicate the element must match to
     * @param c class of the object we want to find by traversing
     * @param <T>
     * @return </T>
     */
    private fun <T> resolveValueDeclaration(scope: Scope?, p: Predicate<T>, c: Class<T>): List<T> {
        if (scope is ValueDeclarationScope) {
            val list =
                scope
                    .valueDeclarations
                    .stream()
                    .filter { obj: ValueDeclaration? -> c.isInstance(obj) }
                    .map { obj: ValueDeclaration? -> c.cast(obj) }
                    .filter(p)
                    .collect(Collectors.toList())
            if (!list.isEmpty()) {
                return list
            }
        }
        return if (scope!!.getParent() != null) resolveValueDeclaration(scope.getParent(), p, c)
        else ArrayList()
    }

    /**
     * Traverses the scope and looks for Declarations of type c which matches f
     *
     * @param scope
     * @param p predicate the element must match to
     * @param c class of the object we want to find by traversing
     * @param <T>
     * @return </T>
     */
    private fun <T> resolveStructureDeclaration(
        scope: Scope?,
        p: Predicate<T>,
        c: Class<T>
    ): List<T> {
        if (scope is StructureDeclarationScope) {
            var list =
                scope
                    .structureDeclarations
                    .stream()
                    .filter { obj: Declaration? -> c.isInstance(obj) }
                    .map { obj: Declaration? -> c.cast(obj) }
                    .filter(p)
                    .collect(Collectors.toList())
            if (list.isEmpty()) {
                for (declaration in scope.structureDeclarations) {
                    if (declaration is RecordDeclaration) {
                        list =
                            declaration
                                .templates
                                .stream()
                                .filter { obj: TemplateDeclaration? -> c.isInstance(obj) }
                                .map { obj: TemplateDeclaration? -> c.cast(obj) }
                                .filter(p)
                                .collect(Collectors.toList())
                    }
                }
            }
            if (!list.isEmpty()) {
                return list
            }
        }
        return if (scope!!.getParent() != null) resolveStructureDeclaration(scope.getParent(), p, c)
        else ArrayList()
    }

    /**
     * @param scope where we are searching for the FunctionTemplateDeclarations
     * @param call CallExpression we want to resolve an invocation target for
     * @return List of FunctionTemplateDeclaration that match the name provided in the
     * CallExpression and therefore are invocation candidates
     */
    private fun resolveFunctionTemplateDeclaration(
        scope: Scope?,
        call: CallExpression
    ): List<FunctionTemplateDeclaration> {
        return resolveStructureDeclaration(
            scope,
            { c: FunctionTemplateDeclaration -> c.name == call.name },
            FunctionTemplateDeclaration::class.java
        )
    }

    /**
     * Resolves a function reference of a call expression, but stops the scope traversal when a
     * FunctionDeclaration with matching name has been found
     *
     * @param scope
     * @param call
     * @return
     */
    private fun resolveFunctionStopScopeTraversalOnDefinition(
        scope: Scope?,
        call: CallExpression
    ): List<FunctionDeclaration> {
        return resolveValueDeclaration(
            scope,
            { f: FunctionDeclaration -> f.name == call.name },
            FunctionDeclaration::class.java
        )
    }

    /**
     * This function tries to resolve a FQN to a scope. The name is the name of the AST-Node
     * associated to a scope. The Name may be the FQN-name or a relative name that with the
     * currently active namespace gives the AST-Nodes, FQN. If the provided name and the current
     * namespace overlap ,they are merged and the FQN is resolved. If there is no node with the
     * merged FQN-name null is returned. This is due to the behaviour of C++ when resolving names
     * for AST-elements that are definitions of exiting declarations.
     *
     * @param astNodeName relative (to the current Namespace) or fqn-Name of an entity associated to
     * a scope.
     * @return The scope that the resolved name is associated to.
     */
    private fun resolveScopeWithPath(astNodeName: String?): Scope? {
        if (astNodeName == null || astNodeName.isEmpty()) {
            return currentScope
        }
        val namePath =
            Arrays.asList(
                *astNodeName.split(Pattern.quote(lang!!.namespaceDelimiter)).toTypedArray()
            )
        val currentPath =
            Arrays.asList(
                *currentNamePrefix.split(Pattern.quote(lang!!.namespaceDelimiter)).toTypedArray()
            )

        // Last index because the inner name has preference
        val nameIndexInCurrent = currentPath.lastIndexOf(namePath[0])
        return if (nameIndexInCurrent >= 0) {
            // Overlapping relative resolution
            val mergedPath = currentPath.subList(0, nameIndexInCurrent)
            mergedPath.addAll(namePath)
            fqnScopeMap.get(
                java.lang.String.join(lang!!.namespaceDelimiter, mergedPath),
            )
        } else {
            // Absolute name of the node by concatenating the current namespace and the relative
            // name
            val relativeToAbsolute =
                (currentNamePrefixWithDelimiter +
                    lang!!.namespaceDelimiter +
                    java.lang.String.join(lang!!.namespaceDelimiter, namePath))
            // Relative resolution
            val scope = fqnScopeMap.get(relativeToAbsolute)
            scope ?: // Absolut resolution: The name is used as absolut name.
            fqnScopeMap.get(astNodeName)
        }
    }

    private fun resolveInSingleScope(
        scope: Scope,
        ref: DeclaredReferenceExpression
    ): ValueDeclaration? {
        if (scope is ValueDeclarationScope) {
            for (valDecl in scope.valueDeclarations) {
                if (valDecl.name == ref.name) return valDecl
            }
        }
        return null
    }

    fun getScopeOfStatement(node: Node?): Scope? {
        return scopeMap.get(node)
    }

    /**
     * Retrieves the [RecordDeclaration] for the given name in the given scope.
     *
     * @param scope the scope
     * @param name the name
     * @return the declaration, or null if it does not exist
     */
    fun getRecordForName(scope: Scope, name: String?): RecordDeclaration? {
        var o: RecordDeclaration? = null

        // check current scope first
        if (scope is StructureDeclarationScope) {
            o =
                scope
                    .structureDeclarations
                    .filter { d: Declaration -> d is RecordDeclaration && d.name == name }
                    .map { d: Declaration? -> d as RecordDeclaration? }
                    .firstOrNull()
        }
        if (o != null) {
            return o
        }

        // no parent left
        return if (scope.getParent() == null) {
            null
        } else getRecordForName(scope.getParent(), name)
    } ///// End copied over for now ///////

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ScopeManager::class.java)
    }

    init {
        pushScope(GlobalScope())
    }
}

fun Scope.isBreakable(): Boolean {
    return this is LoopScope || this is SwitchScope
}

fun Scope.isContinuable(): Boolean {
    return this is LoopScope
}
