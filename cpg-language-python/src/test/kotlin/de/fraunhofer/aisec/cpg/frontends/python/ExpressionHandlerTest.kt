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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
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

        val twoBoolOpCondition = result.functions["twoBoolOp"]?.ifs?.singleOrNull()?.condition
        assertIs<BinaryOperator>(twoBoolOpCondition)
        assertEquals("and", twoBoolOpCondition.operatorCode)
        assertLocalName("a", twoBoolOpCondition.lhs)
        assertLiteralValue(true, twoBoolOpCondition.rhs)

        // We expect that lhs comes first in the EOG and then the rhs.
        assertContains(twoBoolOpCondition.lhs.nextEOG, twoBoolOpCondition.rhs)

        val threeBoolOpCondition = result.functions["threeBoolOp"]?.ifs?.singleOrNull()?.condition
        assertIs<BinaryOperator>(threeBoolOpCondition)
        assertEquals("and", threeBoolOpCondition.operatorCode)
        assertLocalName("a", threeBoolOpCondition.lhs)
        val threeBoolOpConditionRhs = threeBoolOpCondition.rhs
        assertIs<BinaryOperator>(threeBoolOpConditionRhs)
        assertEquals("and", threeBoolOpConditionRhs.operatorCode)
        assertLiteralValue(true, threeBoolOpConditionRhs.lhs)
        assertLocalName("b", threeBoolOpConditionRhs.rhs)

        val threeBoolOpNoBoolCondition =
            result.functions["threeBoolOpNoBool"]?.ifs?.singleOrNull()?.condition
        assertIs<BinaryOperator>(threeBoolOpNoBoolCondition)
        assertEquals("and", threeBoolOpNoBoolCondition.operatorCode)
        assertLocalName("a", threeBoolOpNoBoolCondition.lhs)
        val threeBoolOpNoBoolConditionRhs = threeBoolOpNoBoolCondition.rhs
        assertIs<BinaryOperator>(threeBoolOpNoBoolConditionRhs)
        assertEquals("and", threeBoolOpNoBoolConditionRhs.operatorCode)
        assertLiteralValue(true, threeBoolOpNoBoolConditionRhs.lhs)
        assertLiteralValue("foo", threeBoolOpNoBoolConditionRhs.rhs)

        // We expect that lhs comes first in the EOG and then the lhs of the rhs and last the rhs of
        // the rhs.
        assertContains(threeBoolOpNoBoolCondition.lhs.nextEOG, threeBoolOpNoBoolConditionRhs.lhs)
        assertContains(threeBoolOpNoBoolConditionRhs.lhs.nextEOG, threeBoolOpNoBoolConditionRhs.rhs)

        val nestedBoolOpDifferentOp =
            result.functions["nestedBoolOpDifferentOp"]?.ifs?.singleOrNull()?.condition

        assertIs<BinaryOperator>(nestedBoolOpDifferentOp)
        assertEquals("or", nestedBoolOpDifferentOp.operatorCode)
        assertLocalName("b", nestedBoolOpDifferentOp.rhs)
        val nestedBoolOpDifferentOpLhs = nestedBoolOpDifferentOp.lhs
        assertIs<BinaryOperator>(nestedBoolOpDifferentOpLhs)
        assertEquals("and", nestedBoolOpDifferentOpLhs.operatorCode)
        assertLiteralValue(true, nestedBoolOpDifferentOpLhs.rhs)
        assertLocalName("a", nestedBoolOpDifferentOpLhs.lhs)

        // We expect that lhs of the "and" comes first in the EOG and then the rhs of the "and",
        // then we evaluate the whole "and" and last the rhs of the "or".
        assertContains(nestedBoolOpDifferentOpLhs.lhs.nextEOG, nestedBoolOpDifferentOpLhs.rhs)
        assertContains(nestedBoolOpDifferentOpLhs.rhs.nextEOG, nestedBoolOpDifferentOpLhs)
        assertContains(nestedBoolOpDifferentOpLhs.nextEOG, nestedBoolOpDifferentOp.rhs)

        val nestedBoolOpDifferentOp2 =
            result.functions["nestedBoolOpDifferentOp2"]?.ifs?.singleOrNull()?.condition
        assertIs<BinaryOperator>(nestedBoolOpDifferentOp2)
        assertEquals("or", nestedBoolOpDifferentOp2.operatorCode)
        assertLocalName("a", nestedBoolOpDifferentOp2.lhs)
        val nestedBoolOpDifferentOp2Rhs = nestedBoolOpDifferentOp2.rhs
        assertIs<BinaryOperator>(nestedBoolOpDifferentOp2Rhs)
        assertEquals("and", nestedBoolOpDifferentOp2Rhs.operatorCode)
        assertLiteralValue(true, nestedBoolOpDifferentOp2Rhs.lhs)
        assertLocalName("b", nestedBoolOpDifferentOp2Rhs.rhs)

        // We expect that lhs comes first in the EOG and then the lhs of the rhs and last the rhs of
        // the rhs.
        assertContains(nestedBoolOpDifferentOp2.lhs.nextEOG, nestedBoolOpDifferentOp2Rhs.lhs)
        assertContains(nestedBoolOpDifferentOp2Rhs.lhs.nextEOG, nestedBoolOpDifferentOp2Rhs.rhs)
    }

    @Test
    fun testAssignmentsMultiReturn() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("assignments.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        // `a, b = 1, 2`
        val refA = result.refs["a"]
        assertIs<Reference>(refA)
        val refB = result.refs["b"]
        assertIs<Reference>(refB)

        val assignment = result.assigns.firstOrNull()
        assertIs<AssignExpression>(assignment)
        assertEquals(2, assignment.assignments.size)

        // extract both assignments
        val assignmentA = assignment.assignments[0]
        assertIs<Assignment>(assignmentA)
        val assignmentB = assignment.assignments[1]
        assertIs<Assignment>(assignmentB)

        // check that the assignments assign the correct value and assign to the correct reference
        assertLiteralValue(1, assignmentA.value)
        assertLiteralValue(2, assignmentB.value)
        assertEquals(refA, assignmentA.target)
        assertEquals(refB, assignmentB.target)

        /*
        ```python
        def foo():
            return (1, 2)
        fooA, fooB = foo()
        ```
         */
        val refFooA = result.refs["fooA"]
        assertIs<Reference>(refFooA)
        val refFooB = result.refs["fooB"]
        assertIs<Reference>(refFooB)

        val assignmentFoo = result.assigns.getOrNull(1)
        assertIs<AssignExpression>(assignmentFoo)
        assertEquals(2, assignmentFoo.assignments.size)

        // extract both assignments
        val assignmentFooA = assignmentFoo.assignments[0]
        assertIs<Assignment>(assignmentFooA)
        val assignmentFooB = assignmentFoo.assignments[1]
        assertIs<Assignment>(assignmentFooB)

        // check that the assignments assign the correct value and assign to the correct reference
        assertLiteralValue(3, assignmentFooA.value)
        assertLiteralValue(4, assignmentFooB.value)
        assertEquals(refFooA, assignmentFooA.target)
        assertEquals(refFooB, assignmentFooB.target)

        // check that there is only one call to `foo` modeled
        assertEquals(1, result.calls.filter { it.invokes == result.functions["foo"] }.size)
    }
}
