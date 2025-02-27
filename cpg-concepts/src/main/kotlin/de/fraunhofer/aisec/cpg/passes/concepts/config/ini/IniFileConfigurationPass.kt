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
package de.fraunhofer.aisec.cpg.passes.concepts.config.ini

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.config.*
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.operationNodes
import de.fraunhofer.aisec.cpg.graph.translationResult
import de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.PythonStdLibConfigurationPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.stringValues
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import kotlin.collections.singleOrNull

/**
 * This pass is responsible for creating [ConfigurationOperation] nodes based on the INI file
 * frontend.
 *
 * First, it looks for [LoadConfiguration] operations that match the INI file name to retrieve a
 * [Configuration] data structure, creating a [ProvideConfiguration] node. Then, it creates
 * [ProvideConfigurationGroup] operations for each section in an INI file, and
 * [ProvideConfigurationOption] operations for each option in a section.
 */
@DependsOn(ImportResolver::class)
@DependsOn(PythonStdLibConfigurationPass::class, softDependency = true)
class IniFileConfigurationPass(ctx: TranslationContext) : ConceptPass(ctx) {
    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        // Since we cannot directly depend on the ini frontend, we have to check the language here
        // based on the node's language.
        if (node.language.name.localName != "IniFileLanguage") {
            return
        }

        when (node) {
            is TranslationUnitDeclaration -> handleTranslationUnit(node)
            is RecordDeclaration -> handleRecordDeclaration(node, tu)
            is FieldDeclaration -> handleFieldDeclaration(node)
        }
    }

    private fun handleTranslationUnit(tu: TranslationUnitDeclaration): List<ProvideConfiguration>? {
        // Find all LoadConfigurationFile operations that match the INI file name
        val loadConfigOps =
            tu.translationResult?.operationNodes?.filterIsInstance<LoadConfiguration>()?.filter {
                it.fileExpression.stringValues?.contains(tu.name.toString()) == true
            }

        // And create a ProvideConfiguration node for each of them
        return loadConfigOps?.map { ProvideConfiguration(underlyingNode = tu, conf = it.conf) }
    }

    /**
     * Translates a [RecordDeclaration], which represents a section in an INI file, into a
     * [ProvideConfigurationGroup] node.
     */
    private fun handleRecordDeclaration(
        record: RecordDeclaration,
        tu: TranslationUnitDeclaration,
    ): List<ProvideConfigurationGroup> {
        return tu.operationNodes.filterIsInstance<ProvideConfiguration>().mapNotNull {
            val group = it.conf.groups.singleOrNull { it.name.localName == record.name.localName }
            if (group == null) {
                warnWithFileLocation(
                    record,
                    log,
                    "Could not find configuration group {}",
                    record.name.localName,
                )
                return@mapNotNull null
            }

            val op =
                ProvideConfigurationGroup(underlyingNode = record, conf = it.conf, group = group)

            // Add an incoming DFG edge to the group
            group.prevDFGEdges.add(record)

            op
        }
    }

    /**
     * Translates a [FieldDeclaration], which represents an option in an INI file, into a
     * [ProvideConfigurationOption] node.
     */
    private fun handleFieldDeclaration(field: FieldDeclaration): ProvideConfigurationOption? {
        val option =
            field.astParent
                ?.operationNodes
                ?.filterIsInstance<ProvideConfigurationGroup>()
                ?.singleOrNull()
                ?.group
                ?.options
                ?.singleOrNull { it.name.localName == field.name.localName }

        if (option == null) {
            warnWithFileLocation(
                field,
                log,
                "Could not find configuration option {}",
                field.name.localName,
            )
            return null
        }

        val op =
            ProvideConfigurationOption(
                    underlyingNode = field,
                    option = option,
                    value = field.initializer,
                )
                .also { it.name = field.name }

        // Add an incoming DFG edge to the option
        option.prevDFGEdges.add(field)

        return op
    }
}
