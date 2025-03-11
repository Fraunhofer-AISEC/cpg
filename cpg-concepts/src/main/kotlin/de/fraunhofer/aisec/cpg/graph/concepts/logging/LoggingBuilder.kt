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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation

/**
 * Creates a [Log] with the same metadata as the [underlyingNode].
 *
 * @param underlyingNode The underlying CPG node (e.g. a call expression creating a log).
 * @param name The name of the logger.
 * @return The new [Log].
 */
fun MetadataProvider.newLog(underlyingNode: Node, name: String) =
    newConcept({ Log(underlyingNode = underlyingNode) }, underlyingNode).apply {
        this.logName = name
    }

/**
 * Creates a [LogWrite] node with the same metadata as the [underlyingNode].
 *
 * DFG additions: the [underlyingNode] has a next DFG edge to the node created here and the node
 * created here has a next DFG edge to the log. This enables queries "what data is flowing to a
 * given log" or "is the sensitive data flowing to a log".
 *
 * @param underlyingNode The underlying CPG node (e.g. a call expression writing to a log).
 * @param level The [LogLevel] used for this write operation.
 * @param logArguments The underlying CPG nodes of the logging arguments, i.e. what is written to
 *   the log.
 * @return The new [Log].
 */
fun Log.newLogWrite(underlyingNode: Node, level: LogLevel, logArguments: List<Node>) =
    newOperation(
            { node, concept ->
                LogWrite(
                    underlyingNode = underlyingNode,
                    concept = concept,
                    logArguments = logArguments,
                    logLevel = level,
                )
            },
            underlyingNode = underlyingNode,
            concept = this,
        )
        .apply {
            this.nextDFG += this@newLogWrite
            this.prevDFG += logArguments
        }

/**
 * Creates a [LogGet] node with the same metadata as the [underlyingNode].
 *
 * @param underlyingNode The underlying CPG node (e.g. a call expression writing to a log).
 * @return The new [LogGet].
 */
fun Log.newLogGet(underlyingNode: Node) =
    newOperation(
        { node, concept -> LogGet(underlyingNode = underlyingNode, concept = concept) },
        underlyingNode = underlyingNode,
        concept = this,
    )
