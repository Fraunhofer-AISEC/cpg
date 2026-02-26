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
package de.fraunhofer.aisec.cpg.graph.concepts.file

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Call

/**
 * Creates a new [File] node. This node represents a file on a hard-disk somewhere.
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param fileName The name of the file e.g. `foo/bar/example.txt`
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The new [File] node.
 */
fun MetadataProvider.newFile(underlyingNode: Node, fileName: String, connect: Boolean) =
    newConcept({ File(fileName = fileName) }, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [OpenFile] node. This node represents opening a file.
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param file The [File] this operation is opening.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [file] by its edge [Concept.ops].
 * @return The new [OpenFile] node.
 */
fun MetadataProvider.newFileOpen(underlyingNode: Node, file: File, connect: Boolean) =
    newOperation(
        { concept -> OpenFile(concept = file) },
        underlyingNode = underlyingNode,
        concept = file,
        connect = connect,
    )

/**
 * Creates a new [SetFileMask] node. This node represents changing a files permissions.
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param file The [File] this operation is modifying.
 * @param mask The file mask to set (in UNIX notation).
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [file] by its edge [Concept.ops].
 * @return The new [SetFileMask] node.
 */
fun MetadataProvider.newFileSetMask(
    underlyingNode: Node,
    file: File,
    mask: Long,
    connect: Boolean,
) =
    newOperation(
        { concept -> SetFileMask(concept = concept, mask = mask) },
        underlyingNode = underlyingNode,
        concept = file,
        connect = connect,
    )

/**
 * Creates a new [SetFileFlags] node.
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param file The [File] this operation is working on.
 * @param flags The file flags to set (in UNIX notation).
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [file] by its edge [Concept.ops].
 * @return The new [SetFileFlags] node.
 */
fun MetadataProvider.newFileSetFlags(
    underlyingNode: Node,
    file: File,
    flags: Set<FileAccessModeFlags>,
    connect: Boolean,
) =
    newOperation(
        { concept -> SetFileFlags(concept = file, flags = flags) },
        underlyingNode = underlyingNode,
        concept = file,
        connect = connect,
    )

/**
 * Creates a new [CloseFile] node.
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param file The [File] this operation is closing.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [file] by its edge [Concept.ops].
 * @return The new [CloseFile] node.
 */
fun MetadataProvider.newFileClose(underlyingNode: Node, file: File, connect: Boolean) =
    newOperation(
        { concept -> CloseFile(concept = file) },
        underlyingNode = underlyingNode,
        concept = file,
        connect = connect,
    )

/**
 * Creates a new [DeleteFile] node.
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param file The [File] this operation is deleting.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [file] by its edge [Concept.ops].
 * @return The new [DeleteFile] node.
 */
fun MetadataProvider.newFileDelete(underlyingNode: Node, file: File, connect: Boolean) =
    newOperation(
        { concept -> DeleteFile(concept = file) },
        underlyingNode = underlyingNode,
        concept = file,
        connect = connect,
    )

/**
 * Creates a new [ReadFile] node and attaches the DFG from the corresponding [file] to the new node
 * and then from [this] to the created node.
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param file The [File] this operation is reading from.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [file] by its edge [Concept.ops].
 * @return The new [ReadFile] node.
 */
fun MetadataProvider.newFileRead(underlyingNode: Node, file: File, connect: Boolean) =
    newOperation(
        { concept -> ReadFile(concept = file) },
        underlyingNode = underlyingNode,
        concept = file,
        connect = connect,
    )

/**
 * Creates a new [WriteFile] node and attaches the DFG from [what] to [this] and then from the new
 * node to the [file].
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param file The [File] this operation is writing to.
 * @param what A node being written to the [file] (usually the argument of a `write` call).
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [file] by its edge [Concept.ops].
 * @return The new [WriteFile] node.
 */
fun MetadataProvider.newFileWrite(underlyingNode: Node, file: File, what: Node, connect: Boolean) =
    newOperation(
        { concept -> WriteFile(concept = file, what = what) },
        underlyingNode = underlyingNode,
        concept = file,
        connect = connect,
    )

/**
 * Creates a new [FileHandle] node. Unlike the [File] directly, this represents a handle to a file.
 *
 * @param underlyingNode The underlying CPG node (usually a [Call]).
 * @param fileName The name of the file handle, e.g. `foo/bar/example.txt`
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The new [FileHandle] node.
 */
fun MetadataProvider.newFileHandle(
    underlyingNode: Node,
    fileName: String,
    tempFileStatus: FileTempFileStatus = FileTempFileStatus.UNKNOWN,
    connect: Boolean,
) =
    newConcept(
        { FileHandle(fileName = fileName, isTempFile = tempFileStatus) },
        underlyingNode = underlyingNode,
        connect = connect,
    )
