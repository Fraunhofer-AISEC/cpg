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
import de.fraunhofer.aisec.cpg.graph.ast.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.config.*
import de.fraunhofer.aisec.cpg.helpers.Util.warnWithFileLocation
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.ProvideConfigPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import kotlin.collections.singleOrNull

/**
 * This pass is responsible for creating [ConfigurationSource] nodes based on the INI file frontend.
 */
@DependsOn(ImportResolver::class)
@ExecuteBefore(ProvideConfigPass::class)
class IniFileConfigurationSourcePass(ctx: TranslationContext) : ConceptPass(ctx) {
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

    private fun handleTranslationUnit(tu: TranslationUnitDeclaration): ConfigurationSource {
        return newConfigurationSource(underlyingNode = tu, connect = true).also {
            it.name = tu.name
        }
    }

    /**
     * Translates a [RecordDeclaration], which represents a section in an INI file, into a
     * [ConfigurationGroupSource] node.
     */
    private fun handleRecordDeclaration(
        record: RecordDeclaration,
        tu: TranslationUnitDeclaration,
    ): ConfigurationGroupSource? {
        val conf = tu.conceptNodes.filterIsInstance<ConfigurationSource>().singleOrNull()
        if (conf == null) {
            warnWithFileLocation(
                conf,
                log,
                "Could not find configuration for {}",
                record.name.localName,
            )
            return null
        }

        val group =
            newConfigurationGroupSource(underlyingNode = record, concept = conf, connect = true)
                .also { it.name = record.name }

        // Add an incoming DFG edge from the record
        group.prevDFGEdges.add(record)

        return group
    }

    /**
     * Translates a [FieldDeclaration], which represents an option in an INI file, into a
     * [ConfigurationOptionSource] node.
     */
    private fun handleFieldDeclaration(field: FieldDeclaration): ConfigurationOptionSource? {
        val group =
            field.astParent
                ?.conceptNodes
                ?.filterIsInstance<ConfigurationGroupSource>()
                ?.singleOrNull()

        if (group == null) {
            warnWithFileLocation(
                field,
                log,
                "Could not find configuration group for {}",
                field.name.localName,
            )
            return null
        }

        val option =
            newConfigurationOptionSource(underlyingNode = field, concept = group, connect = true)
                .also { it.name = field.name }

        // Add an incoming DFG edge from the field
        option.prevDFGEdges.add(field)

        return option
    }
}
