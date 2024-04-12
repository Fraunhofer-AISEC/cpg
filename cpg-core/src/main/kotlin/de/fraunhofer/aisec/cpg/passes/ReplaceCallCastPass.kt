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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HasFunctionalCasts
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.executionConfiguration.DependsOn
import de.fraunhofer.aisec.cpg.passes.executionConfiguration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.executionConfiguration.RequiresLanguageTrait

/**
 * If a [Language] has the trait [HasFunctionalCasts], we cannot distinguish between a
 * [CallExpression] and a [CastExpression] during the initial translation. This stems from the fact
 * that we might not know all the types yet. We therefore need to handle them as regular call
 * expression in a [LanguageFrontend] or [Handler] and then later replace them with a
 * [CastExpression], if the [CallExpression.callee] refers to name of a [Type] rather than a
 * function.
 */
@ExecuteBefore(EvaluationOrderGraphPass::class)
@DependsOn(TypeResolver::class)
@RequiresLanguageTrait(HasFunctionalCasts::class)
class ReplaceCallCastPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    private lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(tu: TranslationUnitDeclaration) {
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.registerHandler { _, parent, node ->
            when (node) {
                is CallExpression -> handleCall(node, parent)
            }
        }

        walker.iterate(tu)
    }

    private fun handleCall(call: CallExpression, parent: Node?) {
        // Make sure, we are not accidentally handling construct expressions (since they also derive
        // from call expressions)
        if (call is ConstructExpression) {
            return
        }

        // We need to check, whether the "callee" refers to a type and if yes, convert it into a
        // cast expression. And this is only really necessary, if the function call has a single
        // argument.
        var callee = call.callee
        if (parent != null && callee != null && call.arguments.size == 1) {
            val language = parent.language

            var pointer = false
            // If the argument is a UnaryOperator, unwrap them
            if (callee is UnaryOperator && callee.operatorCode == "*") {
                pointer = true
                callee = callee.input
            }

            // First, check if this is a built-in type
            if (language?.builtInTypes?.contains(callee.name.toString()) == true) {
                walker.replaceCallWithCast(callee.name.toString(), parent, call, false)
            } else {
                // If not, then this could still refer to an existing type. We need to make sure
                // that we take the current namespace into account
                val fqn =
                    if (callee.name.parent == null) {
                        scopeManager.currentNamespace.fqn(callee.name.localName)
                    } else {
                        callee.name
                    }

                if (typeManager.typeExists(fqn.toString())) {
                    walker.replaceCallWithCast(fqn, parent, call, pointer)
                }
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}

context(ContextProvider)
fun SubgraphWalker.ScopedWalker.replaceCallWithCast(
    typeName: CharSequence,
    parent: Node,
    call: CallExpression,
    pointer: Boolean,
) {
    val cast = newCastExpression()
    cast.code = call.code
    cast.language = call.language
    cast.location = call.location
    cast.castType =
        if (pointer) {
            call.objectType(typeName).pointer()
        } else {
            call.objectType(typeName)
        }
    cast.expression = call.arguments.single()
    cast.name = cast.castType.name

    if (parent !is ArgumentHolder) {
        Pass.log.error(
            "Parent AST node of call expression is not an argument holder. Cannot convert to cast expression. Further analysis might not be entirely accurate."
        )
        return
    }

    val success = parent.replaceArgument(call, cast)
    if (!success) {
        Pass.log.error(
            "Replacing call expression with cast expression was not successful. Further analysis might not be entirely accurate."
        )
    } else {
        call.disconnectFromGraph()

        // Make sure to inform the walker about our change
        this.registerReplacement(call, cast)
    }
}
