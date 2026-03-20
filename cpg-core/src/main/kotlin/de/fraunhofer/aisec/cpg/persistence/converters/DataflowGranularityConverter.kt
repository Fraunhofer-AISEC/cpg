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
package de.fraunhofer.aisec.cpg.persistence.converters

import de.fraunhofer.aisec.cpg.graph.edges.flows.FieldDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.Granularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity

/** This converter converts a [Granularity] into a string-based representation in Neo4J. */
class DataflowGranularityConverter : CpgCompositeConverter<Granularity> {
    companion object {
        const val GRANULARITY = "granularity"
        const val PARTIAL_TARGET = "partialTarget"
        const val PARTIAL_TARGET_TYPE = "partialTargetType"
    }

    override fun toGraphProperties(value: Granularity): MutableMap<String, *> {
        val map = mutableMapOf<String, String>()

        val type = value::class.simpleName?.substringBefore("DataflowGranularity")?.uppercase()
        if (type != null) {
            // The type of granularity
            map[GRANULARITY] = type
        }

        // Only for partial
        if (value is FieldDataflowGranularity) {
            map[PARTIAL_TARGET] = value.partialTarget.name.toString()
            map[PARTIAL_TARGET_TYPE] = "field"
        } else if (value is IndexedDataflowGranularity) {
            map[PARTIAL_TARGET] = value.partialTarget.toString()
            map[PARTIAL_TARGET_TYPE] = "index"
        }

        return map
    }

    override val graphSchema: List<Pair<String, String>>
        get() = listOf(Pair("String", GRANULARITY), Pair("String", PARTIAL_TARGET))

    override fun toEntityAttribute(value: Map<String, *>?): Granularity {
        throw UnsupportedOperationException()
    }
}
