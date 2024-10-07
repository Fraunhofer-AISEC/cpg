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
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker

/**
 * This pass looks for [ImportDeclaration] nodes and imports symbols into their respective [Scope]
 */
class ImportResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(t: Component) {
        walker = SubgraphWalker.ScopedWalker(scopeManager)

        walker.registerHandler(::handleImportDeclaration)
        walker.iterate(t)
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
