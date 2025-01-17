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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.GotoStatement
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import java.util.function.BiConsumer
import java.util.function.BiPredicate

/**
 * This class is a piece of legacy code that seems to offer a functionality to register certain
 * listeners that get executed when raw AST nodes are processed into certain nodes (specified in
 * [interestingStatements]).
 */
open class ProcessedListener {
    @JvmField
    protected var interestingStatements =
        listOf(GotoStatement::class.java, LabelStatement::class.java)
    @JvmField
    protected var predicateListeners = mutableMapOf<BiPredicate<Any, Node>, BiConsumer<Any, Node>>()
    @JvmField protected var processedMapping = mutableMapOf<Any, Node>()

    /**
     * Two data structures used to associate Objects input to a pass to results of a pass, e.g.
     * Javaparser AST-Nodes to CPG-Nodes. The "Listeners" in processedListener are called after the
     * node they are saved under get an entry in processedMapping. THis combination allows to keep
     * the information on which AST-Node build which CPG-Node and operate with these associations
     * once they exist, important to resolve connections between labels and label usages.
     */
    protected var objectListeners = mutableMapOf<Any, BiConsumer<Any, Node>>()

    fun clearProcessed() {
        this.objectListeners.clear()
        this.predicateListeners.clear()
        this.processedMapping.clear()
    }

    /**
     * This function should be called by anything that implements this processed listener to
     * indicate that a new [Node] has been processed from the raw node in [from].
     */
    open fun process(from: Any, to: Node) {
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
        val newPredicateListeners = mutableMapOf<BiPredicate<Any, Node>, BiConsumer<Any, Node>>()
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

    /**
     * Registers a new listener ([biConsumer]), that gets called if the raw node specified in [from]
     * gets processed.
     */
    open fun registerObjectListener(from: Any, biConsumer: BiConsumer<Any, Node>) {
        if (from in processedMapping) {
            processedMapping[from]?.let { biConsumer.accept(from, it) }
        }
        objectListeners[from] = biConsumer
    }

    open fun registerPredicateListener(
        predicate: BiPredicate<Any, Node>,
        biConsumer: BiConsumer<Any, Node>,
    ) {
        val matchingEntries: MutableList<Map.Entry<Any, Node>> = ArrayList()
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
