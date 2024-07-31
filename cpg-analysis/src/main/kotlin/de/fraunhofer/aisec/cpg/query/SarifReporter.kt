/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.graph.Node as CpgGraphNode
import io.github.detekt.sarif4k.*
import java.nio.file.Paths

class SarifReporter : Reporter {
    private val pwd = Paths.get("").toAbsolutePath()

    /**
     * Generates a SARIF report from a collection of rules. The Rule must have a query result set by
     * calling [Rule.run] and will not be run by this method.
     *
     * @param rules the [Rule]s to generate the report for
     * @param minify if true, the output json will be minified to reduce file size
     */
    override fun report(rules: Collection<Rule>, minify: Boolean): String {
        // TODO: consider validation of rule fields
        val sarifObj =
            SarifSchema210(
                schema =
                    "https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json",
                version = Version.The210,
                runs =
                    listOf(
                        Run(
                            tool =
                                Tool(
                                    driver =
                                        ToolComponent(
                                            name =
                                                "AISEC cpg-runner", // TODO: mby dont hardcode, at
                                            // least not here
                                            informationURI =
                                                "https://github.com/Fraunhofer-AISEC/cpg/",
                                            rules =
                                                rules.map { rule ->
                                                    ReportingDescriptor(
                                                        id = rule.id,
                                                        name = rule.name,
                                                        shortDescription =
                                                            MultiformatMessageString(
                                                                text = rule.shortDescription,
                                                                markdown = rule.mdShortDescription
                                                            ),
                                                        defaultConfiguration =
                                                            ReportingConfiguration(
                                                                level = mapLevel(rule.level)
                                                            )
                                                        // TODO: consider default message
                                                    )
                                                }
                                        )
                                ),
                            externalPropertyFileReferences =
                                ExternalPropertyFileReferences(
                                    taxonomies =
                                        listOf(
                                            ExternalPropertyFileReference(
                                                location =
                                                    ArtifactLocation(
                                                        // TODO mby dont hardcode?
                                                        uri =
                                                            "https://raw.githubusercontent.com/sarif-standard/taxonomies/main/CWE_v4.4.sarif"
                                                    )
                                            )
                                        )
                                ),
                            // TODO: automationDetails, invocation
                            //  automationDetails is definitely possible if used with the
                            //  [RuleRunner]
                            results = createResults(rules)
                        )
                    )
            )
        return if (minify) SarifSerializer.toMinifiedJson(sarifObj)
        else SarifSerializer.toJson(sarifObj)
    }

    private fun createResults(rules: Collection<Rule>): List<Result> {

        val results = mutableListOf<Result>()
        for ((i, rule) in rules.withIndex()) {
            results.addAll(results(rule, i.toLong()))
        }

        return results
    }

    private fun results(rule: Rule, idx: Long): List<Result> {
        val results = mutableListOf<Result>()
        for (node in rule.queryResult?.children ?: emptyList()) {
            val threadFlowLocations = threadFlows(node)
            results.add(
                Result(
                    ruleID = rule.id,
                    ruleIndex = idx,
                    message =
                        Message(
                            text = rule.message,
                            markdown = rule.mdMessage,
                            arguments = rule.messageArguments
                        ),
                    taxa =
                        listOf(
                            ReportingDescriptorReference(
                                id = if (rule.cweId != null) "CWE-${rule.cweId}" else null
                            )
                        ),
                    locations = locations(threadFlowLocations),
                    codeFlows = codeFlows(threadFlowLocations)
                )
            )
        }
        return results
    }

    private fun codeFlows(threadFlowLocations: MutableList<ThreadFlowLocation>) =
        if (threadFlowLocations.isEmpty()) null
        else listOf(CodeFlow(threadFlows = listOf(ThreadFlow(locations = threadFlowLocations))))

    private fun locations(threadFlowLocations: MutableList<ThreadFlowLocation>) =
        listOf(
            Location(
                physicalLocation =
                    PhysicalLocation(
                        artifactLocation =
                            ArtifactLocation(
                                // TODO: Hacky but idk a better way
                                uri =
                                    threadFlowLocations
                                        .getOrNull(0)
                                        ?.location
                                        ?.physicalLocation
                                        ?.artifactLocation
                                        ?.uri
                                // TODO: no baseId rn even though the spec suggests its use bcs of
                                // editor extension support
                            ),
                    )
            )
        )

    private fun threadFlows(root: QueryTree<*>): MutableList<ThreadFlowLocation> {
        var initDepth: Long = -1
        var nodeValueLocation: de.fraunhofer.aisec.cpg.sarif.PhysicalLocation? = null
        val threadFlowLocations = mutableListOf<ThreadFlowLocation>()

        root.inOrder({ (node, depth): Pair<QueryTree<*>, Long> ->
            if (node.value is CpgGraphNode) {
                nodeValueLocation = (node.value as CpgGraphNode).location
                if (nodeValueLocation != null) {
                    threadFlowLocations.add(
                        ThreadFlowLocation(
                            location =
                                Location(
                                    physicalLocation =
                                        PhysicalLocation(
                                            artifactLocation =
                                                ArtifactLocation(
                                                    uri =
                                                        nodeValueLocation
                                                            ?.artifactLocation
                                                            ?.uri
                                                            .toString()
                                                    // TODO no baseId
                                                ),
                                            region =
                                                Region(
                                                    startLine =
                                                        nodeValueLocation
                                                            ?.region
                                                            ?.startLine
                                                            ?.toLong(),
                                                    endLine =
                                                        nodeValueLocation
                                                            ?.region
                                                            ?.endLine
                                                            ?.toLong(),
                                                    startColumn =
                                                        nodeValueLocation
                                                            ?.region
                                                            ?.startColumn
                                                            ?.toLong(),
                                                    endColumn =
                                                        nodeValueLocation
                                                            ?.region
                                                            ?.endColumn
                                                            ?.toLong()
                                                )
                                        )
                                ),
                            nestingLevel =
                                if (initDepth == -1L) {
                                    initDepth = depth
                                    0
                                } else {
                                    depth - initDepth
                                }
                            // TODO: state (consider?)
                        )
                    )
                }
            } // else return empty list
        })
        return threadFlowLocations
    }

    private fun QueryTree<*>.inOrder(action: (Pair<QueryTree<*>, Long>) -> Unit, depth: Long = 0) {
        action(Pair(this, depth))
        children.forEach { it.inOrder(action, depth + 1) }
    }

    override fun mapLevel(level: Rule.Level): Level {
        return when (level) {
            Rule.Level.Error -> Level.Error
            Rule.Level.Warning -> Level.Warning
            Rule.Level.Note -> Level.Note
            else -> Level.None
        }
    }

    // dont override toFile

    // don't override getDefaultPath
}
