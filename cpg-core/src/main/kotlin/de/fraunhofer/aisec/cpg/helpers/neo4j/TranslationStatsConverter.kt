/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationStats
import de.fraunhofer.aisec.cpg.graph.Name

/**
 * This converter can be used in a Neo4J session to persist the
 * [de.fraunhofer.aisec.cpg.TranslationStats] class into its components:
 * - currently only totalLinesOfCode
 *
 * Additionally, it converts the aforementioned Neo4J attributes in a node back into a [Name].
 */
class TranslationStatsConverter : CpgCompositeConverter<TranslationStats?> {

    companion object {
        const val FIELD_TOTAL_LINES_OF_CODE = "totalLinesOfCode"
    }

    override fun toGraphProperties(value: TranslationStats?): MutableMap<String, *> {
        val map = mutableMapOf<String, String>()

        if (value != null) {

            map[FIELD_TOTAL_LINES_OF_CODE] = value.totalLinesOfCode.toString()
        }

        return map
    }

    override val graphSchema: List<Pair<String, String>>
        get() = listOf(Pair("String", FIELD_TOTAL_LINES_OF_CODE))

    override fun toEntityAttribute(value: MutableMap<String, *>): TranslationStats {
        return TranslationStats().also {
            it.totalLinesOfCode = Integer.parseInt(value[FIELD_TOTAL_LINES_OF_CODE] as String)
        }
    }
}
