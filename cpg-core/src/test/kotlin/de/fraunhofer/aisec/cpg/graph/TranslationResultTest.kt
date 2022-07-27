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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExperimentalGraph
class TranslationResultTest : BaseTest() {
    @Test
    fun testFromTranslationUnit() {
        val file = File("src/test/resources/compiling/RecordDeclaration.java")

        val config =
            TranslationConfiguration.builder()
                .sourceLocations(listOf(file))
                .topLevel(file.parentFile)
                .defaultPasses()
                .debugParser(true)
                .defaultLanguages()
                .failOnError(true)
                .build()

        val analyzer = TranslationManager.builder().config(config).build()

        val result = analyzer.analyze().get()

        val graph = result.graph
        assertNotNull(graph)

        var nodes = graph.query("MATCH (m:MethodDeclaration) RETURN m")
        // returns the method declaration as well as the constructor declaration
        assertEquals(2, nodes.size)

        nodes = graph.query("MATCH (l:Literal) WHERE l.value = 0 RETURN l")
        assertEquals(2, nodes.size)
    }
}
