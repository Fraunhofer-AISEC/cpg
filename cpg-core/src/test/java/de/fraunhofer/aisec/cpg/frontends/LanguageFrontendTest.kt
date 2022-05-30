/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager.Companion.builder
import java.io.File
import java.util.concurrent.ExecutionException
import kotlin.test.Test
import kotlin.test.assertEquals

internal class LanguageFrontendTest : BaseTest() {
    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testParseDirectory() {
        val analyzer =
            builder()
                .config(
                    TranslationConfiguration.builder()
                        .sourceLocations(File("src/test/resources/botan"))
                        .debugParser(true)
                        .defaultLanguages()
                        .build()
                )
                .build()
        val res = analyzer.analyze().get()
        assertEquals(3, res.translationUnits.size)
    }
}
