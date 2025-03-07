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
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.query.executionPath
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        val fileSetFlags = fileNodes.filterIsInstance<FileSetFlags>().singleOrNull()
        assertNotNull(fileSetFlags)
        assertEquals(
            setOf(FileAccessModeFlags.O_RDONLY),
            fileSetFlags.flags,
            "Expected to find access mode \"RDONLY\".",
        )

        val contentRef = result.refs("content").singleOrNull()
        assertNotNull(contentRef)

        assertTrue(
            dataFlow(startNode = file) { it == contentRef }.value,
            "Expected to find dataflow from the \"File\" to the \"content\" variable.",
        )

        val fileRead = fileNodes.filterIsInstance<FileRead>().singleOrNull()
        assertNotNull(fileRead)
        assertEquals(
            fileRead,
            file.nextDFG.singleOrNull(),
            "Expected to have exactly one dataflow from \"File\" (it must be to \"FileRead\").",
        ) // TODO this is not nice. Do we have dfg path queries?

        // TODO test open -> read -> close
        // TODO test always closed
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

        val fileSetFlags = fileNodes.filterIsInstance<FileSetFlags>().singleOrNull()
        assertNotNull(fileSetFlags)
        assertEquals(
            setOf(FileAccessModeFlags.O_WRONLY),
            fileSetFlags.flags,
            "Expected to find access mode \"WRONLY\".",
        )

        val helloWorld = result.literals.singleOrNull { it.value == "Hello world!" }
        assertNotNull(helloWorld)

        assertTrue(
            dataFlow(startNode = helloWorld) { it == file }.value,
            "Expected to find dataflow from the \"Hello world!\" literal to the \"File\" node.",
        )

        val fileWrite = fileNodes.filterIsInstance<FileWrite>().singleOrNull()
        assertNotNull(fileWrite)
        assertEquals(
            fileWrite,
            file.prevDFG.singleOrNull(),
            "Expected to have exactly one dataflow to \"File\" (it must be to \"FileWrite\").",
        ) // TODO this is not nice. Do we have dfg path queries?

        // TODO test open -> write -> close
        // TODO test always closed
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

        val fileNodes =
            result.conceptNodes.filterIsInstance<IsFile>() +
                result.operationNodes.filterIsInstance<
                    IsFile
                >() // TODO why can't I use `overlays`? It's empty.
        assertTrue(fileNodes.isNotEmpty())

        val maskNode = fileNodes.filterIsInstance<FileSetMask>().singleOrNull()
        assertNotNull(maskNode)
        assertEquals(0x180, maskNode.mask, "Expected the mask to have value 0o600.")

        val flagsNode = fileNodes.filterIsInstance<FileSetFlags>().singleOrNull()
        assertNotNull(flagsNode)
        assertEquals(
            setOf(FileAccessModeFlags.O_WRONLY),
            flagsNode.flags,
            "Expected to find exactly the flags \"WRONLY\". \"CREAT\" and \"TRUNC\" are not expected, as they are not access mode flags..",
        )

        // TODO test setMask before write
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

        val write = conceptNodes.filterIsInstance<FileWrite>().singleOrNull()
        assertNotNull(write, "Expected to find a file write operation.")
        assertEquals(file, write.concept, "Expected the write to write to our file node.")

        val chmod = conceptNodes.filterIsInstance<FileChmod>().singleOrNull()
        assertNotNull(chmod, "Expected to find a file chmod operation.")
        assertEquals(file, chmod.concept, "Expected the chmod to operate on our file node.")

        // Let's find our bad example
        val start = write.underlyingNode
        assertNotNull(start, "Expected the operation to have a underlying node.")
        assertTrue(
            executionPath(startNode = start) { it == chmod.underlyingNode }.value,
            "Expected to find a violating execution path from write to chmod.",
        )
    }
}
