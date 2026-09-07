/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg_vis_neo4j

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.cpg.graph.*
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import picocli.CommandLine

class ApplicationTest {

    @Test
    fun testCallListPasses() {
        // --list-passes is handled by call() itself, before setupProject() is ever invoked, so it
        // must not require any source files.
        val cmd = CommandLine(Application::class.java)
        cmd.parseArgs("--list-passes")

        assertEquals(0, cmd.getCommand<Application>().call())
    }

    @Test
    fun testCallSchemaMarkdown() {
        val path = "./tmp-call.md"
        val cmd = CommandLine(Application::class.java)
        cmd.parseArgs("--schema-markdown", path)

        assertEquals(0, cmd.getCommand<Application>().call())
        assertTrue(File(path).length() > 100000)
        Files.deleteIfExists(Path(path))
    }

    @Test
    fun testCallSchemaJson() {
        val path = "./tmp-call.json"
        val cmd = CommandLine(Application::class.java)
        cmd.parseArgs("--schema-json", path)

        assertEquals(0, cmd.getCommand<Application>().call())
        val schema = ObjectMapper().readValue(File(path), List::class.java)
        assertIs<ArrayList<*>>(schema)
        assertTrue(schema.isNotEmpty())
        Files.deleteIfExists(Path(path))
    }

    @Test
    fun testExportMarkdownSchema() {
        val path = "./tmp.md"
        Application().printSchema(listOf(path), Schema.Format.MARKDOWN)
        // Some magic number as size, where the current schema is larger and should never be lower
        // in the future
        assert(File(path).length() > 100000)
        Files.deleteIfExists(Path(path))
    }

    @Test
    fun testExportJSONSchema() {
        val path = "./tmp.json"
        Application().printSchema(listOf(path), Schema.Format.JSON)
        val file = File(path)
        val objectMapper = ObjectMapper()
        val schema = objectMapper.readValue(file, List::class.java)
        assertIs<ArrayList<*>>(schema)
        assertTrue(schema.isNotEmpty())
        Files.deleteIfExists(Path(path))
    }
}
