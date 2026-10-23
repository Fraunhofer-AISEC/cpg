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
package de.fraunhofer.aisec.cpg.graph.scopes

import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Import
import de.fraunhofer.aisec.cpg.graph.edges.scopes.Imports
import de.fraunhofer.aisec.cpg.graph.edges.unwrappingIncoming
import de.fraunhofer.aisec.cpg.passes.updateImportedSymbols
import de.fraunhofer.aisec.cpg.persistence.Relationship

/**
 * A scope which acts as a namespace with a certain name, which is prefixed to all local names
 * declared in it. This could be a package or other structural elements, like a class. In the first
 * case, the derived [NamespaceScope], in the latter case, the derived [RecordScope] should be used.
 */
sealed class NameScope(node: AstNode) : Scope(node) {

    init {
        astNode = node
        // Set the name so that we can use it as a namespace later
        name = node.name
    }

    /**
     * This is the mirror property to [Scope.importedScopeEdges]. It specifies which other [Scope]s
     * are importing this scope (e.g., a namespace or, for languages with
     * [de.fraunhofer.aisec.cpg.frontends.HasImportsFromRecords], a record).
     *
     * This is used in [addSymbol] to update the [Import.importedSymbols] once we add a new symbol
     * here, so that is it also visible in the scope of the [Import].
     */
    @Relationship(value = "IMPORTS_SCOPE", direction = Relationship.Direction.INCOMING)
    val importedByEdges: Imports =
        Imports(this, mirrorProperty = Scope::importedScopeEdges, outgoing = false)

    /** Virtual property for accessing [importedScopeEdges] without property edges. */
    val importedBy: MutableSet<Scope> by unwrappingIncoming(NameScope::importedByEdges)

    context(provider: ContextProvider)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    override fun addSymbol(symbol: Symbol, declaration: Declaration): Declaration {
        val canonical = super.addSymbol(symbol, declaration)

        // Update imported symbols of dependent scopes
        for (edge in importedByEdges) {
            edge.declaration?.let { provider.ctx.scopeManager.updateImportedSymbols(it) }
        }

        return canonical
    }
}
