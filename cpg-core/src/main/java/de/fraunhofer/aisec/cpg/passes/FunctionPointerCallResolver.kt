/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.IncompleteType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend
import java.util.*
import java.util.function.Consumer

/**
 * This [Pass] is responsible for resolving function pointer calls, i.e., [CallExpression] nodes
 * that contain a reference/pointer to a function and are being "called". This pass is intentionally
 * split from the [CallResolver] because it depends on DFG edges. This split allows the
 * [CallResolver] to be run before any DFG passes, which in turn allow us to also populate DFG
 * passes for inferred functions.
 *
 * This pass is currently only run for the [CXXLanguageFrontend], however, in the future we might
 * extend it to other languages that support some kind of function reference/pointer calling, such
 * as Go.
 */
@DependsOn(CallResolver::class)
@DependsOn(DFGPass::class)
@RequiredFrontend(CXXLanguageFrontend::class)
class FunctionPointerCallResolver : Pass() {
    private lateinit var walker: ScopedWalker
    private var inferDfgForUnresolvedCalls = false

    override fun accept(t: TranslationResult) {
        scopeManager = t.scopeManager
        inferDfgForUnresolvedCalls = t.config.inferenceConfiguration.inferDfgForUnresolvedSymbols
        walker = ScopedWalker(t.scopeManager)
        walker.registerHandler { _: RecordDeclaration?, _: Node?, currNode: Node? ->
            walker.collectDeclarations(currNode)
        }
        walker.registerHandler { node, _ -> resolve(node) }

        for (tu in t.translationUnits) {
            walker.iterate(tu)
        }
    }

    private fun resolve(node: Node?) {
        when (node) {
            is MemberCallExpression -> handleMemberCallExpression(node)
            is CallExpression -> handleCallExpression(node)
        }
    }

    /**
     * Resolves function pointers in a [CallExpression] node. We could be referring to a function
     * pointer even though it is not a member call if the usual function pointer syntax (*fp)() has
     * been omitted: fp(). Looks like a normal call, but it isn't.
     */
    private fun handleCallExpression(call: CallExpression) {
        // Since we are using a scoped walker, we can access the current scope here and try to
        // resolve the call expression to a declaration that contains the pointer.
        val pointer =
            scopeManager
                .resolve<ValueDeclaration>(scopeManager.currentScope, true) {
                    it.type is FunctionPointerType && it.name.endsWith(call.name)
                }
                .firstOrNull()
        if (pointer != null) {
            handleFunctionPointerCall(call, pointer)
        }
    }

    /**
     * Resolves function pointers in a [MemberCallExpression]. In this case the
     * [MemberCallExpression.member] field needs to have a [FunctionPointerType].
     */
    private fun handleMemberCallExpression(call: MemberCallExpression) {
        val member = call.member
        if (member is HasType && (member as HasType).type is FunctionPointerType) {
            handleFunctionPointerCall(call, call.member)
        }
    }

    private fun handleFunctionPointerCall(call: CallExpression, pointer: Node?) {
        val pointerType = (pointer as HasType).type as FunctionPointerType
        val invocationCandidates: MutableList<FunctionDeclaration> = ArrayList()
        val work: Deque<Node> = ArrayDeque()
        val seen = IdentitySet<Node>()
        work.push(pointer)
        while (!work.isEmpty()) {
            val curr = work.pop()
            if (!seen.add(curr)) {
                continue
            }

            if (curr is FunctionDeclaration) {
                // Even if it is a function declaration, the dataflow might just come from a
                // situation where the target of a fptr is passed through via a return value. Keep
                // searching if return type or signature don't match

                // In some languages, there might be no explicit return type. In this case we are
                // using a single void return type.
                val returnType: Type =
                    if (curr.returnTypes.isEmpty()) {
                        IncompleteType()
                    } else {
                        // TODO(oxisto): support multiple return types
                        curr.returnTypes[0]
                    }
                if (
                    TypeManager.getInstance()
                        .isSupertypeOf(pointerType.returnType, returnType, call) &&
                        curr.hasSignature(pointerType.parameters)
                ) {
                    invocationCandidates.add(curr)
                    // We have found a target. Don't follow this path any further, but still
                    // continue the other paths that might be left, as we could have several
                    // potential targets at
                    // runtime
                    continue
                }
            }
            curr.prevDFG.forEach(Consumer(work::push))
        }

        call.invokes = invocationCandidates
        // We have to update the dfg edges because this call could now be resolved (which was not
        // the case before).
        DFGPass().handleCallExpression(call, inferDfgForUnresolvedCalls)
    }

    override fun cleanup() {
        // Nothing to do
    }
}
