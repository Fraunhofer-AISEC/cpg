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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.hardDependencies
import de.fraunhofer.aisec.cpg.passes.hardExecuteBefore
import de.fraunhofer.aisec.cpg.passes.isFirstPass
import de.fraunhofer.aisec.cpg.passes.isLastPass
import de.fraunhofer.aisec.cpg.passes.softDependencies
import de.fraunhofer.aisec.cpg.passes.softExecuteBefore
import kotlin.reflect.KClass

private const val UNKNOWN_PASS = "UnknownPass"
private const val FIRST_PASSES_SUBGRAPH_IDENTIFIER = "FirstPassesSubgraph"
private const val NORMAL_PASSES_SUBGRAPH_IDENTIFIER = "NormalPassesSubgraph"
private const val LAST_PASSES_SUBGRAPH_IDENTIFIER = "LastPassesSubgraph"
private const val FIRST_PASS_IDENTIFIER = "FirstPass"
private const val LAST_PASS_IDENTIFIER = "LastPass"

/** Helper function to replace the first and last passes names by their identifier. */
private fun mermaidPassName(pass: KClass<out Pass<*>>): String {
    return when {
        pass.isFirstPass -> FIRST_PASS_IDENTIFIER
        pass.isLastPass -> LAST_PASS_IDENTIFIER
        else -> pass.simpleName ?: UNKNOWN_PASS
    }
}

/**
 * Builds a markdown representation of a pass dependency graph, based on
 * [Mermaid](https://mermaid.js.org) syntax.
 */
internal fun buildMermaid(passes: List<KClass<out Pass<out AstNode>>>): String {
    var s = "```mermaid\n"
    s += "flowchart TD;\n"

    s += "    subgraph $FIRST_PASSES_SUBGRAPH_IDENTIFIER [\"First Passes\"];\n"
    passes
        .filter { it.isFirstPass }
        .forEach { s += "    $FIRST_PASS_IDENTIFIER[\"${it.simpleName}\"];\n" }
    s += "    end;\n"
    s += "    subgraph $LAST_PASSES_SUBGRAPH_IDENTIFIER [\"Last Passes\"];\n"
    passes
        .filter { it.isLastPass }
        .forEach { s += "        $LAST_PASS_IDENTIFIER[\"${it.simpleName}\"];\n" }
    s += "    end;\n"

    s += "    $FIRST_PASSES_SUBGRAPH_IDENTIFIER~~~$NORMAL_PASSES_SUBGRAPH_IDENTIFIER;\n"
    s += "    subgraph $NORMAL_PASSES_SUBGRAPH_IDENTIFIER [\"Normal Passes\"];\n"
    for ((pass, deps) in passes.associateWith { it.softDependencies }.entries) {
        for (dep in deps) {
            s += "        ${mermaidPassName(dep)}-.->${mermaidPassName(pass)};\n"
        }
    }
    for ((pass, deps) in passes.associateWith { it.hardDependencies }.entries) {
        for (dep in deps) {
            s += "        ${mermaidPassName(dep)}-->${mermaidPassName(pass)};\n"
        }
    }
    for ((pass, before) in passes.associateWith { it.softExecuteBefore }.entries) {
        for (execBefore in before) {
            s += "        ${mermaidPassName(pass)}-.->${mermaidPassName(execBefore)};\n"
        }
    }
    for ((pass, beforeList) in passes.associateWith { it.hardExecuteBefore }.entries) {
        for (execBefore in beforeList) {
            s += "        ${mermaidPassName(pass)}-->${mermaidPassName(execBefore)};\n"
        }
    }
    s += "    end;\n"
    s += "    $NORMAL_PASSES_SUBGRAPH_IDENTIFIER~~~$LAST_PASSES_SUBGRAPH_IDENTIFIER;\n"
    s += "```"
    return s
}
