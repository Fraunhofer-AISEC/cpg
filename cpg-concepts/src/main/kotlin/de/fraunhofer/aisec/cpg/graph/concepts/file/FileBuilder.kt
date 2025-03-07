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

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom

fun MetadataProvider.newFile(underlyingNode: Node, fileName: String): File {
    val node = File(underlyingNode = underlyingNode, fileName = fileName)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileOpen(underlyingNode: Node, file: File): FileOpen {
    val node = FileOpen(underlyingNode = underlyingNode, concept = file)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileSetMask(underlyingNode: Node, file: File, mask: Long): FileSetMask {
    val node = FileSetMask(underlyingNode = underlyingNode, concept = file, mask = mask)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileSetFlags(
    underlyingNode: Node,
    file: File,
    flags: Set<FileAccessModeFlags>,
): FileSetFlags {
    val node = FileSetFlags(underlyingNode = underlyingNode, concept = file, flags = flags)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileClose(underlyingNode: Node, file: File): FileClose {
    val node = FileClose(underlyingNode = underlyingNode, concept = file)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [FileRead] node and attaches the DFG from the corresponding [file] to [this] and
 * then from [this] to the created node.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is reading from.
 * @return The new [FileRead] node.
 */
fun MetadataProvider.newFileRead(underlyingNode: Node, file: File): FileRead {
    val node =
        FileRead(underlyingNode = underlyingNode, concept = file, target = underlyingNode.nextDFG)
    node.codeAndLocationFrom(underlyingNode)

    // add DFG
    file.nextDFG += node
    node.nextDFG += underlyingNode

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [FileWrite] node and attaches the DFG from [what] to [this] and then from [this] to
 * the [file].
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is writing to.
 * @param what A list of nodes being written to the [file] (usually the arguments to a `write`
 *   call).
 * @return The new [FileWrite] node.
 */
fun MetadataProvider.newFileWrite(underlyingNode: Node, file: File, what: List<Node>): FileWrite {
    val node = FileWrite(underlyingNode = underlyingNode, concept = file, what = what)
    node.codeAndLocationFrom(underlyingNode)

    // add DFG
    what.forEach { it.nextDFG += node }
    node.nextDFG += file

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newFileChmod(underlyingNode: Node, file: File, mode: Long): FileChmod {
    val node = FileChmod(underlyingNode = underlyingNode, concept = file, mode = mode)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}
