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
import de.fraunhofer.aisec.cpg.analysis.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.config.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.followDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.followPrevDFG
import de.fraunhofer.aisec.cpg.graph.fqn
import de.fraunhofer.aisec.cpg.graph.implicit
import de.fraunhofer.aisec.cpg.graph.operationNodes
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.SubscriptExpression
import de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

/**
 * This pass is responsible for creating [ConfigurationOperation] nodes based on the
 * [`configparser`](https://docs.python.org/3/library/configparser.html) module of the Python
 * standard library.
 */
// @DependsOn(IniFileConfigurationPass::class, softDependency = true)
@DependsOn(SymbolResolver::class)
class PythonStdLibConfigurationPass(ctx: TranslationContext) : ConceptPass(ctx) {
    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        when (node) {
            is ConstructExpression -> handleConstructExpression(node)
            is MemberCallExpression -> handleMemberCallExpression(node)
            is SubscriptExpression -> handleSubscriptExpression(node, tu)
        }
    }

    /**
     * Translates a `configparser.ConfigParser()` constructor call into a [Configuration] concept.
     */
    private fun handleConstructExpression(expr: ConstructExpression): Configuration? {
        if (expr.name.toString() == "configparser.ConfigParser") {
            val conf = Configuration(underlyingNode = expr)
            expr.prevDFG += conf
            return conf
        }

        return null
    }

    /**
     * Translates `configparser.ConfigParser.read(filename)` calls into [LoadConfiguration]
     * operations.
     */
    private fun handleMemberCallExpression(call: MemberCallExpression): List<LoadConfiguration>? {
        val firstArgument = call.arguments.firstOrNull()
        if (call.name.toString() == "configparser.ConfigParser.read" && firstArgument != null) {
            // Look for our config data structure based on our "base" object
            val paths =
                call.base?.followDFGEdgesUntilHit(direction = Backward(GraphToFollow.DFG)) {
                    it is Configuration
                }
            paths?.fulfilled?.map {
                val conf = it.last() as Configuration
                val op = LoadConfiguration(call, conf, fileExpression = firstArgument)
                op
            } ?: listOf()
        }

        return null
    }

    /**
     * Translates `config["group"]` and `config["group"]["option"]` into operations, such as
     * [ReadConfigurationOption] or [ReadConfigurationGroup].
     *
     * Since the `configparser` module does not provide a way to explicitly define/register options
     * or groups, we need to implicitly create them here as well.,
     */
    private fun handleSubscriptExpression(
        sub: SubscriptExpression,
        tu: TranslationUnitDeclaration,
    ): List<ConfigurationOperation> {
        val ops = mutableListOf<ConfigurationOperation>()

        // We need to check, whether we access a group or an option
        val path =
            sub.arrayExpression.followPrevDFG { it is Configuration || it is ConfigurationGroup }
        val last = path?.lastOrNull()
        when (last) {
            // If we can follow it directly to the configuration node, then we access a group
            is Configuration -> {
                handleGroupAccess(last, sub)
            }
            is ConfigurationGroup -> {
                handleOptionAccess(last, sub)
            }
        }

        return emptyList()
    }

    /** Translates a group access (`config["group"]`) into a [ReadConfigurationGroup] operation. */
    private fun handleGroupAccess(
        conf: Configuration,
        sub: SubscriptExpression,
    ): MutableList<ConfigurationOperation>? {
        val ops = mutableListOf<ConfigurationOperation>()

        // Look for the group
        val name = sub.subscriptExpression.evaluate() as? String
        if (name == null) {
            warnWithFileLocation(
                sub,
                log,
                "We could not evaluate the configuration group name to a string",
            )
            return ops
        }

        var group = conf.groups.find { it.name.localName == name }
        if (group == null) {
            // If it does not exist, we create it and implicitly add a registration operation
            group = ConfigurationGroup(sub, conf = conf).also { it.name = Name(name) }.implicit()
            val op = RegisterConfigurationGroup(sub, conf = conf, group = group).implicit()
            ops += op
        }

        val op = ReadConfigurationGroup(sub, conf = conf, group = group)
        ops += op

        // Add an incoming DFG from the option group
        sub.prevDFGEdges.add(group)

        return ops
    }

    private fun handleOptionAccess(
        group: ConfigurationGroup,
        sub: SubscriptExpression,
    ): MutableList<ConfigurationOperation> {
        val ops = mutableListOf<ConfigurationOperation>()

        // Look for the option
        val name = sub.subscriptExpression.evaluate() as? String
        if (name == null) {
            warnWithFileLocation(
                sub,
                log,
                "We could not evaluate the configuration group name to a string",
            )
            return ops
        }

        var option = group.options.find { it.name.localName == name }
        if (option == null) {
            // If it does not exist, we create it and implicitly add a registration operation
            option =
                ConfigurationOption(sub, group = group, key = sub)
                    .also { it.name = group.name.fqn(name) }
                    .implicit()
            val op = RegisterConfigurationOption(sub, conf = group.conf, option = option).implicit()
            ops += op
        }

        val op = ReadConfigurationOption(sub, conf = group.conf, option = option)
        ops += op

        // Add an incoming DFG from the option
        sub.prevDFGEdges.add(option)

        return ops
    }

    /** Registers a configuration group or option if it does not exist yet. */
    private inline fun <reified RegisterType : ConfigurationOperation> registerIfItNotExists(
        tu: TranslationUnitDeclaration,
        filter: ((RegisterType) -> Boolean),
        construct: () -> (RegisterType),
    ): RegisterType {
        var register = tu.operationNodes.filterIsInstance<RegisterType>().singleOrNull(filter)
        if (register == null) {
            register = construct()
        }

        return register
    }
}

val Node.stringValues: Collection<String>?
    get() {
        val eval = this.evaluate(MultiValueEvaluator())
        when (eval) {
            is String -> return listOf(eval)
            is Collection<*> -> return eval.filterIsInstance<String>()
        }

        return null
    }
