/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge

interface DeclarationHolder {

    fun <N : Declaration> addDeclaration(declaration: N)

    /**
     * Adds the specified declaration to this declaration holder. Ideally, the declaration holder
     * should use the [.addIfNotContains] method to consistently add
     * declarations.
     *
     * @param declaration the declaration
     */
    @JvmDefault
    fun <N : Declaration> addIfNotContainsRaw(collection: MutableCollection<N>, declaration: N) {
        if (declaration is ReDeclarable<*>) {
            val previous = collection.firstOrNull { it == declaration } as ReDeclarable<N>?
            (declaration as ReDeclarable<N>).setPreviousDeclaration(previous)
            if(declaration.canBeAdded()) {
                collection.add(declaration)
            }
        } else {
            if (!collection.contains(declaration)) {
                collection.add(declaration)
            }
        }
    }

    @JvmDefault
    fun <N : Node> addIfNotContains(
            collection: MutableCollection<PropertyEdge<N>>, declaration: N) {
        addIfNotContains(collection, declaration, true)
    }

    /**
     * Adds a declaration to a collection of property edges, which contain the declarations
     *
     * @param collection the collection
     * @param declaration the declaration
     * @param <T> the type of the declaration
     * @param outgoing whether the property is outgoing
    </T> */
    @JvmDefault
    fun <T : Node> addIfNotContains(
            collection: MutableCollection<PropertyEdge<T>>, declaration: T, outgoing: Boolean) {
        // create a new property edge
        val propertyEdge = if (outgoing) PropertyEdge(this as Node, declaration) else PropertyEdge(declaration, this as T)

        // set the index property
        propertyEdge.addProperty(Properties.INDEX, collection.size)

        var contains = false
        for (element in collection) {
            if (outgoing) {
                if (element.end == propertyEdge.end) {
                    contains = true
                    break
                }
            } else {
                if (element.start == propertyEdge.start) {
                    contains = true
                    break
                }
            }
        }

        if (declaration is Declaration && declaration is ReDeclarable<*>) {
            val previous = collection
                    .map {
                        if (outgoing) {
                            it.end
                        } else {
                            it.start
                        }
                    }.map { it as ReDeclarable<Declaration> }
                    .firstOrNull { it.isSameDeclaration(declaration) }

            (declaration as ReDeclarable<Declaration>).setPreviousDeclaration(previous)
            if(declaration.canBeAdded()) {
                collection.add(propertyEdge)
            }
        } else {
            if (!contains) {
                collection.add(propertyEdge)
            }
        }
    }
}