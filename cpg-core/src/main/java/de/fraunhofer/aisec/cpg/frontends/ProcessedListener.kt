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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.statements.GotoStatement
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import java.util.function.BiConsumer
import java.util.function.BiPredicate

open class ProcessedListener {
    @JvmField
    protected var interestingStatements =
        listOf(GotoStatement::class.java, LabelStatement::class.java)
    @JvmField
    protected var predicateListeners = mutableMapOf<BiPredicate<Any, Any>, BiConsumer<Any, Any?>>()
    @JvmField protected var processedMapping = mutableMapOf<Any, Any>()

    /**
     * Two data structures used to associate Objects input to a pass to results of a pass, e.g.
     * Javaparser AST-Nodes to CPG-Nodes. The "Listeners" in processedListener are called after the
     * node they are saved under get an entry in processedMapping. THis combination allows to keep
     * the information on which AST-Node build which CPG-Node and operate with these associations
     * once they exist, important to resolve connections between labels and label usages.
     */
    protected var objectListeners = mutableMapOf<Any, BiConsumer<Any, Any?>>()

    fun clearProcessed() {
        this.objectListeners.clear()
        this.predicateListeners.clear()
        this.processedMapping.clear()
    }

    open fun process(from: Any?, to: Any?) {
        if (from == null || to == null) return

        if (interestingStatements.any { c -> c.isInstance(from) || c.isInstance(to) }) {
            processedMapping[from] = to
        }
        val listener = objectListeners[from]
        if (listener != null) {
            listener.accept(from, to)
            // Delete line if Node should be processed multiple times and should again invoke the
            // listener, e.g. refinement.
            objectListeners.remove(from)
        }
        // Iterate over existing predicate based listeners, if the predicate matches the
        // listener/handler is executed on the new object.
        val newPredicateListeners = mutableMapOf<BiPredicate<Any, Any>, BiConsumer<Any, Any?>>()
        for ((key, value) in predicateListeners) {
            if (key.test(from, to)) {
                value.accept(from, to)
            } else {
                // Delete line if Node should be processed multiple times and should again invoke
                // the listener, e.g. refinement.
                newPredicateListeners[key] = value
            }
        }
        predicateListeners = newPredicateListeners
    }

    open fun registerObjectListener(from: Any, biConsumer: BiConsumer<Any, Any?>) {
        if (from in processedMapping) {
            biConsumer.accept(from, processedMapping[from]!!)
        }
        objectListeners[from] = biConsumer
    }

    open fun registerPredicateListener(
        predicate: BiPredicate<Any, Any>,
        biConsumer: BiConsumer<Any, Any?>
    ) {
        val matchingEntries: MutableList<Map.Entry<Any, Any>> = ArrayList()
        for (mapping in processedMapping.entries) {
            if (predicate.test(mapping.key, mapping.value)) {
                matchingEntries.add(mapping)
            }
        }
        for ((key, value) in matchingEntries) {
            biConsumer.accept(key, value)
        }
        predicateListeners[predicate] = biConsumer
    }
}
