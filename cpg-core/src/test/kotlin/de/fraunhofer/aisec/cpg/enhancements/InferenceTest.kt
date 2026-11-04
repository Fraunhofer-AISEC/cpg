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
package de.fraunhofer.aisec.cpg.enhancements

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.*
import de.fraunhofer.aisec.cpg.test.GraphExamples
import kotlin.test.*

class InferenceTest {

    @Test
    fun testRecordInference() {
        val result = GraphExamples.getInferenceRecord()
        val tu = result.components.firstOrNull()?.translationUnits?.firstOrNull()

        assertNotNull(tu)

        with(tu) {
            val main = tu.functions["main"]

            val valueRef = main.refs["value"]
            assertNotNull(valueRef)
            assertContains(valueRef.assignedTypes, primitiveType("int"))

            val nextRef = main.refs["next"]
            assertNotNull(nextRef)
            assertContains(nextRef.assignedTypes, objectType("T").pointer())

            val record = tu.records["T"]
            assertNotNull(record)
            assertLocalName("T", record)
            assertEquals(true, record.isInferred)
            assertEquals("struct", record.kind)

            assertEquals(2, record.fields.size)

            val valueField = record.fields["value"]
            assertNotNull(valueField)
            assertLocalName("int", valueField.type)

            val nextField = record.fields["next"]
            assertNotNull(nextField)
            assertLocalName("T*", nextField.type)
        }
    }

    @Test
    fun testRecordInferencePointer() {
        val tu =
            GraphExamples.getInferenceRecordPtr()
                .components
                .firstOrNull()
                ?.translationUnits
                ?.firstOrNull()

        assertNotNull(tu)

        val record = tu.records["T"]
        assertNotNull(record)
        assertLocalName("T", record)
        assertEquals(true, record.isInferred)
        assertEquals("class", record.kind)

        assertEquals(2, record.fields.size)

        val valueField = record.fields["value"]
        assertNotNull(valueField)
        assertLocalName("int", valueField.type)

        val nextField = record.fields["next"]
        assertNotNull(nextField)
        assertLocalName("T*", nextField.type)
    }

    @Test
    fun testUnaryOperatorReturnType() {
        val result = GraphExamples.getInferenceUnaryOperatorReturnType()
        assertNotNull(result)
        with(result) {
            val longType = assertResolvedType("long")

            val bar = functions["bar"]
            assertNotNull(bar)

            assertEquals(longType, bar.returnTypes.singleOrNull())
        }
    }

    @Test
    fun testTupleTypeReturnType() {
        val result = GraphExamples.getInferenceTupleReturnType()
        assertNotNull(result)
        with(result) {
            val fooType = assertResolvedType("Foo")
            val barType = assertResolvedType("Bar")

            val bar = functions["bar"]
            assertNotNull(bar)

            assertEquals(listOf(fooType, barType), bar.returnTypes)
        }
    }

    @Test
    fun testBinaryOperatorReturnType() {
        val result = GraphExamples.getInferenceBinaryOperatorReturnType()
        assertNotNull(result)
        with(result) {
            val intType = assertResolvedType("int")
            val longType = assertResolvedType("long")

            val bar = functions["bar"]
            assertNotNull(bar)
            assertEquals(intType, bar.returnTypes.singleOrNull())

            val baz = functions["baz"]
            assertNotNull(baz)
            assertEquals(longType, baz.returnTypes.singleOrNull())
        }
    }

    @Test
    fun testNestedNamespace() {
        val result = GraphExamples.getInferenceNestedNamespace()
        with(result) {
            val java = result.namespaces["java"]
            assertNotNull(java)
            assertLocalName("java", java)

            val javaLang = result.namespaces["java.lang"]
            assertNotNull(javaLang)
            assertLocalName("lang", javaLang)
            // should exist in the scope of "java"
            assertEquals(java, javaLang.scope?.astNode)

            val javaLangString = result.records["java.lang.String"]
            assertNotNull(javaLangString)
            assertLocalName("String", javaLangString)
            // should exist in the scope of "java.lang"
            assertEquals(javaLang, javaLangString.scope?.astNode)
        }
    }
}
