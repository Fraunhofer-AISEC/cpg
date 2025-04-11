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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.passes.concepts.flows.cxx.addEntryPoints
import de.fraunhofer.aisec.cpg.passes.concepts.memory.cxx.addDynamicLoading
import kotlin.reflect.KClass
import kotlin.reflect.KFunction2
import kotlin.reflect.full.findAnnotation

class FunctionBasedConceptPass(ctx: TranslationContext) : ConceptPass(ctx) {

    val handlerMap =
        mutableMapOf<KClass<out Node>, MutableList<KFunction2<ContextProvider, *, Unit>>>()

    init {
        register(FunctionDeclaration::addEntryPoints)
        register(CallExpression::addDynamicLoading)
    }

    inline fun <reified T : Node> register(noinline handler: KFunction2<ContextProvider, T, Unit>) {
        val handlers = handlerMap.computeIfAbsent(T::class) { mutableListOf() }

        handlers += handler
    }

    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        // Look for the handlers for the node type we are handling
        // TODO: This is a bit inefficient, we should probably cache the handlers in a better way
        @Suppress("UNCHECKED_CAST")
        val handlers =
            handlerMap.filterKeys { it.isInstance(node) }.values.flatten()
                as List<KFunction2<ContextProvider, Node, Unit>>

        // Call each handler
        for (handler in handlers) {
            // Check if the handler is of the expected language
            val requiresLanguage = handler.findAnnotation<RequiresLanguage>()
            if (
                requiresLanguage != null &&
                    requiresLanguage.language != node.language.name.localName
            ) {
                continue
            }

            // Cast the node to the expected type and call the handler
            handler(this, node)
        }
    }
}
