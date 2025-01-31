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
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.KeyValueExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertNotRefersTo
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.nio.file.Path
import kotlin.test.*

class ExpressionHandlerTest {

    @Test
    fun testComprehensionExpressionTuple() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("comprehension.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val tupleComp = result.functions["tupleComp"]
        assertNotNull(tupleComp)

        val body = tupleComp.body
        assertIs<Block>(body)
        val tupleAsVariableAssignment = body.statements[0]
        assertIs<AssignExpression>(tupleAsVariableAssignment)
        val tupleAsVariable = tupleAsVariableAssignment.rhs[0]
        assertIs<CollectionComprehension>(tupleAsVariable)
        val barCall = tupleAsVariable.statement
        assertIs<CallExpression>(barCall)
        assertLocalName("bar", barCall)
        val argK = barCall.arguments[0]
        assertIs<Reference>(argK)
        assertLocalName("k", argK)
        val argV = barCall.arguments[1]
        assertIs<Reference>(argV)
        assertLocalName("v", argV)
        assertEquals(1, tupleAsVariable.comprehensionExpressions.size)
        val initializerListExpression = tupleAsVariable.comprehensionExpressions[0].variable
        assertIs<InitializerListExpression>(initializerListExpression)
        val variableK = initializerListExpression.initializers[0]
        assertIs<Reference>(variableK)
        assertLocalName("k", variableK)
        val variableV = initializerListExpression.initializers[1]
        assertIs<Reference>(variableV)
        assertLocalName("v", variableV)

        // Check that the declarations exist for the variables k and v
        val declK = variableK.refersTo
        assertIs<VariableDeclaration>(declK)
        assertIs<LocalScope>(declK.scope)
        assertEquals(tupleAsVariable, declK.scope?.astNode)
        assertRefersTo(argK, declK)
        val declV = variableV.refersTo
        assertIs<VariableDeclaration>(declV)
        assertIs<LocalScope>(declV.scope)
        assertEquals(tupleAsVariable, declV.scope?.astNode)
        assertRefersTo(argV, declV)
    }

