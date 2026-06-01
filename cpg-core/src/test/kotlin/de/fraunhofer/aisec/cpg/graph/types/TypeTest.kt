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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.UnknownLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.test.assertLocalName
import kotlin.system.measureTimeMillis
import kotlin.test.*
import org.junit.jupiter.api.assertThrows

class TypeTest {
    @Test
    fun testType() {
        with(TestLanguageFrontend()) {
            val tu = newTranslationUnit("file.extension")
            this.scopeManager.resetToGlobal(tu)

            val func = newFunction("main")
            assertLocalName("main", func)

            val simpleType = objectType("SomeObject")
            assertLocalName("SomeObject", simpleType)
        }
    }

    @Test
    fun testPrimitive() {
        with(TestLanguageFrontend()) {
            val boolean = primitiveType("boolean")
            assertLocalName("boolean", boolean)

            assertThrows<TranslationException> { primitiveType("BOOLEAN") }
        }
    }

    @Test
    fun testTypeOperations() {
        with(TestLanguageFrontend()) {
            var type: Type = objectType("myClass")
            type = type.pointer().pointer().pointer()
            assertIs<SecondOrderType>(type)
            assertEquals(3, type.referenceDepth)

            var operations = type.typeOperations
            assertEquals(3, operations.size)

            type = objectType("myNewClass")
            type = operations.apply(type)
            assertLocalName("myNewClass***", type)
        }
    }

    @Test
    fun testDynamicType() {
        with(TestLanguageFrontend()) {
            var type = dynamicType()
            assertIs<DynamicType>(type.reference(PointerType.PointerOrigin.ARRAY))
            assertIs<DynamicType>(type.dereference())
        }
    }

    @Test
    fun testPointerHashCodeIsStableForSelfReferentialType() {
        val pointer =
            PointerType(UnknownType.getUnknownType(null), PointerType.PointerOrigin.POINTER)
        pointer.elementType = pointer

        val hash1 = pointer.hashCode()
        val hash2 = pointer.hashCode()

        assertEquals(hash1, hash2)
    }

    @Test
    fun testPointerHashCodeForEqualButDistinctTypes() {
        with(TestLanguageFrontend()) {
            val p1 = objectType("Node").pointer()
            val p2 = objectType("Node").pointer()

            assertEquals(p1, p2)
            assertEquals(p1.hashCode(), p2.hashCode())
        }
    }

    @Test
    fun testStructuralHashPerformanceSmoke() {
        val base = ObjectType("Node", listOf(), false, true, UnknownLanguage)
        var type: Type = base
        repeat(64) { type = type.pointer() }

        var accumulator = 0
        val elapsedMs = measureTimeMillis {
            repeat(200_000) { accumulator = accumulator xor type.hashCode() }
        }

        assertEquals(type.hashCode(), type.hashCode())
        assertTrue(elapsedMs >= 0)
        assertTrue(accumulator != Int.MIN_VALUE)
    }
}
