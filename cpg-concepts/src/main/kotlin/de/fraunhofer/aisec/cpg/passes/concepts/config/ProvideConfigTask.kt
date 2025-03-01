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
package de.fraunhofer.aisec.cpg.passes.concepts.config

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.config.Configuration
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationGroup
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationGroupSource
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationOperation
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationOptionSource
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationSource
import de.fraunhofer.aisec.cpg.graph.concepts.config.LoadConfiguration
import de.fraunhofer.aisec.cpg.graph.concepts.config.ProvideConfiguration
import de.fraunhofer.aisec.cpg.graph.concepts.config.ProvideConfigurationGroup
import de.fraunhofer.aisec.cpg.graph.concepts.config.ProvideConfigurationOption
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.operationNodes
import de.fraunhofer.aisec.cpg.graph.translationResult
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptTask
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.stringValues

/**
 * This is a generic pass that is responsible for creating [ProvideConfiguration] nodes based on the
 * configuration sources found in the graph. It connects a [ConfigurationSource] with a matching
 * [Configuration].
 */
class ProvideConfigTask(target: Component, pass: ConceptPass, ctx: TranslationContext) :
    ConceptTask(target, pass, ctx) {
    override fun handleNode(node: Node) {
        when (node) {
            is TranslationUnitDeclaration -> handleTranslationUnit(node)
        }
    }

    private fun handleTranslationUnit(
        tu: TranslationUnitDeclaration
    ): List<ConfigurationOperation>? {
        return tu.translationResult
            ?.operationNodes
            ?.filterIsInstance<LoadConfiguration>()
            ?.flatMap { loadConfig ->
                // Loop through all configuration sources
                val sources =
                    tu.translationResult
                        ?.conceptNodes
                        ?.filterIsInstance<ConfigurationSource>()
                        ?.filter { source ->
                            loadConfig.fileExpression.stringValues?.contains(
                                source.name.toString()
                            ) == true
                        }

                // And create a ProvideConfiguration node for each source
                sources?.flatMap { handleConfiguration(it, loadConfig.conf, tu, loadConfig) }
                    ?: listOf()
            }
    }

    private fun handleConfiguration(
        source: ConfigurationSource,
        conf: Configuration,
        tu: TranslationUnitDeclaration,
        configuration: LoadConfiguration,
    ): MutableList<ConfigurationOperation> {
        val ops = mutableListOf<ConfigurationOperation>()

        // Loop through all groups and options and create ProvideConfigurationGroup and
        // ProvideConfigurationOption nodes
        ops += source.groups.mapNotNull { handleConfigurationGroup(conf, it) }.flatten()

        ops +=
            ProvideConfiguration(underlyingNode = tu, conf = configuration.conf, source = source)
                .also { it.name = source.name }

        return ops
    }

    private fun handleConfigurationGroup(
        conf: Configuration,
        source: ConfigurationGroupSource,
    ): MutableList<ConfigurationOperation>? {
        val ops = mutableListOf<ConfigurationOperation>()

        val sourceUnderlying = source.underlyingNode ?: return null
        val group = conf.groups.singleOrNull { it.name.localName == source.name.localName }
        if (group == null) {
            Util.warnWithFileLocation(
                sourceUnderlying,
                log,
                "Could not find configuration group for source {}",
                source.name.localName,
            )
            return null
        }

        val op =
            ProvideConfigurationGroup(
                    underlyingNode = sourceUnderlying,
                    group = group,
                    source = source,
                )
                .also { it.name = source.name }
        ops += op

        // Add an incoming DFG edge from the source
        group.prevDFGEdges.add(source)

        // Continue with the options
        ops += source.options.mapNotNull { handleConfigurationOption(group, it) }

        return ops
    }

    private fun handleConfigurationOption(
        group: ConfigurationGroup,
        source: ConfigurationOptionSource,
    ): ConfigurationOperation? {
        val sourceUnderlying = source.underlyingNode ?: return null
        val option = group.options.singleOrNull { it.name.localName == source.name.localName }

        if (option == null) {
            Util.warnWithFileLocation(
                sourceUnderlying,
                log,
                "Could not find configuration option for source {}",
                sourceUnderlying.name.localName,
            )
            return null
        }

        val op =
            ProvideConfigurationOption(
                    underlyingNode = sourceUnderlying,
                    option = option,
                    value = sourceUnderlying,
                    source = source,
                )
                .also { it.name = source.name }

        // Add an incoming DFG edge from the source
        option.prevDFGEdges.add(source)

        return op
    }
}
