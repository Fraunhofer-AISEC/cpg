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
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.codeAndLocationFrom
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.fqn
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.replace
import de.fraunhofer.aisec.cpg.nameIsType
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * This pass is responsible for handling Java-specific cases that are not covered by the general CPG
 * logic. For example, Java has static member access, which is not modeled as a member expression,
 * but as a reference with an FQN. This pass will convert such member expressions to references with
 * FQNs.
 */
@DependsOn(TypeResolver::class)
@ExecuteBefore(SymbolResolver::class)
@Description(
    "This pass is responsible for handling Java-specific cases that are not covered by the general CPG logic. For example, Java has static member access, which is not modeled as a member expression, but as a reference with an FQN. This pass will convert such member expressions to references with FQNs."
)
class JavaExtraPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    private lateinit var walker: SubgraphWalker.ScopedWalker<AstNode>

    override fun accept(tu: TranslationUnit) {
        // Loop through all member expressions
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager, Strategy::AST_FORWARD)
        walker.registerHandler { node ->
            when (node) {
                is MemberExpression -> handleMemberExpression(node)
            }
        }

        walker.iterate(tu)
    }

    fun handleMemberExpression(me: MemberExpression) {
        val parent = me.astParent

        // For now, we are only interested in fields and not in calls, since this will open another
        // can of worms
        if (parent is CallExpression && parent.callee == me) return

        // Look at the "base" of the member expression and check if this is referring to a type
        var base = me.base as? Reference
        var type = base?.nameIsType()
        if (type != null) {
            // Our base refers to a type, so this is actually a static reference, which we
            // model not as a member expression, but as a reference with an FQN. Let's build that
            // and exchange the node
            val ref =
                newReference(type.name.fqn(me.name.localName), type = me.type)
                    .codeAndLocationFrom(me)
                    .apply { isStaticAccess = true }
            ref.language = me.language
            walker.replace(parent, me, ref)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
