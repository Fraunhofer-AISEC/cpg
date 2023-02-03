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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.passes.JavaExternalTypeHierarchyResolver
import java.nio.file.Path
import kotlin.test.*

internal class StaticImportsTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "java", "staticImports")

    @Test
    @Throws(Exception::class)
    fun testSingleStaticImport() {
        val result =
            analyze(
                listOf(
                    // we want JavaParser to analyze both files so that resolving works
                    topLevel.resolve("single/A.java").toFile(),
                    topLevel.resolve("single/B.java").toFile()
                ),
                // we need to specify the root of the folder so that the JavaParser correctly
                // resolve the package
                topLevel,
                true
            ) {
                it.registerLanguage(JavaLanguage())
                it.registerPass(JavaExternalTypeHierarchyResolver())
            }
        val methods = result.methods
        val test = findByUniqueName(methods, "test")
        val main = findByUniqueName(methods, "main")
        val call = main.calls.firstOrNull()
        assertNotNull(call)
        assertEquals(listOf(test), call.invokes)

        val testFields = result.fields { it.name.localName == "test" }
        assertEquals(1, testFields.size)

        val staticField = testFields.firstOrNull()
        assertNotNull(staticField)
        assertTrue(staticField.modifiers.contains("static"))

        val memberExpressions = main.allChildren<MemberExpression>()
        // we have two member expressions, one to the field and one to the method
        assertEquals(2, memberExpressions.size)

        // we want the one to the field
        val usage = memberExpressions[{ it.type.name.localName == "int" }]
        assertNotNull(usage)
        assertEquals(staticField, usage.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testAsteriskImport() {
        val result =
            analyze("java", topLevel.resolve("asterisk"), true) {
                it.registerLanguage(JavaLanguage())
                it.registerPass(JavaExternalTypeHierarchyResolver())
            }
        val methods = result.methods
        val main = methods["main", SearchModifier.UNIQUE]
        val records = result.records
        val a = records["A", SearchModifier.UNIQUE]
        val b = records["B", SearchModifier.UNIQUE]

        for (call in main.calls) {
            when (call.name.localName) {
                "a" -> {
                    assertEquals(listOf(findByUniqueName(methods, "a")), call.invokes)
                    assertTrue((call.invokes[0] as MethodDeclaration).isStatic)
                }
                "b" -> {
                    val bs = methods { it.name.localName == "b" && it.isStatic }
                    assertEquals(call.invokes, bs { it.hasSignature(call.signature) })
                }
                "nonStatic" -> {
                    val nonStatic = findByUniqueName(b.methods, "nonStatic")
                    assertTrue(nonStatic.isInferred)
                    assertEquals(listOf(nonStatic), call.invokes)
                }
            }
        }
        val testFields = a.fields
        val staticField = findByUniqueName(testFields, "staticField")
        val nonStaticField = findByUniqueName(testFields, "nonStaticField")
        assertTrue(staticField.modifiers.contains("static"))
        assertFalse(nonStaticField.modifiers.contains("static"))

        val declaredReferences = main.allChildren<MemberExpression>()
        val usage = findByUniqueName(declaredReferences, "staticField")
        assertEquals(staticField, usage.refersTo)

        val nonStatic = findByUniqueName(declaredReferences, "nonStaticField")
        assertNotEquals(nonStaticField, nonStatic.refersTo)
        assertTrue(nonStatic.refersTo!!.isInferred)
    }
}
