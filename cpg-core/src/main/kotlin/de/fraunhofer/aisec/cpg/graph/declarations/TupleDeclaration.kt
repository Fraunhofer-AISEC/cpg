/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.newTupleDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.TupleType

/**
 * This declaration models a tuple of different [VariableDeclaration] nodes. This is primarily used
 * in languages that support multiple assignments in a declaration, such as Go. The tuple is needed
 * because the initializer of this declaration is flowing into the tuple (and then split among its
 * elements) rather than flowing into the declarations individually. For example the following code
 *
 * ```go
 * var a,b = call()
 * ```
 *
 * corresponds to:
 * - two [VariableDeclaration] nodes `a` and `b`, with an empty [VariableDeclaration.initializer]
 * - a [TupleDeclaration], with the auto-generated name `(a,b)` and [TupleDeclaration.elements] `a`
 *   and `b`
 * - an [TupleDeclaration.initializer] that holds a [CallExpression] to `call`.
 *
 * Implementation Note #1: The [VariableDeclaration.initializer] of the element variables MUST be
 * empty; only the [TupleDeclaration.initializer] must be set. Otherwise we are potentially parsing
 * the initializer twice.
 *
 * Implementation Note #2: Currently, we only support [TupleDeclaration] with an initial [AutoType]
 * (set in [newTupleDeclaration]); its actual [TupleType] will be inferred by the
 * [TupleDeclaration.initializer] (see [VariableDeclaration.typeChanged] for the implementation).
 *
 * The same applies to the elements in the tuple. They also need to have an [AutoType], and their
 * respective type will be based on a registered type observer to their tuple and implemented also
 * in [VariableDeclaration.typeChanged]
 */
class TupleDeclaration : VariableDeclaration() {
    /** The list of elements in this tuple. */
    var elementEdges =
        astEdgesOf<VariableDeclaration>(onAdd = { registerTypeObserver(it.end) }) {
            unregisterTypeObserver(it.end)
        }
    var elements by unwrapping(TupleDeclaration::elementEdges)

    override var name: Name
        get() = Name(elements.joinToString(",", "(", ")") { it.name.toString() })
        set(_) {}

    operator fun plusAssign(element: VariableDeclaration) {
        this.elements += element
        // Make sure we inform the new element about our type changes
        registerTypeObserver(element)
    }

    override fun startingPrevEOG(): Collection<Node> {
        return this.initializer?.startingPrevEOG() ?: setOf()
    }
}
