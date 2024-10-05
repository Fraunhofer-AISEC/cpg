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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log

typealias ImportDependencyMap =
    MutableMap<TranslationUnitDeclaration, MutableSet<TranslationUnitDeclaration>>

/**
 * This pass looks for [ImportDeclaration] nodes and imports symbols into their respective [Scope]
 */
class ImportResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker
    lateinit var currentComponent: Component

    override fun accept(t: Component) {
        currentComponent = t

        // Populate the importedBy with all translation units so that we have an entry in our list
        // for all
        t.importDependencies =
            t.translationUnits
                .map { Pair(it, mutableSetOf<TranslationUnitDeclaration>()) }
                .associate { it }
                .toMutableMap()

        // In order to resolve imports as good as possible, we need the information which namespace
        // does an import on which other
        walker = SubgraphWalker.ScopedWalker(scopeManager)
        walker.registerHandler(::collectImportDependencies)
        walker.iterate(t)

        // Now we need to iterate through all translation units
        walker.clearCallbacks()
        walker.registerHandler(::handleImportDeclaration)
        t.importDependencies.resolveDependencies {
            log.debug("Resolving imports for translation unit {}", it.name)
            walker.iterate(it)
        }
    }

    private fun collectImportDependencies(node: Node?) {
        if (node !is ImportDeclaration) {
            return
        }

        // Let's look for imported namespaces
        // First, we need to try to resolve the import
        val list =
            scopeManager.lookupSymbolByName(node.import, node.location, scope).toMutableList()
        for (declaration in list) {
            if (declaration is NamespaceDeclaration) {
                var namespaceTu = declaration.translationUnit
                var importTu = node.translationUnit
                if (namespaceTu == null || importTu == null) {
                    continue
                }

                // If they depend on themselves, we don't care
                if (namespaceTu == importTu) {
                    continue
                }

                var list =
                    currentComponent.importDependencies.computeIfAbsent(importTu) {
                        mutableSetOf<TranslationUnitDeclaration>()
                    }
                var added = list.add(namespaceTu)
                if (added) {
                    log.debug("Added {} as an dependency of {}", namespaceTu.name, importTu.name)
                }
            }
        }
    }

    private fun handleImportDeclaration(node: Node?) {
        if (node !is ImportDeclaration) {
            return
        }

        // We always need to search at the global scope because we are "importing" something, so by
        // definition, this is not in the scope of the current file.
        val scope = scopeManager.globalScope ?: return

        // Let's do some importing. We need to import either a wildcard
        if (node.wildcardImport) {
            val list = scopeManager.lookupSymbolByName(node.import, node.location, scope)
            val symbol = list.singleOrNull()
            if (symbol != null) {
                // In this case, the symbol must point to a name scope
                val symbolScope = scopeManager.lookupScope(symbol)
                if (symbolScope is NameScope) {
                    node.importedSymbols = symbolScope.symbols
                }
            }
        } else {
            // or a symbol directly
            val list =
                scopeManager.lookupSymbolByName(node.import, node.location, scope).toMutableList()
            node.importedSymbols = mutableMapOf(node.symbol to list)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}

val Node.translationUnit: TranslationUnitDeclaration?
    get() {
        var node: Node? = this
        while (node != null) {
            if (node is TranslationUnitDeclaration) {
                return node
            }
            node = node.astParent
        }

        return null
    }

fun ImportDependencyMap.nextTranslationUnitWithoutDependencies(): TranslationUnitDeclaration? {
    // Loop through all to find one without a value
    for (entry in this.entries) {
        if (entry.value.isEmpty()) {
            return entry.key
        }
    }

    return null
}

private fun ImportDependencyMap.markAsDone(tu: TranslationUnitDeclaration) {
    // Remove it from the map
    this.remove(tu)

    // And also remove it from all lists
    this.values.forEach { it.remove(tu) }
}

fun ImportDependencyMap.resolveDependencies(callback: (tu: TranslationUnitDeclaration) -> Unit) {
    var dependencies: ImportDependencyMap =
        this.map { Pair(it.key, it.value.toMutableSet()) }.associate { it }.toMutableMap()

    while (true) {
        // Try to get the next TU
        var tu = dependencies.nextTranslationUnitWithoutDependencies()
        if (tu != null) {
            // Handle tu
            callback(tu)
            // Mark it as done, this will retrieve any dependencies to this TU from the map
            dependencies.markAsDone(tu)
        } else {
            var remaining = dependencies.keys
            // No translation units without dependencies found. If there are no translation
            // units left, this means we are done
            if (remaining.isEmpty()) {
                break
            } else {
                log.warn(
                    "We still have {} translation units with import dependency problems. We will just process them in any order",
                    remaining.size
                )
                // If there are still translation units left, we have a problem. This might be
                // cyclic imports or other cases we did not think of yet. We still want to
                // handle all the TUs, so we just process them in any order
                remaining.forEach { callback(it) }
                break
            }
        }
    }
}
