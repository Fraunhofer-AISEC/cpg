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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import java.io.File
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
class ApplicationTest {

    private var translationResult: TranslationResult? = null

    @Test
    @Throws(InterruptedException::class)
    fun testPush() {
        val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
        val path = topLevel.resolve("client.cpp").toAbsolutePath()
        val file = File(path.toString())
        assert(file.exists() && !file.isDirectory && !file.isHidden)
        val translationConfiguration =
            TranslationConfiguration.builder()
                .sourceLocations(file)
                .topLevel(topLevel.toFile())
                .defaultPasses()
                .defaultLanguages()
                .debugParser(true)
                .build()
        val translationManager =
            TranslationManager.builder().config(translationConfiguration).build()
        translationResult = translationManager.analyze().get()

        val application = Application()

        application.pushToNeo4j(translationResult!!)

        val sessionAndSessionFactoryPair = application.connect()

        val session = sessionAndSessionFactoryPair.first
        session.beginTransaction().use { transaction ->
            val functions = session.loadAll(FunctionDeclaration::class.java)
            assertNotNull(functions)

            assertEquals(38, functions.size)

            transaction.commit()
        }

        session.clear()
        sessionAndSessionFactoryPair.second.close()
    }
}