    @Test
    fun testListComprehensions() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("comprehension.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val listComp = result.functions["listComp"]
        assertNotNull(listComp)

        val body = listComp.body
        assertIs<Block>(body)
        val singleWithIfAssignment = body.statements[0]
        assertIs<AssignExpression>(singleWithIfAssignment)
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithIf)
        val fooCall = singleWithIf.statement
        assertIs<CallExpression>(fooCall)
        val usageI = fooCall.arguments[0]
        assertIs<Reference>(usageI)
        assertEquals(1, singleWithIf.comprehensionExpressions.size)
        val variableI = singleWithIf.comprehensionExpressions[0].variable
        assertIs<Reference>(variableI)
        assertLocalName("i", variableI)
        val declI = variableI.refersTo
        assertIs<VariableDeclaration>(declI)
        assertEquals(singleWithIf, declI.scope?.astNode)
        assertIs<Reference>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithIf.comprehensionExpressions[0].iterable)
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(ifPredicate)
        assertEquals("==", ifPredicate.operatorCode)
        assertRefersTo(usageI, declI)

        val fooIOutside = body.statements[4]
        assertIs<CallExpression>(fooIOutside)
        val outsideI = fooIOutside.arguments[0]
        assertIs<Reference>(outsideI)
        assertLocalName("i", outsideI)
        assertNotRefersTo(outsideI, declI)

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(singleWithoutIfAssignment)
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithoutIf)
        assertIs<CallExpression>(singleWithoutIf.statement)
        assertEquals(1, singleWithoutIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithoutIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithoutIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithoutIf.comprehensionExpressions[0].iterable)
        assertNull(singleWithoutIf.comprehensionExpressions[0].predicate)

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(singleWithDoubleIfAssignment)
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithDoubleIf)
        assertIs<CallExpression>(singleWithDoubleIf.statement)
        assertEquals(1, singleWithDoubleIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithDoubleIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithDoubleIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithDoubleIf.comprehensionExpressions[0].iterable)
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(doubleIfPredicate)
        assertEquals("and", doubleIfPredicate.operatorCode)

        val doubleAssignment = body.statements[3] as? AssignExpression
        assertIs<AssignExpression>(doubleAssignment)
        val double = doubleAssignment.rhs[0] as? CollectionComprehension
        assertNotNull(double)
        assertIs<CallExpression>(double.statement)
        assertEquals(2, double.comprehensionExpressions.size)
        // TODO: Add tests on the comprehension expressions
    }

    @Test
    fun testSetComprehensions() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("comprehension.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val listComp = result.functions["setComp"]
        assertNotNull(listComp)

        val body = listComp.body as? Block
        assertNotNull(body)
        val singleWithIfAssignment = body.statements[0]
        assertIs<AssignExpression>(singleWithIfAssignment)
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithIf)
        assertIs<CallExpression>(singleWithIf.statement)
        assertEquals(1, singleWithIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithIf.comprehensionExpressions[0].iterable)
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(ifPredicate)
        assertEquals("==", ifPredicate.operatorCode)

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(singleWithoutIfAssignment)
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithoutIf)
        assertIs<CallExpression>(singleWithoutIf.statement)
        assertEquals(1, singleWithoutIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithoutIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithoutIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithoutIf.comprehensionExpressions[0].iterable)
        assertNull(singleWithoutIf.comprehensionExpressions[0].predicate)

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(singleWithDoubleIfAssignment)
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithDoubleIf)
        assertIs<CallExpression>(singleWithDoubleIf.statement)
        assertEquals(1, singleWithDoubleIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithDoubleIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithDoubleIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithDoubleIf.comprehensionExpressions[0].iterable)
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(doubleIfPredicate)
        assertEquals("and", doubleIfPredicate.operatorCode)

        val doubleAssignment = body.statements[3]
        assertIs<AssignExpression>(doubleAssignment)
        val double = doubleAssignment.rhs[0]
        assertIs<CollectionComprehension>(double)
        assertIs<CallExpression>(double.statement)
        assertEquals(2, double.comprehensionExpressions.size)
    }

    @Test
    fun testDictComprehensions() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("comprehension.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val listComp = result.functions["dictComp"]
        assertNotNull(listComp)

        val body = listComp.body as? Block
        assertNotNull(body)
        val singleWithIfAssignment = body.statements[0]
        assertIs<AssignExpression>(singleWithIfAssignment)
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithIf)
        var statement = singleWithIf.statement
        assertIs<KeyValueExpression>(statement)
        assertIs<Reference>(statement.key)
        assertLocalName("i", statement.key)
        assertIs<CallExpression>(statement.value)
        assertEquals(1, singleWithIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithIf.comprehensionExpressions[0].iterable)
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(ifPredicate)
        assertEquals("==", ifPredicate.operatorCode)

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(singleWithoutIfAssignment)
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithoutIf)
        statement = singleWithIf.statement
        assertIs<KeyValueExpression>(statement)
        assertIs<Reference>(statement.key)
        assertLocalName("i", statement.key)
        assertIs<CallExpression>(statement.value)
        assertEquals(1, singleWithoutIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithoutIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithoutIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithoutIf.comprehensionExpressions[0].iterable)
        assertNull(singleWithoutIf.comprehensionExpressions[0].predicate)

        val singleWithDoubleIfAssignment = body.statements[2]
        assertIs<AssignExpression>(singleWithDoubleIfAssignment)
        val singleWithDoubleIf = singleWithDoubleIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithDoubleIf)
        statement = singleWithIf.statement
        assertIs<KeyValueExpression>(statement)
        assertIs<Reference>(statement.key)
        assertLocalName("i", statement.key)
        assertIs<CallExpression>(statement.value)
        assertEquals(1, singleWithDoubleIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithDoubleIf.comprehensionExpressions[0].variable)
        assertIs<Reference>(singleWithDoubleIf.comprehensionExpressions[0].iterable)
        assertLocalName("x", singleWithDoubleIf.comprehensionExpressions[0].iterable)
        val doubleIfPredicate = singleWithDoubleIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(doubleIfPredicate)
        assertEquals("and", doubleIfPredicate.operatorCode)

        val doubleAssignment = body.statements[3] as? AssignExpression
        assertIs<AssignExpression>(doubleAssignment)
        val double = doubleAssignment.rhs[0] as? CollectionComprehension
        assertNotNull(double)
        statement = singleWithIf.statement
        assertIs<KeyValueExpression>(statement)
        assertIs<Reference>(statement.key)
        assertLocalName("i", statement.key)
        assertIs<CallExpression>(statement.value)
        assertEquals(2, double.comprehensionExpressions.size)
    }

    @Test
    fun testGeneratorExpr() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("comprehension.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val listComp = result.functions["generator"]
        assertNotNull(listComp)

        val body = listComp.body as? Block
        assertNotNull(body)
        val singleWithIfAssignment = body.statements[0]
        assertIs<AssignExpression>(singleWithIfAssignment)
        val singleWithIf = singleWithIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithIf)
        assertIs<BinaryOperator>(singleWithIf.statement)
        assertEquals(1, singleWithIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithIf.comprehensionExpressions[0].variable)
        assertIs<CallExpression>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("range", singleWithIf.comprehensionExpressions[0].iterable)
        val ifPredicate = singleWithIf.comprehensionExpressions[0].predicate
        assertIs<BinaryOperator>(ifPredicate)
        assertEquals("==", ifPredicate.operatorCode)

        val singleWithoutIfAssignment = body.statements[1]
        assertIs<AssignExpression>(singleWithoutIfAssignment)
        val singleWithoutIf = singleWithoutIfAssignment.rhs[0]
        assertIs<CollectionComprehension>(singleWithoutIf)
        assertIs<BinaryOperator>(singleWithoutIf.statement)
        assertEquals(1, singleWithoutIf.comprehensionExpressions.size)
        assertLocalName("i", singleWithoutIf.comprehensionExpressions[0].variable)
        assertIs<CallExpression>(singleWithIf.comprehensionExpressions[0].iterable)
        assertLocalName("range", singleWithIf.comprehensionExpressions[0].iterable)
        assertNull(singleWithoutIf.comprehensionExpressions[0].predicate)
    }

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
}
