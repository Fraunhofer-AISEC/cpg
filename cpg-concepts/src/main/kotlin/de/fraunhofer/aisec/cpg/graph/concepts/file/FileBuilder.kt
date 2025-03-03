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
package de.fraunhofer.aisec.cpg.graph.concepts.file

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression

fun MetadataProvider.newFileNode(underlyingNode: Node, fileName: String, accessMode: String): File {
    val node =
        File(
            underlyingNode = underlyingNode,
            accessMode =
                when (accessMode) {
                    "r",
                    "rt",
                    "rb" -> FileAccessMode.READ
                    "w",
                    "wt",
                    "wb" -> FileAccessMode.WRITE
                    "a",
                    "at",
                    "ab" -> FileAccessMode.APPEND
                    else -> FileAccessMode.UNKNOWN
                },
            fileName = fileName,
            opNodes = HashSet(),
        )
    node.codeAndLocationFrom(underlyingNode)

    node.name = Name(fileName) // to have a nice name in Neo4j

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileReadNode(underlyingNode: Node, file: File): FileRead {
    val node =
        FileRead(underlyingNode = underlyingNode, concept = file, target = underlyingNode.nextDFG)
    node.codeAndLocationFrom(underlyingNode)

    node.name = Name("read") // to have a nice name in Neo4j

    file.ops += node

    // add DFG
    node.nextDFG += underlyingNode

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileWriteNode(underlyingNode: Node, file: File): FileWrite {
    val node =
        FileWrite(
            underlyingNode = underlyingNode,
            concept = file,
            what = (underlyingNode as? CallExpression)?.arguments ?: listOf(),
        )
    node.codeAndLocationFrom(underlyingNode)

    node.name = Name("write") // to have a nice name in Neo4j

    file.ops += node

    // add DFG
    underlyingNode.parameters.forEach { it.nextDFG += node }

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileAppendNode(underlyingNode: Node, file: File): FileAppend {
    val node =
        FileAppend(
            underlyingNode = underlyingNode,
            concept = file,
            what = (underlyingNode as? CallExpression)?.arguments ?: listOf(),
        )
    node.codeAndLocationFrom(underlyingNode)

    node.name = Name("write") // to have a nice name in Neo4j

    file.ops += node

    // add DFG
    underlyingNode.parameters.forEach { it.nextDFG += node }

    NodeBuilder.log(node)
    return node
}
