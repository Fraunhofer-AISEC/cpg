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

import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull

class SvelteLanguageFrontendTest {

    @Test
    fun `test parsing a simple Svelte component`() {
        val topLevel = Path.of("src", "test", "resources", "svelte")

        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("SimpleComponent.svelte").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<SvelteLanguage>()
            }

        val varName =
            tu.declarations.filterIsInstance<VariableDeclaration>().firstOrNull { declaration ->
                val nameProperty = declaration.name
                val localNameString = nameProperty.localName
                localNameString == "name"
            }
        assertNotNull(varName, "Variable 'name' should be declared")

        val varCount =
            tu.declarations.filterIsInstance<VariableDeclaration>().firstOrNull {
                it.name.localName == "count"
            }
        assertNotNull(varCount, "Variable 'count' should be declared")

        val funcHandleClick =
            tu.declarations.filterIsInstance<FunctionDeclaration>().firstOrNull {
                it.name.localName == "handleClick"
            }
        assertNotNull(funcHandleClick, "Function 'handleClick' should be declared")

        println("Basic assertions passed. Further implementation needed.")
    }
}
