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
                if (tu != null) {
                    // Add tu
                    list += tu
                    // Mark it as done, this will retrieve any dependencies to this TU from the map
                    markAsDone(tu)
                } else {
                    var remaining = keys
                    // No translation units without dependencies found. If there are no translation
                    // units left, this means we are done
                    if (remaining.isEmpty()) {
                        break
                    } else {
                        log.warn(
                            "We still have {} translation units with import dependency problems. We will just process them in any order",
                            remaining.size
                        )
                        // If there are still translation units left, we have a problem. This might
                        // be cyclic imports or other cases we did not think of yet. We still want
                        // to handle all the TUs, so we just process them in any order
                        remaining.forEach { list += it }
                        break
                    }
                }
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

        // We collect a list of all declarations for all parts of the name and filter, whether they
        // belong to a namespace declaration.
        var list =
            parts
                .map {
                    scopeManager
                        .lookupSymbolByName(it, import.location, import.scope)
                        .toMutableList()
                }
                .flatten()
        var namespaces = list.filterIsInstance<NamespaceDeclaration>()

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

            // Lastly, store the namespace TU as an import dependency of the TU where the import was
            var added = currentComponent.importDependencies.add(importTu, namespaceTu)
            if (added) {
                log.debug("Added {} as an dependency of {}", namespaceTu.name, importTu.name)
            }
        }
    }

    private fun handleImportDeclaration(import: ImportDeclaration) {
        // We always need to search at the global scope because we are "importing" something, so by
        // definition, this is not in the scope of the current file.
        val scope = scopeManager.globalScope ?: return

        // Let's do some importing. We need to import either a wildcard
        if (import.wildcardImport) {
            val list = scopeManager.lookupSymbolByName(import.import, import.location, scope)
            val symbol = list.singleOrNull()
            if (symbol != null) {
                // In this case, the symbol must point to a name scope
                val symbolScope = scopeManager.lookupScope(symbol)
                if (symbolScope is NameScope) {
                    import.importedSymbols = symbolScope.symbols
                }
            }
        } else {
            // or a symbol directly
            val list =
                scopeManager
                    .lookupSymbolByName(import.import, import.location, scope)
                    .toMutableList()
            import.importedSymbols = mutableMapOf(import.symbol to list)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
