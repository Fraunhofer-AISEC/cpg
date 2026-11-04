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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdges
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeList

interface DeclarationHolder {
    /**
     * Adds the specified declaration to this declaration holder. Ideally, the declaration holder
     * should use the [addIfNotContains] method to consistently add declarations.
     *
     * @param declaration the declaration
     */
    fun addDeclaration(declaration: Declaration)

    /**
     * Note that "contains" is decided by *identity* throughout the [addIfNotContains] overloads,
     * not by [Any.equals]. What these guard against is wiring the very same node into the same
     * holder twice, which is a question about object identity; two distinct declarations that
     * merely compare equal are two declarations and both belong in the holder. Deciding it
     * structurally was also expensive, since [Node.equals] compares name, code, comment and
     * location, and the scan runs once per added declaration over everything added so far.
     */
    fun <T : Declaration> addIfNotContains(collection: MutableCollection<T>, declaration: T) {
        if (collection.none { it === declaration }) {
            collection.add(declaration)
        }
    }

    fun <T : AstNode, P : AstEdge<T>> addIfNotContains(collection: AstEdges<T, P>, declaration: T) {
        addIfNotContains(collection, declaration, true)
    }

    fun <T : AstNode, P : Edge<T>> addIfNotContains(collection: EdgeList<T, P>, declaration: T) {
        addIfNotContains(collection, declaration, true)
    }

    /**
     * Adds a declaration to a collection of property edges, which contain the declarations
     *
     * @param collection the collection
     * @param declaration the declaration
     * @param <T> the type of the declaration
     * @param outgoing whether the property is outgoing </T>
     */
    fun <T : Node, P : Edge<T>> addIfNotContains(
        collection: EdgeList<T, out P>,
        declaration: T,
        outgoing: Boolean,
    ) {
        // Identity, not equality - see the first addIfNotContains overload.
        var contains = false
        for (element in collection) {
            if (outgoing) {
                if (element.end === declaration) {
                    contains = true
                    break
                }
            } else {
                if (element.start === declaration) {
                    contains = true
                    break
                }
            }
        }

        if (contains) {
            return
        }

        collection.add(declaration)
    }

    val declarations: List<Declaration>
}
