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
package de.fraunhofer.aisec.cpg.passes.concepts.config.python

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.config.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.followPrevDFG
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.SubscriptExpression
import de.fraunhofer.aisec.cpg.graph.translationResult
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.ini.IniFileConfigurationPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

/**
 * This pass is responsible for creating [ConfigurationOperation] nodes based on the
 * [`configparser`](https://docs.python.org/3/library/configparser.html) module of the Python
 * standard library.
 */
@DependsOn(IniFileConfigurationPass::class, softDependency = true)
@DependsOn(SymbolResolver::class)
class PythonStdLibConfigurationPass(ctx: TranslationContext) : ConceptPass(ctx) {
    override fun handleNode(node: Node?, tu: TranslationUnitDeclaration) {
        when (node) {
            is MemberCallExpression -> handleMemberCallExpression(node, tu)
            is SubscriptExpression -> handleSubscriptExpression(node, tu)
        }
    }

    private fun handleMemberCallExpression(
        call: MemberCallExpression,
        tu: TranslationUnitDeclaration,
    ): List<LoadConfigurationFile>? {
        if (call.name.toString() == "configparser.ConfigParser.read") {
            val filename = call.arguments.firstOrNull()?.evaluate() as? String

            if (filename == null) {
                log.warn("Could not determine filename for configparser.ConfigParser.read")
                return null
            }

            // Look for config nodes that match the file name
            val conf =
                tu.translationResult?.conceptNodes?.filterIsInstance<Configuration>()?.filter {
                    it.underlyingNode?.name.toString() == filename
                }
            return conf?.map {
                // Create a LoadConfigurationFile operation for each matching config node
                val op = LoadConfigurationFile(call, it)

                // And add a dataflow edge into the config node. This is a bit trickier, we want
                // to find the construct expression to the ConfigParser object and then inject it.
                // This is not perfect but the read() operation is applied on the object itself, and
                // we would need to set this point as the last write, which we cannot do here.
                val test = call.base?.followPrevDFG { it is ConstructExpression }
                val construct = test?.lastOrNull()
                if (construct is ConstructExpression) {
                    construct.prevDFGEdges.addContextSensitive(
                        it,
                        callingContext = CallingContextOut(construct),
                    )
                }
                op
            }
        }

        return null
    }

    private fun handleSubscriptExpression(
        sub: SubscriptExpression,
        tu: TranslationUnitDeclaration,
    ): ConfigurationOperation? {
        // Try to follow the path to the config file (load operation)
        val path =
            sub.arrayExpression.followPrevDFG { it is Configuration || it is ConfigurationGroup }
        val last = path?.lastOrNull()
        when (last) {
            // If we can follow it directly to the configuration node, then this is a read group
            // operation
            is Configuration -> {
                // Look for the group
                val group =
                    last.groups.find { it.name.localName == sub.subscriptExpression.evaluate() }
                if (group != null) {
                    val op = ReadConfigurationGroup(sub, conf = last, group = group)

                    // Add an incoming DFG from the option group
                    sub.prevDFGEdges.add(group)

                    return op
                }
            }
            is ConfigurationGroup -> {
                // Look for the option
                val option =
                    last.options.find { it.name.localName == sub.subscriptExpression.evaluate() }
                if (option != null) {
                    val op = ReadConfigurationOption(sub, conf = last.conf, option = option)

                    // Add an incoming DFG from the option
                    sub.prevDFGEdges.add(option)

                    return op
                }
            }
        }

        return null
    }
}
