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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.SearchModifier.UNIQUE
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import java.nio.file.Path
import kotlin.test.*

internal class StaticImportsTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "staticImports")

    @Test
    @Throws(Exception::class)
    fun testSingleStaticImport() {
        val result = analyze("java", topLevel.resolve("single"), true)
        val methods = result.methods
        val test = assertNotNull(methods["test", UNIQUE])
        val main = assertNotNull(methods["main", UNIQUE])
        val call = main.calls.firstOrNull()
        assertNotNull(call)
        assertEquals(listOf(test), call.invokes)

        val testFields = result.fields { it.name == "test" }
        assertEquals(1, testFields.size)

        val staticField = testFields.firstOrNull()
        assertNotNull(staticField)
        assertTrue(staticField.modifiers.contains("static"))

        val memberExpressions = main.allChildren<MemberExpression>()
        val usage = assertNotNull(memberExpressions["test", UNIQUE])
        assertEquals(staticField, usage.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testAsteriskImport() {
        val result = analyze("java", topLevel.resolve("asterisk"), true)
        val methods = result.methods
        val main = methods["main", SearchModifier.UNIQUE]
        val records = result.records
        val a = records["A", SearchModifier.UNIQUE]
        val b = records["B", SearchModifier.UNIQUE]

        for (call in main.calls) {
            when (call.name) {
                "a" -> {
                    assertEquals(listOf(assertNotNull(methods["a", UNIQUE])), call.invokes)
                    assertTrue((call.invokes[0] as MethodDeclaration).isStatic)
                }
                "b" -> {
                    val bs = methods { it.name == "b" && it.isStatic }
                    assertEquals(call.invokes, bs { it.hasSignature(call.signature) })
                }
                "nonStatic" -> {
                    val nonStatic = assertNotNull(b.methods["nonStatic", UNIQUE])
                    assertTrue(nonStatic.isInferred)
                    assertEquals(listOf(nonStatic), call.invokes)
                }
            }
        }
        val testFields = a.fields
        val staticField = assertNotNull(testFields["staticField", UNIQUE])
        val nonStaticField = assertNotNull(testFields["nonStaticField", UNIQUE])
        assertTrue(staticField.modifiers.contains("static"))
        assertFalse(nonStaticField.modifiers.contains("static"))

        val declaredReferences = main.allChildren<MemberExpression>()
        val usage = assertNotNull(declaredReferences["staticField", UNIQUE])
        assertEquals(staticField, usage.refersTo)

        val nonStatic = assertNotNull(declaredReferences["nonStaticField", UNIQUE])
        assertNotEquals(nonStaticField, nonStatic.refersTo)
        assertTrue(nonStatic.refersTo!!.isInferred)
    }
}
