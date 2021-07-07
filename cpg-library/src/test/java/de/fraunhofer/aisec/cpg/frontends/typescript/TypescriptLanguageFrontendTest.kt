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
package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.ExperimentalTypeScript
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("experimental")
@ExperimentalTypeScript
class TypescriptLanguageFrontendTest {

    @Test
    fun testFunction() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.ts").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    TypeScriptLanguageFrontend::class.java,
                    TypeScriptLanguageFrontend.TYPESCRIPT_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val functions = tu.declarations.filterIsInstance<FunctionDeclaration>()
        assertNotNull(functions)

        assertEquals(2, functions.size)

        val someFunction = functions.first()
        assertEquals("someFunction", someFunction.name)
        assertEquals(TypeParser.createFrom("Number", false), someFunction.type)

        val someOtherFunction = functions.last()
        assertEquals("someOtherFunction", someOtherFunction.name)
        assertEquals(TypeParser.createFrom("Number", false), someOtherFunction.type)

        val parameters = someOtherFunction.parameters
        assertNotNull(parameters)

        assertEquals(1, parameters.size)

        val parameter = parameters.first()
        assertEquals("s", parameter.name)
        assertEquals(TypeParser.createFrom("String", false), parameter.type)
    }
}
