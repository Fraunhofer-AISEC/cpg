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
package de.fraunhofer.aisec.cpg.persistence.converters

import de.fraunhofer.aisec.cpg.persistence.CompositeAttributeConverter
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation

/**
 * Converts a [PhysicalLocation] into a compact map for the MCP (LLM-facing) node view: a bare
 * [FILE_NAME] instead of [LocationConverter]'s full artifact URI, since the URI's scheme/host are
 * dead weight for an LLM that only needs to reference the file (e.g. to re-read it or point a user
 * at it).
 */
class McpLocationConverter : CompositeAttributeConverter<PhysicalLocation?> {
    override fun toGraphProperty(value: PhysicalLocation?): Map<String, *> {
        val properties: MutableMap<String, Any> = HashMap()
        if (value != null) {
            val path = value.artifactLocation.uri.toString()
            properties[FILE_NAME] = path.substringAfterLast('/').substringAfterLast('\\')
            properties[START_LINE] = value.region.startLine
            properties[END_LINE] = value.region.endLine
            properties[START_COLUMN] = value.region.startColumn
            properties[END_COLUMN] = value.region.endColumn
        }
        return properties
    }

    override fun toEntityAttribute(value: Map<String, *>?): PhysicalLocation? {
        throw UnsupportedOperationException(
            "McpLocationConverter is write-only; it is only used to build the MCP-facing node view."
        )
    }

    companion object {
        const val FILE_NAME = "fileName"
        const val START_LINE = "startLine"
        const val END_LINE = "endLine"
        const val START_COLUMN = "startColumn"
        const val END_COLUMN = "endColumn"
    }
}
