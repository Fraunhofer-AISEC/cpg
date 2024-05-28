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

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.*
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
}
