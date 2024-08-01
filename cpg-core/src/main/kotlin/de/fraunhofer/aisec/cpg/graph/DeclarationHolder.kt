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
import de.fraunhofer.aisec.cpg.graph.edge.AbstractPropertyEdges
import de.fraunhofer.aisec.cpg.graph.edge.AstChildren
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdges

interface DeclarationHolder {
    /**
     * Adds the specified declaration to this declaration holder. Ideally, the declaration holder
     * should use the [addIfNotContains] method to consistently add declarations.
     *
     * @param declaration the declaration
     */
    fun addDeclaration(declaration: Declaration)

    fun <T : Declaration> addIfNotContains(collection: MutableCollection<T>, declaration: T) {
        if (!collection.contains(declaration)) {
            collection.add(declaration)
        }
    }

    fun <T : Node, P : PropertyEdge<T>> addIfNotContains(
        collection: AstChildren<T>,
        declaration: T
    ) {
        addIfNotContains(collection, declaration, true)
    }

    fun <T : Node, P : PropertyEdge<T>> addIfNotContains(
        collection: PropertyEdges<T>,
        declaration: T
    ) {
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
    fun <T : Node, P : PropertyEdge<T>> addIfNotContains(
        collection: AbstractPropertyEdges<T, out P>,
        declaration: T,
        outgoing: Boolean
    ) {
        var contains = false
        for (element in collection) {
            if (outgoing) {
                if (element.end == declaration) {
                    contains = true
                    break
                }
            } else {
                if (element.start == declaration) {
                    contains = true
                    break
                }
            }
        }

        if (outgoing) {
            collection.add(this as Node, declaration)
        } else {
            collection.add(declaration, this as T)
        }
    }

    val declarations: List<Declaration>
}
