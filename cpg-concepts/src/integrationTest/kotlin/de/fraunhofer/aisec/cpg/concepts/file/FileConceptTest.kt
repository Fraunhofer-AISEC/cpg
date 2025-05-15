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
package de.fraunhofer.aisec.cpg.concepts.file

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPrePass
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileJoinPass
import de.fraunhofer.aisec.cpg.query.Must
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.query.executionPath
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

/**
 * A class for integration tests. They depend on the Python frontend, so we classify them as an
 * integration test. This might be replaced with a language-neutral test at some point.
 */
class FileConceptTest : BaseTest() {
    @Test
    fun testRead() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val fileNodes = result.allChildrenWithOverlays<IsFile>()
        assertTrue(fileNodes.isNotEmpty())

        val file = fileNodes.filterIsInstance<File>().singleOrNull()
        assertNotNull(
            file,
            "Expected to find exactly one \"File\" node but found ${fileNodes.filterIsInstance<File>().size}.",
        )
        assertEquals("example.txt", file.fileName, "Expected to find the filename \"example.txt\".")
        assertEquals(
            5,
            file.ops.size,
            "Expected to find 5 operations (open, read, flags, 2 x close (one for normally exiting `with` and one for the `catch` exit)).",
        )

        val fileNameLiteral = result.literals.singleOrNull { it.value == "example.txt" }
        assertNotNull(fileNameLiteral, "Expected to find exactly one literal \"example.txt\".")
        assertEquals(
            fileNameLiteral,
            file.underlyingNode,
            "Expected the file to be connected to the string literal \"example.txt\".",
        )

        val fileHandle = fileNodes.filterIsInstance<FileHandle>().singleOrNull()
        assertNotNull(fileHandle, "Expected to find exactly one `FileHandle` node")

        val setFileFlags = fileNodes.filterIsInstance<SetFileFlags>().singleOrNull()
        assertNotNull(setFileFlags)
        assertEquals(
            setOf(FileAccessModeFlags.O_RDONLY),
            setFileFlags.flags,
            "Expected to find access mode \"RDONLY\".",
        )

        val contentRef = result.refs("content").singleOrNull()
        assertNotNull(contentRef)

        assertTrue(
            dataFlow(startNode = file) { it == contentRef }.value,
            "Expected to find dataflow from the \"File\" to the \"content\" variable.",
        )

        val readFile = fileNodes.filterIsInstance<ReadFile>().singleOrNull()
        assertNotNull(readFile)
        assertEquals(
            readFile,
            file.nextDFG.singleOrNull(),
            "Expected to have exactly one dataflow from \"File\" (it must be to \"ReadFile\").",
        )

        // follow the EOG from open -> read -> close
        // tested in two steps: open -> read and read -> close
        val fileOpen = fileNodes.filterIsInstance<OpenFile>().singleOrNull()
        assertNotNull(fileOpen)
        assertTrue(
            executionPath(startNode = fileOpen, predicate = { it == readFile }).value,
            "Expected to find an execution path from open to read.",
        )

        val fileRead = fileNodes.filterIsInstance<ReadFile>().singleOrNull()
        assertNotNull(fileRead)
        assertEquals(
            2,
            fileNodes.filterIsInstance<CloseFile>().size,
            "We expect 2 x close (one for normally exiting `with` and one for the `catch` exit) operations.",
        )

