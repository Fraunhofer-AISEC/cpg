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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom

/**
 * Creates a [Log] with the same metadata as the [underlyingNode].
 *
 * @param underlyingNode The underlying CPG node (e.g. a call expression creating a log).
 * @param name The name of the logger.
 * @return The new [Log].
 */
fun ContextProvider.newLog(underlyingNode: Node, name: String): Log {
    val node = Log(underlyingNode = underlyingNode)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
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
 * @param logger The corresponding [Log], i.e. the log where the underlying nodes is writing to.
 * @param logArguments The underlying CPG nodes of the logging arguments, i.e. what is written to
 *   the log.
 * @return The new [Log].
 */
fun ContextProvider.newLogWrite(
    underlyingNode: Node,
    level: LogLevel,
    logger: Log,
    logArguments: List<Node>,
): LogWrite {
    val node =
        LogWrite(
            underlyingNode = underlyingNode,
            concept = logger,
            logArguments = logArguments,
            logLevel = level,
        )
    node.codeAndLocationFrom(underlyingNode)

    logger.ops += node

    // connect DFG
    logArguments.forEach { cpgArgNode -> cpgArgNode.nextDFG += node }
    node.nextDFG += logger

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a [LogGet] node with the same metadata as the [underlyingNode].
 *
 * @param underlyingNode The underlying CPG node (e.g. a call expression writing to a log).
 * @param logger The corresponding [Log], i.e. the log where the underlying nodes is writing to.
 * @return The new [LogGet].
 */
fun ContextProvider.newLogGet(underlyingNode: Node, logger: Log): LogGet {
    val node = LogGet(underlyingNode = underlyingNode, concept = logger)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}
