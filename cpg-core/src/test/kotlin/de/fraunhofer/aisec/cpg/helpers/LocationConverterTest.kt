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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.persistence.CompositeAttributeConverter
import de.fraunhofer.aisec.cpg.persistence.converters.LocationConverter
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import de.fraunhofer.aisec.cpg.test.*
import java.net.URI
import java.util.*
import kotlin.collections.HashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class LocationConverterTest : BaseTest() {
    private val sut: CompositeAttributeConverter<PhysicalLocation?>
        get() {
            return LocationConverter()
        }

    @Test
    fun toEntityAttributeWithNullValueLine() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        // act
        val have = sut.toEntityAttribute(null)
        // assert
        assertNull(have)
    }

    @Test
    fun toEntityAttributeWithNullStartLine() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value = mutableMapOf<String, Any?>()
        value[LocationConverter.START_LINE] = null
        // act
        val have = sut.toEntityAttribute(value)
        // assert
        assertNull(have)
    }

    @Test
    fun toEntityAttributeWithInteger() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = 1
        value[LocationConverter.START_LINE] = startLineValue // autoboxing to Integer
        val endLineValue = 2
        value[LocationConverter.END_LINE] = endLineValue
        val startColumnValue = 3
        value[LocationConverter.START_COLUMN] = startColumnValue
        val endColumnValue = 4
        value[LocationConverter.END_COLUMN] = endColumnValue
        value[LocationConverter.ARTIFACT] = URI_STRING
        val region = Region(startLineValue, startColumnValue, endLineValue, endColumnValue)
        val want = PhysicalLocation(URI_TO_TEST, region)
        // act
        val have = sut.toEntityAttribute(value)
        // assert
        assertEquals(want, have)
    }

    @Test
    fun toEntityAttributeWithNullGraph() {
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val have = sut.toGraphProperties(null)
        assertEquals(mapOf<String, Any?>(), have)
    }

    @Test
    fun toEntityAttributeWithIntegerGraph() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = 1
        value[LocationConverter.START_LINE] = startLineValue // autoboxing to Integer
        val endLineValue = 2
        value[LocationConverter.END_LINE] = endLineValue
        val startColumnValue = 3
        value[LocationConverter.START_COLUMN] = startColumnValue
        val endColumnValue = 4
        value[LocationConverter.END_COLUMN] = endColumnValue
        value[LocationConverter.ARTIFACT] = URI_STRING
        val region = Region(startLineValue, startColumnValue, endLineValue, endColumnValue)
        val want = PhysicalLocation(URI_TO_TEST, region)
        // act
        val have = sut.toGraphProperties(want)
        // assert
        assertEquals(value, have)
    }

    @Test
    fun toEntityAttributeWithLong() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue: Long = 1
        value[LocationConverter.START_LINE] = startLineValue // autoboxing to Long
        val endLineValue: Long = 2
        value[LocationConverter.END_LINE] = endLineValue
        val startColumnValue: Long = 3
        value[LocationConverter.START_COLUMN] = startColumnValue
        val endColumnValue: Long = 4
        value[LocationConverter.END_COLUMN] = endColumnValue
        value[LocationConverter.ARTIFACT] = URI_STRING
        val region =
            Region(
                startLineValue.toInt(),
                startColumnValue.toInt(),
                endLineValue.toInt(),
                endColumnValue.toInt(),
            )
        val want = PhysicalLocation(URI_TO_TEST, region)
        // act
        val have = sut.toEntityAttribute(value)
        // assert
        assertEquals(want, have)
    }

    @Test
    fun toEntityAttributeWithIntegerAndLong() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = 1
        value[LocationConverter.START_LINE] = startLineValue // autoboxing to Integer
        val endLineValue = 2
        value[LocationConverter.END_LINE] = endLineValue
        val startColumnValue: Long = 3
        value[LocationConverter.START_COLUMN] = startColumnValue // autoboxing to Long
        val endColumnValue: Long = 4
        value[LocationConverter.END_COLUMN] = endColumnValue
        value[LocationConverter.ARTIFACT] = URI_STRING
        val region =
            Region(startLineValue, startColumnValue.toInt(), endLineValue, endColumnValue.toInt())
        val want = PhysicalLocation(URI_TO_TEST, region)
        // act
        val have = sut.toEntityAttribute(value)
        // assert
        assertEquals(want, have)
    }

    @Test
    fun toEntityAttributeWithStrings() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = "1"
        value[LocationConverter.START_LINE] = startLineValue
        val endLineValue = "2"
        value[LocationConverter.END_LINE] = endLineValue
        val startColumnValue = "3"
        value[LocationConverter.START_COLUMN] = startColumnValue
        val endColumnValue = "4"
        value[LocationConverter.END_COLUMN] = endColumnValue
        value[LocationConverter.ARTIFACT] = URI_STRING
        val region =
            Region(
                startLineValue.toInt(),
                startColumnValue.toInt(),
                endLineValue.toInt(),
                endColumnValue.toInt(),
            )
        val want = PhysicalLocation(URI_TO_TEST, region)
        // act
        val have = sut.toEntityAttribute(value)
        // assert
        assertEquals(want, have)
    }

    @Test
    fun toEntityAttributeWithCustomTypes() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue: Any = CustomNumber(1)
        value[LocationConverter.START_LINE] = startLineValue
        val endLineValue: Any = CustomNumber(2)
        value[LocationConverter.END_LINE] = endLineValue
        val startColumnValue: Any = CustomNumber(3)
        value[LocationConverter.START_COLUMN] = startColumnValue
        val endColumnValue: Any = CustomNumber(4)
        value[LocationConverter.END_COLUMN] = endColumnValue
        value[LocationConverter.ARTIFACT] = URI_STRING
        val region =
            Region(
                startLineValue.toString().toInt(),
                startColumnValue.toString().toInt(),
                endLineValue.toString().toInt(),
                endColumnValue.toString().toInt(),
            )
        val want = PhysicalLocation(URI_TO_TEST, region)
        // act
        val have = sut.toEntityAttribute(value)
        // assert
        assertEquals(want, have)
    }

    @Test
    fun toEntityAttributeWithMixedTypes() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue: Any = 1
        value[LocationConverter.START_LINE] = startLineValue
        val endLineValue: Any = 2L
        value[LocationConverter.END_LINE] = endLineValue
        val startColumnValue: Any = "3"
        value[LocationConverter.START_COLUMN] = startColumnValue
        val endColumnValue: Any = CustomNumber(4)
        value[LocationConverter.END_COLUMN] = endColumnValue
        value[LocationConverter.ARTIFACT] = URI_STRING
        val region =
            Region(
                startLineValue.toString().toInt(),
                startColumnValue.toString().toInt(),
                endLineValue.toString().toInt(),
                endColumnValue.toString().toInt(),
            )
        val want = PhysicalLocation(URI_TO_TEST, region)
        // act
        val have = sut.toEntityAttribute(value)
        // assert
        assertEquals(want, have)
    }

    @Test
    fun toEntityAttributeWithValueBiggerMaxIntBooms() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = Int.MAX_VALUE.toLong() + 1
        value[LocationConverter.START_LINE] = startLineValue

        // act // assert
        assertFailsWith<ArithmeticException> { sut.toEntityAttribute(value) }
    }

    @Test
    fun toEntityAttributeWithValueSmallerMinIntBooms() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = Int.MIN_VALUE.toLong() - 1
        value[LocationConverter.START_LINE] = startLineValue

        // act // assert
        assertFailsWith<ArithmeticException> { sut.toEntityAttribute(value) }
    }

    @Test
    fun toEntityAttributeWithAFloatBooms() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = 1.0.toFloat()
        value[LocationConverter.START_LINE] = startLineValue

        // act // assert
        assertFailsWith<NumberFormatException> { sut.toEntityAttribute(value) }
    }

    @Test
    fun toEntityAttributeWithADoubleBooms() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = 1.0
        value[LocationConverter.START_LINE] = startLineValue

        // act // assert
        assertFailsWith<NumberFormatException> { sut.toEntityAttribute(value) }
    }

    @Test
    fun toEntityAttributeWithAStringBooms() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue: Any = "TEST STRING"
        value[LocationConverter.START_LINE] = startLineValue

        // act // assert
        assertFailsWith<NumberFormatException> { sut.toEntityAttribute(value) }
    }

    @Test
    fun toEntityAttributeWithAObjectBooms() {
        // arrange
        val sut: CompositeAttributeConverter<PhysicalLocation?> = sut
        val value: MutableMap<String, Any?> = HashMap()
        val startLineValue = Any()
        value[LocationConverter.START_LINE] = startLineValue

        // act // assert
        assertFailsWith<NumberFormatException> { sut.toEntityAttribute(value) }
    }

    private class CustomNumber(private val value: Int) : Number() {
        override fun toString(): String {
            return toInt().toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CustomNumber) return false
            return value == other.value
        }

        override fun hashCode(): Int {
            return Objects.hash(value)
        }

        override fun toByte(): Byte {
            return value.toByte()
        }

        @Deprecated(
            "Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.\nIf you override toChar() function in your Number inheritor, it's recommended to gradually deprecate the overriding function and then remove it.\nSee https://youtrack.jetbrains.com/issue/KT-46465 for details about the migration",
            replaceWith = ReplaceWith("this.toInt().toChar()"),
        )
        override fun toChar(): Char {
            return value.toChar()
        }

        override fun toDouble(): Double {
            return value.toDouble()
        }

        override fun toFloat(): Float {
            return value.toFloat()
        }

        override fun toInt(): Int {
            return value
        }

        override fun toLong(): Long {
            return value.toLong()
        }

        override fun toShort(): Short {
            return value.toShort()
        }
    }

    companion object {
        private const val URI_STRING = "test://test:1234"
        private val URI_TO_TEST = URI.create(URI_STRING)
    }
}
