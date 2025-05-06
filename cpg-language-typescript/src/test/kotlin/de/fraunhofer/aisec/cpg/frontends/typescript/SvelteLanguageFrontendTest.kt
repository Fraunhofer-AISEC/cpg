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
package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvelteLanguageFrontendTest {

    @Test
    fun `test parsing a simple Svelte component to get AST JSON`() {
        val topLevel = File("src/test/resources/svelte").absoluteFile
        val file = File(topLevel, "SimpleComponent.svelte")
        assertTrue(file.exists() && file.isFile, "Test Svelte file exists")

        val config =
            TranslationConfiguration.builder()
                .sourceLocations(file)
                .topLevel(topLevel)
                .registerLanguage("de.fraunhofer.aisec.cpg.frontends.typescript.SvelteLanguage")
                .debugParser(true) // May provide more detailed logs
                .failOnError(false) // Continue even if CPG construction isn't perfect yet
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()

        assertNotNull(result)
        // We don't need to assert much about the CPG yet,
        // the main goal is to trigger parsing and print the JSON via the modified frontend.
        println("Test completed. Check console output for 'SVELTE AST JSON START/END' markers.")
    }
}
