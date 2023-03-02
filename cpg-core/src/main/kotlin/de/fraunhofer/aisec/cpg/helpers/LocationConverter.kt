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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter

class LocationConverter : CompositeAttributeConverter<PhysicalLocation?> {
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

    override fun toEntityAttribute(value: Map<String?, *>?): PhysicalLocation? {
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
}
