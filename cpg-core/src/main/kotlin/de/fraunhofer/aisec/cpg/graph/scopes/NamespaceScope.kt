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
package de.fraunhofer.aisec.cpg.graph.scopes

import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Namespace
import de.fraunhofer.aisec.cpg.graph.edges.scopes.Imports
import de.fraunhofer.aisec.cpg.graph.edges.unwrappingIncoming
import de.fraunhofer.aisec.cpg.passes.updateImportedSymbols
import org.neo4j.ogm.annotation.Relationship

/**
 * This scope is opened up by a [Namespace] and represents the scope of the whole namespace. This
 * scope is special in a way that it will only exist once (per [GlobalScope]) and contains all
 * symbols declared in this namespace, even if they are spread across multiple files.
 */
class NamespaceScope(astNode: Namespace) : NameScope(astNode) {

    /**
     * This is the mirror property to [Scope.importedScopeEdges]. It specifies which other [Scope]s
     * are importing this namespace.
     *
     * This is used in [addSymbol] to update the [ImportDeclaration.importedSymbols] once we add a
     * new symbol here, so that is it also visible in the scope of the [ImportDeclaration].
     */
    @Relationship(value = "IMPORTS_SCOPE", direction = Relationship.Direction.INCOMING)
    val importedByEdges: Imports =
        Imports(this, mirrorProperty = Scope::importedScopeEdges, outgoing = false)

    /** Virtual property for accessing [importedScopeEdges] without property edges. */
    val importedBy: MutableSet<Scope> by unwrappingIncoming(NamespaceScope::importedByEdges)

    context(provider: ContextProvider)
    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    override fun addSymbol(symbol: Symbol, declaration: Declaration) {
        super.addSymbol(symbol, declaration)

        // Update imported symbols of dependent scopes
        for (edge in importedByEdges) {
            edge.declaration?.let { provider.ctx.scopeManager.updateImportedSymbols(it) }
        }
    }
}
