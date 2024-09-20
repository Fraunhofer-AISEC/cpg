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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

class ExpressionHandlerTest {
    @Test
    fun testBoolOps() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("boolop.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val twoBoolOpCondition =
            result.functions["twoBoolOp"]?.ifs?.singleOrNull()?.condition as? BinaryOperator
        assertNotNull(twoBoolOpCondition)
        assertEquals("and", twoBoolOpCondition.operatorCode)
        assertEquals("a", (twoBoolOpCondition.lhs as? Reference)?.name?.localName)
        assertEquals(true, (twoBoolOpCondition.rhs as? Literal<*>)?.value)

        // We expect that lhs comes first in the EOG and then the rhs.
        assertContains(twoBoolOpCondition.lhs.nextEOG, twoBoolOpCondition.rhs)

        val threeBoolOpCondition =
            result.functions["threeBoolOp"]?.ifs?.singleOrNull()?.condition as? BinaryOperator
        assertNotNull(threeBoolOpCondition)
        assertEquals("and", threeBoolOpCondition.operatorCode)
        assertEquals("a", (threeBoolOpCondition.lhs as? Reference)?.name?.localName)
        val threeBoolOpConditionRhs = threeBoolOpCondition.rhs as? BinaryOperator
        assertNotNull(threeBoolOpConditionRhs)
        assertEquals("and", threeBoolOpConditionRhs.operatorCode)
        assertEquals(true, (threeBoolOpConditionRhs.lhs as? Literal<*>)?.value)
        assertEquals("b", (threeBoolOpConditionRhs.rhs as? Reference)?.name?.localName)

        val threeBoolOpNoBoolCondition =
            result.functions["threeBoolOpNoBool"]?.ifs?.singleOrNull()?.condition as? BinaryOperator
        assertNotNull(threeBoolOpNoBoolCondition)
        assertEquals("and", threeBoolOpNoBoolCondition.operatorCode)
        assertEquals("a", (threeBoolOpNoBoolCondition.lhs as? Reference)?.name?.localName)
        val threeBoolOpNoBoolConditionRhs = threeBoolOpNoBoolCondition.rhs as? BinaryOperator
        assertNotNull(threeBoolOpNoBoolConditionRhs)
        assertEquals("and", threeBoolOpNoBoolConditionRhs.operatorCode)
        assertEquals(true, (threeBoolOpNoBoolConditionRhs.lhs as? Literal<*>)?.value)
        assertEquals("foo", (threeBoolOpNoBoolConditionRhs.rhs as? Literal<*>)?.value)

        // We expect that lhs comes first in the EOG and then the lhs of the rhs and last the rhs of
        // the rhs.
        assertContains(threeBoolOpNoBoolCondition.lhs.nextEOG, threeBoolOpNoBoolConditionRhs.lhs)
        assertContains(threeBoolOpNoBoolConditionRhs.lhs.nextEOG, threeBoolOpNoBoolConditionRhs.rhs)

        val nestedBoolOpDifferentOp =
            result.functions["nestedBoolOpDifferentOp"]?.ifs?.singleOrNull()?.condition
                as? BinaryOperator
        assertNotNull(nestedBoolOpDifferentOp)
        assertEquals("or", nestedBoolOpDifferentOp.operatorCode)
        assertEquals("b", (nestedBoolOpDifferentOp.rhs as? Reference)?.name?.localName)
        val nestedBoolOpDifferentOpLhs = nestedBoolOpDifferentOp.lhs as? BinaryOperator
        assertNotNull(nestedBoolOpDifferentOpLhs)
        assertEquals("and", nestedBoolOpDifferentOpLhs.operatorCode)
        assertEquals(true, (nestedBoolOpDifferentOpLhs.rhs as? Literal<*>)?.value)
        assertEquals("a", (nestedBoolOpDifferentOpLhs.lhs as? Reference)?.name?.localName)

        // We expect that lhs of the "and" comes first in the EOG and then the rhs of the "and",
        // then we evaluate the whole "and" and last the rhs of the "or".
        assertContains(nestedBoolOpDifferentOpLhs.lhs.nextEOG, nestedBoolOpDifferentOpLhs.rhs)
        assertContains(nestedBoolOpDifferentOpLhs.rhs.nextEOG, nestedBoolOpDifferentOpLhs)
        assertContains(nestedBoolOpDifferentOpLhs.nextEOG, nestedBoolOpDifferentOp.rhs)

        val nestedBoolOpDifferentOp2 =
            result.functions["nestedBoolOpDifferentOp2"]?.ifs?.singleOrNull()?.condition
                as? BinaryOperator
        assertNotNull(nestedBoolOpDifferentOp2)
        assertEquals("or", nestedBoolOpDifferentOp2.operatorCode)
        assertEquals("a", (nestedBoolOpDifferentOp2.lhs as? Reference)?.name?.localName)
        val nestedBoolOpDifferentOp2Rhs = nestedBoolOpDifferentOp2.rhs as? BinaryOperator
        assertNotNull(nestedBoolOpDifferentOp2Rhs)
        assertEquals("and", nestedBoolOpDifferentOp2Rhs.operatorCode)
        assertEquals(true, (nestedBoolOpDifferentOp2Rhs.lhs as? Literal<*>)?.value)
        assertEquals("b", (nestedBoolOpDifferentOp2Rhs.rhs as? Reference)?.name?.localName)

        // We expect that lhs comes first in the EOG and then the lhs of the rhs and last the rhs of
        // the rhs.
        assertContains(nestedBoolOpDifferentOp2.lhs.nextEOG, nestedBoolOpDifferentOp2Rhs.lhs)
        assertContains(nestedBoolOpDifferentOp2Rhs.lhs.nextEOG, nestedBoolOpDifferentOp2Rhs.rhs)
    }
}
