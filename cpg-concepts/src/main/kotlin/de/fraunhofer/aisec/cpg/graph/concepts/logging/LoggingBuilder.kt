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
package de.fraunhofer.aisec.cpg.graph.concepts.logging

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.LogGet
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.LogLevel
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.LogWrite
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.Logging
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.LoggingService
import kotlin.time.Duration

/**
 * Creates a [Logging] with the same metadata as the [underlyingNode].
 *
 * @param underlyingNode The underlying CPG node (e.g. a call expression creating a log).
 * @param name The name of the logger.
 * @param logLevelThreshold The minimum log level threshold for this logger.
 * @param enabled Whether the logger is enabled.
 * @param monitoringEnabled Whether monitoring is enabled for this logger.
 * @param retentionPeriod The retention period for log data.
 * @param securityAlertsEnabled Whether security alerts are enabled.
 * @param loggingService The logging service associated with this logger.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The new [Logging].
 */
fun MetadataProvider.newLogging(
    underlyingNode: Node,
    name: String,
    logLevelThreshold: LogLevel? = null,
    enabled: Boolean? = null,
    monitoringEnabled: Boolean? = null,
    retentionPeriod: Duration? = null,
    securityAlertsEnabled: Boolean? = null,
    loggingService: LoggingService? = null,
    connect: Boolean,
) =
    newConcept(
            {
                Logging(
                    logLevelThreshold = logLevelThreshold,
                    enabled = enabled,
                    monitoringEnabled = monitoringEnabled,
                    name = name,
                    retentionPeriod = retentionPeriod,
                    securityAlertsEnabled = securityAlertsEnabled,
                    loggingService = loggingService,
                    underlyingNode = underlyingNode,
                )
            },
            underlyingNode = underlyingNode,
            connect = connect,
        )
        .apply { this.name = Name(localName = name) }

/**
 * Creates a [LogWrite] node with the same metadata as the [underlyingNode].
 *
 * DFG additions: the [underlyingNode] has a next DFG edge to the node created here and the node
 * created here has a next DFG edge to the log. This enables queries "what data is flowing to a
 * given log" or "is the sensitive data flowing to a log".
 *
 * @param underlyingNode The underlying CPG node (e.g. a call expression writing to a log).
 * @param concept The [Logging] concept this operation belongs to.
 * @param level The [LogLevel] used for this write operation.
 * @param logArguments The underlying CPG nodes of the logging arguments, i.e. what is written to
 *   the log.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The new [LogWrite].
 */
fun MetadataProvider.newLogWrite(
    underlyingNode: Node,
    concept: Logging,
    level: LogLevel,
    logArguments: List<Node?>,
    connect: Boolean,
) =
    newOperation(
            { concept ->
                LogWrite(linkedConcept = concept, logArguments = logArguments, logLevel = level)
            },
            underlyingNode = underlyingNode,
            concept = concept,
            connect = connect,
        )
        .apply {
            this.nextDFG += concept
            this.prevDFG += logArguments.filterNotNull()
        }

/**
 * Creates a [LogGet] node with the same metadata as the [underlyingNode].
 *
 * @param underlyingNode The underlying CPG node (e.g. a call expression writing to a log).
 * @param concept The [Logging] concept this operation belongs to.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The new [LogGet].
 */
fun MetadataProvider.newLogGet(underlyingNode: Node, concept: Logging, connect: Boolean) =
    newOperation(
        { concept -> LogGet(linkedConcept = concept) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )
