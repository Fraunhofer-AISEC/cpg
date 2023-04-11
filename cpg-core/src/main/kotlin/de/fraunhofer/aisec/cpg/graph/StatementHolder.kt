/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.transformIntoOutgoingPropertyEdgeList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.graph.statements.Statement

/**
 * This interface denotes an AST node that can contain code. This code is stored as statements. This
 * includes Translation units namespaces and classes as some languages, mainly scripting languages
 * allow code placement outside explicit functions.
 *
 * The reason for not only using a statement property that encapsulates all code in an implicit
 * compound statements is that code can be distributed between functions and an encapsulating
 * compound statement would imply a block of code with a code region containing only the statements.
 */
interface StatementHolder : Holder<Statement> {
    /** List of statements as property edges. */
    var statementEdges: MutableList<PropertyEdge<Statement>>

    /** Virtual property to access [statementEdges] without property edges. */
    var statements: List<Statement>
        get() {
            return unwrap(statementEdges)
        }
        set(value) {
            statementEdges = transformIntoOutgoingPropertyEdgeList(value, this as Node)
        }

    /**
     * Adds the specified statement to this statement holder. The statements have to be stored as a
     * list of statements as we try to avoid adding new AST-nodes that do not exist, e.g. a code
     * body to hold statements
     *
     * @param s the statement
     */
    fun addStatement(s: Statement) {
        val propertyEdge = PropertyEdge((this as Node), s)
        propertyEdge.addProperty(Properties.INDEX, statementEdges.size)
        statementEdges.add(propertyEdge)
    }

    /** Inserts the statement [s] before the statement specified in [before]. */
    fun insertStatementBefore(s: Statement, before: Statement) {
        val statements = this.statements
        val idx = statements.indexOf(before)
        if (idx != -1) {
            val before = statements.subList(0, idx)
            val after = statements.subList(idx, statements.size)

            this.statements = listOf(*before.toTypedArray(), s, *after.toTypedArray())
        }
    }

    override operator fun plusAssign(node: Statement) {
        addStatement(node)
    }
}
