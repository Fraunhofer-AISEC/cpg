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
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder

class GotoStatement : Statement() {
    var labelName: String = ""
    var targetLabel: LabelStatement? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is GotoStatement) {
            return false
        }
        return super.equals(other) && labelName == other.labelName
    }

    override fun hashCode() = Objects.hash(super.hashCode(), labelName, targetLabel)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append("labelName", labelName)
            .append("targetName", targetLabel)
            .append("location", location)
            .toString()
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.prevEOG
    }
}
