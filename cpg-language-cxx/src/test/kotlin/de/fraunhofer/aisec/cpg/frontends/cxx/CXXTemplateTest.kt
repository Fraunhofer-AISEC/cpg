/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.ast.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.methods
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * This class contains several tests that test our ability to understand templates to some degree.
 */
class CXXTemplateTest {
    @Test
    fun testConstructor() {
        val file = File("src/test/resources/cxx/template_constructor.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        // We should have two constructors with a "double-reference" type
        var method =
            tu.methods.firstOrNull { it.signature == "MyTemplateClass(T&&)MyTemplateClass" }
        assertIs<ConstructorDeclaration>(method)
        assertNotNull(method)

        val paramType = method.parameters.firstOrNull()?.type?.root
        assertIs<ParameterizedType>(paramType)
        assertLocalName("T", paramType)

        method = tu.methods.firstOrNull { it.signature == "MyClass(MyClass&&)MyClass" }
        assertIs<ConstructorDeclaration>(method)
        assertNotNull(method)
    }

    @Test
    fun testFunctionTemplateCall() {
        val file = File("src/test/resources/cxx/templates/function_template.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)
        assertTrue(tu.functions.all { !it.isInferred })
    }
}
