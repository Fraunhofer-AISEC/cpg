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

/**
 * Creates a new [File] node. This node represents a file on a hard-disk somewhere.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param fileName The name of the file e.g. `foo/bar/example.txt`
 * @return The new [File] node.
 */
fun MetadataProvider.newFile(underlyingNode: Node, fileName: String): File {
    val node = File(underlyingNode = underlyingNode, fileName = fileName)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [OpenFile] node. This node represents opening a file.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is opening.
 * @return The new [OpenFile] node.
 */
fun MetadataProvider.newFileOpen(underlyingNode: Node, file: File): OpenFile {
    val node = OpenFile(underlyingNode = underlyingNode, concept = file)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [SetFileMask] node. This node represents changing a files permissions.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is modifying.
 * @param mask The file mask to set (in UNIX notation).
 * @return The new [SetFileMask] node.
 */
fun MetadataProvider.newFileSetMask(underlyingNode: Node, file: File, mask: Long): SetFileMask {
    val node = SetFileMask(underlyingNode = underlyingNode, concept = file, mask = mask)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [SetFileFlags] node.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is working on.
 * @param flags The file flags to set (in UNIX notation).
 * @return The new [SetFileFlags] node.
 */
fun MetadataProvider.newFileSetFlags(
    underlyingNode: Node,
    file: File,
    flags: Set<FileAccessModeFlags>,
): SetFileFlags {
    val node = SetFileFlags(underlyingNode = underlyingNode, concept = file, flags = flags)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [CloseFile] node.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is closing.
 * @return The new [CloseFile] node.
 */
fun MetadataProvider.newFileClose(underlyingNode: Node, file: File): CloseFile {
    val node = CloseFile(underlyingNode = underlyingNode, concept = file)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [DeleteFile] node.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is deleting.
 * @return The new [DeleteFile] node.
 */
fun MetadataProvider.newFileDelete(underlyingNode: Node, file: File): DeleteFile {
    val node = DeleteFile(underlyingNode = underlyingNode, concept = file)
    node.codeAndLocationFrom(underlyingNode)

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [ReadFile] node and attaches the DFG from the corresponding [file] to the new node
 * and then from [this] to the created node.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is reading from.
 * @return The new [ReadFile] node.
 */
fun MetadataProvider.newFileRead(underlyingNode: Node, file: File): ReadFile {
    val node =
        ReadFile(underlyingNode = underlyingNode, concept = file, target = underlyingNode.nextDFG)
    node.codeAndLocationFrom(underlyingNode)

    // add DFG
    file.nextDFG += node
    node.nextDFG += underlyingNode

    NodeBuilder.log(node)
    return node
}

/**
 * Creates a new [WriteFile] node and attaches the DFG from [what] to [this] and then from the new
 * node to the [file].
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The [File] this operation is writing to.
 * @param what A list of nodes being written to the [file] (usually the arguments to a `write`
 *   call).
 * @return The new [WriteFile] node.
 */
fun MetadataProvider.newFileWrite(underlyingNode: Node, file: File, what: List<Node>): WriteFile {
    val node = WriteFile(underlyingNode = underlyingNode, concept = file, what = what)
    node.codeAndLocationFrom(underlyingNode)

    // add DFG
    what.forEach { it.nextDFG += node }
    node.nextDFG += file

    NodeBuilder.log(node)
    return node
}
