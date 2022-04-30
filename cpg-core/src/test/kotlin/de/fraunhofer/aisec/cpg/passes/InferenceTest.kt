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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.get
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class InferenceTest {

    @Test
    fun testRecordInference() {
        val file = File("src/test/resources/inference/record.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferRecords(true).build()
                )
            }

        assertNotNull(tu)

        val record = tu.byNameOrNull<RecordDeclaration>("T")
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

    @Test
    fun testRecordInferencePointer() {
        val file = File("src/test/resources/inference/record_ptr.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferRecords(true).build()
                )
            }

        assertNotNull(tu)

        val record = tu.byNameOrNull<RecordDeclaration>("T")
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
