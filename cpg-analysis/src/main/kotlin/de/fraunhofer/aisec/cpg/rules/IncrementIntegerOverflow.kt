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
package de.fraunhofer.aisec.cpg.rules

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.query.*
import kotlin.math.pow

class IncrementIntegerOverflow : Rule {
    override var queryResult: QueryTree<*>? = null // to be set
    override val id = "cpg-0014" // TODO IDS
    override val name = "Increment Integer Overflow"
    override val cweId: String = "128"
    override val shortDescription =
        "Detects (post- or prefix) unary increments that may cause the their target to " +
                "overflow"
    override val level = Rule.Level.Error
    override val message = "Increment may cause overflow"

    override fun run(result: TranslationResult) {
        queryResult =
            result.allExtended<UnaryOperator>(
                { it.operatorCode == "++" && it.type is NumericType },
                {
                    val max = max(it.input)
                    (max eq maxSizeOfType(it.input.type)) or
                            when (max.value) {
                                is Long -> max as QueryTree<Long> eq
                                        const((2L shl (it.input.type as NumericType).bitWidth!!) - 1)

                                is Int -> max as QueryTree<Int> eq
                                        const((2 shl (it.input.type as NumericType).bitWidth!!) - 1)

                                is Float -> max as QueryTree<Float> eq
                                        const(2.0f.pow((it.input.type as NumericType).bitWidth!!) - 1)

                                is Double -> max as QueryTree<Double> eq
                                        const(2.0.pow((it.input.type as NumericType).bitWidth!!) - 1)

                                else -> const(false)
                            }
                }
            )
    }
}
