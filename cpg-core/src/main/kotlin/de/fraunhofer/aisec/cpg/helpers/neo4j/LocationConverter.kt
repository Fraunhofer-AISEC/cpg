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
package de.fraunhofer.aisec.cpg.helpers.neo4j

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter

interface CpgCompositeConverter<A> : CompositeAttributeConverter<A> {
    /**
     * Determines to which properties and their types the received value will be split in the neo4j
     * representation. The type is the first element in the pair and the property name is the second
     * one.
     */
    val graphSchema: List<Pair<String, String>>
}

/**
 * This class converts a [PhysicalLocation] into the necessary composite attributes when persisting
 * a node into a Neo4J graph database.
 */
class LocationConverter : CpgCompositeConverter<PhysicalLocation?> {
    override fun toGraphProperties(value: PhysicalLocation?): Map<String, *> {
        val properties: MutableMap<String, Any> = HashMap()
        if (value != null) {
            properties[ARTIFACT] = value.artifactLocation.uri.toString()
            properties[START_LINE] = value.region.startLine
            properties[END_LINE] = value.region.endLine
            properties[START_COLUMN] = value.region.startColumn
            properties[END_COLUMN] = value.region.endColumn
        }
        return properties
    }

    override val graphSchema: List<Pair<String, String>>
        get() =
            listOf(
                Pair("String", ARTIFACT),
                Pair("int", START_LINE),
                Pair("int", END_LINE),
                Pair("int", START_COLUMN),
                Pair("int", END_COLUMN),
            )

    override fun toEntityAttribute(value: Map<String, *>?): PhysicalLocation? {
        return try {
            val startLine = toInt(value?.get(START_LINE)) ?: return null
            val endLine = toInt(value?.get(END_LINE)) ?: return null
            val startColumn = toInt(value?.get(START_COLUMN)) ?: return null
            val endColumn = toInt(value?.get(END_COLUMN)) ?: return null
            val uri = URI.create(value?.get(ARTIFACT) as? String ?: "")
            PhysicalLocation(uri, Region(startLine, startColumn, endLine, endColumn))
        } catch (e: NullPointerException) {
            null
        }
    }

    private fun toInt(objectToMap: Any?): Int? {
        val value = objectToMap?.toString()?.toLong() ?: return null
        return Math.toIntExact(value)
    }

    companion object {
        const val START_LINE = "startLine"
        const val END_LINE = "endLine"
        const val START_COLUMN = "startColumn"
        const val END_COLUMN = "endColumn"
        const val ARTIFACT = "artifact"
    }

    class LocationSerializer : JsonSerializer<PhysicalLocation>() {
        override fun serialize(
            value: PhysicalLocation?,
            gen: com.fasterxml.jackson.core.JsonGenerator?,
            serializers: com.fasterxml.jackson.databind.SerializerProvider?,
        ) {
            if (value != null && gen != null) {
                gen.writeStartObject()
                gen.writeStringField(ARTIFACT, value.artifactLocation.uri.toString())
                gen.writeNumberField(START_LINE, value.region.startLine)
                gen.writeNumberField(END_LINE, value.region.endLine)
                gen.writeNumberField(START_COLUMN, value.region.startColumn)
                gen.writeNumberField(END_COLUMN, value.region.endColumn)
                gen.writeEndObject()
            }
        }
    }

    class LocationDeserializer : JsonDeserializer<PhysicalLocation>() {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): PhysicalLocation? {
            if (p != null) {
                val node: JsonNode = p.codec.readTree(p)
                val startLine = node.get(START_LINE)?.asInt() ?: return null
                val endLine = node.get(END_LINE)?.asInt() ?: return null
                val startColumn = node.get(START_COLUMN)?.asInt() ?: return null
                val endColumn = node.get(END_COLUMN)?.asInt() ?: return null
                val uri = URI.create(node.get(ARTIFACT)?.asText() ?: "")
                return PhysicalLocation(uri, Region(startLine, startColumn, endLine, endColumn))
            }
            return null
        }
    }
}
