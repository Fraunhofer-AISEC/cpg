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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.persistence.createJsonGraph
import de.fraunhofer.aisec.cpg.persistence.persistJson
import java.nio.file.Paths
import kotlin.io.path.createTempFile
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

fun createTranslationResult(file: String = "client.cpp"): TranslationResult {
    val topLevel = Paths.get("src").resolve("integrationTest").resolve("resources").toAbsolutePath()
    val path = topLevel.resolve(file).toAbsolutePath()

    val result =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .topLevel(path.toFile())
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
                    .sourceLocations(path.toFile())
                    .defaultPasses()
                    .build()
            )
            .build()
            .analyze()
            .get()

    return result
}

/**
 * A class for integration tests. They depend on the C++ frontend, so we classify them as an
 * integration test. This might be replaced with a language-neutral test at some point.
 */
class IntegrationTest {

    @Test
    fun testBuildJsonGraph() {
        val translationResult = createTranslationResult()

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
                it.type == "INVOKES" &&
                    it.startNode == connectToCallExpr.id &&
                    it.endNode == connectToFuncDel.id
            }
        assertNotNull(invokesEdge)
    }

    @Test
    fun testExportToJson() {
        val translationResult = createTranslationResult()
        // 22 inferred functions, 1 inferred method, 2 inferred constructors, 11 regular functions
        assertEquals(36, translationResult.functions.size)
        val path = createTempFile().toFile()
        translationResult.persistJson(path)
        assert(path.length() > 0)
    }
}
