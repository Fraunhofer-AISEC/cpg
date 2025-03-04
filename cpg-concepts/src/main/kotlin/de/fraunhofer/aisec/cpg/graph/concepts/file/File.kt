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

enum class FileFlags {
    RDONLY,
    WRONLY,
    RDWR,
    APPEND,
    BINARY,
    CREAT,
    EXCL,
    NOCTTY,
    NONBLOCK,
    TRUNC,
    UNKNOWN,
}

class File(underlyingNode: Node, val fileName: String) :
    Concept(underlyingNode = underlyingNode), IsFile {

    // TODO: encoding? newline?
}

class FileAppend(underlyingNode: Node, override val concept: File, val what: List<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

class FileSetFlags(underlyingNode: Node, override val concept: File, val flags: Set<FileFlags>) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsFile

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
