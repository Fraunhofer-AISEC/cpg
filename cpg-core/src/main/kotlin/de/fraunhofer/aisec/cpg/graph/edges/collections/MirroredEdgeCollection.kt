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
package de.fraunhofer.aisec.cpg.graph.edges.collections

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge

/** Internal operations for mirroring without recursively triggering hooks. */
internal interface MirrorBacklinkCollection<ElementType> {
    fun addMirrorBacklink(element: ElementType): Boolean

    fun removeMirrorBacklink(element: ElementType): Boolean

    fun containsMirrorBacklinkByIdentity(element: ElementType): Boolean
}

/**
 * This interface can be used for edge collections, which "mirror" its content to another property.
 * This can be used to automatically populate next/prev flow edges.
 */
interface MirroredEdgeCollection<NodeType : Node, PropertyEdgeType : Edge<NodeType>> :
    EdgeCollection<NodeType, PropertyEdgeType> {
    /** Provides direct access to the mirrored collection without reflection. */
    var mirroredCollection: (Node) -> MutableCollection<PropertyEdgeType>

    override fun handleOnRemove(edge: PropertyEdgeType) {
        // Handle our mirror property.
        val mirror = if (outgoing) mirroredCollection(edge.end) else mirroredCollection(edge.start)

        @Suppress("UNCHECKED_CAST")
        if (mirror is MirrorBacklinkCollection<*>) {
            (mirror as MirrorBacklinkCollection<PropertyEdgeType>).removeMirrorBacklink(edge)
        } else if (edge in mirror) {
            mirror.remove(edge)
        }

        // Execute any remaining pre actions
        super.handleOnRemove(edge)
    }

    /**
     * Adds this particular edge to its mirror collection. We need the information if this is an
     * [outgoing] or incoming edge collection.
     */
    override fun handleOnAdd(edge: PropertyEdgeType) {
        // Handle our mirror collection.
        val mirror = if (outgoing) mirroredCollection(edge.end) else mirroredCollection(edge.start)

        @Suppress("UNCHECKED_CAST")
        if (mirror is MirrorBacklinkCollection<*>) {
            val backlink = mirror as MirrorBacklinkCollection<PropertyEdgeType>
            if (!backlink.containsMirrorBacklinkByIdentity(edge)) {
                backlink.addMirrorBacklink(edge)
            }
        } else if (edge !in mirror) {
            mirror.add(edge)
        }

        // Execute any remaining post actions
        super.handleOnAdd(edge)
    }
}
