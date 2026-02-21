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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HasCallExpressionAmbiguity
import de.fraunhofer.aisec.cpg.frontends.HasFunctionStyleCasts
import de.fraunhofer.aisec.cpg.frontends.HasFunctionStyleConstruction
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.replace
import de.fraunhofer.aisec.cpg.helpers.toConstructExpression
import de.fraunhofer.aisec.cpg.nameIsType
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiresLanguageTrait
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * If a [Language] has the trait [HasCallExpressionAmbiguity], we cannot distinguish between
 * [CallExpression], [CastExpression] or [ConstructExpression] during the initial translation. This
 * stems from the fact that we might not know all the types yet. We therefore need to handle them as
 * regular call expression in a [LanguageFrontend] or [Handler] and then later replace them with a
 * [CastExpression] or [ConstructExpression], if the [CallExpression.callee] refers to name of a
 * [Type] / [Record] rather than a function.
 */
@ExecuteBefore(EvaluationOrderGraphPass::class)
@DependsOn(TypeResolver::class)
@RequiresLanguageTrait(HasCallExpressionAmbiguity::class)
@Description(
    "Tries to identify and resolve ambiguous CallExpressions that could also be CastExpressions or ConstructExpressions. The initial translation cannot distinguish between these expression types in some languages (having the trait HasCallExpressionAmbiguity) in the CPG and try to fix these issues by this pass."
)
class ResolveCallExpressionAmbiguityPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    private lateinit var walker: SubgraphWalker.ScopedWalker<AstNode>

    override fun accept(tu: TranslationUnit) {
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager, Strategy::AST_FORWARD)
        walker.registerHandler { node ->
            when (node) {
                is CallExpression -> handleCall(node)
            }
        }

        walker.iterate(tu)
    }

    private fun handleCall(call: CallExpression) {
        val parent = call.astParent

        // Make sure, we are not accidentally handling construct expressions (since they also derive
        // from call expressions)
        if (call is ConstructExpression) {
            return
        }

        // We really need a parent, otherwise we cannot replace the node
        if (parent == null) {
            return
        }

        // Some local copies for easier smart casting
        var callee = call.callee
        val language = callee.language

        // We need to "unwrap" some references because they might be nested in unary operations such
        // as pointers. We are interested in the references in the "core". We can skip all
        // references that are member expressions
        val ref = callee.unwrapReference()
        if (ref == null || ref is MemberExpression) {
            return
        }

        // Check, if this is cast is really a construct expression (if the language supports
        // functional-constructs)
        if (language is HasFunctionStyleConstruction) {
            // Check for our type. We are only interested in object types
            val type = ref.nameIsType()
            if (type is ObjectType && !type.isPrimitive) {
                walker.replaceCallWithConstruct(type, parent, call)
            }
        }

        // We need to check, whether the "callee" refers to a type and if yes, convert it into a
        // cast expression. And this is only really necessary, if the function call has a single
        // argument.
        if (language is HasFunctionStyleCasts && call.arguments.size == 1) {
            // Check if it is type and replace the call
            var type = ref.nameIsType()
            if (type != null) {
                walker.replaceCallWithCast(type, parent, call, false)
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}

context(provider: ContextProvider)
fun SubgraphWalker.ScopedWalker<Node>.replaceCallWithCast(
    type: Type,
    parent: AstNode,
    call: CallExpression,
    pointer: Boolean,
) {
    val cast = provider.newCastExpression()
    cast.code = call.code
    cast.language = call.language
    cast.location = call.location
    cast.castType =
        if (pointer) {
            type.pointer()
        } else {
            type
        }
    cast.expression = call.arguments.single()
    cast.name = cast.castType.name

    replace(parent, call, cast)
}

context(_: ContextProvider)
fun SubgraphWalker.ScopedWalker<Node>.replaceCallWithConstruct(
    type: ObjectType,
    parent: AstNode,
    call: CallExpression,
) {
    val callee = call.callee
    if (callee is Reference) {
        val construct = call.toConstructExpression(callee)
        construct.type = type
        replace(parent, call, construct)
    }
}
