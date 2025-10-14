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
package de.fraunhofer.aisec.cpg.graph.ast.statements

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects

/**
 * Statement used to interrupt further execution of a loop body and jump to the evaluation of the
 * loop condition. Can have a loop label, e.g. in Java, to specify which of the nested loops
 * condition should be reevaluated.
 */
class ContinueStatement : Statement() {
    /** Specifies the loop in a nested structure that the label will 'continue' */
    var label: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContinueStatement) return false
        return super.equals(other) && label == other.label
    }

    override fun hashCode() = Objects.hash(super.hashCode(), label)

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.prevEOG
    }
}
