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
import de.fraunhofer.aisec.cpg.frontends.HasNameOnlyCallResolution
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.ForEach
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.expressions.KeyValue
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.expressions.Subscription
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
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

    /** Real (not invented) records of the component, indexed by their unqualified name. */
    private val realRecordsByLocalName = mutableMapOf<String, MutableList<Record>>()

    /** Methods declared by a real record, indexed by their unqualified name. */
    private val realMethodsByName = mutableMapOf<String, MutableList<Function>>()

    override fun accept(component: Component) {
        // We only use this instance for its member resolution; it is not walking anything.
        val resolver = SymbolResolver(ctx)
        val calls = component.allChildren<Call>()
        var total = 0

        propagateArgumentTypesToParameters(component)

        realRecordsByLocalName.clear()
        realMethodsByName.clear()
        for (record in component.allChildren<Record>()) {
            if (!record.isInferred) {
                realRecordsByLocalName
                    .getOrPut(record.name.localName) { mutableListOf() }
                    .add(record)
            }
        }
        for (method in component.allChildren<Method>()) {
            if (!method.isInferred) {
                realMethodsByName.getOrPut(method.name.localName) { mutableListOf() }.add(method)
            }
        }

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
            // A call answered only by inferred stubs is as good as unanswered: a stub has no body,
            // so nothing is reachable through it. Those are retried too, and the invented
            // receiver below is exactly how most of them come about.
            val onlyStubs = call.invokes.isNotEmpty() && call.invokes.all { it.isInferred }
            if (call.invokes.isNotEmpty() && !onlyStubs) {
                continue
            }
            val callee = call.callee as? MemberAccess ?: continue

            if (onlyStubs) {
                val byName = retryAgainstRealRecordsOfTheSameName(call, callee, resolver)
                if (byName != null) {
                    resolved += byName
                } else {
                    resolved += retryByMemberName(call, callee)
                }
                continue
            }

            val base = callee.base
            if (base.type is UnknownType) {
                // We never worked out what the receiver is. Resolving by the member's name alone
                // is all that is left; see [retryByMemberName].
                resolved += retryByMemberName(call, callee)
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

    /**
     * Retries a call whose receiver type we had to invent, against the real records that carry the
     * same unqualified name.
     *
     * A type we never resolved gets an empty inferred record, and a call on it finds no member and
     * is answered with an inferred stub -- through which nothing is reachable. The type the program
     * meant is very often right there in the graph under a different qualifier: we name a Go
     * package after the last element of its import path, so the qualifier we recorded and the one
     * the record carries need not agree. Across the four validation sets, 3225 of the 4210 calls on
     * an invented receiver had at least one real record of that name to offer.
     *
     * Where several records share the name we take all of them. That is a deliberate
     * over-approximation: an extra target costs precision, a missing one removes everything
     * reachable behind the call, and only the latter is a soundness problem.
     */
    private fun retryAgainstRealRecordsOfTheSameName(
        call: Call,
        callee: MemberAccess,
        resolver: SymbolResolver,
    ): Int? {
        val baseRecord = (callee.base.type.root as? ObjectType)?.recordDeclaration
        if (baseRecord != null && !baseRecord.isInferred) {
            // We do have the type; the member is genuinely not on it.
            return null
        }

        val localName = callee.base.type.root.name.localName
        val replacements = realRecordsByLocalName[localName] ?: return null

        val candidates =
            resolver.resolveMemberByName(
                callee.name.localName,
                replacements.mapTo(mutableSetOf()) { it.toType() },
            )
        if (candidates.none { it is Function }) {
            return null
        }

        val before = call.invokes.toList()
        callee.candidates = candidates
        decideInvokesBasedOnCandidates(callee, call)
        if (call.invokes.isEmpty()) {
            // Do not end up worse off than the stub we started with.
            call.invokes = before.toMutableList()
            return null
        }
        return 1
    }

    /**
     * Resolves a call whose receiver type we never worked out by the name of the member alone.
     *
     * When the base is untyped -- usually because the call that produced it was itself unresolved
     * -- there is no record to look in, so the call is left without a target and everything behind
     * it disappears. Every method of that name is therefore taken as a possible target, restricted
     * to those that could accept this many arguments.
     *
     * This is the same answer we already give for a call through an interface, where the receiver
     * admits any implementation: too many targets, but none missing. It is the last resort, used
     * only once looking the member up on an actual type has failed.
     */
    private fun retryByMemberName(call: Call, callee: MemberAccess): Int {
        // Only for a language where the name of a call already determines its target. Where a
        // name can legitimately mean different things -- overloading, static imports -- answering
        // with every method of that name is not an over-approximation but a wrong answer.
        if (callee.language !is HasNameOnlyCallResolution) {
            return 0
        }
        val methods = realMethodsByName[callee.name.localName] ?: return 0
        val arity = call.arguments.size
        val candidates =
            methods.filterTo(mutableListOf()) {
                it.parameters.size == arity || it.parameters.any { p -> p.isVariadic }
            }
        if (candidates.isEmpty()) {
            return 0
        }

        call.invokes = candidates
        call.invokeEdges.forEach { it.dynamicInvoke = true }
        return 1
    }

    /**
     * Pushes the type of every argument into the parameter it is bound to, and the type of a member
     * call's base into the receiver of the method it reaches.
     *
     * [de.fraunhofer.aisec.cpg.graph.types.HasType.assignedTypes] is meant to carry the types a
     * node actually holds at runtime, as opposed to the one it was declared with, and it is
     * propagated by an observer network over assignments, initializers, returns and references.
     * What that network has no edge for is the call boundary, so the concrete type never reaches
     * the place where it would do the most good:
     * ```go
     * func writeAll(w io.Writer) { w.Write(p) }   // w only ever knows it is an io.Writer
     * writeAll(&bytes.Buffer{})                   // although the caller knows exactly what it is
     * ```
     *
     * Measured against the VTA reference on the validation corpus, the receiver of a dispatching
     * call carried 1.1 assigned types on average and that one type was, almost always, the declared
     * interface itself -- while VTA had found 6 to 12 concrete types for the same receiver. Without
     * this edge class, assigned types say nothing a declaration does not already say.
     *
     * Adding a type informs the observers, so each addition propagates onwards by itself; the loop
     * is only there because a type that arrives at one parameter can travel to another on the next
     * pass.
     */
    private fun propagateArgumentTypesToParameters(component: Component) {
        val calls = component.allChildren<Call>()

        // What each function actually returns, as opposed to what it says it returns.
        // Function.assignedTypeChanged gives up unless the declared return type is a DynamicType,
        // which no statically typed language ever has, so for Go this propagation is simply off:
        // `func newErr() error { return &myErr{} }` never tells its callers about *myErr. That is
        // why every receiver of an `error` sees nothing but `error`, and `error` is on its own the
        // largest single source of over-approximated dispatch in the corpus.
        // Kept per result position, because `return bytesEncoder(...), nil` and `e, err :=
        // makeField(...)` is how Go returns almost everything. Flattening the positions would
        // hand `error` to `e` and the concrete encoder to `err`, which is worse than useless.
        val returnedTypes = mutableMapOf<Function, MutableList<MutableSet<Type>>>()
        for (function in component.allChildren<Function>()) {
            for (ret in function.allChildren<Return>()) {
                ret.returnValues.forEachIndexed { index, value ->
                    val slots = returnedTypes.getOrPut(function) { mutableListOf() }
                    while (slots.size <= index) slots.add(mutableSetOf())
                    slots[index] += value.type
                    slots[index] += value.assignedTypes
                }
            }
        }

        // What concrete types were ever put into a container, per the declaration that holds it.
        // Subscription already maps a container's assigned types to the element type when the
        // container is read, so reading works -- but nothing ever records the writes, and an
        // `[]encoder` that three concrete encoders were appended to still claims to hold nothing
        // but `encoder`.
        val containerElements = mutableMapOf<Declaration, MutableSet<Type>>()

        fun recordInto(container: Expression?, element: Expression?) {
            val declaration = (container as? Reference)?.refersTo ?: return
            val value = element ?: return
            val into = containerElements.getOrPut(declaration) { mutableSetOf() }
            into += value.type
            into += value.assignedTypes
        }

        for (call in calls) {
            // `s = append(s, x, y)` -- Go's way of growing a slice.
            if (call.name.localName == "append" && call.arguments.size >= 2) {
                call.arguments.drop(1).forEach { recordInto(call.arguments.firstOrNull(), it) }
            }
        }
        for (assign in component.allChildren<Assign>()) {
            // `s[i] = x`
            for ((index, lhs) in assign.lhs.withIndex()) {
                val subscription = lhs as? Subscription ?: continue
                recordInto(subscription.arrayExpression, assign.rhs.getOrNull(index))
            }
        }
        for (list in component.allChildren<InitializerList>()) {
            // `[]encoder{a, b}` and `map[string]encoder{...}`, attributed to what holds them.
            val holder = (list.astParent as? Variable) ?: continue
            for (initializer in list.initializers) {
                val value = (initializer as? KeyValue)?.value ?: initializer
                val into = containerElements.getOrPut(holder) { mutableSetOf() }
                into += value.type
                into += value.assignedTypes
            }
        }

        for (round in 1..5) {
            var added = 0

            fun push(target: HasType, source: HasType) {
                val before = target.assignedTypes.size
                target.addAssignedType(source.type)
                target.addAssignedTypes(source.assignedTypes)
                added += target.assignedTypes.size - before
            }

            for (call in calls) {
                for (invoked in call.invokes) {
                    for ((i, parameter) in invoked.parameters.withIndex()) {
                        if (parameter.isVariadic) {
                            // Everything from here on is collected into this parameter.
                            call.arguments.drop(i).forEach { push(parameter, it) }
                            break
                        }
                        call.arguments.getOrNull(i)?.let { push(parameter, it) }
                    }

                    // The base of a member call is what the method's receiver will hold.
                    val receiver = (invoked as? Method)?.receiver
                    val base = (call as? MemberCall)?.base
                    if (receiver != null && base != null) {
                        push(receiver, base)
                    }

                    // ... and a single-valued call holds whatever the callee actually returns.
                    returnedTypes[invoked]?.singleOrNull()?.let { types ->
                        val before = call.assignedTypes.size
                        call.addAssignedTypes(types)
                        added += call.assignedTypes.size - before
                    }
                }
            }

            // `v, err := f()` -- give each target of the destructuring the types that position
            // of the callee actually returns.
            for (assign in component.allChildren<Assign>()) {
                if (assign.lhs.size < 2) continue
                val call = assign.rhs.singleOrNull() as? Call ?: continue
                for (invoked in call.invokes) {
                    val slots = returnedTypes[invoked] ?: continue
                    assign.lhs.forEachIndexed { index, target ->
                        val types = slots.getOrNull(index) ?: return@forEachIndexed
                        val holder = target as? HasType ?: return@forEachIndexed
                        val before = holder.assignedTypes.size
                        holder.addAssignedTypes(types)
                        (holder as? Reference)?.refersTo?.let { declaration ->
                            (declaration as? HasType)?.addAssignedTypes(types)
                        }
                        added += holder.assignedTypes.size - before
                    }
                }
            }

            // Hand the recorded element types to everything that reads the container: an index
            // expression, or the variable of a range loop over it.
            for (subscription in component.allChildren<Subscription>()) {
                val declaration = (subscription.arrayExpression as? Reference)?.refersTo ?: continue
                containerElements[declaration]?.let {
                    val before = subscription.assignedTypes.size
                    subscription.addAssignedTypes(it)
                    added += subscription.assignedTypes.size - before
                }
            }
            for (loop in component.allChildren<ForEach>()) {
                val declaration = (loop.iterable as? Reference)?.refersTo ?: continue
                val elements = containerElements[declaration] ?: continue
                val declared = (loop.variable as? DeclarationStatement)?.declarations ?: loop.locals
                for (variable in declared.filterIsInstance<HasType>()) {
                    val before = variable.assignedTypes.size
                    variable.addAssignedTypes(elements)
                    added += variable.assignedTypes.size - before
                }
            }

            if (added == 0) {
                break
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
