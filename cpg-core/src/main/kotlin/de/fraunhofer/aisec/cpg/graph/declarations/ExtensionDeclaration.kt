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

/**
 * Represents an extension to an existing declaration (e.g. [RecordDeclaration], such as Rust `impl`
 * blocks or C# partial classes. The declarations contained within this node are part of the AST
 * structure of the extension but are added to the symbol table of the [extendedDeclaration] they
 * are extending.
 *
 * The [name] of this extension identifies the construct it is extending.
 */
class ExtensionDeclaration : Declaration(), DeclarationHolder {
    /**
     * Edges to [Declaration] nodes (e.g. a [MethodDeclaration]) contained in this extension
     * declaration.
     */
    val declarationEdges = astEdgesOf<Declaration>()
    override val declarations by unwrapping(ExtensionDeclaration::declarationEdges)

    /**
     * The [Declaration] we are "extending" with this extension declaration. All children of this
     * extension MUST be placed in the [Declaration.declaringScope] of this declaration. Currently,
     * we only accept a [RecordDeclaration].
     */
    var extendedDeclaration: RecordDeclaration? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExtensionDeclaration) return false
        return super.equals(other) &&
            declarations == other.declarations &&
            extendedDeclaration == other.extendedDeclaration
    }

    override fun hashCode() = Objects.hash(super.hashCode(), declarations, extendedDeclaration)

    override fun addDeclaration(declaration: Declaration) {
        addIfNotContains(declarations, declaration)
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return setOf()
    }

    override fun getExitNextEOG(): Collection<Node> {
        return setOf()
    }
}
