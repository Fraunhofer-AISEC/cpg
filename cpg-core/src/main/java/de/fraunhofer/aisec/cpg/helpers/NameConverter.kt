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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.parseName
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter

/**
 * This converter can be used in a Neo4J session to persist the [Name] class into its components:
 * - fully qualified name
 * - local name
 * - the delimiter
 *
 * Additionally, it converts the aforementioned Neo4J attributes in a node back into a [Name].
 */
class NameConverter : CompositeAttributeConverter<Name> {

    companion object {
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_LOCAL_NAME = "localName"
        const val FIELD_NAME_DELIMITER = "nameDelimiter"
    }

    override fun toGraphProperties(value: Name): MutableMap<String, *> {
        val map = mutableMapOf<String, String>()
        map[FIELD_FULL_NAME] = value.toString()
        map[FIELD_LOCAL_NAME] = value.localName
        map[FIELD_NAME_DELIMITER] = value.delimiter

        return map
    }

    override fun toEntityAttribute(value: MutableMap<String, *>): Name {
        return parseName(value[FIELD_FULL_NAME].toString(), value[FIELD_NAME_DELIMITER].toString())
    }
}
