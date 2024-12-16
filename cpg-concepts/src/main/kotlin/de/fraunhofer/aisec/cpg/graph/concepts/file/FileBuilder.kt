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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression

fun MetadataProvider.newFileNode(
    underlayingNode: Node,
    result: TranslationResult,
    fileName: String,
    accessMode: String
): FileNode {
    val node =
        FileNode(
            underlayingNode = underlayingNode,
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
            ops = HashSet()
        )
    node.codeAndLocationFrom(underlayingNode)

    node.name = Name(fileName) // to have a nice name in Neo4j

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileReadNode(
    underlayingNode: Node,
    result: TranslationResult,
    fileNode: FileNode,
): FileReadNode {
    val node =
        FileReadNode(underlayingNode = result, concept = fileNode, target = underlayingNode.nextDFG)
    node.codeAndLocationFrom(underlayingNode)

    node.name = Name("read") // to have a nice name in Neo4j

    fileNode.ops += node

    // add DFG
    node.nextDFG += underlayingNode

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileWriteNode(
    underlayingNode: Node,
    result: TranslationResult,
    fileNode: FileNode,
): FileWriteNode {
    val node =
        FileWriteNode(
            underlayingNode = underlayingNode,
            concept = fileNode,
            what = (underlayingNode as? CallExpression)?.arguments ?: listOf()
        )
    node.codeAndLocationFrom(underlayingNode)

    node.name = Name("write") // to have a nice name in Neo4j

    fileNode.ops += node

    // add DFG
    underlayingNode.parameters.forEach { it.nextDFG += node }

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileAppendNode(
    underlayingNode: Node,
    result: TranslationResult,
    fileNode: FileNode,
): FileAppendNode {
    val node =
        FileAppendNode(
            underlayingNode = underlayingNode,
            concept = fileNode,
            what = (underlayingNode as? CallExpression)?.arguments ?: listOf()
        )
    node.codeAndLocationFrom(underlayingNode)

    node.name = Name("write") // to have a nice name in Neo4j

    fileNode.ops += node

    // add DFG
    underlayingNode.parameters.forEach { it.nextDFG += node }

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}
