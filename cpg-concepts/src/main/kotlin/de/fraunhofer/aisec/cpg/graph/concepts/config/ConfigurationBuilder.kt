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

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation

/**
 * Creates a new [Configuration] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [Configuration] concept.
 */
fun MetadataProvider.newConfiguration(underlyingNode: Node, connect: Boolean) =
    newConcept(::Configuration, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [ConfigurationSource] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [ConfigurationSource] concept.
 */
fun MetadataProvider.newConfigurationSource(underlyingNode: Node, connect: Boolean) =
    newConcept(::ConfigurationSource, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [ConfigurationGroupSource] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [ConfigurationSource] concept to which the group belongs.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [ConfigurationGroupSource] concept.
 */
fun MetadataProvider.newConfigurationGroupSource(
    underlyingNode: Node,
    concept: ConfigurationSource,
    connect: Boolean,
) =
    newConcept(::ConfigurationGroupSource, underlyingNode = underlyingNode, connect = connect)
        .apply { concept.groups += this }

/**
 * Creates a new [ConfigurationOptionSource] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [ConfigurationGroupSource] concept to which the operation belongs.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [ConfigurationOptionSource] concept.
 */
fun MetadataProvider.newConfigurationOptionSource(
    underlyingNode: Node,
    concept: ConfigurationGroupSource,
    connect: Boolean,
) =
    newConcept(
            { ConfigurationOptionSource(group = concept) },
            underlyingNode = underlyingNode,
            connect = connect,
        )
        .apply { concept.options += this }

/**
 * Creates a new [ConfigurationGroup] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [Configuration] concept to which the group belongs.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [ConfigurationGroup] concept.
 */
fun MetadataProvider.newConfigurationGroup(
    underlyingNode: Node,
    concept: Configuration,
    connect: Boolean,
) =
    newConcept(
            { ConfigurationGroup(conf = concept) },
            underlyingNode = underlyingNode,
            connect = connect,
        )
        .apply { concept.groups += this }

/**
 * Creates a new [ConfigurationOption] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [ConfigurationGroup] concept to which the option belongs.
 * @param key The key node for the configuration option.
 * @param value The value node for the configuration option.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [ConfigurationOption] concept.
 */
fun MetadataProvider.newConfigurationOption(
    underlyingNode: Node,
    concept: ConfigurationGroup,
    key: Node,
    value: Node?,
    connect: Boolean,
) =
    newConcept(
            { ConfigurationOption(group = concept, key = key, value = value) },
            underlyingNode = underlyingNode,
            connect = connect,
        )
        .apply { concept.options += this }

/**
 * Creates a new [LoadConfiguration] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [Configuration] concept to which the load operation belongs.
 * @param fileExpression The expression representing the file to load.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [LoadConfiguration] operation.
 */
fun MetadataProvider.newLoadConfiguration(
    underlyingNode: Node,
    concept: Configuration,
    fileExpression: Expression,
    connect: Boolean,
) =
    newOperation(
        { concept -> LoadConfiguration(conf = concept, fileExpression = fileExpression) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [ReadConfigurationGroup] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [ConfigurationGroup] concept to which the read operation belongs.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [ReadConfigurationGroup] operation.
 */
fun MetadataProvider.newReadConfigurationGroup(
    underlyingNode: Node,
    concept: ConfigurationGroup,
    connect: Boolean,
) =
    newOperation(
            { concept -> ReadConfigurationGroup(group = concept) },
            underlyingNode = underlyingNode,
            concept = concept,
            connect = connect,
        )
        .apply { name = group.name }

/**
 * Creates a new [RegisterConfigurationGroup] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [ConfigurationGroup] concept to which the register operation belongs.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [RegisterConfigurationGroup] operation.
 */
fun MetadataProvider.newRegisterConfigurationGroup(
    underlyingNode: Node,
    concept: ConfigurationGroup,
    connect: Boolean,
) =
    newOperation(
            { concept -> RegisterConfigurationGroup(group = concept) },
            underlyingNode = underlyingNode,
            concept = concept,
            connect = connect,
        )
        .apply { name = group.name }

/**
 * Creates a new [ProvideConfiguration] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param conf The [Configuration] concept which is the target which is provided by this operation.
 * @param source The [ConfigurationSource] concept to which the provide operation belongs and the
 *   source of the [Configuration].
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [ProvideConfiguration] operation.
 */
fun MetadataProvider.newProvideConfiguration(
    underlyingNode: Node,
    conf: Configuration,
    source: ConfigurationSource,
    connect: Boolean,
) =
    newOperation(
        { concept -> ProvideConfiguration(source = source, conf = conf) },
        underlyingNode = underlyingNode,
        concept = source,
        connect = connect,
    )

/**
 * Creates a new [ProvideConfigurationGroup] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param group The [ConfigurationGroup] concept which is the target which is provided by this
 *   operation.
 * @param source The [ConfigurationSource] concept to which the provide operation belongs and the
 *   source of the [ConfigurationGroup].
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [ProvideConfigurationGroup] operation.
 */
fun MetadataProvider.newProvideConfigurationGroup(
    underlyingNode: Node,
    group: ConfigurationGroup,
    source: ConfigurationGroupSource,
    connect: Boolean,
) =
    newOperation(
            { concept -> ProvideConfigurationGroup(source = source, group = group) },
            underlyingNode = underlyingNode,
            concept = source,
            connect = connect,
        )
        .apply { name = concept.name }

/**
 * Creates a new [ReadConfigurationOption] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [ConfigurationOption] concept to which the read operation belongs.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [ReadConfigurationOption] operation.
 */
fun MetadataProvider.newReadConfigurationOption(
    underlyingNode: Node,
    concept: ConfigurationOption,
    connect: Boolean,
) =
    newOperation(
            { concept -> ReadConfigurationOption(option = concept) },
            underlyingNode = underlyingNode,
            concept = concept,
            connect = connect,
        )
        .apply { name = option.name }

/**
 * Creates a new [RegisterConfigurationOption] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [ConfigurationOption] concept to which the register operation belongs.
 * @param defaultValue The default value for the configuration option.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [RegisterConfigurationOption] operation.
 */
fun MetadataProvider.newRegisterConfigurationOption(
    underlyingNode: Node,
    concept: ConfigurationOption,
    defaultValue: Node?,
    connect: Boolean,
) =
    newOperation(
            { concept ->
                RegisterConfigurationOption(option = concept, defaultValue = defaultValue)
            },
            underlyingNode = underlyingNode,
            concept = concept,
            connect = connect,
        )
        .apply { name = option.name }

/**
 * Creates a new [ProvideConfigurationOption] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param option The [ConfigurationOption] concept which is provided by this operation.
 * @param source The [ConfigurationOptionSource] representing the concept to which this operation
 *   belongs and the source providing this [ConfigurationOption].
 * @param value The value of the configuration option.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [ProvideConfigurationOption] operation.
 */
fun MetadataProvider.newProvideConfigurationOption(
    underlyingNode: Node,
    option: ConfigurationOption,
    source: ConfigurationOptionSource,
    value: Node?,
    connect: Boolean,
) =
    newOperation(
            { concept ->
                ProvideConfigurationOption(source = source, option = option, value = value)
            },
            underlyingNode = underlyingNode,
            concept = source,
            connect = connect,
        )
        .apply { name = concept.name }
