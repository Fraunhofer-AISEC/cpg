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
import de.fraunhofer.aisec.cpg.frontends.python.PythonHandler
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.dRefs
import de.fraunhofer.aisec.cpg.graph.dStatements
import de.fraunhofer.aisec.cpg.graph.statements.EmptyStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.TryStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WithStatementTest : BaseTest() {

    private lateinit var topLevel: Path
    private lateinit var result: TranslationResult

    @BeforeAll
    fun setup() {
        topLevel = Path.of("src", "test", "resources", "python")
        analyzeFile()
    }

    fun analyzeFile() {
        result =
            analyze(listOf(topLevel.resolve("with.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
    }

    @Test
    fun testWithSingleStatement() {
        // Test: with open("file.txt", "r") as file:
        val blockStmts =
            result.dStatements.filterIsInstance<Block>().filter {
                it.astParent is NamespaceDeclaration
            }

        val ref = result.dRefs["contextManager_00000000-11a2-7efe-0000-0000461d42fc"]
        assertNotNull(
            ref,
            "Expected to find a reference to the context manager with a deterministic ID.",
        )

        val blockStmt = blockStmts.firstOrNull()
        assertNotNull(blockStmt)
        assertEquals(true, blockStmt.isImplicit)

        val ctxManagerAssign =
            blockStmt.statements.filterIsInstance<AssignExpression>().firstOrNull()
        assertNotNull(ctxManagerAssign)
        assertEquals(true, ctxManagerAssign.isImplicit)

        val ctxManagerAssignLhs = ctxManagerAssign.lhs.firstOrNull()
        assertIs<Reference>(ctxManagerAssignLhs)

        val ctxManagerAssignRhs = ctxManagerAssign.rhs.firstOrNull()
        assertLocalName("open", ctxManagerAssignRhs)

        val enterCallAssign = blockStmt.statements.filterIsInstance<AssignExpression>()[1]
        assertIs<AssignExpression>(enterCallAssign)

        val tmpEnterVar = enterCallAssign.lhs.firstOrNull()
        assertIs<Reference>(tmpEnterVar)
        assertTrue(tmpEnterVar.name.localName.startsWith(PythonHandler.WITH_TMP_VAL))

        val tmpEnterVarAssignRhs = enterCallAssign.rhs.firstOrNull()
        assertIs<MemberCallExpression>(tmpEnterVarAssignRhs)
        assertLocalName("__enter__", tmpEnterVarAssignRhs)
        val base = tmpEnterVarAssignRhs.base
        assertIs<Reference>(base)
        assertRefersTo(base, ctxManagerAssignLhs.refersTo)

        val tryStatement = blockStmt.statements.filterIsInstance<TryStatement>().firstOrNull()
        assertNotNull(tryStatement)

        val tryBlock = tryStatement.tryBlock
        assertNotNull(tryBlock)
        assertEquals(2, tryBlock.statements.size)

        val enterCallAssignToCmVar = tryBlock.statements.firstOrNull()
        assertIs<AssignExpression>(enterCallAssignToCmVar)

        val enterCallAssignLhs = enterCallAssignToCmVar.lhs.firstOrNull()
        assertIs<Reference>(enterCallAssignLhs)
        assertLocalName("file", enterCallAssignLhs)

        val enterCallAssignRhs = enterCallAssignToCmVar.rhs.firstOrNull()
        assertIs<Reference>(enterCallAssignRhs)
        assertRefersTo(enterCallAssignRhs, tmpEnterVar.refersTo)

        val withBodyAssign = tryBlock.statements[1]
        assertIs<AssignExpression>(withBodyAssign)

        val withBodyAssignLhs = withBodyAssign.lhs.firstOrNull()
        assertIs<Reference>(withBodyAssignLhs)
        assertLocalName("data", withBodyAssignLhs)

        val withBodyAssignRhs = withBodyAssign.rhs.firstOrNull()
        assertIs<MemberCallExpression>(withBodyAssignRhs)
        assertLocalName("read", withBodyAssignRhs)

        val catchClause = tryStatement.catchClauses.singleOrNull()
        assertNotNull(catchClause)
        assertEquals(true, catchClause.isImplicit)
        val catchBody = catchClause.body
        assertNotNull(catchBody)
        assertEquals(1, catchBody.statements.size)

        val exitCallCatchIf = catchBody.statements.first()
        assertIs<IfStatement>(exitCallCatchIf)
        val condition = exitCallCatchIf.condition
        assertIs<UnaryOperator>(condition)
        val exitCallCatch = condition.input
        assertIs<MemberCallExpression>(exitCallCatch)
        assertLocalName("__exit__", exitCallCatch)
        assertRefersTo(exitCallCatch.base, ctxManagerAssignLhs.refersTo)

        val elseBlock = tryStatement.elseBlock
        assertNotNull(elseBlock)
        assertEquals(true, elseBlock.isImplicit)
        assertEquals(1, elseBlock.statements.size)

        val exitCallElse = elseBlock.statements.first()
        assertIs<MemberCallExpression>(exitCallElse)
        assertLocalName("__exit__", exitCallElse)
    }

    @Test
    fun testWithWithoutVar() {
        // Test: with open("file.txt", "r"):
        val blockStmts =
            result.dStatements.filterIsInstance<Block>().filter {
                it.astParent is NamespaceDeclaration
            }

        val blockStmt = blockStmts[1]
        assertNotNull(blockStmt)
        assertEquals(true, blockStmt.isImplicit)

        val ctxManagerAssign =
            blockStmt.statements.filterIsInstance<AssignExpression>().firstOrNull()
        assertNotNull(ctxManagerAssign)
        assertEquals(true, ctxManagerAssign.isImplicit)

        val ctxManagerAssignLhs = ctxManagerAssign.lhs.firstOrNull()
        assertIs<Reference>(ctxManagerAssignLhs)

        val ctxManagerAssignRhs = ctxManagerAssign.rhs.firstOrNull()
        assertLocalName("open", ctxManagerAssignRhs)

        val enterCallAssign = blockStmt.statements.filterIsInstance<AssignExpression>()[1]
        assertIs<AssignExpression>(enterCallAssign)

        val tmpEnterVar = enterCallAssign.lhs.firstOrNull()
        assertIs<Reference>(tmpEnterVar)
        assertTrue(tmpEnterVar.name.localName.startsWith(PythonHandler.WITH_TMP_VAL))

        val tmpEnterVarAssignRhs = enterCallAssign.rhs.firstOrNull()
        assertIs<MemberCallExpression>(tmpEnterVarAssignRhs)
        assertLocalName("__enter__", tmpEnterVarAssignRhs)
        val base = tmpEnterVarAssignRhs.base
        assertIs<Reference>(base)
        assertRefersTo(base, ctxManagerAssignLhs.refersTo)

        val tryStatement = blockStmt.statements.filterIsInstance<TryStatement>().firstOrNull()
        assertNotNull(tryStatement)

        val tryBlock = tryStatement.tryBlock
        assertNotNull(tryBlock)
        assertEquals(1, tryBlock.statements.size)

        val emptyStmt = tryBlock.statements.firstOrNull()
        assertIs<EmptyStatement>(emptyStmt)

        val catchClause = tryStatement.catchClauses.singleOrNull()
        assertNotNull(catchClause)
        assertEquals(true, catchClause.isImplicit)
        val catchBody = catchClause.body
        assertNotNull(catchBody)
        assertEquals(1, catchBody.statements.size)

        val exitCallCatchIf = catchBody.statements.first()
        assertIs<IfStatement>(exitCallCatchIf)
        val condition = exitCallCatchIf.condition
        assertIs<UnaryOperator>(condition)
        val exitCallCatch = condition.input
        assertIs<MemberCallExpression>(exitCallCatch)
        assertLocalName("__exit__", exitCallCatch)
        assertRefersTo(exitCallCatch.base, ctxManagerAssignLhs.refersTo)

        val elseBlock = tryStatement.elseBlock
        assertNotNull(elseBlock)
        assertEquals(true, elseBlock.isImplicit)
        assertEquals(1, elseBlock.statements.size)

        val exitCallElse = elseBlock.statements.first()
        assertIs<MemberCallExpression>(exitCallElse)
        assertLocalName("__exit__", exitCallElse)
    }

    @Test
    fun testWithContextManager() {
        val testFunction = result.dFunctions.firstOrNull { it -> it.name.contains("test_function") }
        assertNotNull(testFunction)

        val withBlock =
            testFunction.body.dStatements
                .filterIsInstance<Block>()
                .flatMap { it.statements.filterIsInstance<Block>() }
                .firstOrNull()
        assertNotNull(withBlock)

        val withBlockStmts = withBlock.statements
        assertEquals(3, withBlockStmts.size)

        val ctxManagerAssign = withBlockStmts.filterIsInstance<AssignExpression>().firstOrNull()
        assertNotNull(ctxManagerAssign)
        assertEquals(true, ctxManagerAssign.isImplicit)

        val ctxManagerAssignLhs = ctxManagerAssign.lhs.firstOrNull()
        assertIs<Reference>(ctxManagerAssignLhs)

        val ctxManagerAssignRhs = ctxManagerAssign.rhs.firstOrNull()
        assertLocalName("TestContextManager", ctxManagerAssignRhs)

        val enterCallAssign = withBlock.statements.filterIsInstance<AssignExpression>()[1]
        assertIs<AssignExpression>(enterCallAssign)

        val tmpEnterVar = enterCallAssign.lhs.firstOrNull()
        assertIs<Reference>(tmpEnterVar)
        assertTrue(tmpEnterVar.name.localName.startsWith(PythonHandler.WITH_TMP_VAL))

        val tmpEnterVarAssignRhs = enterCallAssign.rhs.firstOrNull()
        assertIs<MemberCallExpression>(tmpEnterVarAssignRhs)
        assertLocalName("__enter__", tmpEnterVarAssignRhs)
        val base = tmpEnterVarAssignRhs.base
        assertIs<Reference>(base)
        assertRefersTo(base, ctxManagerAssignLhs.refersTo)

        val parentNameOfEnterCall = tmpEnterVarAssignRhs.name.parent
        assertEquals("TestContextManager", parentNameOfEnterCall?.localName)

        val tryStatement = withBlockStmts.filterIsInstance<TryStatement>().firstOrNull()
        assertNotNull(tryStatement)

        val tryBlock = tryStatement.tryBlock
        assertNotNull(tryBlock)
        assertEquals(2, tryBlock.statements.size)

        val enterCallAssignToCmVar = tryBlock.statements.firstOrNull()
        assertIs<AssignExpression>(enterCallAssignToCmVar)

        val enterCallAssignLhs = enterCallAssignToCmVar.lhs.firstOrNull()
        assertIs<Reference>(enterCallAssignLhs)
        assertLocalName("cm", enterCallAssignLhs)

        val enterCallAssignRhs = enterCallAssignToCmVar.rhs.firstOrNull()
        assertIs<Reference>(enterCallAssignRhs)
        assertRefersTo(enterCallAssignRhs, tmpEnterVar.refersTo)

        val withBodyStatement = tryBlock.statements[1]
        assertIs<CallExpression>(withBodyStatement)

        val catchClause = tryStatement.catchClauses.singleOrNull()
        assertNotNull(catchClause)
        assertEquals(true, catchClause.isImplicit)
        val catchBody = catchClause.body
        assertNotNull(catchBody)
        assertEquals(1, catchBody.statements.size)

        val exitCallCatchIf = catchBody.statements.first()
        assertIs<IfStatement>(exitCallCatchIf)
        val condition = exitCallCatchIf.condition
        assertIs<UnaryOperator>(condition)
        val exitCallCatch = condition.input
        assertIs<MemberCallExpression>(exitCallCatch)
        assertLocalName("__exit__", exitCallCatch)
        assertRefersTo(exitCallCatch.base, ctxManagerAssignLhs.refersTo)
        val parentNameOfExitCallCatch = exitCallCatch.name.parent
        assertEquals("TestContextManager", parentNameOfExitCallCatch?.localName)

        val elseBlock = tryStatement.elseBlock
        assertNotNull(elseBlock)
        assertEquals(true, elseBlock.isImplicit)
        assertEquals(1, elseBlock.statements.size)

        val exitCallElse = elseBlock.statements.first()
        assertIs<MemberCallExpression>(exitCallElse)
        assertLocalName("__exit__", exitCallElse)
        val parentNameOfExitCallElse = exitCallElse.name.parent
        assertEquals("TestContextManager", parentNameOfExitCallElse?.localName)
    }

    @Test
    fun testMultiple() {
        val testFunction = result.dFunctions.firstOrNull { it -> it.name.contains("test_multiple") }
        assertNotNull(testFunction)

        val withBlock =
            testFunction.body.dStatements
                .filterIsInstance<Block>()
                .flatMap { it.statements.filterIsInstance<Block>() }
                .firstOrNull()
        assertNotNull(withBlock)

        val withBlockStmts = withBlock.statements
        assertEquals(3, withBlockStmts.size)

        // Test the first block containing "a"
        val ctxManagerAssignA = withBlockStmts.filterIsInstance<AssignExpression>().firstOrNull()
        assertNotNull(ctxManagerAssignA)
        assertEquals(true, ctxManagerAssignA.isImplicit)

        val ctxManagerAssignALhs = ctxManagerAssignA.lhs.firstOrNull()
        assertIs<Reference>(ctxManagerAssignALhs)

        val ctxManagerAssignARhs = ctxManagerAssignA.rhs.firstOrNull()
        assertLocalName("A", ctxManagerAssignARhs)

        val enterCallAssignA = withBlock.statements.filterIsInstance<AssignExpression>()[1]
        assertIs<AssignExpression>(enterCallAssignA)

        val tmpEnterVarA = enterCallAssignA.lhs.firstOrNull()
        assertIs<Reference>(tmpEnterVarA)
        assertTrue(tmpEnterVarA.name.localName.startsWith(PythonHandler.WITH_TMP_VAL))

        val tmpEnterVarAssignARhs = enterCallAssignA.rhs.firstOrNull()
        assertIs<MemberCallExpression>(tmpEnterVarAssignARhs)
        assertLocalName("__enter__", tmpEnterVarAssignARhs)
        val baseA = tmpEnterVarAssignARhs.base
        assertIs<Reference>(baseA)
        assertRefersTo(baseA, ctxManagerAssignALhs.refersTo)

        val tryStatementA = withBlockStmts.filterIsInstance<TryStatement>().firstOrNull()
        assertNotNull(tryStatementA)

        val tryBlockA = tryStatementA.tryBlock
        assertNotNull(tryBlockA)
        assertEquals(4, tryBlockA.statements.size)

        val enterCallAssignToA = tryBlockA.statements.firstOrNull()
        assertIs<AssignExpression>(enterCallAssignToA)

        val enterCallAssignALhs = enterCallAssignToA.lhs.firstOrNull()
        assertIs<Reference>(enterCallAssignALhs)
        assertLocalName("a", enterCallAssignALhs)

        val enterCallAssignARhs = enterCallAssignToA.rhs.firstOrNull()
        assertIs<Reference>(enterCallAssignARhs)
        assertRefersTo(enterCallAssignARhs, tmpEnterVarA.refersTo)

        val tryStatementB = tryBlockA.statements[3]
        assertIs<TryStatement>(tryStatementB)

        val catchClauseA = tryStatementA.catchClauses.singleOrNull()
        assertNotNull(catchClauseA)
        assertEquals(true, catchClauseA.isImplicit)
        val catchBodyA = catchClauseA.body
        assertNotNull(catchBodyA)
        assertEquals(1, catchBodyA.statements.size)

        val exitCallCatchAIf = catchBodyA.statements.first()
        assertIs<IfStatement>(exitCallCatchAIf)
        val conditionA = exitCallCatchAIf.condition
        assertIs<UnaryOperator>(conditionA)
        val exitCallCatchA = conditionA.input
        assertIs<MemberCallExpression>(exitCallCatchA)
        assertLocalName("__exit__", exitCallCatchA)
        assertRefersTo(exitCallCatchA.base, ctxManagerAssignALhs.refersTo)

        val elseBlockA = tryStatementA.elseBlock
        assertNotNull(elseBlockA)
        assertEquals(true, elseBlockA.isImplicit)
        assertEquals(1, elseBlockA.statements.size)

        val exitCallElseA = elseBlockA.statements.first()
        assertIs<MemberCallExpression>(exitCallElseA)
        assertLocalName("__exit__", exitCallElseA)

        // Test the second block containing "b"
        val ctxManagerAssignB = tryBlockA.statements.filterIsInstance<AssignExpression>()[1]
        assertNotNull(ctxManagerAssignB)
        assertEquals(true, ctxManagerAssignB.isImplicit)

        val ctxManagerAssignBLhs = ctxManagerAssignB.lhs.firstOrNull()
        assertIs<Reference>(ctxManagerAssignBLhs)

        val ctxManagerAssignBRhs = ctxManagerAssignB.rhs.firstOrNull()
        assertLocalName("B", ctxManagerAssignBRhs)

        val enterCallAssignB = tryBlockA.statements.filterIsInstance<AssignExpression>()[2]
        assertIs<AssignExpression>(enterCallAssignB)

        val tmpEnterVarB = enterCallAssignB.lhs.firstOrNull()
        assertIs<Reference>(tmpEnterVarB)
        assertTrue(tmpEnterVarB.name.localName.startsWith(PythonHandler.WITH_TMP_VAL))

        val tmpEnterVarBAssignRhs = enterCallAssignB.rhs.firstOrNull()
        assertIs<MemberCallExpression>(tmpEnterVarBAssignRhs)
        assertLocalName("__enter__", tmpEnterVarBAssignRhs)
        val base = tmpEnterVarBAssignRhs.base
        assertIs<Reference>(base)
        assertRefersTo(base, ctxManagerAssignBLhs.refersTo)

        val tryBlockB = tryStatementB.tryBlock
        assertNotNull(tryBlockB)
        assertEquals(4, tryBlockB.statements.size)

        val enterCallAssignToB = tryBlockB.statements.firstOrNull()
        assertIs<AssignExpression>(enterCallAssignToB)

        val enterCallAssignBLhs = enterCallAssignToB.lhs.firstOrNull()
        assertIs<Reference>(enterCallAssignBLhs)
        assertLocalName("b", enterCallAssignBLhs)

        val enterCallAssignBRhs = enterCallAssignToB.rhs.firstOrNull()
        assertIs<Reference>(enterCallAssignBRhs)
        assertRefersTo(enterCallAssignBRhs, tmpEnterVarB.refersTo)

        val tryStatementC = tryBlockB.statements[3]
        assertIs<TryStatement>(tryStatementC)

        val catchClauseB = tryStatementB.catchClauses.singleOrNull()
        assertNotNull(catchClauseB)
        assertEquals(true, catchClauseB.isImplicit)
        val catchBodyB = catchClauseB.body
        assertNotNull(catchBodyB)
        assertEquals(1, catchBodyB.statements.size)

        val exitCallCatchBIf = catchBodyB.statements.first()
        assertIs<IfStatement>(exitCallCatchBIf)
        val conditionB = exitCallCatchBIf.condition
        assertIs<UnaryOperator>(conditionB)
        val exitCallCatchB = conditionB.input
        assertIs<MemberCallExpression>(exitCallCatchB)
        assertLocalName("__exit__", exitCallCatchB)
        assertRefersTo(exitCallCatchB.base, ctxManagerAssignBLhs.refersTo)

        val elseBlockB = tryStatementB.elseBlock
        assertNotNull(elseBlockB)
        assertEquals(true, elseBlockB.isImplicit)
        assertEquals(1, elseBlockB.statements.size)

        val exitCallElseB = elseBlockB.statements.first()
        assertIs<MemberCallExpression>(exitCallElseB)
        assertLocalName("__exit__", exitCallElseB)

        // Test the third block containing "c"
        val ctxManagerAssignC = tryBlockB.statements.filterIsInstance<AssignExpression>()[1]
        assertNotNull(ctxManagerAssignC)
        assertEquals(true, ctxManagerAssignC.isImplicit)

        val ctxManagerAssignCLhs = ctxManagerAssignC.lhs.firstOrNull()
        assertIs<Reference>(ctxManagerAssignCLhs)

        val ctxManagerAssignCRhs = ctxManagerAssignC.rhs.firstOrNull()
        assertLocalName("C", ctxManagerAssignCRhs)

        val enterCallAssignC = tryBlockB.statements.filterIsInstance<AssignExpression>()[2]
        assertIs<AssignExpression>(enterCallAssignC)

        val tmpEnterVarC = enterCallAssignC.lhs.firstOrNull()
        assertIs<Reference>(tmpEnterVarC)
        assertTrue(tmpEnterVarC.name.localName.startsWith(PythonHandler.WITH_TMP_VAL))

        val tmpEnterVarCAssignRhs = enterCallAssignC.rhs.firstOrNull()
        assertIs<MemberCallExpression>(tmpEnterVarCAssignRhs)
        assertLocalName("__enter__", tmpEnterVarCAssignRhs)
        val baseC = tmpEnterVarCAssignRhs.base
        assertIs<Reference>(baseC)
        assertRefersTo(baseC, ctxManagerAssignCLhs.refersTo)

        val tryBlockC = tryStatementC.tryBlock
        assertNotNull(tryBlockC)
        assertEquals(2, tryBlockC.statements.size)

        val enterCallAssignToC = tryBlockC.statements.firstOrNull()
        assertIs<AssignExpression>(enterCallAssignToC)

        val enterCallAssignCLhs = enterCallAssignToC.lhs.firstOrNull()
        assertIs<Reference>(enterCallAssignCLhs)
        assertLocalName("c", enterCallAssignCLhs)

        val enterCallAssignCRhs = enterCallAssignToC.rhs.firstOrNull()
        assertIs<Reference>(enterCallAssignCRhs)
        assertRefersTo(enterCallAssignCRhs, tmpEnterVarC.refersTo)

        val whileBody = tryBlockC.statements[1]
        assertIs<CallExpression>(whileBody)
        assertLocalName("doSomething", whileBody)

        val catchClauseC = tryStatementC.catchClauses.singleOrNull()
        assertNotNull(catchClauseC)
        assertEquals(true, catchClauseC.isImplicit)
        val catchBodyC = catchClauseC.body
        assertNotNull(catchBodyC)
        assertEquals(1, catchBodyC.statements.size)

        val exitCallCatchCIf = catchBodyC.statements.first()
        assertIs<IfStatement>(exitCallCatchCIf)
        val conditionC = exitCallCatchCIf.condition
        assertIs<UnaryOperator>(conditionC)
        val exitCallCatchC = conditionC.input
        assertIs<MemberCallExpression>(exitCallCatchC)
        assertLocalName("__exit__", exitCallCatchC)
        assertRefersTo(exitCallCatchC.base, ctxManagerAssignCLhs.refersTo)

        val elseBlockC = tryStatementC.elseBlock
        assertNotNull(elseBlockC)
        assertEquals(true, elseBlockC.isImplicit)
        assertEquals(1, elseBlockC.statements.size)

        val exitCallElseC = elseBlockC.statements.first()
        assertIs<MemberCallExpression>(exitCallElseC)
        assertLocalName("__exit__", exitCallElseC)
    }
}
