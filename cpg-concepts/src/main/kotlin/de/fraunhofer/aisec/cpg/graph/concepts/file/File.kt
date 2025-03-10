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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

/**
 * This interface indicates that the corresponding node is connected to a file concept or operation.
 */
interface IsFile

/**
 * Represents a file access mode flag.
 *
 * man 0p fcntl.h
 *
 * ```
 * The <fcntl.h> header shall define the following symbolic constants for use as the  file access  modes  for  open(),  openat(), and fcntl().  The values shall be unique, except that O_EXEC and O_SEARCH may have equal values. The values shall be suitable for use in #if preprocessing directives.
 *
 * O_EXEC      Open for execute only (non-directory files). The result is  unspecified  if this flag is applied to a directory.
 *
 * O_RDONLY    Open for reading only.
 *
 * O_RDWR      Open for reading and writing.
 *
 * O_SEARCH    Open  directory  for search only. The result is unspecified if this flag is applied to a non-directory file.
 *
 * O_WRONLY    Open for writing only.
 * ```
 */
enum class FileAccessModeFlags(val value: Long) : IsFile {
    // O_EXEC(-1), not supported
    O_RDONLY(0),
    O_RDWR(2),
    // O_SEARCH(-1), not supported
    O_WRONLY(1),
}

/** The bit-mask to be used to get the [FileAccessModeFlags] from an entire flags value. */
val O_ACCMODE_MODE_MASK = 3L

/** Represents a file. */
class File(underlyingNode: Node, val fileName: String) :
    Concept(underlyingNode = underlyingNode), IsFile

/** Represents setting flags on a file. For example when opening the file. */
class SetFileFlags(
    underlyingNode: Node,
    override val concept: File,
    val flags: Set<FileAccessModeFlags>,
) : Operation(underlyingNode = underlyingNode, concept = concept), IsFile

/**
 * Represents setting the umask, for example with the `mode` parameter in a Python `os.open` call or
 * a `chmod` call.
 */
class SetFileMask(underlyingNode: Node, override val concept: File, val mask: Long) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

/** Represents closing a file. */
class CloseFile(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

/** Represents deleting a file. */
class DeleteFile(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

/**
 * Represents opening a file. This is usually done with the same underlying node the [concept] field
 * is attached to.
 */
class OpenFile(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

/** Represents reading from a file. */
class ReadFile(underlyingNode: Node, override val concept: File, val target: Set<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

/** Represents writing to a file. */
class WriteFile(underlyingNode: Node, override val concept: File, val what: List<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile
