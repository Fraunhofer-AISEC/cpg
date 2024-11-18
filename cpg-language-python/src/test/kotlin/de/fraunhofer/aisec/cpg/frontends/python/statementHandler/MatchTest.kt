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
package de.fraunhofer.aisec.cpg.frontends.python.statementHandler

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.statements.BreakStatement
import de.fraunhofer.aisec.cpg.graph.statements.CaseStatement
import de.fraunhofer.aisec.cpg.graph.statements.DefaultStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.switches
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchTest {
    private lateinit var topLevel: Path
    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        topLevel = Path.of("src", "test", "resources", "python")
        result =
            analyze(listOf(topLevel.resolve("match.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
    }

    @Test
    fun testMatchSingleton() {
        val func = result.functions["matchSingleton"]
        assertNotNull(func)

        val switchStatement = func.switches.singleOrNull()
        assertNotNull(switchStatement)

        assertLocalName("x", switchStatement.selector)
        assertIs<Reference>(switchStatement.selector)
        val paramX = func.parameters.singleOrNull()
        assertNotNull(paramX)
        assertRefersTo(switchStatement.selector, paramX)

        val statementBlock = switchStatement.statement
        assertIs<Block>(statementBlock)
        assertEquals(3, statementBlock.statements.size)
        val caseSingleton = statementBlock[0]
        assertIs<CaseStatement>(caseSingleton)
        val singletonCheck = caseSingleton.caseExpression
        assertIs<BinaryOperator>(singletonCheck)
        assertEquals("===", singletonCheck.operatorCode)
        assertRefersTo(singletonCheck.lhs, paramX)
        val singletonRhs = singletonCheck.rhs
        assertIs<Literal<*>>(singletonRhs)
        assertNull(singletonRhs.value)
        assertIs<BreakStatement>(statementBlock[2])
    }

    @Test
    fun testMatchValue() {
        val func = result.functions["matchValue"]
        assertNotNull(func)

        val switchStatement = func.switches.singleOrNull()
        assertNotNull(switchStatement)

        assertLocalName("x", switchStatement.selector)
        assertIs<Reference>(switchStatement.selector)
        val paramX = func.parameters.singleOrNull()
        assertNotNull(paramX)
        assertRefersTo(switchStatement.selector, paramX)

        val statementBlock = switchStatement.statement
        assertIs<Block>(statementBlock)
        assertEquals(3, statementBlock.statements.size)
        val caseValue = statementBlock[0]
        assertIs<CaseStatement>(caseValue)
        val valueCheck = caseValue.caseExpression
        assertIs<BinaryOperator>(valueCheck)
        assertEquals("==", valueCheck.operatorCode)
        assertRefersTo(valueCheck.lhs, paramX)
        assertLiteralValue("value", valueCheck.rhs)
        assertIs<BreakStatement>(statementBlock[2])
    }

    @Test
    fun testMatchOr() {
        val func = result.functions["matchOr"]
        assertNotNull(func)

        val switchStatement = func.switches.singleOrNull()
        assertNotNull(switchStatement)

        assertLocalName("x", switchStatement.selector)
        assertIs<Reference>(switchStatement.selector)
        val paramX = func.parameters.singleOrNull()
        assertNotNull(paramX)
        assertRefersTo(switchStatement.selector, paramX)

        val statementBlock = switchStatement.statement
        assertIs<Block>(statementBlock)
        assertEquals(3, statementBlock.statements.size)
        val caseOr = statementBlock[0]
        assertIs<CaseStatement>(caseOr)
        val orExpr = caseOr.caseExpression
        assertIs<BinaryOperator>(orExpr)
        assertEquals("or", orExpr.operatorCode)
        assertIs<BinaryOperator>(orExpr.lhs)
        assertIs<BinaryOperator>(orExpr.rhs)
        assertIs<BreakStatement>(statementBlock[2])
    }

    @Test
    fun testMatchDefault() {
        val func = result.functions["matchDefault"]
        assertNotNull(func)

        val switchStatement = func.switches.singleOrNull()
        assertNotNull(switchStatement)

        assertLocalName("x", switchStatement.selector)
        assertIs<Reference>(switchStatement.selector)
        val paramX = func.parameters.singleOrNull()
        assertNotNull(paramX)
        assertRefersTo(switchStatement.selector, paramX)

        val statementBlock = switchStatement.statement
        assertIs<Block>(statementBlock)
        assertEquals(3, statementBlock.statements.size)
        val caseDefault = statementBlock[0]
        assertIs<DefaultStatement>(caseDefault)
        assertIs<BreakStatement>(statementBlock[2])
    }

    @Test
    fun testMatchGuard() {
        val func = result.functions["matchAnd"]
        assertNotNull(func)

        val switchStatement = func.switches.singleOrNull()
        assertNotNull(switchStatement)

        assertLocalName("x", switchStatement.selector)
        assertIs<Reference>(switchStatement.selector)
        val paramX = func.parameters.singleOrNull()
        assertNotNull(paramX)
        assertRefersTo(switchStatement.selector, paramX)

        val statementBlock = switchStatement.statement
        assertIs<Block>(statementBlock)
        val caseAnd = statementBlock[0]
        assertIs<CaseStatement>(caseAnd)
        val andExpr = caseAnd.caseExpression
        assertIs<BinaryOperator>(andExpr)
        assertEquals("and", andExpr.operatorCode)
        val andRhs = andExpr.rhs
        assertIs<BinaryOperator>(andRhs)
        assertEquals(">", andRhs.operatorCode)
        assertRefersTo(andRhs.lhs, paramX)
        assertLiteralValue(0L, andRhs.rhs)
        assertIs<BreakStatement>(statementBlock[2])
    }

    @Test
    fun testMatchCombined() {
        val func = result.functions["matcher"]
        assertNotNull(func)

        val switchStatement = func.switches.singleOrNull()
        assertNotNull(switchStatement)

        assertLocalName("x", switchStatement.selector)
        assertIs<Reference>(switchStatement.selector)
        val paramX = func.parameters.singleOrNull()
        assertNotNull(paramX)
        assertRefersTo(switchStatement.selector, paramX)

        val statementBlock = switchStatement.statement
        assertIs<Block>(statementBlock)
        val caseSingleton = statementBlock[0]
        assertIs<CaseStatement>(caseSingleton)
        val singletonCheck = caseSingleton.caseExpression
        assertIs<BinaryOperator>(singletonCheck)
        assertEquals("===", singletonCheck.operatorCode)
        assertRefersTo(singletonCheck.lhs, paramX)
        val singletonRhs = singletonCheck.rhs
        assertIs<Literal<*>>(singletonRhs)
        assertNull(singletonRhs.value)
        assertIs<BreakStatement>(statementBlock[2])

        val caseValue = statementBlock[3]
        assertIs<CaseStatement>(caseValue)
        val valueCheck = caseValue.caseExpression
        assertIs<BinaryOperator>(valueCheck)
        assertEquals("==", valueCheck.operatorCode)
        assertRefersTo(valueCheck.lhs, paramX)
        assertLiteralValue("value", valueCheck.rhs)
        assertIs<BreakStatement>(statementBlock[5])

        val caseAnd = statementBlock[6]
        assertIs<CaseStatement>(caseAnd)
        val andExpr = caseAnd.caseExpression
        assertIs<BinaryOperator>(andExpr)
        assertEquals("and", andExpr.operatorCode)
        val andRhs = andExpr.rhs
        assertIs<BinaryOperator>(andRhs)
        assertEquals(">", andRhs.operatorCode)
        assertRefersTo(andRhs.lhs, paramX)
        assertLiteralValue(0L, andRhs.rhs)
        assertIs<BreakStatement>(statementBlock[8])

        assertIs<CaseStatement>(statementBlock[9])
        assertIs<BreakStatement>(statementBlock[11])
        assertIs<CaseStatement>(statementBlock[12])
        assertIs<BreakStatement>(statementBlock[14])
        assertIs<CaseStatement>(statementBlock[15])
        assertIs<BreakStatement>(statementBlock[17])
        assertIs<CaseStatement>(statementBlock[18])
        assertIs<BreakStatement>(statementBlock[20])
        assertIs<CaseStatement>(statementBlock[21])
        assertIs<BreakStatement>(statementBlock[23])
        assertIs<CaseStatement>(statementBlock[24])
        assertIs<BreakStatement>(statementBlock[26])

        val caseOr = statementBlock[27]
        assertIs<CaseStatement>(caseOr)
        val orExpr = caseOr.caseExpression
        assertIs<BinaryOperator>(orExpr)
        assertEquals("or", orExpr.operatorCode)
        assertIs<BinaryOperator>(orExpr.lhs)
        assertIs<BinaryOperator>(orExpr.rhs)
        assertIs<BreakStatement>(statementBlock[29])

        val caseDefault = statementBlock[30]
        assertIs<DefaultStatement>(caseDefault)
        assertIs<BreakStatement>(statementBlock[32])
    }

    @Test
    fun testMatch2() {
        val func = result.functions["match_weird"]
        assertNotNull(func)

        val switchStatement = func.switches.singleOrNull()
        assertNotNull(switchStatement)

        assertIs<CallExpression>(switchStatement.selector)

        val statementBlock = switchStatement.statement
        assertIs<Block>(statementBlock)
        val case = statementBlock[0]
        assertIs<CaseStatement>(case)
        assertIs<ProblemExpression>(case.caseExpression)
    }
}
