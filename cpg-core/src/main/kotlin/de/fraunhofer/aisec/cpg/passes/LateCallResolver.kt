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
import de.fraunhofer.aisec.cpg.frontends.HasFirstClassFunctions
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore

/**
 * Retries the calls that the [SymbolResolver] left without any target because the type of their
 * base was not known yet when it got there.
 *
 * The [SymbolResolver] runs per EOG starter, and the type of a variable that is not declared with
 * an explicit type only becomes known once the call it is initialised from has been resolved:
 * ```go
 * var defaultLogger = zap.New(zapcore.NewCore(...))   // type comes from zap.New's return type
 * ...
 * defaultLogger.Sugar().Infof(...)                    // in another file of the same package
 * ```
 *
 * If the function containing the second line is visited before the one declaring `defaultLogger`,
 * the base still has an [UnknownType] at that moment, no containing record can be derived, and the
 * call ends up with no target at all -- not even an inferred one, since there is no record to infer
 * a method on. By the time the pass is finished the type *is* there, it just no longer triggers
 * anything. On the Go validation corpus such calls are the single largest group of missing call
 * edges, and because the result of the call is then untyped as well, each of them takes the rest of
 * its expression chain down with it.
 *
 * The retry is deliberately additive: it only looks at calls that currently have no target at all,
 * so it can add edges but never remove or replace one.
 */
@DependsOn(SymbolResolver::class)
@DependsOn(TypeResolver::class)
@ExecuteBefore(DFGPass::class)
@Description(
    "Retries calls that were left unresolved because the type of their base only became known after the symbol resolver had passed them."
)
class LateCallResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    override fun accept(component: Component) {
        // We only use this instance for its member resolution; it is not walking anything.
        val resolver = SymbolResolver(ctx)
        val calls = component.allChildren<Call>()
        var total = 0

        // Resolving one call makes the type of its result known, which is the base of the next
        // call in the chain: `defaultLogger.Sugar().Infof(...)` needs one round to learn that
        // `Sugar()` returns a `*SugaredLogger` and a second one to find `Infof` on it. So iterate
        // until nothing moves any more. The bound only guards against a pathological graph; in
        // practice this converges after a handful of rounds, because each round has to extend a
        // chain of member accesses that the source code actually spells out.
        for (round in 1..10) {
            val resolved = resolveRound(calls, resolver)
            total += resolved
            if (resolved == 0) {
                break
            }
        }

        if (total > 0) {
            log.info(
                "Late call resolution: resolved {} call(s) whose base type was not known yet when the symbol resolver visited them",
                total,
            )
        }
    }

    private fun resolveRound(calls: Collection<Call>, resolver: SymbolResolver): Int {
        var resolved = 0

        for (call in calls) {
            if (call.invokes.isNotEmpty()) {
                continue
            }
            val callee = call.callee as? MemberAccess ?: continue
            val base = callee.base
            if (base.type is UnknownType) {
                // Still nothing to go on.
                continue
            }

            val (possibleTypes, _) = getPossibleContainingTypes(callee)
            val candidates = resolver.resolveMemberByName(callee.name.localName, possibleTypes)
            if (candidates.isEmpty()) {
                continue
            }
            callee.candidates = candidates

            // The member may be a function-typed field rather than a method, in which case the
            // call is a call of that field's value. Bind it so that the DynamicInvokeResolver can
            // follow the data flow; see SymbolResolver.handleMemberAccess for the same case.
            if (callee.language is HasFirstClassFunctions && candidates.none { it is Function }) {
                val field = candidates.filterIsInstance<Field>().singleOrNull()
                if (field != null && field.type.isFunctionLike) {
                    callee.refersTo = field
                    callee.type = field.type
                }
                continue
            }

            decideInvokesBasedOnCandidates(callee, call)
            if (call.invokes.isNotEmpty()) {
                resolved++
            }
        }

        return resolved
    }

    override fun cleanup() {
        // Nothing to do
    }
}
