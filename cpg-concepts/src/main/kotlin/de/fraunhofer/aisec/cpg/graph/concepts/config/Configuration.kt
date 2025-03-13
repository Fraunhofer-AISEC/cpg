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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

/**
 * Represents the abstract concept of a "configuration". This is a common pattern in many
 * programming languages, where a data structure in code represents an aggregation of configuration
 * values. For example, in Python, the
 * [`configparser`](https://docs.python.org/3/library/configparser.html) module is used to read INI
 * files, and the config values are represented as a dictionary-like object.
 *
 * Often, the configuration is loaded from multiple sources, such as INI files, environment
 * variables, and command-line arguments.
 */
class Configuration(underlyingNode: Node) : Concept(underlyingNode = underlyingNode) {
    var groups: MutableList<ConfigurationGroup> = mutableListOf()

    /**
     * The individual operations that target parts of the configuration are assigned to
     * [Concept.ops] of their respective concept. For example a [ReadConfigurationGroup] is part of
     * the [ConfigurationGroup]'s ops. This property returns all operations of all groups and
     * options as well as the ones targeting the complete configuration.
     */
    val allOps: Set<Operation>
        get() {
            return ops + groups.flatMap { it.ops + it.options.flatMap { option -> option.ops } }
        }
}

/**
 * Represents a group of configuration values within one [conf]. Depending on the type of
 * configuration data structure, there might only be one group (e.g., a "default" one), or there
 * might be several groups.
 *
 * For example, when loading a config from an INI file, each section would be mapped to a
 * [ConfigurationGroup], and each key-value pair would be mapped to an [ConfigurationOption] within
 * this group.
 */
class ConfigurationGroup(underlyingNode: Node, var conf: Configuration) :
    Concept(underlyingNode = underlyingNode) {
    var options: MutableList<ConfigurationOption> = mutableListOf()
}

/**
 * Represents a configuration option within one [group]. Usually there is one option for each entry
 * in a configuration data structure.
 */
class ConfigurationOption(
    underlyingNode: Node,
    var group: ConfigurationGroup,
    /**
     * The node that represents the "key" of this option. For example, in an INI file, this would be
     * the [FieldDeclaration] node that represents the key.
     */
    var key: Node,
    /**
     * The node that represents the "value" of this option. For example, in an INI file, this would
     * be the [FieldDeclaration.initializer] node that represents the value.
     *
     * Since initializers could potentially be empty, we make this nullable.
     */
    var value: Node? = null,
) : Concept(underlyingNode = underlyingNode)

/**
 * A common abstract class for configuration operations, such as reading options or a whole file.
 */
abstract class ConfigurationOperation(underlyingNode: Node, concept: Concept) :
    Operation(underlyingNode = underlyingNode, concept = concept)

/** Represents an operation to load a configuration from a source, such as a file. */
class LoadConfiguration(
    underlyingNode: Node,
    var conf: Configuration,
    /** The expression that holds the file that is loaded. */
    val fileExpression: Expression,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = conf)

/**
 * Represents an operation to read a specific configuration group. Often this is done with a member
 * access or a subscript operation on the configuration object, such as `conf.GROUP` or
 * `conf["GROUP"]`.
 */
class ReadConfigurationGroup(
    underlyingNode: Node,
    /** The config group that is being read with this operation. */
    var group: ConfigurationGroup,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = group)

/**
 * Represents an operation to read a specific configuration option. Often this is done with a member
 * access such as `group.option` or a subscript operation such as `group["option"]`.
 */
class ReadConfigurationOption(
    underlyingNode: Node,
    /** The config option that is being read with this operation. */
    var option: ConfigurationOption,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = option)

/**
 * Represents an operation to register a new [ConfigurationGroup]. This is often done with a call,
 * such as `conf.registerGroup("group")`. This might not be necessary for all configuration
 * frameworks, some might allow to directly read the group (via [ReadConfigurationGroup]) without
 * registering it first, or it is done implicitly.
 *
 * When code and configuration is interacting, we expect that the configuration file (such as an INI
 * file) contains the [ConfigurationGroup] node and the code contains the
 * [RegisterConfigurationGroup] and [ReadConfigurationGroup] nodes.
 */
