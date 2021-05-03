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
import java.io.File
import java.net.ConnectException
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class ApplicationTest {

    @Test
    @Throws(InterruptedException::class)
    fun pushToNeo4jWithoutConnectionBooms() {
        // arrange // act // assert
        Assertions.assertThrows(ConnectException::class.java) {
            Application().pushToNeo4j(translationResult!!)
        }
    }

    companion object {
        private var translationResult: TranslationResult? = null
        @BeforeAll
        @JvmStatic
        @Throws(ExecutionException::class, InterruptedException::class)
        private fun init() {
            val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
            val path = topLevel.resolve("Implementor1.java").toAbsolutePath()
            val file = File(path.toString())
            assert(file.exists() && !file.isDirectory && !file.isHidden)
            val translationConfiguration =
                TranslationConfiguration.builder()
                    .sourceLocations(file)
                    .topLevel(topLevel.toFile())
                    .defaultPasses()
                    .debugParser(true)
                    .build()
            val translationManager =
                TranslationManager.builder().config(translationConfiguration).build()
            translationResult = translationManager.analyze().get()
        }
    }
}
