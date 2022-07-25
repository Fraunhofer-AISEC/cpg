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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.ExperimentalGraph
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.graph
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*

/** Returns all [CallExpression]s in this graph. */
@OptIn(ExperimentalGraph::class)
val TranslationResult.calls: List<CallExpression>
    get() = this.graph.nodes.filterIsInstance<CallExpression>()

/** Returns all [CallExpression]s in this graph which call a method with the given [name]. */
@OptIn(ExperimentalGraph::class)
fun TranslationResult.callByName(name: String): List<CallExpression> {
    return this.graph.nodes.filter { node ->
        (node as? CallExpression)?.invokes?.any { it.name == name } == true
    } as List<CallExpression>
}

/**
 * Filters a list of [CallExpression]s for expressions which call a method with the given [name].
 */
fun List<CallExpression>.name(name: String): List<CallExpression> {
    return this.filter { n -> n.invokes.any { it.name == name } }
}

/**
 * Returns the expression specifying the dimension (i.e., size) of the array during its
 * initialization.
 */
val ArraySubscriptionExpression.size: Expression
    get() =
        (((this.arrayExpression as DeclaredReferenceExpression).refersTo as VariableDeclaration)
                .initializer as ArrayCreationExpression)
            .dimensions[0]
