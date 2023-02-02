/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.SubGraph
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * A label attached to a statement that is used to change control flow by labeled continue and
 * breaks (Java) or goto(C++).
 */
class LabelStatement : Statement() {
    /** Statement that the label is attached to. Can be a simple or compound statement. */
    @field:SubGraph("AST") var subStatement: Statement? = null

    /** Label in the form of a String */
    var label: String? = null

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("subStatement", subStatement)
            .append("label", label)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LabelStatement) return false
        return super.equals(other) && subStatement == other.subStatement && label == other.label
    }

    override fun hashCode() = Objects.hash(super.hashCode(), label)
}
