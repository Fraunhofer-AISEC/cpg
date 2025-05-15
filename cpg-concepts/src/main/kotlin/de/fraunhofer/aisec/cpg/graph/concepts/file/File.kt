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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import java.util.*

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
 * O_EXEC      Open for execute only (non-directory files). The result is  unspecified  if this flag is applied to a directory.
 * O_RDONLY    Open for reading only.
 * O_RDWR      Open for reading and writing.
 * O_SEARCH    Open  directory  for search only. The result is unspecified if this flag is applied to a non-directory file.
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

/**
 * Represents the status of a file. This is used to determine if a file is a temporary file or not.
 */
enum class FileTempFileStatus {
    TEMP_FILE,
    TEMP_OR_NOT_TEMP,
    NOT_A_TEMP_FILE,
    UNKNOWN,
}

/** The bit-mask to be used to get the [FileAccessModeFlags] from an entire flags value. */
const val O_ACCMODE_MODE_MASK = 3L

/**
 * Represents a file.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param fileName The name of the file e.g. `foo/bar/example.txt`
 * @param isTempFile Whether this file is a temporary file or not.
 * @param deleteOnClose Whether this file will be automatically deleted when closed.
 */
class File(
    underlyingNode: Node? = null,
    val fileName: String,
    var isTempFile: FileTempFileStatus = FileTempFileStatus.UNKNOWN,
    var deleteOnClose: Boolean = false,
) : Concept(underlyingNode = underlyingNode), IsFile {
    override fun equals(other: Any?): Boolean {
        return other is File &&
            super.equals(other) &&
            other.fileName == this.fileName &&
            other.isTempFile == this.isTempFile &&
            other.deleteOnClose == this.deleteOnClose
    }

    override fun hashCode() = Objects.hash(super.hashCode(), fileName, isTempFile, deleteOnClose)
}

/**
 * Represents setting flags on a file. For example when opening the file.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param concept The corresponding [File] node.
 * @param flags A set of file flags (see [FileAccessModeFlags]).
 */
class SetFileFlags(
    underlyingNode: Node? = null,
    concept: File,
    val flags: Set<FileAccessModeFlags>,
) : FileOperation(underlyingNode = underlyingNode, file = concept), IsFile {
    override fun equals(other: Any?): Boolean {
        return other is SetFileFlags && super.equals(other) && other.flags == this.flags
    }

    override fun hashCode() = Objects.hash(super.hashCode(), flags)
}

/**
 * Represents setting the umask, for example with the `mode` parameter in a Python `os.open` call or
 * a `chmod` call.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param concept The corresponding [File] node.
 * @param mask The file mask in UNIX notation (i.e. 0o644)
 */
class SetFileMask(underlyingNode: Node? = null, concept: File, val mask: Long) :
    FileOperation(underlyingNode = underlyingNode, file = concept), IsFile {
    override fun equals(other: Any?): Boolean {
        return other is SetFileMask && super.equals(other) && other.mask == this.mask
    }

    override fun hashCode() = Objects.hash(super.hashCode(), mask)
}

/**
 * Represents closing a file.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param concept The corresponding [File] node.
 */
class CloseFile(underlyingNode: Node? = null, concept: File) :
    FileOperation(underlyingNode = underlyingNode, file = concept), IsFile {}

/**
 * Represents deleting a file.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param concept The corresponding [File] node.
 */
class DeleteFile(underlyingNode: Node? = null, concept: File) :
    FileOperation(underlyingNode = underlyingNode, file = concept), IsFile {}

/**
 * Represents opening a file. This is usually done with the same underlying node the [concept] field
 * is attached to.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param concept The corresponding [File] node.
 */
class OpenFile(underlyingNode: Node? = null, concept: File) :
    FileOperation(underlyingNode = underlyingNode, file = concept), IsFile {}

/**
 * Represents reading from a file.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param concept The corresponding [File] node.
 */
class ReadFile(underlyingNode: Node? = null, concept: File) :
    FileOperation(underlyingNode = underlyingNode, file = concept), IsFile {
    override fun setDFG() {
        this.file.nextDFG += this
        this.underlyingNode?.let { underlyingNode -> this.nextDFG += underlyingNode }
    }
}

/**
 * Represents writing to a file.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param concept The corresponding [File] node.
 * @param what The node being written to the file.
 */
class WriteFile(underlyingNode: Node? = null, concept: File, val what: Node) :
    FileOperation(underlyingNode = underlyingNode, file = concept), IsFile {
    override fun equals(other: Any?): Boolean {
        return other is WriteFile && super.equals(other) && other.what == this.what
    }

    override fun hashCode() = Objects.hash(super.hashCode(), what)

    override fun setDFG() {
        what.nextDFG += this
        this.nextDFG += file
    }
}

/**
 * All [File] [Operation]s inherit from this class. This makes the [file] field available for
 * [FileOperation], resulting in easier to read queries (one can use [FileOperation.file] instead of
 * [Operation.concept]). There is no logic involved - just a simple forwarding of the field.
 *
 * @param underlyingNode The underlying CPG node (usually a [CallExpression]).
 * @param file The corresponding [File] node.
 */
abstract class FileOperation(underlyingNode: Node? = null, file: File) :
    Operation(underlyingNode = underlyingNode, concept = file) {
    /**
     * The corresponding [File] [Concept] node. This is a convenience field and has the same effect
     * as using [Operation.concept].
     */
    val file: File
        get() = this.concept as File
}

/** TODO */
class FileHandle(underlyingNode: Node? = null) : Concept(underlyingNode = underlyingNode), IsFile
