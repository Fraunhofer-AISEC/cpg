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

import de.fraunhofer.aisec.cpg.graph.ast.statements.Statement
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdges
import de.fraunhofer.aisec.cpg.graph.edges.collections.UnwrappedEdgeList.Delegate

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
    var statementEdges: AstEdges<Statement, AstEdge<Statement>>

    /**
     * Virtual property to access [statementEdges] without property edges.
     *
     * Note: We cannot use [Delegate] because delegates are not allowed in interfaces.
     */
    var statements: MutableList<Statement>

    override fun replace(old: Statement, new: Statement): Boolean {
        return statementEdges.replace(old, new)
    }

    override operator fun plusAssign(node: Statement) {
        statementEdges += node
    }
}