class RegisterConfigurationGroup(
    underlyingNode: Node,
    /** The config group that is being registered with this operation. */
    var group: ConfigurationGroup,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = group)

/**
 * Represents an operation to register a new [ConfigurationOption]. This is often done with a call,
 * such as `conf.registerOption("option", "defaultValue")`. This might not be necessary for all
 * configuration frameworks, some might allow to directly read the group (via
 * [RegisterConfigurationOption]) without registering it first, or it is done implicitly.
 *
 * When code and configuration is interacting, we expect that the configuration file (such as an INI
 * file) contains the [ConfigurationOption] node and the code contains the
 * [RegisterConfigurationOption] and [ReadConfigurationOption] nodes.
 */
class RegisterConfigurationOption(
    underlyingNode: Node,
    /** The config option that is being registered with this operation. */
    var option: ConfigurationOption,
    /** An optional default value of the option. */
    var defaultValue: Node? = null,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = option)

/**
 * Represents an operation to provide a [Configuration], e.g., in the form of a configuration file
 * (through a [ConfigurationSource]).
 *
 * When the configuration file is loaded, a [LoadConfiguration] operation would be found in the code
 * component (matching the configuration file's name in [LoadConfiguration.fileExpression]) and the
 * [ProvideConfiguration] operation would be found in the configuration component.
 *
 * But also other sources of configuration could be represented by a [ProvideConfiguration]
 * operation, such as environment variables or command-line arguments.
 *
 * Note: The [ProvideConfiguration] operation is part of the [ConfigurationSource.ops] and not of
 * the [Configuration.ops] as it's an operation of the source, not the target.
 */
class ProvideConfiguration(
    underlyingNode: Node,
    var source: ConfigurationSource,
    var conf: Configuration,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = source)

/**
 * Represents an operation to provide a [ConfigurationGroup]. It connects a
 * [ConfigurationGroupSource] with a [ConfigurationGroup].
 */
class ProvideConfigurationGroup(
    underlyingNode: Node,
    var source: ConfigurationGroupSource,
    var group: ConfigurationGroup,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = source)

/**
 * Represents an operation to provide a [ConfigurationOption]. It connects a
 * [ConfigurationOptionSource] with a [ConfigurationOption].
 */
class ProvideConfigurationOption(
    underlyingNode: Node,
    var source: ConfigurationOptionSource,
    var option: ConfigurationOption,
    var value: Node?,
) : ConfigurationOperation(underlyingNode = underlyingNode, concept = source)

/**
 * Represents a possible source for a configuration. For example, when loading an INI file with our
 * INI file frontend, the whole file would be represented as a [TranslationUnitDeclaration]. This
 * translation unit declaration would be the source of the configuration.
 */
class ConfigurationSource(underlyingNode: Node) : Concept(underlyingNode = underlyingNode) {
    val groups: MutableList<ConfigurationGroupSource> = mutableListOf()

    /**
     * The individual operations that target parts of the configuration are assigned to
     * [Concept.ops] of their respective concept. For example a [ReadConfigurationGroup] is part of
     * the [ConfigurationGroup]'s ops. This property returns all operations of all groups and
     * options as well as the ones targeting the complete configuration.
     */
    val allOps: Set<Operation>
        get() {
            return ops + groups.flatMap { it.ops + it.options.flatMap { option -> option.ops } }
        }
}

/**
 * Represents a possible group source for a configuration group. For example, when loading an INI
 * file with our INI file frontend, each section is presented as a [RecordDeclaration]. This record
 * declaration would be the source of the configuration group.
 */
class ConfigurationGroupSource(underlyingNode: Node) : Concept(underlyingNode = underlyingNode) {
    val options: MutableList<ConfigurationOptionSource> = mutableListOf()
}

/**
 * Represents a possible option source for a configuration option. For example, when loading an INI
 * file with our INI file frontend, each key-value pair is presented as a [FieldDeclaration]. This
 * field declaration would be the source to the configuration option.
 */
class ConfigurationOptionSource(underlyingNode: Node, var group: ConfigurationGroupSource) :
    Concept(underlyingNode = underlyingNode)
