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
package de.fraunhofer.aisec.cpg.analysis.collectioneval

import de.fraunhofer.aisec.cpg.analysis.collectioneval.collection.Collection
import de.fraunhofer.aisec.cpg.analysis.collectioneval.collection.MutableList
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

// We assume that we only work with lists in this operator
class ListSizeEvaluator {
    fun evaluate(node: Node): LatticeInterval {
        val name = node.name
        // TODO: add check whether node operates on a list -> DFG flow to java.util.List?
        val initializer = getInitializerOf(node)
        var range = getInitialRange(initializer)
        val type = MutableList::class
        // TODO: evaluate effect of each operation on the list until we reach "node"
        var current = initializer
        do {
            val next = current.nextEOG.first()
            // TODO: apply each effect only once if EOG branches
            range = range.applyEffect(next, name.toString(), type)
            current = next
        } while (next != node)

        return range
    }

    private fun getInitializerOf(node: Node?): Node {
        return when (node) {
            null -> null!!
            is Reference -> getInitializerOf(node.refersTo)
            is VariableDeclaration -> node.initializer!!
            else -> getInitializerOf(node.prevDFG.firstOrNull())
        }
    }

    private fun getInitialRange(initializer: Node): LatticeInterval {
        val size = (initializer as MemberCallExpression).arguments.size
        return LatticeInterval.Bounded(size, size)
    }

    private fun LatticeInterval.applyEffect(
        node: Node,
        name: String,
        type: KClass<out Collection>
    ): LatticeInterval {
        return type.createInstance().applyEffect(this, node, name)
    }
}
