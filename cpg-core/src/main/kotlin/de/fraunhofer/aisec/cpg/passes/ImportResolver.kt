/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.scopes.Import
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.pComponent
import de.fraunhofer.aisec.cpg.graph.pTranslationUnit
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.NamespaceScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import de.fraunhofer.aisec.cpg.passes.inference.tryNamespaceInference
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.IdentityHashMap

/**
 * This class holds the information about import dependencies between nodes that represent some kind
 * of "module" (usually either a [TranslationUnitDeclaration] or a [Component]). The dependency is
 * based on which module imports symbols of another module (usually in the form of a
 * [NamespaceDeclaration]). The idea is to provide a sorted list of modules in which to resolve
 * symbols and imports ideally. This is stored in [sorted] and is automatically computed the fist
 * time someone accesses the property.
 */
class ImportDependencies<T : Node>(modules: MutableList<T>) : IdentityHashMap<T, IdentitySet<T>>() {

    init {
        // Populate the map with all modules so that we have an entry in our list
        // for all
        this += modules.map { Pair(it, identitySetOf()) }
    }

    /**
     * The list of [T] module nodes, sorted by their position in the dependency graph. Nodes without
     * dependencies are first in the list, following by nodes that import nodes without
     * dependencies, and so on.
     */
    val sorted: List<T> by lazy { WorkList(this).resolveDependencies() }

    /** Adds a dependency from [importer] to [imported]. */
    fun add(importer: T, imported: T): Boolean {
        if (importer === imported) {
            return false
        }

        var list = this.computeIfAbsent(importer) { identitySetOf<T>() }
        var added = list.add(imported)

        return added
    }

    /**
     * A work-list, which contains a local copy of our dependency map, so that we can remove items
     * from it while determining the order.
     */
    class WorkList<T : Node>(start: ImportDependencies<T>) : IdentityHashMap<T, IdentitySet<T>>() {

        init {
            // Populate the work-list with a copy of the import dependency map
            this +=
                start.map { entry ->
                    Pair(
                        entry.key,
                        // We ignore entries that point to different parents. For example, if we
                        // analyze translation units, we are only interested in resolving
                        // relationships within the current component (which is the AST parent of a
                        // translation unit). The reason for that is that we are already processing
                        // the components in the order they are necessary and then process the TUs
                        // in the desired order within the component. We might change that in the
                        // future, if we see a benefit to NOT group processing per component but
                        // rather just process all TUs in the order they are necessary regardless of
                        // their parent component.
                        entry.value.filter { entry.key.astParent == it.astParent }.toIdentitySet(),
                    )
                }
        }

        /**
         * Resolves the import dependencies and returns the list in which modules should be
         * processed. The algorithm is as follows:
         * - We loop through all modules in the map
         * - We try to fetch the next modules without any dependencies
         * - If there is one, we add it to the list and call [markAsDone]. This will remove the
         *   module as dependency from the map
         * - If there is none, we are either finished (if no modules are left) -- or we ran into a
         *   problem
         * - If we ran into a problem, we add all leftover modules in a nondeterministic order to
         *   the list
         */
        fun resolveDependencies(): List<T> {
            var list = mutableListOf<T>()

            while (true) {
                // Try to get the next module
                var tu = nextWithoutDependencies()
                if (tu == null) {
                    var remaining = keys
                    // No modules without dependencies found. If there are no modules left, this
                    // means we are done
                    if (remaining.isEmpty()) {
                        break
                    } else {
                        // If there are still modules left, we have a problem. This might
                        // be cyclic imports or other cases we did not think of yet. We still want
                        // to handle all the modules, so we pick the one with the least
                        // dependencies, hoping that this could unlock more
                        log.warn(
                            "We still have {} items with import dependency problems. We will just pick the one with the least dependencies",
                            remaining.size,
                        )
                        tu = remaining.sortedBy { this[it]?.size }.firstOrNull()
                        if (tu == null) {
                            break
                        }
                    }
                }

                // Add tu
                list += tu
                // Mark it as done, this will remove any dependencies to this module from the map
                markAsDone(tu)
            }

            return list
        }

