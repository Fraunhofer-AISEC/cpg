/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.time.Duration

/**
 * This feature is, e.g., available on some VM services to automatically update their software. It
 * ensures that a resource is protected from tampering with its state.
 */
public open class AutomaticUpdates(
    public val enabled: Boolean?,
    public val interval: Duration?,
    public val securityOnly: Boolean?,
    underlyingNode: Node? = null,
) : Integrity(underlyingNode) {
    override fun equals(other: Any?): Boolean =
        other is AutomaticUpdates &&
            super.equals(other) &&
            other.enabled == this.enabled &&
            other.interval == this.interval &&
            other.securityOnly == this.securityOnly

    override fun hashCode(): Int = Objects.hash(super.hashCode(), enabled, interval, securityOnly)
}
