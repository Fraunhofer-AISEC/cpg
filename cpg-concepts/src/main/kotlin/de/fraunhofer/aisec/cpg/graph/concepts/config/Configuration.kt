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
package de.fraunhofer.aisec.cpg.graph.concepts.config

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.passes.concepts.config.ini.IniFileConfigurationPass

/**
 * Represents a configuration file. Common examples include .properties, .json, .ini, etc. Depending
 * on how the language frontend is implemented, the [underlyingNode] can be different things. For
 * example, the INI frontend models the configuration file as a [RecordDeclaration], which
 * individual entries being [FieldDeclaration] nodes. For more information, see
 * [IniFileConfigurationPass].
 */
class Configuration(underlyingNode: Node) : Concept(underlyingNode = underlyingNode) {
    var groups: MutableList<ConfigurationGroup> = mutableListOf()
}

/**
 * Represents a configuration group within one [conf]. Depending on the type of configuration, there
 * might only be one group (e.g., a "default" one), or there might be several groups. For example,
 * in an INI file, each section would be a group, and each key-value pair would be an option.
 */
class ConfigurationGroup(underlyingNode: Node, var conf: Configuration) :
    Concept(underlyingNode = underlyingNode) {
    var options: MutableList<ConfigurationOption> = mutableListOf()

    init {
        conf.groups.add(this)
    }
}

/**
 * Represents a configuration option within one [group]. Usually there is one option for each entry
 * in a configuration file.
 */
class ConfigurationOption(underlyingNode: Node, var group: ConfigurationGroup) :
    Concept(underlyingNode = underlyingNode) {
    init {
        group.options.add(this)
    }
}

/**
 * A common abstract class for configuration operations, such as reading options or a whole file.
 */
abstract class ConfigurationOperation(underlyingNode: Node, concept: Concept) :
    Operation(underlyingNode = underlyingNode, concept = concept) {}

class LoadConfigurationFile(underlyingNode: Node, conf: Configuration) :
    ConfigurationOperation(underlyingNode = underlyingNode, concept = conf)

class ReadConfigurationOption(
    underlyingNode: Node,
    conf: Configuration,
    var option: ConfigurationOption,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = conf) {
    init {
        name = option.name
    }
}

class ReadConfigurationGroup(
    underlyingNode: Node,
    conf: Configuration,
    var group: ConfigurationGroup,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = conf) {
    init {
        name = group.name
    }
}
