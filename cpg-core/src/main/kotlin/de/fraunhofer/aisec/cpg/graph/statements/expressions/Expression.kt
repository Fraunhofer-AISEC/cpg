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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Transient

/**
 * Represents one expression. It is used as a base class for multiple different types of
 * expressions. The only constraint is, that each expression has a type.
 *
 * <p>Note: In our graph, {@link Expression} is inherited from {@link Statement}. This is a
 * constraint of the C++ language. In C++, it is valid to have an expression (for example a {@link
 * Literal}) as part of a function body, even though the expression value is not used. Consider the
 * following code: <code> int main() { 1; } </code>
 *
 * <p>This is not possible in Java, the aforementioned code example would prompt a compile error.
 */
abstract class Expression : Statement(), HasType {
    @Transient override val typeObservers = mutableListOf<HasType.TypeObserver>()

    override var type: Type = unknownType()
        set(value) {
            val old = field
            field = value

            // Only inform our observer if the type has changed. This should not trigger if we
            // "squash" types into one, because they should still be regarded as "equal", but not
            // the "same".
            if (old != value) {
                informObservers(HasType.TypeObserver.ChangeType.DECLARED_TYPE, mutableListOf(this))
            }
        }

    override var assignedTypes: Set<Type> = mutableSetOf()
        set(value) {
            if (field == value) {
                return
            }

            field = value
            informObservers(HasType.TypeObserver.ChangeType.ASSIGNED_TYPE, mutableListOf(this))
        }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("type", type)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Expression) {
            return false
        }
        return (super.equals(other) && type == other.type)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
