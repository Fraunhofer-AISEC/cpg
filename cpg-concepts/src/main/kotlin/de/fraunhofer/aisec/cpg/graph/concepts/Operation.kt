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
package de.fraunhofer.aisec.cpg.graph.concepts

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import java.util.Objects

/**
 * Represents an operation executed on/with a [Concept] (stored in [concept]). This is typically a
 * `write` on a file or log object or an `execute` on a database.
 */
abstract class Operation(
    underlyingNode: Node,
    /** The [Concept] this operation belongs to. */
    open val concept: Concept,
) : OverlayNode() {

    init {
        this.underlyingNode = underlyingNode
        this::class.simpleName?.let { name = Name(it) }
    }

    override fun equals(other: Any?): Boolean {
        return other is Operation && super.equals(other) && other.concept == this.concept
    }

    override fun hashCode() = Objects.hash(super.hashCode(), concept)
}
