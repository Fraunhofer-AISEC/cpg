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
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvelteLanguageFrontendTest {

    @Test
    fun `test parsing a simple Svelte component`() {
        val topLevel = File("src/test/resources/svelte").absoluteFile
        val file = File(topLevel, "SimpleComponent.svelte")
        assertTrue(file.exists() && file.isFile, "Test Svelte file exists")

        val config =
            TranslationConfiguration.builder()
                .sourceLocations(file)
                .topLevel(topLevel)
                .registerLanguage("de.fraunhofer.aisec.cpg.frontends.typescript.SvelteLanguage")
                .registerLanguage(
                    "de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguageFrontend"
                )
                .debugParser(true)
                .failOnError(false)
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()

        assertNotNull(result)
        val tud =
            result.components.flatMap { it.translationUnits }.firstOrNull { it.name == file.name }
        assertNotNull(tud, "TUD for SimpleComponent.svelte should exist")

        val varName =
            tud.declarations.filterIsInstance<VariableDeclaration>().firstOrNull { declaration ->
                val nameProperty = declaration.name
                val localNameString = nameProperty.localName
                localNameString == "name"
            }
        assertNotNull(varName, "Variable 'name' should be declared")

        val varCount =
            tud.declarations.filterIsInstance<VariableDeclaration>().firstOrNull {
                it.name.localName == "count"
            }
        assertNotNull(varCount, "Variable 'count' should be declared")

        val funcHandleClick =
            tud.declarations.filterIsInstance<FunctionDeclaration>().firstOrNull {
                it.name.localName == "handleClick"
            }
        assertNotNull(funcHandleClick, "Function 'handleClick' should be declared")

        println("Basic assertions passed. Further implementation needed.")
    }
}
