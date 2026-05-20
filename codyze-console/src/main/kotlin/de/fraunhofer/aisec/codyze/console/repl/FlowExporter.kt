/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze.console.repl

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.SinglePathResult
import java.io.File
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Exports query results (typically a [QueryTree]) as a SARIF 2.1 document with `codeFlows`
 * populated, then asks the OS to open the file with its default `.sarif` handler — usually the VS
 * Code SARIF Viewer extension, which renders each flow as a clickable step-through panel.
 *
 * The whole point: the terminal can only express "open this single file at this line" via OSC 8
 * hyperlinks. To show *a path* through several locations as a connected sequence, we need a richer
 * renderer. SARIF + the SARIF viewer is the lightest-weight way to get there without shipping our
 * own VS Code extension.
 *
 * The SARIF document we emit is intentionally minimal — just enough structure for the viewer to
 * discover the flow:
 * * one [run](https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317484)
 *   with a fake `tool.driver.name = "Codyze REPL"`
 * * one [result](https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317638)
 *   per extracted flow, each with a `codeFlows[0].threadFlows[0].locations` array
 * * each location keys off the [Node]'s
 *   [PhysicalLocation][de.fraunhofer.aisec.cpg.sarif.PhysicalLocation]
 */
object FlowExporter {

    /**
     * Extracts node-paths from [value] (typically a [QueryTree]), writes them as SARIF to a temp
     * file, and returns the file. Returns null if no paths could be extracted.
     */
    fun export(value: Any?): File? {
        val flows = extractFlows(value).filter { it.isNotEmpty() }
        if (flows.isEmpty()) return null

        val sarif = buildSarif(flows)
        val file =
            File(
                System.getProperty("java.io.tmpdir"),
                "codyze-flow-${System.currentTimeMillis()}.sarif",
            )
        file.writeText(sarif)
        return file
    }

    /**
     * Opens [file]. When VS Code is detected (same heuristic as the OSC 8 hyperlinks — see
     * [LinkScheme.detect]) we route through the `vscode://file/<path>` URL so the file lands inside
     * VS Code regardless of what the user's OS-level handler for `.sarif` is. Otherwise we fall
     * back to the OS default opener (`open` on macOS, `xdg-open` on Linux, `cmd /c start` on
     * Windows). Best-effort — failure is silent.
     */
    fun openInOs(file: File) {
        val osOpen =
            when {
                System.getProperty("os.name").lowercase().contains("mac") -> "open"
                System.getProperty("os.name").lowercase().contains("windows") -> null
                else -> "xdg-open"
            }
        val target =
            when (LinkScheme.detect()) {
                LinkScheme.VSCODE -> "vscode://file${file.absolutePath}"
                else -> file.absolutePath
            }
        val cmd =
            if (osOpen != null) {
                arrayOf(osOpen, target)
            } else {
                // Windows fallback — `start` needs an empty title arg first.
                arrayOf("cmd", "/c", "start", "", target)
            }
        runCatching { ProcessBuilder(*cmd).inheritIO().start() }
    }

    /**
     * Pulls node-paths out of a [QueryTree] structure. Mirrors the `getCodeflow()` logic in
     * codyze-core but is inlined here to avoid a module dependency just for one ten-line walk.
     *
     * The tree is heterogeneous: leaves are either lists of nodes (a path) or other trees that
     * recurse. We walk depth-first and collect every list-of-Node leaf we hit.
     */
    private fun extractFlows(value: Any?): List<List<Node>> =
        when (value) {
            null -> emptyList()
            is QueryTree<*> -> extractFlowsFromTree(value)
            is List<*> -> {
                if (value.all { it is Node }) {
                    @Suppress("UNCHECKED_CAST") listOf(value as List<Node>)
                } else {
                    value.flatMap { extractFlows(it) }
                }
            }
            else -> emptyList()
        }

    private fun extractFlowsFromTree(tree: QueryTree<*>): List<List<Node>> {
        if (tree is SinglePathResult) return tree.children.flatMap { extractFlowsFromTree(it) }
        val v = tree.value
        if (v is List<*> && v.all { it is Node } && v.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return listOf(v as List<Node>)
        }
        return tree.children.flatMap { extractFlowsFromTree(it) }
    }

    private fun buildSarif(flows: List<List<Node>>): String {
        val root = buildJsonObject {
            put("\$schema", "https://json.schemastore.org/sarif-2.1.0.json")
            put("version", "2.1.0")
            putJsonArray("runs") {
                addJsonObject {
                    putJsonObject("tool") {
                        putJsonObject("driver") {
                            put("name", "Codyze REPL")
                            put("informationUri", "https://www.codyze.io/")
                        }
                    }
                    putJsonArray("results") {
                        flows.forEach { flow -> addJsonObject { result(flow) } }
                    }
                }
            }
        }
        return root.toString()
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.result(flow: List<Node>) {
        put("ruleId", "CodyzeFlow")
        put("level", "note")
        putJsonObject("message") {
            val start = flow.first().name.localName.ifEmpty { flow.first().javaClass.simpleName }
            val end = flow.last().name.localName.ifEmpty { flow.last().javaClass.simpleName }
            put("text", "Flow from $start to $end (${flow.size} steps)")
        }
        // Primary location = endpoint of the flow (where the viewer focuses first).
        putJsonArray("locations") { addJsonObject { location(flow.last()) } }
        putJsonArray("codeFlows") {
            addJsonObject {
                putJsonArray("threadFlows") {
                    addJsonObject {
                        putJsonArray("locations") {
                            flow.forEachIndexed { idx, node ->
                                addJsonObject {
                                    putJsonObject("location") {
                                        location(node)
                                        putJsonObject("message") {
                                            val name = node.name.localName.ifEmpty { "" }
                                            val kind = node.javaClass.simpleName
                                            put(
                                                "text",
                                                if (name.isEmpty()) "step ${idx + 1}: $kind"
                                                else "step ${idx + 1}: $kind '$name'",
                                            )
                                        }
                                    }
                                    put("executionOrder", idx + 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.location(node: Node) {
        val loc = node.location
        val uri = loc?.artifactLocation?.uri?.toString() ?: return
        val region = loc.region
        putJsonObject("physicalLocation") {
            putJsonObject("artifactLocation") { put("uri", uri) }
            putJsonObject("region") {
                put("startLine", region.startLine.coerceAtLeast(1))
                if (region.startColumn > 0) put("startColumn", region.startColumn)
                if (region.endLine > 0) put("endLine", region.endLine)
                if (region.endColumn > 0) put("endColumn", region.endColumn)
            }
        }
    }
}
