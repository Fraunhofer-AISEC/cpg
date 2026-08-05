/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VisibilityTest {

    private val topLevel = Path.of("src", "test", "resources", "typescript")

    private fun <T : Declaration> Iterable<T>.byName(name: String): T =
        assertNotNull(this.firstOrNull { it.name.localName == name }, "no declaration named $name")

    /**
     * A `#private` member has an empty local name and is identified by its raw [HARD_PRIVATE]
     * modifier.
     */
    private fun <T : Declaration> Iterable<T>.hardPrivate(): T =
        assertNotNull(this.firstOrNull { HARD_PRIVATE in it.modifiers }, "no #private declaration")

    @Test
    fun testTypeScriptFieldVisibility() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("visibility.ts").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<TypeScriptLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["TsClass"]
        assertNotNull(record)

        // public -> PUBLIC, raw keyword retained
        val publicField = record.fields.byName("publicField")
        assertEquals(Visibility.PUBLIC, publicField.visibility)
        assertTrue(PUBLIC in publicField.modifiers)
        assertFalse(publicField.isStatic)

        // protected -> PROTECTED
        val protectedField = record.fields.byName("protectedField")
        assertEquals(Visibility.PROTECTED, protectedField.visibility)
        assertTrue(PROTECTED in protectedField.modifiers)

        // private -> PRIVATE (compile-time)
        val privateField = record.fields.byName("privateField")
        assertEquals(Visibility.PRIVATE, privateField.visibility)
        assertTrue(PRIVATE in privateField.modifiers)

        // static member: isStatic true, but visibility defaults to PUBLIC
        val staticField = record.fields.byName("staticField")
        assertTrue(staticField.isStatic)
        assertEquals(Visibility.PUBLIC, staticField.visibility)
        assertTrue(STATIC in staticField.modifiers)

        // combined `private static` -> PRIVATE *and* isStatic (folded from both keywords)
        val privateStaticField = record.fields.byName("privateStaticField")
        assertTrue(privateStaticField.isStatic)
        assertEquals(Visibility.PRIVATE, privateStaticField.visibility)
        assertTrue(PRIVATE in privateStaticField.modifiers)
        assertTrue(STATIC in privateStaticField.modifiers)

        // no explicit modifier -> defaults to PUBLIC
        val defaultField = record.fields.byName("defaultField")
        assertEquals(Visibility.PUBLIC, defaultField.visibility)
        assertTrue(defaultField.modifiers.isEmpty())
        assertFalse(defaultField.isStatic)

        // #private -> PRIVATE (hard/runtime private)
        val hardField = record.fields.firstOrNull { HARD_PRIVATE in it.modifiers && !it.isStatic }
        assertNotNull(hardField)
        assertEquals(Visibility.PRIVATE, hardField.visibility)

        // combined `static #x` -> PRIVATE *and* isStatic
        val hardStaticField =
            record.fields.firstOrNull { HARD_PRIVATE in it.modifiers && it.isStatic }
        assertNotNull(hardStaticField)
        assertEquals(Visibility.PRIVATE, hardStaticField.visibility)
    }

    @Test
    fun testTypeScriptMethodVisibility() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("visibility.ts").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<TypeScriptLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["TsClass"]
        assertNotNull(record)

        assertEquals(Visibility.PUBLIC, record.methods.byName("publicMethod").visibility)
        assertEquals(Visibility.PROTECTED, record.methods.byName("protectedMethod").visibility)
        assertEquals(Visibility.PRIVATE, record.methods.byName("privateMethod").visibility)

        val staticMethod = record.methods.byName("staticMethod")
        assertTrue(staticMethod.isStatic)
        assertEquals(Visibility.PUBLIC, staticMethod.visibility)

        // combined `private static` on a method -> PRIVATE *and* isStatic
        val privateStaticMethod = record.methods.byName("privateStaticMethod")
        assertTrue(privateStaticMethod.isStatic)
        assertEquals(Visibility.PRIVATE, privateStaticMethod.visibility)

        assertEquals(Visibility.PRIVATE, record.methods.hardPrivate().visibility)

        // access modifiers on a constructor are interpreted as well (Constructor extends Method)
        val constructor = record.constructors.firstOrNull()
        assertNotNull(constructor)
        assertEquals(Visibility.PRIVATE, constructor.visibility)
    }

    @Test
    fun testJavaScriptVisibility() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("visibility.js").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JavaScriptLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["JsClass"]
        assertNotNull(record)

        // JavaScript has no access-modifier keywords: an ordinary member is public by default.
        val normalField = record.fields.byName("normalField")
        assertEquals(Visibility.PUBLIC, normalField.visibility)
        assertTrue(normalField.modifiers.isEmpty())

        // A `static` member is public but flagged static.
        val staticField = record.fields.byName("staticField")
        assertTrue(staticField.isStatic)
        assertEquals(Visibility.PUBLIC, staticField.visibility)

        // A `#private` member is the only truly private thing in JavaScript.
        val hardField = record.fields.firstOrNull { HARD_PRIVATE in it.modifiers && !it.isStatic }
        assertNotNull(hardField)
        assertEquals(Visibility.PRIVATE, hardField.visibility)

        // combined `static #x` -> PRIVATE *and* isStatic
        val hardStaticField =
            record.fields.firstOrNull { HARD_PRIVATE in it.modifiers && it.isStatic }
        assertNotNull(hardStaticField)
        assertEquals(Visibility.PRIVATE, hardStaticField.visibility)

        assertEquals(Visibility.PUBLIC, record.methods.byName("normalMethod").visibility)
        assertTrue(record.methods.byName("staticMethod").isStatic)
        assertEquals(Visibility.PRIVATE, record.methods.hardPrivate().visibility)
    }
}
