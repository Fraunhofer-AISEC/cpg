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

enum class FileAccessMode {
    READ,
    WRITE,
    APPEND,
    UNKNOWN,

    // what do we want to have here? binary? text? r+ vs w+? create mode? ...?
}

class File(
    underlyingNode: Node,
    val opNodes: MutableSet<Operation>,
    val fileName: String,
    val accessMode: FileAccessMode,
) : Concept(underlyingNode = underlyingNode), IsFile {
    init { // TODO this is ugly
        ops += opNodes
    }

    /*
    override fun hashCode(): Int {
        return Objects.hash(
            super.hashCode(),
            underlyingNode,
            fileName,
            accessMode,
        ) // TODO: exclude ops because this would result in a circular reference. how to do this in
        // a nice way?
    }
     */

    // TODO: encoding? newline?
}

class FileAppend(underlyingNode: Node, override val concept: File, val what: List<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept)

class FileChangePermissions(
    underlyingNode: Node,
    override val concept: File,
    val newPermissions: String,
) : Operation(underlyingNode = underlyingNode, concept = concept)

class FileClose(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept)

class FileDelete(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept)

class FileOpen(underlyingNode: Node, override val concept: File) :
    Operation(underlyingNode = underlyingNode, concept = concept)

class FileRead(underlyingNode: Node, override val concept: File, val target: Set<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept)

class FileWrite(underlyingNode: Node, override val concept: File, val what: List<Node>) :
    Operation(underlyingNode = underlyingNode, concept = concept)
