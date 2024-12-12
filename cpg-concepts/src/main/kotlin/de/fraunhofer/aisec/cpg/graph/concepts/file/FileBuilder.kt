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
    cpgNode: Node,
    result: TranslationResult,
    fileName: String,
    accessMode: String
): FileNode {
    val node =
        FileNode(
            cpgNode = cpgNode,
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
    node.codeAndLocationFrom(cpgNode)

    node.name = Name(fileName) // to have a nice name in Neo4j

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileReadNode(
    cpgNode: Node,
    result: TranslationResult,
    fileNode: FileNode,
): FileReadNode {
    val node = FileReadNode(cpgNode = result, fileNode = fileNode, target = cpgNode.nextDFG)
    node.codeAndLocationFrom(cpgNode)

    node.name = Name("read") // to have a nice name in Neo4j

    fileNode.ops += node

    // add DFG
    node.nextDFG += cpgNode

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileWriteNode(
    cpgNode: Node,
    result: TranslationResult,
    fileNode: FileNode,
): FileWriteNode {
    val node =
        FileWriteNode(
            cpgNode = cpgNode,
            fileNode = fileNode,
            what = (cpgNode as? CallExpression)?.arguments ?: listOf()
        )
    node.codeAndLocationFrom(cpgNode)

    node.name = Name("write") // to have a nice name in Neo4j

    fileNode.ops += node

    // add DFG
    cpgNode.parameters.forEach { it.nextDFG += node }

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileAppendNode(
    cpgNode: Node,
    result: TranslationResult,
    fileNode: FileNode,
): FileAppendNode {
    val node =
        FileAppendNode(
            cpgNode = cpgNode,
            fileNode = fileNode,
            what = (cpgNode as? CallExpression)?.arguments ?: listOf()
        )
    node.codeAndLocationFrom(cpgNode)

    node.name = Name("write") // to have a nice name in Neo4j

    fileNode.ops += node

    // add DFG
    cpgNode.parameters.forEach { it.nextDFG += node }

    result.additionalNodes += node
    NodeBuilder.log(node)
    return node
}
