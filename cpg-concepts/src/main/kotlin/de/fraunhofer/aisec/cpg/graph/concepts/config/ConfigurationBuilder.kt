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
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

/**
 * Creates a new [Configuration] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @return The created [Configuration] concept.
 */
fun MetadataProvider.newConfiguration(underlyingNode: Node) =
    newConcept(::Configuration, underlyingNode = underlyingNode)

/**
 * Creates a new [ConfigurationSource] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @return The created [ConfigurationSource] concept.
 */
fun MetadataProvider.newConfigurationSource(underlyingNode: Node) =
    newConcept(::ConfigurationSource, underlyingNode = underlyingNode)

/**
 * Creates a new [ConfigurationGroupSource] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @return The created [ConfigurationGroupSource] concept.
 */
fun ConfigurationSource.newConfigurationGroupSource(underlyingNode: Node) =
    newConcept(
        { ConfigurationGroupSource(it, this@newConfigurationGroupSource) },
        underlyingNode = underlyingNode,
    )

/**
 * Creates a new [ConfigurationOptionSource] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @return The created [ConfigurationOptionSource] concept.
 */
fun ConfigurationGroupSource.newConfigurationOptionSource(underlyingNode: Node) =
    newConcept(
        { ConfigurationOptionSource(it, this@newConfigurationOptionSource) },
        underlyingNode = underlyingNode,
    )

/**
 * Creates a new [ConfigurationGroup] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @return The created [ConfigurationGroup] concept.
 */
fun Configuration.newConfigurationGroup(underlyingNode: Node) =
    newConcept(
        { ConfigurationGroup(underlyingNode = it, conf = this) },
        underlyingNode = underlyingNode,
    )

/**
 * Creates a new [ConfigurationOption] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param key The key node for the configuration option.
 * @param value The value node for the configuration option.
 * @return The created [ConfigurationOption] concept.
 */
fun ConfigurationGroup.newConfigurationOption(underlyingNode: Node, key: Node, value: Node?) =
    newConcept(
        { ConfigurationOption(underlyingNode = it, group = this, key = key, value = value) },
        underlyingNode = underlyingNode,
    )

/**
 * Creates a new [LoadConfiguration] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param fileExpression The expression representing the file to load.
 * @return The created [LoadConfiguration] operation.
 */
fun Configuration.newLoadConfiguration(underlyingNode: Node, fileExpression: Expression) =
    newOperation(
        { underlyingNode, concept ->
            LoadConfiguration(
                underlyingNode = underlyingNode,
                conf = concept,
                fileExpression = fileExpression,
            )
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [ReadConfigurationGroup] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @return The created [ReadConfigurationGroup] operation.
 */
fun ConfigurationGroup.newReadConfigurationGroup(underlyingNode: Node) =
    newOperation(
        { underlyingNode, concept ->
            ReadConfigurationGroup(underlyingNode = underlyingNode, group = concept)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [RegisterConfigurationGroup] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @return The created [RegisterConfigurationGroup] operation.
 */
fun ConfigurationGroup.newRegisterConfigurationGroup(underlyingNode: Node) =
    newOperation(
        { underlyingNode, concept ->
            RegisterConfigurationGroup(underlyingNode = underlyingNode, group = concept)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [ProvideConfiguration] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param source The source of the configuration.
 * @return The created [ProvideConfiguration] operation.
 */
fun Configuration.newProvideConfiguration(underlyingNode: Node, source: ConfigurationSource) =
    newOperation(
        { underlyingNode, concept ->
            ProvideConfiguration(underlyingNode = underlyingNode, source = source, conf = concept)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [ProvideConfigurationGroup] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param source The source of the configuration group.
 * @return The created [ProvideConfigurationGroup] operation.
 */
fun ConfigurationGroup.newProvideConfigurationGroup(
    underlyingNode: Node,
    source: ConfigurationGroupSource,
) =
    newOperation(
        { underlyingNode, concept ->
            ProvideConfigurationGroup(
                underlyingNode = underlyingNode,
                source = source,
                group = concept,
            )
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [ReadConfigurationOption] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @return The created [ReadConfigurationOption] operation.
 */
fun ConfigurationOption.newReadConfigurationOption(underlyingNode: Node) =
    newOperation(
        { underlyingNode, concept ->
            ReadConfigurationOption(underlyingNode = underlyingNode, option = concept)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [RegisterConfigurationOption] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param defaultValue The default value for the configuration option.
 * @return The created [RegisterConfigurationOption] operation.
 */
fun ConfigurationOption.newRegisterConfigurationOption(underlyingNode: Node, defaultValue: Node?) =
    newOperation(
        { underlyingNode, concept ->
            RegisterConfigurationOption(
                underlyingNode = underlyingNode,
                option = concept,
                defaultValue = defaultValue,
            )
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [ProvideConfigurationOption] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param source The source of the configuration option.
 * @param value The value of the configuration option.
 * @return The created [ProvideConfigurationOption] operation.
 */
fun ConfigurationOption.newProvideConfigurationOption(
    underlyingNode: Node,
    source: ConfigurationOptionSource,
    value: Node?,
) =
    newOperation(
        { underlyingNode, concept ->
            ProvideConfigurationOption(
                underlyingNode = underlyingNode,
                source = source,
                option = concept,
                value = value,
            )
        },
        underlyingNode = underlyingNode,
        concept = this,
    )
