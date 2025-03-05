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

/** This interface indicates that the corresponding node is connected to a file concept. */
interface IsFile

/**
 * man 0p fcntl.h
 *
 * ```
 * The <fcntl.h> header shall define the following symbolic constants for use as the  file
 *        access  modes  for  open(),  openat(), and fcntl().  The values shall be unique, except
 *        that O_EXEC and O_SEARCH may have equal values. The values shall be suitable for use in
 *        #if preprocessing directives.
 *
 *        O_EXEC      Open for execute only (non-directory files). The result is  unspecified  if
 *                    this flag is applied to a directory.
 *
 *        O_RDONLY    Open for reading only.
 *
 *        O_RDWR      Open for reading and writing.
 *
 *        O_SEARCH    Open  directory  for search only. The result is unspecified if this flag is
 *                    applied to a non-directory file.
 *
 *        O_WRONLY    Open for writing only.
 * ```
 */
enum class FileAccessModeFlags(val value: Long) : IsFile {
    /*
    ```python
    print("\n".join(['{}({}L),'.format(symbol.replace('O_', ''), os.__dict__[symbol]) for symbol in os.__all__ if symbol.startswith('O_')]))
    ```
     */

    // O_EXEC(-1), not supported
    O_RDONLY(0),
    O_RDWR(2),
    // O_SEARCH(-1), not supported
    O_WRONLY(1),
}

val O_ACCMODE_MODE_MASK = 3L

class File(underlyingNode: Node, val fileName: String) :
    Concept(underlyingNode = underlyingNode), IsFile {

    // TODO: encoding? newline?
}

class FileAppend(underlyingNode: Node, override val concept: File, val what: List<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileSetFlags(
    underlyingNode: Node,
    override val concept: File,
    val flags: Set<FileAccessModeFlags>,
) : Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileSetMask(underlyingNode: Node, override val concept: File, val mask: Long) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileClose(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileDelete(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileOpen(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileRead(underlyingNode: Node, override val concept: File, val target: Set<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileWrite(underlyingNode: Node, override val concept: File, val what: List<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileChmod(underlyingNode: Node, override val concept: File, val mode: Long) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile
