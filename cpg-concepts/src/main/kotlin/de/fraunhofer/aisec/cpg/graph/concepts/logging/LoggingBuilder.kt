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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*

fun MetadataProvider.newLoggingNode(underlayingNode: Node, result: TranslationResult): LoggingNode {
    val node = LoggingNode(underlayingNode = underlayingNode)
    node.codeAndLocationFrom(underlayingNode)

    node.name = Name("Log") // to have a nice name in Neo4j

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newLogOperationNode(
    underlayingNode: Node,
    result: TranslationResult,
    level: String,
    logger: LoggingNode,
    logArguments: List<Node>
): LogOperationNode {
    val node =
        LogOperationNode(
            underlayingNode = underlayingNode,
            concept = logger,
            logArguments = logArguments,
            logLevel =
                when (level) {
                    "critical" -> LogLevel.CRITICAL
                    "error" -> LogLevel.ERROR
                    "warning" -> LogLevel.WARN
                    "info" -> LogLevel.INFO
                    "debug" -> LogLevel.DEBUG
                    else -> LogLevel.UNKNOWN
                }
        )
    node.codeAndLocationFrom(underlayingNode)

    node.name = Name("log." + node.logLevel) // to have a nice name in Neo4j

    logger.ops += node

    // connect DFG
    logArguments.forEach { cpgArgNode ->
        cpgArgNode.nextDFG += node
        // cpgArgNode.nextEOG += node
        // node.nextEOG += cpgArgNode
    }

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}
