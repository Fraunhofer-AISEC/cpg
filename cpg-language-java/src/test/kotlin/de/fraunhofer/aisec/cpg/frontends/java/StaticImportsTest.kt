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

import de.fraunhofer.aisec.cpg.IncompatibleSignature
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.matchesSignature
import de.fraunhofer.aisec.cpg.test.*
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
                    topLevel.resolve("single/B.java").toFile(),
                ),
                // we need to specify the root of the folder so that the JavaParser correctly
                // resolve the package
                topLevel,
                true,
            ) {
                it.registerLanguage<JavaLanguage>()
            }
        val methods = result.methods
        val test = findByUniqueName(methods, "test")
        val main = findByUniqueName(methods, "main")
        val call = main.calls.firstOrNull()
        assertNotNull(call)
        assertInvokes(call, test)

        val testFields = result.fields { it.name.localName == "test" }
        assertEquals(1, testFields.size)

        val staticField = testFields.firstOrNull()
        assertNotNull(staticField)
        assertTrue(staticField.modifiers.contains("static"))

        val memberExpressionExpressions = main.allChildren<MemberAccess>()
        // we have two member expressions, one to the field and one to the method
        assertEquals(2, memberExpressionExpressions.size)

        // we want the one to the field
        val usage = memberExpressionExpressions[{ it.type.name.localName == "int" }]
        assertNotNull(usage)
        assertEquals(staticField, usage.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testInferredSingleStaticImport() {
        val result =
            analyze(
                listOf(
                    // we want JavaParser to analyze both files so that resolving works
                    topLevel.resolve("inferred/Provider.java").toFile(),
                    topLevel.resolve("inferred/Consumer.java").toFile(),
                ),
                // we need to specify the root of the folder so that the JavaParser correctly
                // resolve the package
                topLevel,
                true,
            ) {
                it.registerLanguage<JavaLanguage>()
            }

        val provider = result.records["Provider", SearchModifier.UNIQUE]
        assertNotNull(provider)
        val consumer = result.records["Consumer", SearchModifier.UNIQUE]
        assertNotNull(consumer)

        // "doesNotExist" is not declared by Provider. Since it is used both as a field and as a
        // method in Consumer, both are inferred as static members of Provider (the target of the
        // static import), but not of Consumer. We look them up with UNIQUE to also assert that
        // exactly one of each was inferred.
        val inferredField = provider.fields["doesNotExist", SearchModifier.UNIQUE]
        assertNotNull(inferredField)
        assertTrue(inferredField.isInferred)
        assertTrue(inferredField.isStatic)
        val inferredMethod = provider.methods["doesNotExist", SearchModifier.UNIQUE]
        assertNotNull(inferredMethod)
        assertTrue(inferredMethod.isInferred)
        assertTrue(inferredMethod.isStatic)
        assertTrue(consumer.fields.none { it.name.localName == "doesNotExist" })
        assertTrue(consumer.methods.none { it.name.localName == "doesNotExist" })

        val main = findByUniqueName(result.methods, "main")

        // The field usage "doesNotExist" must resolve to the inferred field on Provider and the
        // call to the inferred method
        val fieldUsage =
            main.refs.single { it.name.localName == "doesNotExist" && it.astParent !is Call }
        assertRefersTo(fieldUsage, inferredField)
        val call = main.calls["doesNotExist"]
        assertNotNull(call)
        assertInvokes(call, inferredMethod)
    }

    @Test
    @Throws(Exception::class)
    fun testAsteriskImport() {
        val result =
            analyze("java", topLevel.resolve("asterisk"), true) {
                it.registerLanguage<JavaLanguage>()
            }
        val methods = result.methods
        val main = methods["main", SearchModifier.UNIQUE]
        val records = result.records
        val a = records["A", SearchModifier.UNIQUE]
        val b = records["B", SearchModifier.UNIQUE]

        for (call in main.calls) {
            when (call.name.localName) {
                "a" -> {
                    assertInvokes(call, methods["a"])
                    assertTrue((call.invokes[0] as Method).isStatic)
                }
                "b" -> {
                    val bs = methods { it.name.localName == "b" && it.isStatic }
                    assertEquals(
                        call.invokes.toList(),
                        bs { it.matchesSignature(call.signature) != IncompatibleSignature },
                    )
                }
                "nonStatic" -> {
                    val nonStatic = findByUniqueName(b.methods, "nonStatic")
                    assertTrue(nonStatic.isInferred)
                    assertInvokes(call, nonStatic)
                }
            }
        }
        val testFields = a.fields
        val staticField = a.fields["staticField"]
        val inferredNonStaticField = b.fields["nonStaticField"]
        assertNotNull(staticField)
        assertNotNull(inferredNonStaticField)
        assertTrue(staticField.modifiers.contains("static"))
        assertFalse(inferredNonStaticField.modifiers.contains("static"))

        val declaredReferences = main.refs
        val usage = findByUniqueName(declaredReferences, "staticField")
        assertRefersTo(usage, staticField)

        val nonStatic = findByUniqueName(declaredReferences, "nonStaticField")
        assertRefersTo(nonStatic, inferredNonStaticField)
        assertTrue(nonStatic.refersTo?.isInferred == true)
    }
}
