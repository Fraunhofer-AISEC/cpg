/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.implicit
import de.fraunhofer.aisec.cpg.graph.newConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.order.ExecuteBefore

/**
 * This [Pass] executes certain C++ specific conversions on initializers, that are only possible
 * once we know all the types. It may be extended in the future with other things that we currently
 * still do in the frontend, but might be more accurate to do once we parsed all files and have all
 * type information.
 */
@ExecuteBefore(EvaluationOrderGraphPass::class)
@DependsOn(TypeResolver::class)
class CXXExtraPass(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun accept(component: Component) {
        val walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)

        walker.registerHandler(::fixInitializers)
        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
    }

    protected fun fixInitializers(node: Node?) {
        if (node is VariableDeclaration) {
            // check if we have the corresponding class for this type
            val record = node.type.root.recordDeclaration
            val typeString = node.type.root.name
            if (record != null) {
                val currInitializer = node.initializer
                if (currInitializer == null && node.isImplicitInitializerAllowed) {
                    val initializer =
                        newConstructExpression(typeString).implicit(code = "$typeString()")
                    initializer.language = node.language
                    initializer.type = node.type
                    node.initializer = initializer
                    node.templateParameters?.let {
                        SymbolResolver.addImplicitTemplateParametersToCall(it, initializer)
                    }
                } else if (
                    currInitializer !is ConstructExpression &&
                        currInitializer is CallExpression &&
                        currInitializer.name.localName == node.type.root.name.localName
                ) {
                    // This should actually be a construct expression, not a call!
                    val arguments = currInitializer.arguments
                    val signature = arguments.map(Node::code).joinToString(", ")
                    val initializer =
                        newConstructExpression(typeString)
                            .implicit(code = "$typeString($signature)")
                    initializer.language = node.language
                    initializer.type = node.type
                    initializer.arguments = mutableListOf(*arguments.toTypedArray())
                    node.initializer = initializer
                    currInitializer.disconnectFromGraph()
                }
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
