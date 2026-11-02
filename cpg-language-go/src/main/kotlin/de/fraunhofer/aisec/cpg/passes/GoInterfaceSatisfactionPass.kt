/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.RequiresLanguage

/**
 * Establishes the implicit interface satisfaction relation of Go.
 *
 * Unlike languages with nominal subtyping, a Go type never declares which interfaces it implements;
 * satisfaction is structural, i.e. a type implements an interface as soon as its method set
 * contains every method of that interface. [TypeHierarchyResolver] however derives the hierarchy
 * from [Record.superTypes], which for Go records stays empty. As a consequence
 * `Function.overriddenBy` is never populated for interface methods, and
 * `SymbolResolver.getOverridingCandidates` cannot devirtualize a call through an interface: the
 * call is resolved to the interface method, which has no body, and the call graph stops there.
 *
 * This pass closes that gap by computing the satisfaction relation structurally and recording it in
 * [Record.implementedInterfaces], so that the hierarchy is available by the time
 * [TypeHierarchyResolver] runs.
 *
 * Note that we deliberately do not distinguish between value and pointer receivers. Go's method set
 * of `T` only contains the value-receiver methods whereas `*T` also contains the pointer-receiver
 * ones, so considering all declared methods over-approximates satisfaction. For the purpose of call
 * graph construction an over-approximation is the safe direction: it may add a dispatch target that
 * cannot occur, but it will not drop one that can.
 */
@RequiresLanguage(GoLanguage::class)
@ExecuteBefore(TypeHierarchyResolver::class)
@Description(
    "Computes Go's implicit, structural interface satisfaction and records it as implemented interfaces, so that the type hierarchy and therefore method devirtualization become available."
)
class GoInterfaceSatisfactionPass(ctx: TranslationContext) : ComponentPass(ctx) {

    /** Identifies a method by name and parameter count. */
    private data class MethodKey(val name: String, val arity: Int)

    private fun Record.methodKeys(): Set<MethodKey> =
        this.methods.mapTo(mutableSetOf()) { MethodKey(it.name.localName, it.parameters.size) }

    /**
     * Collects the method set of every concrete type by grouping [Method] declarations on their
     * receiver.
     *
     * We cannot rely on [Record.methods] here: in Go a method is a top-level declaration carrying a
     * receiver rather than part of the type declaration, so it is only attached to its [Record] by
     * a later pass. At the point where the interface hierarchy has to be established, the method
     * list of a concrete type is therefore still empty, while an interface -- whose methods *are*
     * written inside the type -- already has one.
     */
    private fun methodsByReceiverType(component: Component): Map<String, Set<MethodKey>> {
        val result = mutableMapOf<String, MutableSet<MethodKey>>()
        val allMethods = component.allChildren<Method>()
        var withReceiver = 0
        for (method in allMethods) {
            val receiverType = method.receiver?.type?.root ?: continue
            withReceiver++
            result
                .getOrPut(receiverType.name.localName) { mutableSetOf() }
                .add(MethodKey(method.name.localName, method.parameters.size))
        }
        log.info(
            "  methodsByReceiverType: {} Method nodes, {} with a receiver type, {} distinct receivers; sample receivers={}",
            allMethods.size,
            withReceiver,
            result.size,
            result.keys.take(3),
        )
        return result
    }

    override fun accept(component: Component) {
        val records = component.allChildren<Record>()
        if (records.isEmpty()) return

        val interfaces = records.filter { it.kind == "interface" }
        val concretes = records.filter { it.kind != "interface" }
        log.info(
            "  sample concrete record names={}; sample interface names={}",
            concretes.take(3).map { it.name.toString() },
            interfaces.take(3).map { it.name.toString() },
        )
        if (interfaces.isEmpty() || concretes.isEmpty()) return

        // Pre-compute the required method set of every interface once. Interfaces
        // without methods (such as `any`) are skipped: every type would satisfy
        // them, which adds a dispatch edge from every call to every method and
        // is not useful for call graph construction.
        val required =
            interfaces.mapNotNull { iface ->
                val keys = iface.methodKeys()
                if (keys.isEmpty()) null else iface to keys
            }
        if (required.isEmpty()) return

        val byReceiver = methodsByReceiverType(component)

        var added = 0
        for (record in concretes) {
            // Prefer the receiver-based method set; fall back to the record's own
            // list for types whose methods are already attached.
            val provided = byReceiver[record.name.localName] ?: record.methodKeys()
            if (provided.isEmpty()) continue

            for ((iface, needed) in required) {
                if (iface == record) continue
                if (!provided.containsAll(needed)) continue

                val ifaceType = iface.toType()
                if (record.implementedInterfaces.none { it.name == ifaceType.name }) {
                    record.implementedInterfaces.add(ifaceType)
                    added++
                }
            }
        }

        val concretesWithMethods =
            concretes.count { (byReceiver[it.name.localName] ?: it.methodKeys()).isNotEmpty() }
        log.info(
            "Go interface satisfaction: {} records ({} interfaces, {} with methods; {} concrete types, {} of which have methods), {} relations added",
            records.size,
            interfaces.size,
            required.size,
            concretes.size,
            concretesWithMethods,
            added,
        )
        if (concretesWithMethods > 0 && added == 0) {
            val sample =
                concretes.firstOrNull {
                    (byReceiver[it.name.localName] ?: it.methodKeys()).isNotEmpty()
                }
            log.info(
                "  sample concrete {} provides {}; sample interface {} requires {}",
                sample?.name,
                sample?.let { byReceiver[it.name.localName] ?: it.methodKeys() },
                required.firstOrNull()?.first?.name,
                required.firstOrNull()?.second,
            )
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}
