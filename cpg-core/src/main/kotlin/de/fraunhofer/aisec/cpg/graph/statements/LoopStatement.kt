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
package de.fraunhofer.aisec.cpg.graph.statements

import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import org.neo4j.ogm.annotation.Relationship

abstract class LoopStatement : Statement() {

    @Relationship("STATEMENT") var statementEdge = astOptionalEdgeOf<Statement>()
    /**
     * This field contains the body of the loop, usually a [Block]. The looping criterion can be a
     * condition or the iteration over all Elements in a list or defined
     */
    var statement by unwrapping(LoopStatement::statementEdge)

    /**
     * This represents a block whose statements are executed when the loop terminates regularly,
     * e.g. the loop finishes iterating over all elements or the loop-condition evaluates to false.
     * The exact situation when this is happening depends on the language that supports an
     * `else`-BLock at loop level. E.g. in Python the [elseStatement] is executed when the loop was
     * not left through a break.
     */
    @Relationship(value = "ELSE_STATEMENT") var elseStatementEdge = astOptionalEdgeOf<Statement>()
    var elseStatement by unwrapping(LoopStatement::elseStatementEdge)
}
