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
import kotlin.reflect.KProperty

/**
 * This interface can be used for edge collections, which "mirror" its content to another property.
 * This can be used to automatically populate next/prev flow edges.
 */
interface MirroredEdgeCollection<NodeType : Node, PropertyEdgeType : Edge<NodeType>> :
    EdgeCollection<NodeType, PropertyEdgeType> {
    var mirrorProperty: KProperty<MutableCollection<PropertyEdgeType>>

    override fun handleOnRemove(edge: PropertyEdgeType) {
        // Handle our mirror property.
        if (outgoing) {
            var prevOfNext = mirrorProperty.call(edge.end)
            if (edge in prevOfNext) {
                prevOfNext.remove(edge)
            }
        } else {
            var nextOfPrev = mirrorProperty.call(edge.start)
            if (edge in nextOfPrev) {
                nextOfPrev.remove(edge)
            }
        }

        // Execute any remaining pre actions
        super.handleOnRemove(edge)
    }

    /**
     * Adds this particular edge to its [mirrorProperty]. We need the information if this is an
     * [outgoing] or incoming edge collection.
     */
    override fun handleOnAdd(edge: PropertyEdgeType) {
        // Handle our mirror property. We add some extra "in" checks here, otherwise things will
        // loop.
        if (outgoing) {
            var prevOfNext = mirrorProperty.call(edge.end)
            if (edge !in prevOfNext) {
                prevOfNext.add(edge)
            }
        } else {
            var nextOfPrev = mirrorProperty.call(edge.start)
            if (edge !in nextOfPrev) {
                nextOfPrev.add(edge)
            }
        }

        // Execute any remaining post actions
        super.handleOnAdd(edge)
    }
}
