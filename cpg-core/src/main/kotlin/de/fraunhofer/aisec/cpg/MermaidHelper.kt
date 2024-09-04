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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.hardDependencies
import de.fraunhofer.aisec.cpg.passes.hardExecuteBefore
import de.fraunhofer.aisec.cpg.passes.isFirstPass
import de.fraunhofer.aisec.cpg.passes.isLastPass
import de.fraunhofer.aisec.cpg.passes.softDependencies
import de.fraunhofer.aisec.cpg.passes.softExecuteBefore
import kotlin.reflect.KClass

/** Helper to create a mermaid graph given a list of passes. */
object MermaidHelper {
    /** Helper function to replace the first and last passes names by their identifier. */
    private const val UNKNOWNPASS = "UnknownPass"
    private const val FIRSTPASSESSUBGRAPHIDENTIFIER = "FirstPassesSubgraph"
    private const val NORMALPASSESUBGRAPHIDENTIFIER = "NormalPassesSubgraph"
    private const val LASTPASSESSUBGRAPHIDENTIFIER = "LastPassesSubgraph"
    private const val FIRSTPASSIDENTIFIER = "FirstPass"
    private const val LASTPASSIDEINTIFIER = "LastPass"

    private fun mermaidPassName(pass: KClass<out Pass<*>>): String {
        return when {
            pass.isFirstPass -> FIRSTPASSIDENTIFIER
            pass.isLastPass -> LASTPASSIDEINTIFIER
            else -> pass.simpleName ?: UNKNOWNPASS
        }
    }
    /**
     * Builds a markdown representation of a pass dependency graph, based on
     * [Mermaid](https://mermaid.js.org) syntax.
     */
    fun buildMermaid(passes: List<KClass<out Pass<out Node>>>): String {
        var s = "```mermaid\n"
        s += "flowchart TD;\n"

        s += "    subgraph $FIRSTPASSESSUBGRAPHIDENTIFIER [\"First Passes\"];\n"
        passes
            .filter { it.isFirstPass }
            .forEach { s += "    $FIRSTPASSIDENTIFIER[\"${it.simpleName}\"];\n" }
        s += "    end;\n"
        s += "    subgraph $LASTPASSESSUBGRAPHIDENTIFIER [\"Last Passes\"];\n"
        passes
            .filter { it.isLastPass }
            .forEach { s += "        $LASTPASSIDEINTIFIER[\"${it.simpleName}\"];\n" }
        s += "    end;\n"

        s += "    $FIRSTPASSESSUBGRAPHIDENTIFIER~~~$NORMALPASSESUBGRAPHIDENTIFIER;\n"
        s += "    subgraph $NORMALPASSESUBGRAPHIDENTIFIER [\"Normal Passes\"];\n"
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
        s += "    $NORMALPASSESUBGRAPHIDENTIFIER~~~$LASTPASSESSUBGRAPHIDENTIFIER;\n"
        s += "```"
        return s
    }
}
