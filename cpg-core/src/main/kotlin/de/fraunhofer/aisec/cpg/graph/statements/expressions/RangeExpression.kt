/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents the specification of a range (e.g., of an array). Usually used in combination with an
 * [SubscriptExpression] as the [SubscriptExpression.subscriptExpression].
 *
 * Examples can be found in Go:
 * ```go
 * a := []int{1,2,3}
 * b := a[:1]
 * ```
 *
 * or Python:
 * ```python
 * a = (1,2,3)
 * b = a[:1]
 * ```
 *
 * In C/C++ this can be also part of a [DesignatedInitializerExpression], as part of a GCC
 * extension:
 * ```c
 * int a[] = { [0...4] = 1 };
 * ```
 *
 * Individual meaning of the range indices might differ per language.
 */
class RangeExpression internal constructor(ctx: TranslationContext) : Expression(ctx) {
    @Relationship("FLOOR") var floorEdge = astOptionalEdgeOf<Expression>()
    /** The lower bound ("floor") of the range. This index is usually *inclusive*. */
    var floor by unwrapping(RangeExpression::floorEdge)

    @Relationship("CEILING") var ceilingEdge = astOptionalEdgeOf<Expression>()
    /** The upper bound ("ceiling") of the range. This index is usually *exclusive*. */
    var ceiling by unwrapping(RangeExpression::ceilingEdge)

    @Relationship("THIRD") var thirdEdge = astOptionalEdgeOf<Expression>()
    /**
     * Some languages offer a third value. The meaning depends completely on the language. For
     * example, Python allows specifying a step, while Go allows to control the underlying array's
     * capacity (not length).
     */
    var third by unwrapping(RangeExpression::thirdEdge)

    /** The operator code that separates the range elements. Common cases are `:` or `...` */
    var operatorCode = ":"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RangeExpression) return false
        return super.equals(other) &&
            floor == other.floor &&
            ceiling == other.ceiling &&
            third == other.third &&
            operatorCode == other.operatorCode
    }

    override fun hashCode() = Objects.hash(super.hashCode(), floor, ceiling, third, operatorCode)
}
