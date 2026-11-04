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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.frontends.FrontendConfiguration
import de.fraunhofer.aisec.cpg.graph.FrontendProvider
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method

/**
 * @param packagesToIgnore Fully-qualified package prefixes whose method bodies should not be
 *   parsed.
 * @param useJimpleTextPositions When true, every class is round-tripped through its textual Jimple
 *   representation (see `sootup.jimple.frontend.JimpleTextPositions`) before translation, so that
 *   statements and values receive per-line, column-precise positions into the reprinted Jimple text
 *   instead of the coarse, frequently collapsed line numbers a compiled artifact (`.class`/dex)
 *   carries. The reprinted text is written to a `.jimple` file (under a temporary directory) and
 *   used as the location's file name so that "the node on line N" resolves to a real, readable
 *   line.
 *
 *   This trades the original source file name (e.g. `MainActivity.kt`, only recoverable from dex
 *   debug info) for coherent per-node positions, which is why it is opt-in and defaults to false.
 */
class JVMFrontendConfiguration(
    val packagesToIgnore: List<String> = listOf(),
    val useJimpleTextPositions: Boolean = true,
) : FrontendConfiguration<JVMLanguageFrontend>() {
    /**
     * Determines whether the body of a function should NOT be parsed.
     *
     * @param provider A provider for a [JVMLanguageFrontend]
     * @param node The function declaration to check
     * @return true if the function's package matches any package in [packagesToIgnore] (skip
     *   parsing), false otherwise (parse the body)
     */
    context(provider: FrontendProvider<JVMLanguageFrontend>)
    override fun doNotParseBody(node: Function): Boolean {
        return this.packagesToIgnore.any {
            (node as? Method)?.recordDeclaration?.name.toString().startsWith(it)
        }
    }
}
