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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.config.Configuration
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationGroup
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationGroupSource
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationOperation
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationOptionSource
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationSource
import de.fraunhofer.aisec.cpg.graph.concepts.config.LoadConfiguration
import de.fraunhofer.aisec.cpg.graph.concepts.config.ProvideConfiguration
import de.fraunhofer.aisec.cpg.graph.concepts.config.newProvideConfiguration
import de.fraunhofer.aisec.cpg.graph.concepts.config.newProvideConfigurationGroup
import de.fraunhofer.aisec.cpg.graph.concepts.config.newProvideConfigurationOption
import de.fraunhofer.aisec.cpg.graph.operationNodes
import de.fraunhofer.aisec.cpg.graph.translationResult
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.stringValues

/**
 * This is a generic pass that is responsible for creating [ProvideConfiguration] nodes based on the
 * configuration sources found in the graph. It connects a [ConfigurationSource] with a matching
 * [Configuration].
 */
class ProvideConfigPass(ctx: TranslationContext) : ConceptPass(ctx) {
    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        when (node) {
            is TranslationUnitDeclaration -> handleTranslationUnit(node)
        }
    }

    private fun handleTranslationUnit(
        tu: TranslationUnitDeclaration
    ): List<ConfigurationOperation> {
        // Loop through all configuration sources
        return tu.conceptNodes.filterIsInstance<ConfigurationSource>().flatMap { source ->
            // Find all LoadConfigurationFile operations that match the INI file name
            val loadConfigOps =
                tu.translationResult
                    ?.operationNodes
                    ?.filterIsInstance<LoadConfiguration>()
                    ?.filter {
                        it.fileExpression.stringValues?.contains(source.name.toString()) == true
                    }

            // And create a ProvideConfiguration node for each of them
            loadConfigOps?.forEach { handleConfiguration(source, it.conf, tu, it) }
            listOf()
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
        source.groups.mapNotNull { handleConfigurationGroup(conf, it) }.flatten()

        newProvideConfiguration(
                underlyingNode = tu,
                conf = configuration.conf,
                source = source,
                connect = true,
            )
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

        newProvideConfigurationGroup(
                underlyingNode = sourceUnderlying,
                group = group,
                source = source,
                connect = true,
            )
            .also { it.name = source.name }

        // Add an incoming DFG edge from the source
        group.prevDFGEdges.add(source)

        // Continue with the options
        source.options.forEach { handleConfigurationOption(group, it) }

        return ops
    }

    private fun handleConfigurationOption(
        group: ConfigurationGroup,
        source: ConfigurationOptionSource,
    ) {
        val sourceUnderlying = source.underlyingNode ?: return
        val option = group.options.singleOrNull { it.name.localName == source.name.localName }

        if (option == null) {
            Util.warnWithFileLocation(
                sourceUnderlying,
                log,
                "Could not find configuration option for source {}",
                sourceUnderlying.name.localName,
            )
            return
        }

        newProvideConfigurationOption(
                underlyingNode = sourceUnderlying,
                option = option,
                value = sourceUnderlying,
                source = source,
                connect = true,
            )
            .also { it.name = source.name }

        // Add an incoming DFG edge from the source
        option.prevDFGEdges.add(source)
    }
}
