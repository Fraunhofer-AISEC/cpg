/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.neo4j

import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.persistence.createJsonGraph
import de.fraunhofer.aisec.cpg.persistence.persistJson
import de.fraunhofer.aisec.cpg_vis_neo4j.Application
import java.nio.file.Paths
import kotlin.io.path.createTempFile
import kotlin.reflect.jvm.javaField
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.neo4j.ogm.annotation.Relationship
import picocli.CommandLine

fun createTranslationResult(file: String = "client.cpp"): Pair<Application, TranslationResult> {
    val topLevel = Paths.get("src").resolve("integrationTest").resolve("resources").toAbsolutePath()
    val path = topLevel.resolve(file).toAbsolutePath()

    val cmd = CommandLine(Application::class.java)
    cmd.parseArgs(path.toString())
    val application = cmd.getCommand<Application>()

    val translationConfiguration = application.setupTranslationConfiguration()
    val translationResult =
        TranslationManager.builder().config(translationConfiguration).build().analyze().get()
    return application to translationResult
}

/**
 * A class for integration tests. They depend on the C++ frontend, so we classify them as an
 * integration test. This might be replaced with a language-neutral test at some point.
 */
class IntegrationTest {

    @Test
    fun testBuildJsonGraph() {
        val (application, translationResult) = createTranslationResult()

        // 22 inferred functions, 1 inferred method, 2 inferred constructors, 11 regular functions
        assertEquals(36, translationResult.functions.size)

        val graph = translationResult.createJsonGraph()
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
        translationResult.persistJson(path)
        assert(path.length() > 0)
    }
}
