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

class Configuration(underlyingNode: Node) : Concept(underlyingNode = underlyingNode) {
    var groups: MutableList<ConfigurationGroup> = mutableListOf()
}

class ConfigurationOption(underlyingNode: Node, var group: ConfigurationGroup) :
    Concept(underlyingNode = underlyingNode) {
    init {
        group.options.add(this)
    }
}

class ConfigurationGroup(underlyingNode: Node, var conf: Configuration) :
    Concept(underlyingNode = underlyingNode) {
    var options: MutableList<ConfigurationOption> = mutableListOf()

    init {
        conf.groups.add(this)
    }
}

abstract class ConfigurationOperation(underlyingNode: Node, concept: Concept) :
    Operation(underlyingNode = underlyingNode, concept = concept) {}

class LoadConfigurationFile(underlyingNode: Node, conf: Configuration) :
    ConfigurationOperation(underlyingNode = underlyingNode, concept = conf)

class ReadConfigurationOption(
    underlyingNode: Node,
    conf: Configuration,
    var option: ConfigurationOption,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = conf)

class ReadConfigurationGroup(
    underlyingNode: Node,
    conf: Configuration,
    var group: ConfigurationGroup,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = conf)
