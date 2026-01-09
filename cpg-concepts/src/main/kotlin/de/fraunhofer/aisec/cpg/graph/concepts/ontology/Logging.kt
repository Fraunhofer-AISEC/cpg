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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.time.Duration

public open class Logging(
    val logLevelThreshold: LogLevel?,
    public val enabled: Boolean?,
    public val monitoringEnabled: Boolean?,
    name: String?,
    public val retentionPeriod: Duration?,
    public val securityAlertsEnabled: Boolean?,
    public val loggingService: LoggingService?,
    underlyingNode: Node? = null,
) : Auditing(underlyingNode) {
    init {
        name?.let { this.name = Name(localName = it) }
    }

    override fun equals(other: Any?): Boolean =
        other is Logging &&
            super.equals(other) &&
            other.enabled == this.enabled &&
            other.monitoringEnabled == this.monitoringEnabled &&
            other.name == this.name &&
            other.retentionPeriod == this.retentionPeriod &&
            other.securityAlertsEnabled == this.securityAlertsEnabled &&
            other.loggingService == this.loggingService &&
            other.logLevelThreshold == this.logLevelThreshold

    override fun hashCode(): Int =
        Objects.hash(
            super.hashCode(),
            enabled,
            monitoringEnabled,
            name,
            retentionPeriod,
            securityAlertsEnabled,
            loggingService,
            logLevelThreshold,
        )
}
