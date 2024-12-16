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
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.createTempFile
import kotlin.reflect.jvm.javaField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.neo4j.ogm.annotation.Relationship
import picocli.CommandLine

fun createTranslationResult(file: String = "client.cpp"): Pair<Application, TranslationResult> {
    val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
    val path = topLevel.resolve(file).toAbsolutePath()

    val cmd = CommandLine(Application::class.java)
    cmd.parseArgs(path.toString())
    val application = cmd.getCommand<Application>()

    val translationConfiguration = application.setupTranslationConfiguration()
    val translationResult =
        TranslationManager.builder().config(translationConfiguration).build().analyze().get()
    return application to translationResult
}

class ApplicationTest {

    @Test
    fun testSerializeCpgViaOGM() {
        val (application, translationResult) = createTranslationResult()

        // 22 inferred functions, 1 inferred method, 2 inferred constructors, 11 regular functions
        assertEquals(36, translationResult.functions.size)

        val (nodes, edges) = application.translateCPGToOGMBuilders(translationResult)
        val graph = application.buildJsonGraph(nodes, edges)
        val connectToFuncDel =
            graph.nodes.firstOrNull {
                it.labels.contains(FunctionDeclaration::class.simpleName) &&
                    it.properties["name"] == "connectTo"
            }
        assertNotNull(connectToFuncDel)

        val connectToCallExpr =
            graph.nodes.firstOrNull {
                it.labels.contains(CallExpression::class.simpleName) &&
                    it.properties["name"] == "connectTo"
            }
        assertNotNull(connectToCallExpr)

        val invokesEdge =
            graph.edges.firstOrNull {
                it.type ==
                    (CallExpression::invokeEdges.javaField?.getAnnotation(Relationship::class.java))
                        ?.value &&
                    it.startNode == connectToCallExpr.id &&
                    it.endNode == connectToFuncDel.id
            }
        assertNotNull(invokesEdge)
    }

    @Test
    fun testExportToJson() {
        val (application, translationResult) = createTranslationResult()
        // 22 inferred functions, 1 inferred method, 2 inferred constructors, 11 regular functions
        assertEquals(36, translationResult.functions.size)
        val path = createTempFile().toFile()
        application.exportToJson(translationResult, path)
        assert(path.length() > 0)
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
        val objectMapper: ObjectMapper = ObjectMapper()
        val schema = objectMapper.readValue(file, List::class.java)
        assert(schema is ArrayList)
        assert((schema as ArrayList).size > 0)
        Files.deleteIfExists(Path(path))
    }
}
