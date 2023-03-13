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

import de.fraunhofer.aisec.cpg.graph.types.Type

interface HasType {
    var type: Type

    /**
     * @return The returned Type is always the same as getType() with the exception of ReferenceType
     *   since there is no case in which we want to propagate a reference when using typeChanged()
     */
    val propagationType: Type

    /**
     * Side-effect free type modification WARNING: This should only be used by the TypeSystem Pass
     *
     * @param type new type
     */
    fun updateType(type: Type)
    fun updatePossibleSubtypes(types: List<Type>)

    /**
     * Set the node's type. This may start a chain of type listener notifications
     *
     * @param type new type
     * @param root The nodes which we have seen in the type change chain. When a node receives a
     *   type setting command where root.contains(this), we know that we have a type listener circle
     *   and can abort. If root is an empty list, the type change is seen as an externally triggered
     *   event and subsequent type listeners receive the current node as their root.
     */
    fun setType(type: Type, root: MutableList<HasType>?)
    var possibleSubTypes: List<Type>

    /**
     * Set the node's possible subtypes. Listener circle detection works the same way as with
     * [ ][.setType]
     *
     * @param possibleSubTypes the set of possible sub types
     * @param root A list of already seen nodes which is used for detecting loops.
     */
    fun setPossibleSubTypes(possibleSubTypes: List<Type>, root: MutableList<HasType>)
    fun registerTypeListener(listener: TypeListener)
    fun unregisterTypeListener(listener: TypeListener)
    val typeListeners: Set<TypeListener>
    fun refreshType()

    /**
     * Used to set the type and clear the possible subtypes list for when a type is more precise
     * than the current.
     *
     * @param type the more precise type
     */
    fun resetTypes(type: Type)
    interface TypeListener {
        fun typeChanged(src: HasType, root: MutableList<HasType>, oldType: Type)
        fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>)
    }

    /**
     * The Typeresolver needs to be aware of all outgoing edges to types in order to merge equal
     * types to the same node. For the primary type edge, this is achieved through the hasType
     * interface. If a node has additional type edges (e.g. default type in [ ]) the node must
     * implement the updateType method, so that the current type is always replaced with the merged
     * one
     */
    interface SecondaryTypeEdge {
        fun updateType(typeState: Collection<Type>)
    }
}
