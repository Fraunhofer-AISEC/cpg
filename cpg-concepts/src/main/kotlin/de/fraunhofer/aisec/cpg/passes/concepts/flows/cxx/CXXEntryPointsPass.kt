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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.passes.concepts.flows.cxx

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.concepts.arch.POSIX
import de.fraunhofer.aisec.cpg.graph.concepts.arch.Win32
import de.fraunhofer.aisec.cpg.graph.concepts.flows.EntryPoint
import de.fraunhofer.aisec.cpg.graph.concepts.flows.newLibraryEntryPoint
import de.fraunhofer.aisec.cpg.graph.concepts.flows.newMain
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.translationUnit
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.Produces
import de.fraunhofer.aisec.cpg.passes.concepts.RequiresLanguage
import de.fraunhofer.aisec.cpg.passes.concepts.getConceptOrCreate

/** A pass that fills the [EntryPoint] concept into the CPG. */
class CXXEntryPointsPass(ctx: TranslationContext) : ConceptPass(ctx) {

    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        when (node) {
            is FunctionDeclaration -> handleFunctionDeclaration(node)
        }
    }

    /** Translates a suitable [FunctionDeclaration] into an [EntryPoint] concept. */
    private fun handleFunctionDeclaration(func: FunctionDeclaration) {
        func.addEntryPoints()
    }
}

/**
 * Adds the [EntryPoint] concept to the [FunctionDeclaration] if it is a main function or a library
 * entry point.
 */
context(ContextProvider)
@Produces(EntryPoint::class)
@RequiresLanguage("CPPLanguage")
fun FunctionDeclaration.addEntryPoints() {
    val tu = translationUnit ?: throw TranslationException("No translation unit found")
    val component = component ?: throw TranslationException("No component found")

    val entry =
        when (name.toString()) {
            "main" -> newMain(underlyingNode = this, os = tu.getConceptOrCreate<POSIX>())
            "WinMain" -> newMain(underlyingNode = this, os = tu.getConceptOrCreate<Win32>())
            "DllMain" ->
                newLibraryEntryPoint(underlyingNode = this, os = tu.getConceptOrCreate<Win32>())
            else -> return
        }

    component.incomingInteractions += entry
}
