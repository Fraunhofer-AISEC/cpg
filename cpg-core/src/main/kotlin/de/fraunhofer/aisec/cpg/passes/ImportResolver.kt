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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.namespaces
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.translationUnit
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log

/**
 * This class holds the information about import dependencies between [TranslationUnitDeclaration]
 * nodes. The dependency is based on which translation unit imports symbols of another translation
 * unit (usually in the form of a [NamespaceDeclaration]). The idea is to provide a sorted list of
 * TUs in which to resolve symbols and imports ideally. This is stored in [sortedTranslationUnits]
 * and is automatically computed the fist time someone accesses the property.
 */
class ImportDependencies(tus: MutableList<TranslationUnitDeclaration>) :
    HashMap<TranslationUnitDeclaration, MutableSet<TranslationUnitDeclaration>>() {

    init {
        // Populate the map with all translation units so that we have an entry in our list
        // for all
        this += tus.map { Pair(it, mutableSetOf()) }
    }

    /**
     * The list of [TranslationUnitDeclaration] nodes, sorted by their position in the dependency
     * graph. TUs without dependencies are first in the list, following by TUs that import TUs
     * without dependencies, and so on.
     */
    val sortedTranslationUnits: List<TranslationUnitDeclaration> by lazy {
        WorkList(this).resolveDependencies()
    }

    /** Adds a dependency from [importer] to [imported]. */
    fun add(importer: TranslationUnitDeclaration, imported: TranslationUnitDeclaration): Boolean {
        var list = this.computeIfAbsent(importer) { mutableSetOf<TranslationUnitDeclaration>() }
        return list.add(imported)
    }

    /**
     * A work-list, which contains a local copy of our dependency map, so that we can remove items
     * from it while determining the order.
     */
    class WorkList(start: ImportDependencies) :
        HashMap<TranslationUnitDeclaration, MutableSet<TranslationUnitDeclaration>>() {

        init {
            // Populate the work-list with a copy of the import dependency map
            this += start.map { Pair(it.key, it.value.toMutableSet()) }
        }

        /**
         * Resolves the import dependencies and returns the list in which translation units should
         * be processed. The algorithm is as follows:
         * - We loop through all translation units in the map
         * - We try to fetch the next translation unit without any dependencies
         * - If there is one, we add it to the list and call [markAsDone]. This will remove the TU
         *   as dependency from the map
         * - If there is none, we are either finished (if no TUs are left) -- or we ran into a
         *   problem
         * - If we ran into a problem, we add all leftover TUs in a nondeterministic order to the
         *   list
         */
        fun resolveDependencies(): List<TranslationUnitDeclaration> {
            var list = mutableListOf<TranslationUnitDeclaration>()

            while (true) {
                // Try to get the next TU
                var tu = nextTranslationUnitWithoutDependencies()
                if (tu == null) {
                    var remaining = keys
                    // No translation units without dependencies found. If there are no translation
                    // units left, this means we are done
                    if (remaining.isEmpty()) {
                        break
                    } else {
                        // If there are still translation units left, we have a problem. This might
                        // be cyclic imports or other cases we did not think of yet. We still want
                        // to handle all the TUs, so we pick the one with the least dependencies,
                        // hoping that this could unlock more
                        log.warn(
                            "We still have {} translation units with import dependency problems. We will just pick the one with the least dependencies",
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
                // Mark it as done, this will remove any dependencies to this TU from the map
                markAsDone(tu)
            }

            return list
        }

        /**
         * Retrieves the next [TranslationUnitDeclaration] without any dependencies to others.
         * Returns null if only translation units WITH dependencies are left.
         */
        fun nextTranslationUnitWithoutDependencies(): TranslationUnitDeclaration? {
            // Loop through all entries to find one without a dependency
            for (entry in entries) {
                if (entry.value.isEmpty()) {
                    return entry.key
                }
            }

            return null
        }

        /**
         * Marks the processing of this [TranslationUnitDeclaration] as done. It removes it from the
         * map and also removes it from the dependencies of all other TUs.
         */
        private fun markAsDone(tu: TranslationUnitDeclaration) {
            log.debug("Next suitable translation unit is {}", tu.name)
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
class ImportResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker
    lateinit var currentComponent: Component

    override fun accept(t: Component) {
        currentComponent = t

        t.importDependencies = ImportDependencies(t.translationUnits)

        // In order to resolve imports as good as possible, we need the information which namespace
        // does an import on which other
        walker = SubgraphWalker.ScopedWalker(scopeManager)
        walker.registerHandler { node ->
            if (node is ImportDeclaration) {
                collectImportDependencies(node)
            }
        }
        walker.iterate(t)

        // Now we need to iterate through all translation units
        walker.clearCallbacks()
        walker.registerHandler { node ->
            if (node is ImportDeclaration) {
                handleImportDeclaration(node)
            }
        }
        t.importDependencies.sortedTranslationUnits.forEach {
            log.debug("Resolving imports for translation unit {}", it.name)
            walker.iterate(it)
        }
    }

    /**
     * This callback collects dependencies between [TranslationUnitDeclaration] nodes based on a
     * [ImportDeclaration].
     */
    private fun collectImportDependencies(import: ImportDeclaration) {
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
                scopeManager
                    .lookupSymbolByName(part, import.language, import.location, import.scope)
                    .filterIsInstance<NamespaceDeclaration>()

            // We are only interested in "leaf" namespace declarations, meaning that they do not
            // have sub-declarations. The reason for that is that we usually need to nest namespace
            // declarations, and thus a "parent" namespace declaration often exists in more than one
            // file. We only want to depend on the particular translation unit that is the
            // authoritative source of this namespace and this is the case if there is no
            // sub-declaration.
            namespaces =
                namespaces.filter {
                    // Note: the "namespaces" extension contains the starting node itself as well,
                    // so if we have no sub-namespace declaration, the size == 1
                    it.namespaces.size == 1
                }

            // Next, we loop through all namespaces in order to "connect" them to our current TU
            for (declaration in namespaces) {
                // Retrieve the TU of the declarations
                var namespaceTu = declaration.translationUnit
                var importTu = import.translationUnit
                // Skip, if we cannot find the TU or if they belong to the same TU (we do not want
                // self-references)
                if (namespaceTu == null || importTu == null || namespaceTu == importTu) {
                    continue
                }

                // Lastly, store the namespace TU as an import dependency of the TU where the import
                // was
                var added = currentComponent.importDependencies.add(importTu, namespaceTu)
                if (added) {
                    log.debug("Added {} as an dependency of {}", namespaceTu.name, importTu.name)
                }
            }

            // If we had any imported namespaces, we break here
            if (namespaces.isNotEmpty()) {
                break
            }
        }
    }

    private fun handleImportDeclaration(import: ImportDeclaration) {
        import.updateImportedSymbols()
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
context(Pass<*>)
fun ImportDeclaration.updateImportedSymbols() {
    // We always need to search at the global scope because we are "importing" something, so by
    // definition, this is not in the scope of the current file.
    val scope = scopeManager.globalScope ?: return

    // Let's do some importing. We need to import either a wildcard
    if (this.wildcardImport) {
        val list = scopeManager.lookupSymbolByName(this.import, this.language, this.location, scope)
        val symbol = list.singleOrNull()
        if (symbol != null) {
            // In this case, the symbol must point to a name scope
            val symbolScope = scopeManager.lookupScope(symbol)
            if (symbolScope is NameScope) {
                this.importedSymbols = symbolScope.symbols
            }
        }
    } else {
        // or a symbol directly
        val list =
            scopeManager
                .lookupSymbolByName(this.import, this.language, this.location, scope)
                .toMutableList()
        this.importedSymbols = mutableMapOf(this.symbol to list)
    }
}