        val executionPathReadToCloseMust =
            executionPath(startNode = fileRead, type = Must, predicate = { it is CloseFile })
        assertTrue(
            executionPathReadToCloseMust.value,
            "Expected to find an execution path from read to close.",
        )
    }

    @Test
    fun testWrite() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_write.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val fileNodes = result.allChildrenWithOverlays<IsFile>()
        assertTrue(fileNodes.isNotEmpty())

        val file = fileNodes.filterIsInstance<File>().singleOrNull()
        assertNotNull(file, "Expected to find exactly one \"File\" node.")
        assertEquals("example.txt", file.fileName, "Expected to find the filename \"example.txt\".")
        assertEquals(
            5,
            file.ops.size,
            "Expected to find 5 operations (open, read, flags, 2 x close (one for normally exiting `with` and one for the `catch` exit)).",
        )

        val setFileFlags = fileNodes.filterIsInstance<SetFileFlags>().singleOrNull()
        assertNotNull(setFileFlags)
        assertEquals(
            setOf(FileAccessModeFlags.O_WRONLY),
            setFileFlags.flags,
            "Expected to find access mode \"WRONLY\".",
        )

        val helloWorld = result.literals.singleOrNull { it.value == "Hello world!" }
        assertNotNull(helloWorld)

        assertTrue(
            dataFlow(startNode = helloWorld) { it == file }.value,
            "Expected to find dataflow from the \"Hello world!\" literal to the \"File\" node.",
        )

        val writeFile = fileNodes.filterIsInstance<WriteFile>().singleOrNull()
        assertNotNull(writeFile)
        assertEquals(
            writeFile,
            file.prevDFG.singleOrNull(),
            "Expected to have exactly one dataflow to \"File\" (it must be to \"WriteFile\").",
        )
    }

    @Test
    fun testMaskWrite() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_os_open.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val fileNodes = result.allChildrenWithOverlays<IsFile>()
        assertTrue(fileNodes.isNotEmpty())

        val maskNode = fileNodes.filterIsInstance<SetFileMask>().singleOrNull()
        assertNotNull(maskNode)
        assertEquals(0x180, maskNode.mask, "Expected the mask to have value 0o600.")

        val flagsNode = fileNodes.filterIsInstance<SetFileFlags>().singleOrNull()
        assertNotNull(flagsNode)
        assertEquals(
            setOf(FileAccessModeFlags.O_WRONLY),
            flagsNode.flags,
            "Expected to find exactly the flags \"WRONLY\". \"CREAT\" and \"TRUNC\" are not expected, as they are not access mode flags..",
        )

        // Tests mask is set before any write:
        // for all files
        //   for all WriteFile on the current file
        //     there is no SetFileMask on the current file after the WriteFile
        assertTrue(
            // See also testBadChmodQuery for a failing example
            fileNodes.filterIsInstance<File>().all { file ->
                file.ops.filterIsInstance<WriteFile>().none { write ->
                    val startNode =
                        write.underlyingNode
                            ?: return@none true // fail if there is no underlyingNode
                    executionPath(startNode = startNode, direction = Forward(GraphToFollow.EOG)) {
                            it.overlays.any { overlay ->
                                overlay is SetFileMask && write.file == overlay.file
                            }
                        }
                        .value == true
                }
            },
            "Found a chmod after a write. But there isn't one.",
        )
    }

    @Test
    fun testBadChmodQuery() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_os_open_bad.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val fileWrite = result.allChildrenWithOverlays<WriteFile>().singleOrNull()
        assertNotNull(fileWrite)
        assertNotNull(fileWrite.underlyingNode)
        val fileSetFMask =
            result.allChildrenWithOverlays<SetFileMask>().singleOrNull {
                it.underlyingNode?.location?.region?.startLine == 6
            }
        assertNotNull(fileSetFMask)
        assertEquals(fileWrite.file, fileSetFMask.file)
        val fileWriteEOG = fileWrite.collectAllNextEOGPaths(true).flatten().toSet()
        assertTrue(fileSetFMask in fileWriteEOG)

        assertEquals(1, result.allChildrenWithOverlays<File>().size)

        // Tests mask is set before any write:
        // for all files
        //   for all WriteFile on the current file
        //     there is no SetFileMask on the current file after the WriteFile
        assertFalse(
            // See also testBadChmodQuery for a failing example
            result.conceptNodes.filterIsInstance<File>().all { file ->
                file.ops.filterIsInstance<WriteFile>().none { write ->
                    executionPath(startNode = write, direction = Forward(GraphToFollow.EOG)) {
                            it is SetFileMask && write.file == it.file
                        }
                        .value == true
                }
            },
            "Didn't find a chmod after a write. But there is one.",
        )
    }

    @Test
    fun testChmodBadExample() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("write_before_chmod.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val conceptNodes = result.allChildrenWithOverlays<IsFile>()
        assertTrue(conceptNodes.isNotEmpty())

        val file = conceptNodes.filterIsInstance<File>().singleOrNull()
        val fileName = "/tmp/foo.txt"
        assertNotNull(file, "Expected to find a file.")
        assertEquals(fileName, file.fileName, "Expected the file to be \"$fileName\".")

        val write = conceptNodes.filterIsInstance<WriteFile>().singleOrNull()
        assertNotNull(write, "Expected to find a file write operation.")
        assertEquals(file, write.file, "Expected the write to write to our file node.")

        val chmod = conceptNodes.filterIsInstance<SetFileMask>().singleOrNull()
        assertNotNull(chmod, "Expected to find a file chmod operation.")
        assertEquals(file, chmod.file, "Expected the chmod to operate on our file node.")

        // Let's find our bad example
        assertTrue(
            executionPath(startNode = write, predicate = { it == chmod }).value,
            "Expected to find a violating execution path from write to chmod.",
        )
    }

    @Test
    fun testBranching() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_with_branching.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val conceptNodes = result.allChildrenWithOverlays<IsFile>()
        assertTrue(conceptNodes.isNotEmpty())

        val files = conceptNodes.filterIsInstance<File>()
        assertEquals(2, files.size, "Expected to find two `File` nodes (\"foo\" and \"bar\").")

        files.forEach { file ->
            assertTrue(
                dataFlow(startNode = file) { it is ReadFile }.value,
                "Expected to find a dataflow to `file.read()`.",
            )
        }
    }

    @Test
    fun testDelete() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_delete.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val conceptNodes = result.allChildrenWithOverlays<IsFile>()
        assertTrue(conceptNodes.isNotEmpty())

        val file = conceptNodes.filterIsInstance<File>().singleOrNull()
        assertNotNull(file, "Expected to find exactly one `File` node (\"example.txt\").")

        val fileRead = conceptNodes.filterIsInstance<ReadFile>().singleOrNull()
        assertNotNull(fileRead, "Expected to find a single file read operation.")

        assertEquals(
            2,
            conceptNodes.filterIsInstance<FileHandle>().size,
            "Expected to find one `FileHandle` node for each `open`.",
        )

        val fileDelete = conceptNodes.filterIsInstance<DeleteFile>().singleOrNull()
        assertNotNull(fileDelete, "Expected to find a file delete operation.")

        val fileWrite = conceptNodes.filterIsInstance<WriteFile>().singleOrNull()
        assertNotNull(fileWrite, "Expected to find a file write operation.")

        assertTrue(
            dataFlow(startNode = fileRead) { it == fileWrite }.value,
            "Expected to find a dataflow from the file read to the file write operation.",
        )

        assertTrue(
            executionPath(startNode = fileRead, predicate = { it == fileDelete }).value,
            "Expected to find an execution path from read to remove.",
        )

        assertTrue(
            executionPath(startNode = fileDelete, predicate = { it == fileWrite }).value,
            "Expected to find an execution path from remove to write.",
        )
    }

    @Test
    fun testLoop() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_loop.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val conceptNodes = result.allChildrenWithOverlays<IsFile>()
        assertTrue(conceptNodes.isNotEmpty())

        val files = conceptNodes.filterIsInstance<File>()
        assertEquals(
            setOf("a", "b"),
            files.map { it.fileName }.toSet(),
            "Expected to find two `File` nodes (\"a\" and \"b\").",
        )

        val writes = conceptNodes.filterIsInstance<WriteFile>()
        assertEquals(
            setOf("a", "b"),
            writes.map { it.file.fileName }.toSet(),
            "Expected to find two `WriteFile` nodes (to \"a\" and \"b\").",
        )
    }

    @Test
    fun testTempfile() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("tempfile.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val allTempFiles = result.conceptNodes.filterIsInstance<File>()

        assertEquals(2, allTempFiles.size, "Expected to find two temporary files")

        assertTrue(
            allTempFiles.all { it.isTempFile == FileTempFileStatus.TEMP_FILE },
            "Expected the files to be marked as temporary files.",
        )

        assertTrue(
            allTempFiles.all { it.deleteOnClose == true },
            "Expected the files to be marked as \"deleted on close\".",
        )

        assertEquals( // sanity check
            2,
            result.operationNodes.filterIsInstance<OpenFile>().size,
            "Expected to find two open operations.",
        )

        assertTrue(
            allTempFiles.all { tempFile ->
                val open =
                    result.operationNodes.singleOrNull { it is OpenFile && it.file == tempFile }
                assertNotNull(open, "Expected to find exactly one corresponding `OpenFile` node.")
                executionPath(startNode = open, type = Must, predicate = { it is CloseFile }).value
            },
            "Expected all temporary files to be closed.",
        )

        assertTrue(
            allTempFiles.all { tempFile ->
                val open =
                    result.operationNodes.singleOrNull { it is OpenFile && it.file == tempFile }
                assertNotNull(open, "Expected to find exactly one corresponding `OpenFile` node.")
                executionPath(startNode = open, type = Must, predicate = { it is DeleteFile })
                    .value // TODO: ASSUMPTION: this only works with `with` because we currently
                // only model `close` here.
            },
            "Expected all temporary files to be deleted.",
        )

        assertTrue(
            allTempFiles.all { tempFile ->
                val setPermissions = tempFile.ops.filterIsInstance<SetFileMask>().singleOrNull()
                assertNotNull(setPermissions)
                setPermissions.mask == 384L /* 0600 in decimal notation */
            },
            "Expected to find 0600 permissions for the temp files.",
        )

        assertEquals(
            allTempFiles.size,
            allTempFiles.map { it.fileName }.toSet().count(),
            "Expected the temp files to have unique names.",
        )
    }

    @Test
    fun testTempfileInTmp() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("tempInTmp.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val file = result.conceptNodes.filterIsInstance<File>().singleOrNull()
        assertNotNull(file, "Expected to find exactly one `File` node (\"/tmp/example.txt\").")

        assertTrue(
            file.isTempFile == FileTempFileStatus.TEMP_FILE,
            "Expected the file to be marked as a temporary file because it lives in `/tmp`.",
        )

        assertFalse(
            file.deleteOnClose,
            "Expected the file to not be marked as \"deleted on close\".",
        )
    }

    @Test
    fun testTempOrNot() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("tempOrNot.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val files = result.conceptNodes.filterIsInstance<File>()
        assertEquals(2, files.size, "Expected to find exactly two `File` nodes.")

        assertEquals(
            1,
            files.filter { it.isTempFile == FileTempFileStatus.TEMP_FILE }.size,
            "Expected to find one file marked as temp",
        )
        assertEquals(
            1,
            files.filter { it.isTempFile == FileTempFileStatus.NOT_A_TEMP_FILE }.size,
            "Expected to find one file marked as not temp",
        )

        val literal =
            result.literals.singleOrNull { it.value is String && it.value == "Hello world!" }
        assertNotNull(literal)

        assertTrue(
            dataFlow(
                    startNode = literal,
                    predicate = { it is File && it.isTempFile == FileTempFileStatus.TEMP_FILE },
                )
                .value,
            "Expected to find a write to the temp file.",
        )
        assertTrue(
            dataFlow(
                    startNode = literal,
                    predicate = {
                        it is File && it.isTempFile == FileTempFileStatus.NOT_A_TEMP_FILE
                    },
                )
                .value,
            "Expected to find a write to the non-temp file.",
        )
    }

    @Test
    fun testTempfilesMultiple() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("multipleTempFiles.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val allTempFiles = result.conceptNodes.filterIsInstance<File>()

        assertEquals(2, allTempFiles.size, "Expected to find exactly two `File` nodes.")

        assertEquals(
            allTempFiles.size,
            allTempFiles.map { it.fileName }.toSet().count(),
            "Expected the temp files to have unique names.",
        )
    }

    @Test
    fun testTempfilesGettmpdirBAD() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("tempfile_bad_gettmpdir.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val file = result.conceptNodes.filterIsInstance<File>().singleOrNull()
        assertNotNull(file)

        assertEquals(
            FileTempFileStatus.TEMP_FILE,
            file.isTempFile,
            "Expected the file to be marked as temp.",
        )
    }

    @Test
    fun testTempOrNot2() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("tempOrNot2.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonFileConceptPass>()
                it.registerPass<PythonFileConceptPrePass>()
                it.registerPass<PythonFileJoinPass>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
            }
        assertNotNull(result)

        val file = result.conceptNodes.filterIsInstance<File>()
        // assertEquals(2, file.size, "Expected to find 2 files. One for the tempfile and one for
        // the normal file.")

        val tempFile = file.singleOrNull { it.isTempFile == FileTempFileStatus.TEMP_FILE }
        assertNotNull(tempFile)

        val notTempFile = file.singleOrNull { it.isTempFile == FileTempFileStatus.NOT_A_TEMP_FILE }
        assertNotNull(notTempFile)

        val barLiteral = result.literals.singleOrNull { it.value is String && it.value == "bar" }
        assertNotNull(barLiteral)

        assertTrue(
            dataFlow(startNode = barLiteral, predicate = { it === tempFile }).value,
            "Expected to find a dataflow from the literal \"bar\" to the temp file.",
        )

        assertTrue(
            dataFlow(startNode = barLiteral, predicate = { it === notTempFile }).value,
            "Expected to find a dataflow from the literal \"bar\" to the non-temp file.",
        )
    }
}
