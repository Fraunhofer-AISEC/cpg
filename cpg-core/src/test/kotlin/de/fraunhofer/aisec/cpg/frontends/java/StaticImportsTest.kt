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
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.flattenIsInstance
import de.fraunhofer.aisec.cpg.TestUtils.flattenListIsInstance
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import java.nio.file.Path
import kotlin.test.*

internal class StaticImportsTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "staticImports")

    @Test
    @Throws(Exception::class)
    fun testSingleStaticImport() {
        val result = analyze("java", topLevel.resolve("single"), true)
        val methods = flattenListIsInstance<MethodDeclaration>(result)
        val test = findByUniqueName(methods, "test")
        val main = findByUniqueName(methods, "main")
        val call = flattenIsInstance<CallExpression>(main).firstOrNull()
        assertNotNull(call)
        assertEquals(listOf(test), call.invokes)

        val testFields =
            flattenListIsInstance<FieldDeclaration>(result).filter { it.name == "test" }
        assertEquals(1, testFields.size)

        val staticField = testFields.firstOrNull()
        assertNotNull(staticField)
        assertTrue(staticField.modifiers.contains("static"))

        val memberExpressions = flattenIsInstance<MemberExpression>(main)
        val usage = findByUniqueName(memberExpressions, "test")
        assertEquals(staticField, usage.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testAsteriskImport() {
        val result = analyze("java", topLevel.resolve("asterisk"), true)
        val methods = flattenListIsInstance<MethodDeclaration>(result)
        val main = findByUniqueName(methods, "main")
        val records = flattenListIsInstance<RecordDeclaration>(result)
        val a = findByUniqueName(records, "A")
        val b = findByUniqueName(records, "B")
        for (call in flattenIsInstance<CallExpression>(main)) {
            when (call.name) {
                "a" -> {
                    assertEquals(listOf(findByUniqueName(methods, "a")), call.invokes)
                    assertTrue((call.invokes[0] as MethodDeclaration).isStatic)
                }
                "b" -> {
                    val bs = methods.filter { it.name == "b" && it.isStatic }
                    assertEquals(call.invokes, bs.filter { it.hasSignature(call.signature) })
                }
                "nonStatic" -> {
                    val nonStatic = findByUniqueName(b.methods, "nonStatic")
                    assertTrue(nonStatic.isInferred)
                    assertEquals(listOf(nonStatic), call.invokes)
                }
            }
        }
        val testFields = flattenIsInstance<FieldDeclaration>(a)
        val staticField = findByUniqueName(testFields, "staticField")
        val nonStaticField = findByUniqueName(testFields, "nonStaticField")
        assertTrue(staticField.modifiers.contains("static"))
        assertFalse(nonStaticField.modifiers.contains("static"))

        val declaredReferences = flattenIsInstance<MemberExpression>(main)
        val usage = findByUniqueName(declaredReferences, "staticField")
        assertEquals(staticField, usage.refersTo)

        val nonStatic = findByUniqueName(declaredReferences, "nonStaticField")
        assertNotEquals(nonStaticField, nonStatic.refersTo)
        assertTrue(nonStatic.refersTo!!.isInferred)
    }
}
