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
package de.fraunhofer.aisec.cpg.passes.concepts.memory

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.memory.DynamicLoading
import de.fraunhofer.aisec.cpg.graph.concepts.memory.DynamicLoadingOperation
import de.fraunhofer.aisec.cpg.graph.concepts.memory.LoadFunction
import de.fraunhofer.aisec.cpg.graph.concepts.memory.LoadLibrary
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.followPrevDFG
import de.fraunhofer.aisec.cpg.graph.operationNodes
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.translationResult
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DynamicInvokeResolver
import de.fraunhofer.aisec.cpg.passes.TranslationUnitPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

/** A pass that fills the [DynamicLoading] concept into the CPG. */
@DependsOn(ControlFlowSensitiveDFGPass::class)
@ExecuteBefore(DynamicInvokeResolver::class)
class CXXDynamicLoadingPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(tu: TranslationUnitDeclaration) {
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.registerHandler { _, _, node ->
            when (node) {
                is CallExpression -> handleCallExpression(node, tu)
            }
        }

        walker.iterate(tu)
    }

    /** Handles a [CallExpression] node and checks if it is a dynamic loading operation. */
    @Suppress("UNCHECKED_CAST")
    private fun handleCallExpression(call: CallExpression, tu: TranslationUnitDeclaration) {
        val concept = getConceptOrCreate<DynamicLoading>(tu)

        val op =
            when (call.name.toString()) {
                "dlopen" -> {
                    handleLibraryLoad(call, concept)
                }
                "dlsym" -> {
                    handleLoadFunction(call, concept)
                }
                else -> {
                    return
                }
            }

        concept.ops += op
    }

    /**
     * This function handles the loading of a function. It creates a [LoadFunction] concept and adds
     * it to the [DynamicLoading] concept. The tricky part is to find the [FunctionDeclaration] that
     * is loaded.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleLoadFunction(call: CallExpression, concept: DynamicLoading): LoadFunction {
        // The first argument is the handle to the library. We can follow the DFG back to find the
        // call to dlopen.
        val path =
            call.arguments.getOrNull(0)?.followPrevDFG {
                it is CallExpression && it.operationNodes.any { it is LoadLibrary }
            }

        val loadLibrary =
            path?.lastOrNull()?.operationNodes?.filterIsInstance<LoadLibrary>()?.singleOrNull()

        val symbolName = call.arguments.getOrNull(1)?.evaluate() as? String
        val candidates = loadLibrary?.findSymbol(symbolName)

        val op =
            LoadFunction(
                underlyingNode = call,
                concept = concept as Concept<DynamicLoadingOperation<FunctionDeclaration>>,
                what = candidates?.singleOrNull() as FunctionDeclaration?,
                loader = loadLibrary,
            )

        // We can help the dynamic invoke resolver by adding a DFG path from the function
        // declaration to the assignment of our call (which is the next DFG)
        call.nextFullDFG.forEach { assignee ->
            candidates?.forEach { candidate -> assignee.prevDFG += candidate }
        }

        return op
    }

    /**
     * This function handles the loading of a library. It creates a [LoadLibrary] concept and adds
     * it to the [DynamicLoading] concept. The tricky part is to find the [Component] that
     * represents the [LoadLibrary.what].
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleLibraryLoad(call: CallExpression, concept: DynamicLoading): LoadLibrary {
        // The first argument of dlopen is the path to the library. We can try to evaluate the
        // argument to check if it's a constant string.
        val path = call.arguments.getOrNull(0)?.evaluate() as? String

        // We can check, whether we have a matching component based on the base filename
        val component =
            path?.let {
                call.translationResult?.findComponentForLibrary(
                    Path(it).fileName.nameWithoutExtension.toString()
                )
            }

        // Create the op
        val op =
            LoadLibrary(
                underlyingNode = call,
                concept = concept as Concept<DynamicLoadingOperation<Component>>,
                what = component,
            )

        return op
    }

    fun TranslationResult.findComponentForLibrary(libraryName: String): Component? {
        return this.components.find { it.name.localName == libraryName }
    }

    /**
     * Gets the [DynamicLoading] concept for this [TranslationUnitDeclaration] or creates a new one
     * if it does not exist.
     */
    private inline fun <reified T : Concept<*>> getConceptOrCreate(
        tu: TranslationUnitDeclaration
    ): T {
        var concept = tu.conceptNodes.filterIsInstance<T>().singleOrNull()
        if (concept == null) {
            concept = T::class.constructors.first().call(tu)
        }

        return concept
    }

    override fun cleanup() {
        // Nothing to do
    }
}
