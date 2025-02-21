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
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.config.Configuration
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationGroup
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationOption
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

@DependsOn(ImportResolver::class)
class IniFileConfigurationPass(ctx: TranslationContext) : ConceptPass(ctx) {
    override fun handleNode(node: Node?, tu: TranslationUnitDeclaration) {
        if (node == null || node.language::class.simpleName != "IniFileLanguage") {
            return
        }

        when (node) {
            is RecordDeclaration -> handleRecordDeclaration(node, tu)
            is FieldDeclaration -> handleFieldDeclaration(node, tu)
        }
    }

    private fun handleFieldDeclaration(
        field: FieldDeclaration,
        tu: TranslationUnitDeclaration,
    ): ConfigurationOption? {
        val group =
            (field.astParent as? RecordDeclaration)
                ?.conceptNodes
                ?.filterIsInstance<ConfigurationGroup>()
                ?.singleOrNull()

        if (group == null) {
            return null
        }

        val option =
            ConfigurationOption(underlyingNode = field, group = group).also { it.name = field.name }

        // Add an incoming DFG edge to the option
        option.prevDFGEdges.add(field)

        return option
    }

    private fun handleRecordDeclaration(
        record: RecordDeclaration,
        tu: TranslationUnitDeclaration,
    ): ConfigurationGroup {
        val group =
            ConfigurationGroup(
                    underlyingNode = record,
                    conf = tu.getConceptOrCreate<Configuration>(),
                )
                .also { it.name = record.name }

        // Add an incoming DFG edge to the group
        group.prevDFGEdges.add(record)

        return group
    }
}