        /**
         * Retrieves the next [T] without any dependencies to others. Returns null if only modules
         * WITH dependencies are left.
         */
        fun nextWithoutDependencies(): T? {
            // Loop through all entries to find one without a dependency
            for (entry in entries) {
                if (entry.value.isEmpty()) {
                    return entry.key
                }
            }

            return null
        }

        /**
         * Marks the processing of this [T] as done. It removes it from the map and also removes it
         * from the dependencies of all other modules.
         */
        private fun markAsDone(tu: T) {
            log.debug("Next suitable item is {}", tu.name)
            // Remove it from the map
            remove(tu)

            // And also remove it from all lists
            values.forEach { it.remove(tu) }
        }
    }
}

/**
 * This pass looks for [ImportDeclaration] nodes and imports symbols into their respective [Scope].
 * It does so by first building a dependency map between [TranslationUnitDeclaration] nodes, based
 * on their [ImportDeclaration] nodes.
 */
class ImportResolver(ctx: TranslationContext) : TranslationResultPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker
    lateinit var tr: TranslationResult

    override fun accept(tr: TranslationResult) {
        this.tr = tr

        // Create a new import dependency object for the result, to make sure that all components
        // are included.
        tr.componentDependencies = ImportDependencies(tr.components)

        // In order to resolve imports as good as possible, we need the information which namespace
        // does an import on which other
        walker = SubgraphWalker.ScopedWalker(scopeManager)
        walker.registerHandler { node ->
            if (node is Component) {
                // Create a new import dependency object for the component, to make sure that all
                // TUs are included.
                node.translationUnitDependencies = ImportDependencies(node.translationUnits)
            } else if (node is ImportDeclaration) {
                collectImportDependencies(node)
            }
        }
        walker.iterate(tr)

        // Now we need to iterate through all modules
        walker.clearCallbacks()
        walker.registerHandler { node ->
            if (node is ImportDeclaration) {
                handleImportDeclaration(node)
            }
        }

        for (tu in
            (Strategy::COMPONENTS_LEAST_IMPORTS)(tr).asSequence().flatMap {
                (Strategy::TRANSLATION_UNITS_LEAST_IMPORTS)(it).asSequence()
            }) {
            log.debug("Resolving imports for translation unit {}", tu.name)
            walker.iterate(tu)
        }
    }

    /**
     * This callback collects dependencies between [TranslationUnitDeclaration] nodes based on a
     * [ImportDeclaration].
     */
    private fun collectImportDependencies(import: ImportDeclaration) {
        val currentComponent = import.pComponent
        if (currentComponent == null) {
            errorWithFileLocation(import, log, "Cannot determine component of import node")
            return
        }

        // Populate the imported scopes edges
        import.populateImportedScopes()

        // Let's look for imported namespaces
        // First, we collect the individual parts of the name
        var parts = mutableListOf<Name>()
        var name: Name? = import.import
        while (name != null) {
            parts += name
            name = name.parent
        }

        // We collect a list of all declarations for all parts of the name, beginning with the
        // "largest" part and filter, whether they belong to a namespace declaration. Once we have
        // found something, we need to abort in order to only import the most specific namespace.
        //
        // For example, in the Python snippet `from backend.app import db`, we first need to look
        // whether `backend.app.db` is a namespace, if not, we look at `backend.app` and lastly at
        // `backend`. We do this in order to make the dependency as fine-grained as possible.
        for (part in parts) {
            var namespaces =
                scopeManager.lookupSymbolByName(
                    part,
                    import.language,
                    import.location,
                    import.scope,
                ) {
                    // We are only interested in "leaf" namespace declarations, meaning that they do
                    // not have sub-declarations. The reason for that is that we usually need to
                    // nest namespace declarations, and thus a "parent" namespace declaration often
                    // exists in more than one file. We only want to depend on the particular
                    // translation unit that is the authoritative source of this namespace and this
                    // is the case if there is no sub-declaration.
                    //
                    // Note: One might be inclined to use the "namespaces" extensions to filter for
                    // leaf namespaces. However, this can be extremely slow because it first gathers
                    // all children and then filters them. Instead, we can directly filter for the
                    // child declarations.
                    it is NamespaceDeclaration &&
                        !it.isInferred &&
                        it.declarations.filterIsInstance<NamespaceDeclaration>().isEmpty()
                }

            // We are only interested in "leaf" namespace declarations, meaning that they do not
            // have sub-declarations. The reason for that is that we usually need to nest namespace
            // declarations, and thus a "parent" namespace declaration often exists in more than one
            // file. We only want to depend on the particular translation unit that is the
            // authoritative source of this namespace and this is the case if there is no
            // sub-declaration.

            // Next, we loop through all namespaces in order to "connect" them to our current module
            for (declaration in namespaces) {
                // Retrieve the module of the declarations
                var namespaceTu = declaration.pTranslationUnit
                var namespaceComponent = declaration.pComponent
                var importTu = import.pTranslationUnit
                // Skip, if we cannot find the module or if they belong to the same module (we do
                // not want self-references)
                if (
                    namespaceTu == null ||
                        namespaceComponent == null ||
                        importTu == null ||
                        namespaceTu == importTu
                ) {
                    continue
                }

                // Lastly, store the namespace module as an import dependency of the module where
                // the import was
                var added =
                    currentComponent.translationUnitDependencies?.add(importTu, namespaceTu) == true
                if (added) {
                    log.debug("Added {} as an dependency of {}", namespaceTu.name, importTu.name)
                }

                // Add it on translation result level as well
                added = tr.componentDependencies?.add(currentComponent, namespaceComponent) == true
                if (added) {
                    log.debug(
                        "Added {} as an dependency of {}",
                        namespaceComponent.name,
                        currentComponent.name,
                    )
                }
            }

            // If we had any imported namespaces, we break here
            if (namespaces.isNotEmpty()) {
                break
            }
        }
    }

    private fun handleImportDeclaration(import: ImportDeclaration) {
        ctx.scopeManager.updateImportedSymbols(import)
    }

    /**
     * This function populates the [Scope.importedScopes] property of the [Scope] that the
     * [ImportDeclaration] "lives" in.
     */
    private fun ImportDeclaration.populateImportedScopes() {
        val startScope = scope
        val name =
            when (style) {
                ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE -> {
                    import.parent
                }

                ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE,
                ImportStyle.IMPORT_NAMESPACE -> {
                    import
                }
            }
        if (name == null) {
            errorWithFileLocation(this, log, "Could not get namespace name from import declaration")
            return
        } else if (startScope == null) {
            errorWithFileLocation(this, log, "Could not get scope from import declaration")
            return
        }

        // Try to look up the namespace scope by the name
        var targetScope = scopeManager.lookupScope(name) as? NamespaceScope
        if (targetScope == null) {
            // Try to infer it, if inference is configured
            val decl = tryNamespaceInference(name, this)
            if (decl != null) {
                targetScope = scopeManager.lookupScope(name) as? NamespaceScope
            }
        }

        // If we have a target scope, we can create an "import" edge
        if (targetScope != null) {
            // Create a new import edge with all the necessary information
            val edge = Import(startScope, targetScope, this)
            startScope.importedScopeEdges += edge
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}

/**
 * This function updates the [ImportDeclaration.importedSymbols]. This is done once at the beginning
 * by the [ImportResolver]. However, we need to update this list once we infer new symbols in
 * namespaces that are imported at a later stage (e.g., in the [TypeResolver]), otherwise they won't
 * be visible to the later passes.
 */
fun ScopeManager.updateImportedSymbols(import: ImportDeclaration) {
    // We always need to search at the global scope because we are "importing" something, so by
    // definition, this is not in the scope of the current file.
    val scope = globalScope ?: return

    // Let's do some importing. We need to import either a wildcard
    if (import.style == ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE) {
        val list = lookupSymbolByName(import.import, import.language, import.location, scope)
        val symbol = list.singleOrNull()
        if (symbol != null) {
            // In this case, the symbol must point to a name scope
            val symbolScope = lookupScope(symbol)
            if (symbolScope is NameScope) {
                import.importedSymbols = symbolScope.symbols
            }
        }
    } else {
        // or a symbol directly
        val list =
            lookupSymbolByName(import.import, import.language, import.location, scope)
                .toMutableList()
        import.importedSymbols = mutableMapOf(import.symbol to list)
    }
}
