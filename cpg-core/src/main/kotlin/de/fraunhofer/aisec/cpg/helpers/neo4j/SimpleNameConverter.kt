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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.persistence.AttributeConverter

/**
 * This converter converts a [Name] into a single [String] (in contrast to the [NameConverter],
 * which splits it up into several properties).
 */
class SimpleNameConverter : AttributeConverter<Name, String> {
    override fun toGraphProperty(value: Name): String {
        return value.toString()
    }

    override fun toEntityAttribute(value: String?): Name {
        if (value == null) {
            // Return an empty name if value is null
            return Name("")
        }

        // We cannot really know what the actual delimiter was, so we need to supply some delimiters
        // and hope for the best. Unfortunately, we do not get access to the "language" node here...
        return parseName(value, ".", ",", "::")
    }
}
