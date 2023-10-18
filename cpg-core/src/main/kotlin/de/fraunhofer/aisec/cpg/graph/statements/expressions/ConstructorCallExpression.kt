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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder

// TODO Merge and/or refactor
/**
 * Explicit call of a constructor inside the records code that is constructed. Use it for Expressions like `this()`
 * and `super`. As well as other similar expressions of the same type.
 */
class ConstructorCallExpression : CallExpression() {
    var containingClass: String? = null

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("invokes", invokes)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConstructorCallExpression) return false
        return super.equals(other) && containingClass == other.containingClass
    }

    override fun hashCode() = Objects.hash(super.hashCode(), containingClass)
}
