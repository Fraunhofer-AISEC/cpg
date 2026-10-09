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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.*
import de.fraunhofer.aisec.cpg.graph.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent

/** The maximum number of entries of each ranked list in the [CpgOverview]. */
const val OVERVIEW_LIST_LIMIT = 25

fun Server.addOverview() {
    val toolDescription =
        """
        This tool gives a compact, fixed-size overview of the analyzed code. Call it first to get
        oriented before listing or inspecting individual nodes. It contains:
        - the number of functions, records and calls, and the analyzed components,
        - the files containing the most functions,
        - the most-called functions defined in the code,
        - the external functions called by the code, i.e., functions without a body in the graph,
          such as memcpy or recv in C.
        - candidate entry points, i.e., functions defined in the code which are never called.
        Each ranked list is limited to the top $OVERVIEW_LIST_LIMIT entries. The overview is plain text and names
        functions without node ids: look a function up by its name with cpg_list_functions (e.g.,
        with 'pattern', 'calls' or 'file') to get its id, then use cpg_get_node to dig deeper.

        Example prompts:
        - "What is this code about?"
        - "Give me an overview of the analyzed project"
        """
            .trimIndent()

    this.addTool<NoArguments>(name = "cpg_overview", description = toolDescription) {
        result: TranslationResult,
        _ ->
        CallToolResult(content = listOf(TextContent(result.overview().toText())))
    }
}

/** Computes the [CpgOverview] of this [TranslationResult]. */
fun TranslationResult.overview(): CpgOverview {
    val functions = functions.filter { !it.isImplicit }
    val (defined, declaredOnly) = functions.partition { it.body != null }
    val calls = calls

    // Calls that do not reach any function with a body, grouped by the name of the callee
    val externalCalls =
        calls
            .filter { call -> call.invokes.none { it.body != null } }
            .groupingBy { it.name.localName }
            .eachCount()

    return CpgOverview(
        components =
            components.map { ComponentOverview(it.name.toString(), it.translationUnits.size) },
        definedFunctions = defined.size,
        declaredOnlyFunctions = declaredOnly.size,
        records = records.size,
        calls = calls.size,
        filesWithMostFunctions =
            defined
                .groupingBy { it.location?.artifactLocation?.fileName ?: "<unknown>" }
                .eachCount()
                .toRanked(),
        mostCalledFunctions =
            defined
                .map { NamedCount(it.name.toString(), it.calledBy.size) }
                .filter { it.count > 0 }
                .sortedByDescending { it.count }
                .take(OVERVIEW_LIST_LIMIT),
        externalFunctionsCalled = externalCalls.toRanked(),
        entryPointCandidates =
            defined
                .filter { it.calledBy.isEmpty() }
                .map { NamedCount(it.name.toString(), it.calls.size) }
                .sortedByDescending { it.count }
                .take(OVERVIEW_LIST_LIMIT),
        entryPointCandidatesTotal = defined.count { it.calledBy.isEmpty() },
    )
}

/** Returns the top [OVERVIEW_LIST_LIMIT] entries of this map, ordered by their count. */
private fun Map<String, Int>.toRanked(): List<NamedCount> =
    entries
        .sortedByDescending { it.value }
        .take(OVERVIEW_LIST_LIMIT)
        .map { NamedCount(it.key, it.value) }

/**
 * The overview as compact text, which is cheaper for the model to read than JSON: no repeated keys,
 * and no node ids - a function is looked up by its name with the filters of `cpg_list_functions`.
 */
fun CpgOverview.toText(): String = buildString {
    appendLine(
        "Components: " +
            components.joinToString(", ") {
                "${it.name} (${it.translationUnits} translation units)"
            }
    )
    appendLine(
        "Functions: $definedFunctions defined in the code, $declaredOnlyFunctions declared only. " +
            "Records: $records. Calls: $calls."
    )
    section("Files with the most functions", filesWithMostFunctions)
    section("Most called functions defined in the code (calls to them)", mostCalledFunctions)
    section("External functions called (calls)", externalFunctionsCalled)
    section(
        "Entry point candidates: $entryPointCandidatesTotal defined functions are never called, " +
            "the top ones by the number of calls they make",
        entryPointCandidates,
    )
    append("Next: cpg_list_functions with pattern, file or calls; cpg_get_node with an id.")
}

private fun StringBuilder.section(title: String, entries: List<NamedCount>) {
    if (entries.isEmpty()) return
    appendLine("$title:")
    appendLine(entries.joinToString(" · ") { "${it.name} ${it.count}" })
}
