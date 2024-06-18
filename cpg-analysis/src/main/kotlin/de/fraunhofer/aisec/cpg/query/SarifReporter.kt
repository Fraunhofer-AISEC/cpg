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

    override fun report(rules: Collection<Rule>, minify: Boolean): String {
        // TODO: consider validation of rule fields
        // TODO: maybe work more with indices to reduce filesize at the cost of readability
        //  in particular locations and results

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
                                                "AISEC cpg-console", // TODO: mby dont hardcode, at
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
                            // TODO: automationDetails, invocation?
                            results = createResults(rules)
                        )
                    )
            )
        return if (minify) SarifSerializer.toMinifiedJson(sarifObj)
        else SarifSerializer.toJson(sarifObj)
    }

    private fun createResults(rules: Collection<Rule>): List<Result>? {

        val results = mutableListOf<Result>()
        for ((i, rule) in rules.withIndex()) {
            results.addAll(createResultsExtended(rule, i.toLong()))
        }

        return results
    }

    /*
        private fun createResultsRegular(rule: Rule, idx: Long): List<Result> {
            val results = mutableListOf<Result>()
            if (rule.queryResult?.second?.second != null) {
                for (node in rule.queryResult?.second?.second!!) { // non-null
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
                            locations =
                            listOf(
                                Location(
                                    physicalLocation =
                                    if (node is CpgGraphNode) {
                                        PhysicalLocation(
                                            artifactLocation =
                                            ArtifactLocation(
                                                uri =
                                                node.location
                                                    ?.artifactLocation
                                                    ?.uri
                                                    .toString()
                                                //         ?.let { uri ->
                                                //
                                                // pwd.relativize(Paths.get(uri))
                                                //                 .toString()
                                                //         },
                                                // uriBaseID = pwd.toString()
                                            ),
                                            region =
                                            run {
                                                val region = node.location?.region
                                                Region(
                                                    startLine = region?.startLine?.toLong(),
                                                    endLine = region?.endLine?.toLong(),
                                                    startColumn =
                                                    region?.startColumn?.toLong(),
                                                    endColumn = region?.endColumn?.toLong()
                                                )
                                            }
                                        )
                                    } else null
                                )
                            )
                        )
                    )
                }
            }
            return results
        }
    */

    private fun createResultsExtended(rule: Rule, idx: Long): List<Result> {
        val results = mutableListOf<Result>()
        for (node in rule.queryResult?.children ?: emptyList()) {
            val threadFlowLocations = createThreadFlowLocations(node)
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
                    // TODO: wonky
                    locations =
                        listOf(
                            Location(
                                physicalLocation =
                                    PhysicalLocation(
                                        artifactLocation =
                                            ArtifactLocation(
                                                uri =
                                                    threadFlowLocations[0]
                                                        .location
                                                        ?.physicalLocation
                                                        ?.artifactLocation
                                                        ?.uri
                                                // TODO no baseid rn
                                                //         ?.let { uri ->
                                                //             pwd.relativize(
                                                //
                                                // Paths.get(uri).toAbsolutePath()
                                                //                 )
                                                //                 .toString()
                                                //         },
                                                // uriBaseID = pwd.toString()
                                            ),
                                    )
                            )
                        ),
                    codeFlows =
                        listOf(
                            CodeFlow(
                                threadFlows = listOf(ThreadFlow(locations = threadFlowLocations))
                            )
                        )
                )
            )
        }
        return results
    }

    private fun createThreadFlowLocations(root: QueryTree<*>): MutableList<ThreadFlowLocation> {
        var firstDepth: Long = -1
        var nodeValueLocation: de.fraunhofer.aisec.cpg.sarif.PhysicalLocation? = null
        val threadFlowLocations = mutableListOf<ThreadFlowLocation>()

        root.inOrder({ (node, depth): Pair<QueryTree<*>, Long> ->
            if (node.value is CpgGraphNode) {
                nodeValueLocation = (node.value as CpgGraphNode).location
                threadFlowLocations.add(
                    ThreadFlowLocation(
                        location =
                            Location(
                                // message = Message(text = node.stringRepresentation),
                                physicalLocation =
                                    PhysicalLocation(
                                        artifactLocation =
                                            ArtifactLocation(
                                                uri =
                                                    nodeValueLocation
                                                        ?.artifactLocation
                                                        ?.uri
                                                        .toString()
                                                // TODO no baseid rn
                                                //         ?.let {
                                                //         pwd.relativize(Paths.get(it)).toString()
                                                //     },
                                                // uriBaseID = pwd.toString()
                                            ),
                                        region =
                                            Region(
                                                startLine =
                                                    nodeValueLocation?.region?.startLine?.toLong(),
                                                endLine =
                                                    nodeValueLocation?.region?.endLine?.toLong(),
                                                startColumn =
                                                    nodeValueLocation
                                                        ?.region
                                                        ?.startColumn
                                                        ?.toLong(),
                                                endColumn =
                                                    nodeValueLocation?.region?.endColumn?.toLong()
                                            )
                                    )
                            ),
                        nestingLevel =
                            if (firstDepth == -1L) {
                                firstDepth = depth
                                0
                            } else depth - firstDepth
                        // TODO: state (consider?)
                    )
                )
            }
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
