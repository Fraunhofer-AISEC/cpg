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
package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

/**
 * Represents a group of configuration values within one [conf]. Depending on the type of
 * configuration data structure, there might only be one group (e.g., a "default" one), or there
 * might be several groups. For example, when loading a config from an INI file, each section would
 * be mapped to a [ConfigurationGroup], and each key-value pair would be mapped to an
 * [ConfigurationOption] within this group.
 */
public abstract class ConfigurationGroup(
    public val configuration: Configuration?,
    public val configurationOptions: MutableList<ConfigurationOption?>,
    dataLocation: DataLocation?,
    creation_time: ZonedDateTime?,
    description: String?,
    labels: MutableMap<String, String>?,
    name: String?,
    raw: String?,
    parent: Resource?,
    underlyingNode: Node?,
) : Data(dataLocation, creation_time, description, labels, name, raw, parent, underlyingNode) {
    init {
        name?.let { this.name = Name(localName = it) }
    }

    override fun equals(other: Any?): Boolean =
        other is ConfigurationGroup &&
            super.equals(other) &&
            other.configuration == this.configuration &&
            other.configurationOptions == this.configurationOptions

    override fun hashCode(): Int =
        Objects.hash(super.hashCode(), configuration, configurationOptions)
}
