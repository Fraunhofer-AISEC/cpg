/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*

/**
 * This sealed (and abstract) class represents a [Scope] that in addition to declare variables also
 * defines structures, such as classes, namespaces, etc.
 *
 * This is actually only needed because of the legacy [ScopeManager.resolve] function.
 */
sealed class StructureDeclarationScope(astNode: Node?) : ValueDeclarationScope(astNode) {
    val structureDeclarations: List<Declaration>
        get() {
            return symbols
                .flatMap { it.value }
                .filter {
                    it is EnumDeclaration ||
                        it is RecordDeclaration ||
                        it is NamespaceDeclaration ||
                        it is TemplateDeclaration
                }
        }

    private fun addStructureDeclaration(declaration: Declaration) {
        if (astNode is DeclarationHolder) {
            val holder = astNode as DeclarationHolder
            holder.addDeclaration(declaration)
        } else {
            log.error(
                "Trying to add a value declaration to a scope which does not have a declaration holder AST node"
            )
        }
    }

    override fun addDeclaration(declaration: Declaration, addToAST: Boolean) {
        if (declaration is ValueDeclaration) {
            addValueDeclaration(declaration, addToAST)
        } else {
            addStructureDeclaration(declaration)
        }
    }
}
