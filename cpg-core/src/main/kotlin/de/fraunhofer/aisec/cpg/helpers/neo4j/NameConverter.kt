/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.parseName

/**
 * This converter can be used in a Neo4J session to persist the [Name] class into its components:
 * - fully qualified name
 * - local name
 * - the delimiter
 *
 * Additionally, it converts the aforementioned Neo4J attributes in a node back into a [Name].
 */
class NameConverter : CpgCompositeConverter<Name?> {

    companion object {
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_NAME = "name"
        const val FIELD_LOCAL_NAME = "localName"
        const val FIELD_NAME_DELIMITER = "nameDelimiter"
    }

    override fun toGraphProperties(value: Name?): MutableMap<String, *> {
        val map = mutableMapOf<String, String>()

        if (value != null) {
            // The full name of the node
            map[FIELD_FULL_NAME] = value.toString()

            // The local name of the node
            map[FIELD_LOCAL_NAME] = value.localName

            // The delimiter
            map[FIELD_NAME_DELIMITER] = value.delimiter

            // For reasons such as backwards compatibility and the fact that Neo4J likes to display
            // nodes in the UI with a "name" field as default, we also persist the full name (aka
            // the toString() representation) as "name"
            map[FIELD_NAME] = value.toString()
        }

        return map
    }

    override val graphSchema: List<Pair<String, String>>
        get() =
            listOf(
                Pair("String", FIELD_FULL_NAME),
                Pair("String", FIELD_LOCAL_NAME),
                Pair("String", FIELD_NAME),
                Pair("String", FIELD_NAME_DELIMITER),
            )

    override fun toEntityAttribute(value: MutableMap<String, *>): Name {
        return parseName(value[FIELD_FULL_NAME].toString(), value[FIELD_NAME_DELIMITER].toString())
    }
}
