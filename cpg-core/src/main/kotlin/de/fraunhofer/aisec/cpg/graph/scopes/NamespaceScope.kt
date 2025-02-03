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

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.scopes.Imports
import de.fraunhofer.aisec.cpg.graph.edges.unwrappingIncoming
import de.fraunhofer.aisec.cpg.graph.translationResult
import de.fraunhofer.aisec.cpg.passes.updateImportedSymbols

/**
 * This scope is opened up by a [NamespaceDeclaration] and represents the scope of the whole
 * namespace. This scope is special in a way that it will only exist once (per [GlobalScope]) and
 * contains all symbols declared in this namespace, even if they are spread across multiple files.
 */
class NamespaceScope(astNode: NamespaceDeclaration) : NameScope(astNode) {

    val importedByEdges: Imports =
        Imports(this, mirrorProperty = Scope::importedScopeEdges, outgoing = false)
    val importedBy: MutableSet<Scope> by unwrappingIncoming(NamespaceScope::importedByEdges)

    override fun addDeclaration(declaration: Declaration, addToAST: Boolean) {
        val result = super.addDeclaration(declaration, addToAST)

        // Some dirty hack to get a working scope manager
        val finalCtx = declaration.translationResult?.finalCtx

        // Update imported symbols of dependent scopes
        for (edge in importedByEdges) {
            with(finalCtx!!) { edge.declaration!!.updateImportedSymbols() }
        }

        return result
    }
}
