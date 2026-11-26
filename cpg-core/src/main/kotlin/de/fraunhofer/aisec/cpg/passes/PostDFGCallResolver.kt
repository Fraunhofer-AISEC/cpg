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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("PostDFGCallResolver")

/**
 * Retries unresolved member calls *after* the [DFGPass] has propagated type information through
 * data-flow edges.
 *
 * The [LateCallResolver] runs before the [DFGPass], so any type that only becomes available through
 * DFG propagation — for example, the element type of a container annotation that flows from a field
 * through a `list()` call to a loop variable and then to the reference inside
 * `connection.is_closed()` — is not yet known when it runs. This pass runs after the DFG pass (and
 * after [ControlFlowSensitiveDFGPass] / [PointsToPass] if they are enabled) and retries the same
 * calls using the now-complete type information.
 *
 * The most common pattern this fixes in Python is:
 * ```python
 * class Pool:
 *     def __init__(self):
 *         self._connections: list[Connection] = []  # annotation on the field
 *
 *     def cleanup(self):
 *         for connection in list(self._connections):  # loop variable typed from field annotation
 *             connection.is_closed()                 # call on the loop variable
 * ```
 *
 * The annotation `list[Connection]` is parsed by [PythonAddDeclarationsPass], the element type is
 * propagated to the loop variable by an [ElementTypePropagation] observer, but the `connection`
 * reference inside the loop body only receives the type when the DFG edge from the loop variable
 * fires — which is after the [LateCallResolver] has already passed.
 *
 * The retry is deliberately additive: it only looks at calls that currently have no target at all
 * (or only inferred stubs), so it can add edges but never remove or replace one.
 */
@DependsOn(DFGPass::class)
@ExecuteBefore(DynamicInvokeResolver::class)
@Description(
    "Retries calls that were left unresolved because the type of their base only became known after the DFG pass propagated it."
)
class PostDFGCallResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    override fun accept(component: Component) {
        val resolver = SymbolResolver(ctx)
        val calls = component.allChildren<Call>()
        val (realRecordsByLocalName, realMethodsByName) = buildCallResolutionIndexes(component)

        var total = 0
        for (round in 1..10) {
            val resolved = resolveRound(calls, resolver, realRecordsByLocalName, realMethodsByName)
            total += resolved
            if (resolved == 0) {
                break
            }
        }

        if (total > 0) {
            log.info(
                "Post-DFG call resolution: resolved {} additional call(s) using types propagated by the DFG pass",
                total,
            )
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
