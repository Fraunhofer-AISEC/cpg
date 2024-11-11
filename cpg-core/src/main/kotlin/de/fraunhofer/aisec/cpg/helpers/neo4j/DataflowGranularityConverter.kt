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
package de.fraunhofer.aisec.cpg.helpers.neo4j

import de.fraunhofer.aisec.cpg.graph.edges.flows.Granularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity

/** This converter converts a [Granularity] into a string-based representation in Neo4J. */
class DataflowGranularityConverter : CpgCompositeConverter<Granularity> {
    companion object {
        const val FIELD_GRANULARITY = "granularity"
        const val FIELD_PARTIAL_TARGET = "partialTarget"
    }

    override fun toGraphProperties(value: Granularity): MutableMap<String, *> {
        val map = mutableMapOf<String, String>()

        val type = value::class.simpleName?.substringBefore("DataflowGranularity")?.uppercase()
        if (type != null) {
            // The type of granularity
            map[FIELD_GRANULARITY] = type
        }

        // Only for partial
        if (value is PartialDataflowGranularity) {
            map[FIELD_PARTIAL_TARGET] = value.partialTarget?.name.toString()
        }

        return map
    }

    override val graphSchema: List<Pair<String, String>>
        get() = listOf(Pair("String", FIELD_GRANULARITY), Pair("String", FIELD_PARTIAL_TARGET))

    override fun toEntityAttribute(value: MutableMap<String, *>): Granularity {
        throw UnsupportedOperationException()
    }
}
