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
package de.fraunhofer.aisec.cpg.graph.concepts

import de.fraunhofer.aisec.cpg.graph.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class GenericFieldTest {

    private class TestConcept(underlyingNode: Node? = null) : Concept(underlyingNode)

    @Test
    fun `of parses recognized scalar type names`() {
        assertEquals(GenericPropertyValue.IntegerValue(42), GenericPropertyValue.of("Int", "42"))
        assertEquals(GenericPropertyValue.FloatValue(1.5), GenericPropertyValue.of("Double", "1.5"))
        assertEquals(
            GenericPropertyValue.BooleanValue(true),
            GenericPropertyValue.of("Boolean", "true"),
        )
        assertEquals(
            GenericPropertyValue.StringValue("hello"),
            GenericPropertyValue.of("String", "hello"),
        )
    }

    @Test
    fun `of falls back to StringValue for unrecognized or missing type`() {
        assertEquals(
            GenericPropertyValue.StringValue("hello"),
            GenericPropertyValue.of("SomeUnknownType", "hello"),
        )
        assertEquals(
            GenericPropertyValue.StringValue("hello"),
            GenericPropertyValue.of(null, "hello"),
        )
    }

    @Test
    fun `of returns null when value does not parse as the declared scalar type`() {
        assertEquals(null, GenericPropertyValue.of("Int", "notAnInt"))
        assertEquals(null, GenericPropertyValue.of("Double", "notADouble"))
        assertEquals(null, GenericPropertyValue.of("Boolean", "notABoolean"))
    }

    @Test
    fun `of threads the description through to the parsed value`() {
        val value = GenericPropertyValue.of("Int", "42", description = "the answer")
        assertIs<GenericPropertyValue.IntegerValue>(value)
        assertEquals("the answer", value.description)
    }

    @Test
    fun `description defaults to null when not supplied`() {
        assertNull(GenericPropertyValue.StringValue("hello").description)
    }

    @Test
    fun `rawValue exposes the underlying value for every kind`() {
        val node = TestConcept()
        assertEquals("hello", GenericPropertyValue.StringValue("hello").rawValue)
        assertEquals(42L, GenericPropertyValue.IntegerValue(42).rawValue)
        assertEquals(1.5, GenericPropertyValue.FloatValue(1.5).rawValue)
        assertEquals(true, GenericPropertyValue.BooleanValue(true).rawValue)
        assertEquals(node, GenericPropertyValue.NodeReferenceValue(node).rawValue)
    }

    @Test
    fun `nodeReferences extracts only NodeReferenceValue entries, keyed by property name`() {
        val node = TestConcept()
        val properties =
            GenericProperties(
                mapOf(
                    "ref" to GenericPropertyValue.NodeReferenceValue(node),
                    "name" to GenericPropertyValue.StringValue("value"),
                    "count" to GenericPropertyValue.IntegerValue(1),
                )
            )

        assertEquals(mapOf("ref" to node), properties.nodeReferences)
    }

    @Test
    fun `converter flattens scalars and skips node references`() {
        val node = TestConcept()
        val properties =
            GenericProperties(
                mapOf(
                    "aString" to GenericPropertyValue.StringValue("value"),
                    "anInt" to GenericPropertyValue.IntegerValue(42),
                    "aFloat" to GenericPropertyValue.FloatValue(1.5),
                    "aBool" to GenericPropertyValue.BooleanValue(true),
                    "aRef" to GenericPropertyValue.NodeReferenceValue(node),
                )
            )

        val graphProperties = GenericPropertiesConverter().toGraphProperty(properties)

        assertEquals(
            mapOf(
                "${GenericPropertiesConverter.GRAPH_PROPERTY_PREFIX}aString" to "value",
                "${GenericPropertiesConverter.GRAPH_PROPERTY_PREFIX}anInt" to 42L,
                "${GenericPropertiesConverter.GRAPH_PROPERTY_PREFIX}aFloat" to 1.5,
                "${GenericPropertiesConverter.GRAPH_PROPERTY_PREFIX}aBool" to true,
            ),
            graphProperties,
        )
    }

    @Test
    fun `converter round-trips scalars through graph properties`() {
        val properties =
            GenericProperties(
                mapOf(
                    "aString" to GenericPropertyValue.StringValue("value"),
                    "anInt" to GenericPropertyValue.IntegerValue(42),
                    "aFloat" to GenericPropertyValue.FloatValue(1.5),
                    "aBool" to GenericPropertyValue.BooleanValue(true),
                )
            )

        val converter = GenericPropertiesConverter()
        val restored = converter.toEntityAttribute(converter.toGraphProperty(properties))

        assertEquals(properties, restored)
    }

    @Test
    fun `converter ignores graph properties without the generic property prefix`() {
        val restored =
            GenericPropertiesConverter().toEntityAttribute(mapOf("name" to "someNodeName"))

        assertEquals(GenericProperties(emptyMap()), restored)
    }
}
