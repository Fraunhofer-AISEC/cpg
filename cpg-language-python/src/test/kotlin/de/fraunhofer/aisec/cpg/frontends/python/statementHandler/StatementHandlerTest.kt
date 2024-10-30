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
import de.fraunhofer.aisec.cpg.frontends.python.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.test.*
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertResolvedType
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatementHandlerTest : BaseTest() {

    private lateinit var topLevel: Path
    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        topLevel = Path.of("src", "test", "resources", "python")
    }

    fun analyzeFile(fileName: String) {
        result =
            analyze(listOf(topLevel.resolve(fileName).toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
    }

    @Test
    fun testMatch() {
        analyzeFile("match.py")

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
        assertNotNull(singletonCheck)
        assertEquals("===", singletonCheck.operatorCode)
        assertRefersTo(singletonCheck.lhs, paramX)
        assertIs<ProblemExpression>(singletonCheck.rhs)
        assertIs<BreakStatement>(statementBlock[2])

        val caseValue = statementBlock[3]
        assertIs<CaseStatement>(caseValue)
        val valueCheck = caseValue.caseExpression
        assertIs<BinaryOperator>(valueCheck)
        assertNotNull(valueCheck)
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
        assertNotNull(orExpr)
        assertEquals("or", orExpr.operatorCode)
        assertIs<BreakStatement>(statementBlock[29])
    }

    @Test
    fun testTry() {
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("try.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val tryAll = tu.functions["tryAll"]?.trys?.singleOrNull()
        assertNotNull(tryAll)

        assertEquals(1, tryAll.tryBlock?.statements?.size)

        assertEquals(3, tryAll.catchClauses.size)
        assertLocalName("", tryAll.catchClauses[0].parameter)
        assertLocalName("e", tryAll.catchClauses[1].parameter)
        assertNull(tryAll.catchClauses[2].parameter)

        assertEquals(1, tryAll.elseBlock?.statements?.size)
        assertEquals(1, tryAll.finallyBlock?.statements?.size)

        val tryOnlyFinally = tu.functions["tryOnlyFinally"]?.trys?.singleOrNull()
        assertNotNull(tryOnlyFinally)

        assertEquals(1, tryOnlyFinally.tryBlock?.statements?.size)

        assertEquals(0, tryOnlyFinally.catchClauses.size)

        assertNull(tryOnlyFinally.elseBlock)
        assertEquals(1, tryOnlyFinally.finallyBlock?.statements?.size)

        val tryOnlyExcept = tu.functions["tryOnlyExcept"]?.trys?.singleOrNull()
        assertNotNull(tryOnlyExcept)

        assertEquals(1, tryOnlyExcept.tryBlock?.statements?.size)

        assertEquals(1, tryOnlyExcept.catchClauses.size)
        assertNull(tryOnlyExcept.catchClauses.single().parameter)

        assertNull(tryOnlyExcept.elseBlock)
        assertNull(tryOnlyExcept.finallyBlock)

        // Test EOG integrity with else block

        // All entries to the else block must come from the try block
        assertTrue(
            Util.eogConnect(
                n = tryAll.elseBlock,
                en = Util.Edge.ENTRIES,
                refs = listOf(tryAll.tryBlock)
            )
        )

        // All exits from the else block must go to the entries of the non-empty finals block
        assertTrue(
            Util.eogConnect(
                n = tryAll.elseBlock,
                en = Util.Edge.EXITS,
                refs = listOf(tryAll.finallyBlock)
            )
        )
    }

    @Test
    fun testAsync() {
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("async.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val myFunc = tu.functions["my_func"]
        assertNotNull(myFunc)
        assertEquals(1, myFunc.parameters.size)

        val myOtherFunc = tu.functions["my_other_func"]
        assertNotNull(myOtherFunc)
        assertEquals(1, myOtherFunc.parameters.size)
    }

    @Test
    fun testOperatorOverload() {
        analyzeFile("operator.py")

        with(result) {
            val numberType = assertResolvedType("operator.Number")
            val strType = assertResolvedType("str")

            // we should have an operator call to __add__ (+) now
            var opCall = result.operatorCalls("+").getOrNull(0)
            assertNotNull(opCall)
            assertEquals(numberType, opCall.type)

            val add = result.operators["__add__"]
            assertNotNull(add)
            assertInvokes(opCall, add)

            // ... and one to __pos__ (+)
            opCall = result.operatorCalls("+").getOrNull(1)
            assertNotNull(opCall)
            assertEquals(strType, opCall.type)

            val pos = result.operators["__pos__"]
            assertNotNull(pos)
            assertInvokes(opCall, pos)
        }
    }

    @Test
    fun testAssert() {
        analyzeFile("assert.py")

        val func = result.functions["test_assert"]
        assertNotNull(func, "Function 'test_assert' should be found")

        val assertStatement = func.body.statements.firstOrNull { it is AssertStatement }
        assertIs<AssertStatement>(assertStatement, "Assert statement should be found")

        val condition = assertStatement.condition
        assertNotNull(condition, "Assert statement should have a condition")
        assertEquals("1 == 1", condition.code, "The condition is incorrect")

        val message = assertStatement.message
        assertIs<Literal<*>>(message, "Assert statement should have a message")
        assertEquals("Test message", message.value, "The assert message is incorrect")
    }

    @Test
    fun testDeleteStatements() {
        analyzeFile("delete.py")

        val deleteExpressions = result.statements.filterIsInstance<DeleteExpression>()
        assertEquals(4, deleteExpressions.size)

        // Test for `del a`
        val deleteStmt1 = deleteExpressions[0]
        assertEquals(0, deleteStmt1.operands.size)
        assertEquals(1, deleteStmt1.additionalProblems.size)

        // Test for `del my_list[2]`
        val deleteStmt2 = deleteExpressions[1]
        assertEquals(1, deleteStmt2.operands.size)
        assertIs<SubscriptExpression>(deleteStmt2.operands.firstOrNull())
        assertTrue(deleteStmt2.additionalProblems.isEmpty())

        // Test for `del my_dict['b']`
        val deleteStmt3 = deleteExpressions[2]
        assertEquals(1, deleteStmt3.operands.size)
        assertIs<SubscriptExpression>(deleteStmt3.operands.firstOrNull())
        assertTrue(deleteStmt3.additionalProblems.isEmpty())

        // Test for `del obj.d`
        val deleteStmt4 = deleteExpressions[3]
        assertEquals(0, deleteStmt4.operands.size)
        assertEquals(1, deleteStmt4.additionalProblems.size)
    }

    @Test
    fun testTypeHints() {
        analyzeFile("type_hints.py")
        with(result) {
            // type comments
            val a = result.refs["a"]
            assertNotNull(a)
            assertEquals(assertResolvedType("int"), a.type)

            // type annotation
            val b = result.refs["b"]
            assertNotNull(b)
            assertEquals(assertResolvedType("str"), b.type)
        }
    }

    @Test
    fun testGlobal() {
        var file = topLevel.resolve("global.py").toFile()
        val result = analyze(listOf(file), topLevel, true) { it.registerLanguage<PythonLanguage>() }
        assertNotNull(result)

        // There should be three variable declarations, two local and one global
        var cVariables = result.variables("c")
        assertEquals(3, cVariables.size)

        // Our scopes do not match 1:1 to python scopes, but rather the python "global" scope is a
        // name space with the name of the file and the function scope is a block scope of the
        // function body
        var pythonGlobalScope = result.finalCtx.scopeManager.lookupScope(file.nameWithoutExtension)

        var globalC = cVariables.firstOrNull { it.scope == pythonGlobalScope }
        assertNotNull(globalC)

        var localC1 =
            cVariables.firstOrNull {
                it.scope?.astNode?.astParent?.name?.localName == "local_write"
            }
        assertNotNull(localC1)

        var localC2 =
            cVariables.firstOrNull {
                it.scope?.astNode?.astParent?.name?.localName == "error_write"
            }
        assertNotNull(localC2)

        // In global_write, all references should point to global c
        var cRefs = result.functions["global_write"]?.refs("c")
        assertNotNull(cRefs)
        cRefs.forEach { assertRefersTo(it, globalC) }

        // In global_read, all references should point to global c
        cRefs = result.functions["global_read"]?.refs("c")
        assertNotNull(cRefs)
        cRefs.forEach { assertRefersTo(it, globalC) }

        // In local_write, all references should point to local c
        cRefs = result.functions["local_write"]?.refs("c")
        assertNotNull(cRefs)
        cRefs.forEach { assertRefersTo(it, localC1) }

        // In error_write, all references will point to local c; even though the c on the right side
        // SHOULD be unresolved - but this a general shortcoming because the resolving will not take
        // the EOG into consideration (yet)
        cRefs = result.functions["error_write"]?.refs("c")
        assertNotNull(cRefs)
        cRefs.forEach { assertRefersTo(it, localC2) }
    }

    @Test
    fun testNonLocal() {
        var file = topLevel.resolve("nonlocal.py").toFile()
        val result = analyze(listOf(file), topLevel, true) { it.registerLanguage<PythonLanguage>() }
        assertNotNull(result)

        // There should be three variable declarations, two local and one global
        var cVariables = result.variables("c")
        assertEquals(3, cVariables.size)
    }
}
