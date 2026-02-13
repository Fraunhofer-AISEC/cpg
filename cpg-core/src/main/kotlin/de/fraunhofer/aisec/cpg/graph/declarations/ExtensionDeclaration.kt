/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.Objects
import org.slf4j.LoggerFactory

/**
 * Children in this declaration are added to an existing node with namespace. This can be a Record,
 * or another similar construct that contains declarations. The children are therefore in this ast
 * construct but are added to the symbol table of the construct it is extending.
 *
 * The name that this extension has needs to identify the construct, it is extending
 */
class ExtensionDeclaration : Declaration(), DeclarationHolder {
    /**
     * Edges to nested namespaces, records, functions, fields etc. contained in the current
     * namespace.
     */
    val declarationEdges = astEdgesOf<Declaration>()
    override val declarations by unwrapping(ExtensionDeclaration::declarationEdges)

    /**
     * In some languages, there is a relationship between paths / directories and the package
     * structure. Therefore, we need to be aware of the path this namespace / package is in.
     */
    var path: String? = null

    var extendedDeclaration: DeclarationHolder? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExtensionDeclaration) return false
        return super.equals(other) && declarations == other.declarations
    }

    override fun hashCode() = Objects.hash(super.hashCode(), declarations)

    override fun addDeclaration(declaration: Declaration) {
        extendedDeclaration?.let { addDeclaration(declaration) }
            ?: log.warn(
                "{} could not be added to a declaration that {} this tries to extend. No declaration to extend was set.",
                declaration,
                this,
            )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExtensionDeclaration::class.java)
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return setOf()
    }

    override fun getExitNextEOG(): Collection<Node> {
        return setOf()
    }
}
