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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * A node where the statement could not be translated by the graph. We use ProblemExpressions
 * whenever the CPG library requires an [Expression].
 */
class ProblemExpression(
    override var problem: String = "",
    override var type: ProblemNode.ProblemType = ProblemNode.ProblemType.TRANSLATION
) : Expression(), ProblemNode {
    override fun toString(): String {
        return ToStringBuilder(this, Node.TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("problem", problem)
            .toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is ProblemExpression) {
            return false
        }
        val that = o
        return (super.equals(that) && problem == that.problem)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
