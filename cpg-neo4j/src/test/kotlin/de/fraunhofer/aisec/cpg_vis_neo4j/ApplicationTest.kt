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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import java.nio.file.Paths
import kotlin.io.path.createTempFile
import kotlin.reflect.jvm.javaField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Tag
import org.neo4j.ogm.annotation.Relationship
import picocli.CommandLine

@Tag("integration")
class ApplicationTest {
    private fun createTranslationResult(): Pair<Application, TranslationResult> {
        val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
        val path = topLevel.resolve("client.cpp").toAbsolutePath()

        val cmd = CommandLine(Application::class.java)
        cmd.parseArgs(path.toString())
        val application = cmd.getCommand<Application>()

        val translationConfiguration = application.setupTranslationConfiguration()
        val translationResult =
            TranslationManager.builder().config(translationConfiguration).build().analyze().get()
        return application to translationResult
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPush() {
        val (application, translationResult) = createTranslationResult()

        assertEquals(32, translationResult.functions.size)

        application.pushToNeo4j(translationResult)

        val sessionAndSessionFactoryPair = application.connect()

        val session = sessionAndSessionFactoryPair.first
        session.beginTransaction().use { transaction ->
            val functions = session.loadAll(FunctionDeclaration::class.java)
            assertNotNull(functions)

            assertEquals(32, functions.size)

            transaction.commit()
        }

        session.clear()
        sessionAndSessionFactoryPair.second.close()
    }

    @Test
    fun testSerializeCpgViaOGM() {
        val (application, translationResult) = createTranslationResult()

        assertEquals(32, translationResult.functions.size)

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
        assertEquals(32, translationResult.functions.size)
        val path = createTempFile().toFile()
        application.exportToJson(translationResult, path)
        assert(path.length() > 0)
    }
}
