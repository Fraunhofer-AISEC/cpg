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

fun MetadataProvider.newConfiguration(underlyingNode: Node) =
    newConcept(::Configuration, underlyingNode = underlyingNode)

fun MetadataProvider.newConfigurationSource(underlyingNode: Node) =
    newConcept(::ConfigurationSource, underlyingNode = underlyingNode)

fun ConfigurationSource.newConfigurationGroupSource(underlyingNode: Node) =
    newConcept(
        { ConfigurationGroupSource(it, this@newConfigurationGroupSource) },
        underlyingNode = underlyingNode,
    )

fun ConfigurationGroupSource.newConfigurationOptionSource(underlyingNode: Node) =
    newConcept(
        { ConfigurationOptionSource(it, this@newConfigurationOptionSource) },
        underlyingNode = underlyingNode,
    )

fun Configuration.newConfigurationGroup(underlyingNode: Node) =
    newConcept(
        { ConfigurationGroup(underlyingNode = it, conf = this) },
        underlyingNode = underlyingNode,
    )

fun ConfigurationGroup.newConfigurationOption(underlyingNode: Node, key: Node, value: Node?) =
    newConcept(
        { ConfigurationOption(underlyingNode = it, group = this, key = key, value = value) },
        underlyingNode = underlyingNode,
    )

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

fun ConfigurationGroup.newReadConfigurationGroup(underlyingNode: Node) =
    newOperation(
        { underlyingNode, concept ->
            ReadConfigurationGroup(underlyingNode = underlyingNode, group = concept)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

fun ConfigurationGroup.newRegisterConfigurationGroup(underlyingNode: Node) =
    newOperation(
        { underlyingNode, concept ->
            RegisterConfigurationGroup(underlyingNode = underlyingNode, group = concept)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

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

fun ConfigurationOption.newReadConfigurationOption(underlyingNode: Node) =
    newOperation(
        { underlyingNode, concept ->
            ReadConfigurationOption(underlyingNode = underlyingNode, option = concept)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

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
