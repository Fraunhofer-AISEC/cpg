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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.*

class Annotation : AstNode() {
    var members = mutableListOf<AnnotationMember>()

    fun getValueForName(name: String): Expression? {
        return members
            .filter { member: AnnotationMember -> member.name.lastPartsMatch(name) }
            .map { obj: AnnotationMember -> obj.value }
            .firstOrNull()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Annotation) return false
        return super.equals(other) && members == other.members
    }

    override fun hashCode() = Objects.hash(super.hashCode(), members)
}
