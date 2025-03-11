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
import de.fraunhofer.aisec.cpg.frontends.python.PythonValueEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass
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
                it.configurePass<PythonFileConceptPass>(
                    ConceptPass.Config(evaluator = PythonValueEvaluator())
                )
            }
        assertNotNull(result)

        val fileNodes =
            result.conceptNodes.filterIsInstance<IsFile>() +
                result.operationNodes.filterIsInstance<
                    IsFile
                >() // TODO why can't I use `overlays`? It's empty.
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
            executionPathHelper(startNode = fileOpen, endNode = readFile),
            "Expected to find an execution path from open to read.",
        )

        val fileRead = fileNodes.filterIsInstance<ReadFile>().singleOrNull()
        assertNotNull(fileRead)
        val closeFile = fileNodes.filterIsInstance<CloseFile>().singleOrNull()
        assertNotNull(closeFile)
        assertTrue(
            executionPathHelper(startNode = fileRead, endNode = closeFile),
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
                it.configurePass<PythonFileConceptPass>(
                    ConceptPass.Config(evaluator = PythonValueEvaluator())
                )
            }
        assertNotNull(result)

        val fileNodes =
            result.conceptNodes.filterIsInstance<IsFile>() +
                result.operationNodes.filterIsInstance<
                    IsFile
                >() // TODO why can't I use `overlays`? It's empty.
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
                it.configurePass<PythonFileConceptPass>(
                    ConceptPass.Config(evaluator = PythonValueEvaluator())
                )
            }
        assertNotNull(result)

        val fileNodes =
            result.conceptNodes.filterIsInstance<IsFile>() +
                result.operationNodes.filterIsInstance<
                    IsFile
                >() // TODO why can't I use `overlays`? It's empty.
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
                                overlay is SetFileMask && write.concept == overlay.concept
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
                it.configurePass<PythonFileConceptPass>(
                    ConceptPass.Config(evaluator = PythonValueEvaluator())
                )
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
                it.configurePass<PythonFileConceptPass>(
                    ConceptPass.Config(evaluator = PythonValueEvaluator())
                )
            }
        assertNotNull(result)

        // Tests mask is set before any write:
        // for all files
        //   for all WriteFile on the current file
        //     there is no SetFileMask on the current file after the WriteFile
        assertFalse(
            // See also testBadChmodQuery for a failing example
            result.conceptNodes.filterIsInstance<File>().all { file ->
                file.ops.filterIsInstance<WriteFile>().none { write ->
                    val startNode =
                        write.underlyingNode
                            ?: return@none true // fail if there is no underlyingNode
                    executionPath(startNode = startNode, direction = Forward(GraphToFollow.EOG)) {
                            it.overlays.any { overlay ->
                                overlay is SetFileMask && write.concept == overlay.concept
                            }
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
                it.configurePass<PythonFileConceptPass>(
                    ConceptPass.Config(evaluator = PythonValueEvaluator())
                )
            }
        assertNotNull(result)

        val conceptNodes =
            result.conceptNodes.filterIsInstance<IsFile>() +
                result.operationNodes.filterIsInstance<
                    IsFile
                >() // TODO why can't I use `overlays`? It's empty.
        assertTrue(conceptNodes.isNotEmpty())

        val file = conceptNodes.filterIsInstance<File>().singleOrNull()
        val fileName = "/tmp/foo.txt"
        assertNotNull(file, "Expected to find a file.")
        assertEquals(fileName, file.fileName, "Expected the file to be \"$fileName\".")

        val write = conceptNodes.filterIsInstance<WriteFile>().singleOrNull()
        assertNotNull(write, "Expected to find a file write operation.")
        assertEquals(file, write.concept, "Expected the write to write to our file node.")

        val chmod = conceptNodes.filterIsInstance<SetFileMask>().singleOrNull()
        assertNotNull(chmod, "Expected to find a file chmod operation.")
        assertEquals(file, chmod.concept, "Expected the chmod to operate on our file node.")

        // Let's find our bad example
        assertTrue(
            executionPathHelper(startNode = write, endNode = chmod),
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
                it.configurePass<PythonFileConceptPass>(
                    ConceptPass.Config(evaluator = PythonValueEvaluator())
                )
            }
        assertNotNull(result)

        val conceptNodes =
            result.conceptNodes.filterIsInstance<IsFile>() +
                result.operationNodes.filterIsInstance<
                    IsFile
                >() // TODO why can't I use `overlays`? It's empty.
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
                it.configurePass<PythonFileConceptPass>(
                    ConceptPass.Config(evaluator = PythonValueEvaluator())
                )
            }
        assertNotNull(result)

        val conceptNodes =
            result.conceptNodes.filterIsInstance<IsFile>() +
                result.operationNodes.filterIsInstance<
                    IsFile
                >() // TODO why can't I use `overlays`? It's empty.
        assertTrue(conceptNodes.isNotEmpty())

        val file = conceptNodes.filterIsInstance<File>().singleOrNull()
        assertNotNull(file, "Expected to find exactly one `File` node (\"example.txt\").")

        val fileRead = conceptNodes.filterIsInstance<ReadFile>().singleOrNull()
        assertNotNull(fileRead, "Expected to find a file read operation.")

        val fileDelete = conceptNodes.filterIsInstance<DeleteFile>().singleOrNull()
        assertNotNull(fileDelete, "Expected to find a file delete operation.")

        val fileWrite = conceptNodes.filterIsInstance<WriteFile>().singleOrNull()
        assertNotNull(fileWrite, "Expected to find a file write operation.")

        assertTrue(
            dataFlow(startNode = fileRead) { it == fileWrite }.value,
            "Expected to find a dataflow from the file read to the file write operation.",
        )

        assertTrue(
            executionPathHelper(startNode = fileRead, endNode = fileDelete),
            "Expected to find an execution path from read to remove.",
        )

        assertTrue(
            executionPathHelper(startNode = fileDelete, endNode = fileWrite),
            "Expected to find an execution path from remove to write.",
        )
    }

    internal fun executionPathHelper(startNode: OverlayNode?, endNode: OverlayNode?): Boolean {
        assertNotNull(startNode, "Expected a start node.")
        assertNotNull(endNode, "Expected an end node.")
        val startUnderlyingNode = startNode.underlyingNode
        assertNotNull(startUnderlyingNode, "Expected to find an underlying node.")
        return executionPath(startNode = startUnderlyingNode) { node ->
                node.overlays.any { overlay -> overlay == endNode }
            }
            .value
    }
}
