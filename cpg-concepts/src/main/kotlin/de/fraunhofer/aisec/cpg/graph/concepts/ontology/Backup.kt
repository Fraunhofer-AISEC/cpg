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
import java.time.Duration
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/** RetentionPeriod in hours */
public abstract class Backup(
    public val enabled: Boolean?,
    public val interval: Duration?,
    public val retentionPeriod: Duration?,
    public val storage: Storage?,
    public val transportEncryption: Boolean?,
    underlyingNode: Node?,
) : Availability(underlyingNode) {
    override fun equals(other: Any?): Boolean =
        other is Backup &&
            super.equals(other) &&
            other.enabled == this.enabled &&
            other.interval == this.interval &&
            other.retentionPeriod == this.retentionPeriod &&
            other.storage == this.storage &&
            other.transportEncryption == this.transportEncryption

    override fun hashCode(): Int =
        Objects.hash(
            super.hashCode(),
            enabled,
            interval,
            retentionPeriod,
            storage,
            transportEncryption,
        )
}
