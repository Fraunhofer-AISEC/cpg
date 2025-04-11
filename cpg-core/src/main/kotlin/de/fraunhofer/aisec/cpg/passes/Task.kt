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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TypeManager
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.RawNodeTypeProvider
import de.fraunhofer.aisec.cpg.graph.ScopeProvider
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class Task<PassTargetType : Node, PassType : Pass<PassTargetType>>(
    override val ctx: TranslationContext,
    /** A reference to the [Pass] that is executing this task. */
    var pass: PassType,
) : ContextProvider, RawNodeTypeProvider<Nothing>, ScopeProvider {
    companion object {
        val log: Logger = LoggerFactory.getLogger(Task::class.java)
    }

    val config: TranslationConfiguration = ctx.config
    val scopeManager: ScopeManager = ctx.scopeManager
    val typeManager: TypeManager = ctx.typeManager

    /**
     * The current [Scope] of the [scopeManager]. Please note, that each pass is responsible for
     * actually setting the correct scope within the [scopeManager], e.g., by using the
     * [ScopedWalker].
     */
    override val scope: Scope?
        get() = scopeManager.currentScope
}

abstract class EOGIteratorTask<PassTargetType : Node, PassType : Pass<PassTargetType>, T : Node>(
    ctx: TranslationContext,
    pass: PassType,
) : Task<PassTargetType, PassType>(ctx, pass) {

    /**
     * Generates [T]s belonging to the given [node]. The [state] contains a map of nodes to their
     * respective [T]s created by this instance of the pass.
     */
    open fun handleCallExpression(
        state: MapLattice.Element<Node, PowersetLattice.Element<T>>,
        node: CallExpression,
    ): Collection<T> {
        return emptySet()
    }

    /**
     * Generates [T]s belonging to the given [node]. The [state] contains a map of nodes to their
     * respective [T]s created by this instance of the pass.
     *
     * This is the advanced version and passes the [lattice] in case the [state] should be
     * manipulated. We do not recommend using this!
     */
    open fun handleCallExpression(
        lattice: MapLattice<Node, PowersetLattice.Element<T>>,
        state: MapLattice.Element<Node, PowersetLattice.Element<T>>,
        node: CallExpression,
    ): Collection<T> {
        return emptySet()
    }

    /**
     * Generates [T]s belonging to the given [node]. The [state] contains a map of nodes to their
     * respective [T]s created by this instance of the pass.
     */
    open fun handleMemberCallExpression(
        state: MapLattice.Element<Node, PowersetLattice.Element<T>>,
        node: MemberCallExpression,
    ): Collection<T> {
        return emptySet()
    }

    /**
     * Generates [T]s belonging to the given [node]. The [state] contains a map of nodes to their
     * respective [T]s created by this instance of the pass.
     *
     * This is the advanced version and passes the [lattice] in case the [state] should be
     * manipulated. We do not recommend using this!
     */
    open fun handleMemberCallExpression(
        lattice: MapLattice<Node, PowersetLattice.Element<T>>,
        state: MapLattice.Element<Node, PowersetLattice.Element<T>>,
        node: MemberCallExpression,
    ): Collection<T> {
        return emptySet()
    }

    /**
     * This function is called for each node in the graph. The specific nodes are always handled in
     * the same order. It calls the basic and advanced version of the handleX-methods.
     */
    fun handleNode(
        lattice: MapLattice<Node, PowersetLattice.Element<T>>,
        state: MapLattice.Element<Node, PowersetLattice.Element<T>>,
        node: Node,
    ): Collection<T> {
        return when (node) {
            is MemberCallExpression ->
                handleMemberCallExpression(lattice, state, node) +
                    handleMemberCallExpression(state, node)
            is CallExpression ->
                handleCallExpression(lattice, state, node) + handleCallExpression(state, node)
            else -> emptySet()
        }
    }
}
