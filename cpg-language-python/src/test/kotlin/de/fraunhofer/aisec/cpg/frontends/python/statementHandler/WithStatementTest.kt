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
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.statements.TryStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLocalName
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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
            result.statements.filterIsInstance<Block>().filter {
                it.astParent is NamespaceDeclaration
            }

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

        val tryStatement = blockStmt.statements.filterIsInstance<TryStatement>().firstOrNull()
        assertNotNull(tryStatement)

        val tryBlock = tryStatement.tryBlock
        assertNotNull(tryBlock)
        assertEquals(2, tryBlock.statements.size)

        val enterCallAssign = tryBlock.statements.firstOrNull()
        assertIs<AssignExpression>(enterCallAssign)

        val enterCallAssignLhs = enterCallAssign.lhs.firstOrNull()
        assertIs<Reference>(enterCallAssignLhs)
        assertLocalName("file", enterCallAssignLhs)

        val enterCallAssignRhs = enterCallAssign.rhs.firstOrNull()
        assertIs<MemberCallExpression>(enterCallAssignRhs)
        assertLocalName("__enter__", enterCallAssignRhs)

        val withBodyAssign = tryBlock.statements[1]
        assertIs<AssignExpression>(withBodyAssign)

        val withBodyAssignLhs = withBodyAssign.lhs.firstOrNull()
        assertIs<Reference>(withBodyAssignLhs)
        assertLocalName("data", withBodyAssignLhs)

        val withBodyAssignRhs = withBodyAssign.rhs.firstOrNull()
        assertIs<MemberCallExpression>(withBodyAssignRhs)
        assertLocalName("read", withBodyAssignRhs)

        val finallyBlock = tryStatement.finallyBlock
        assertNotNull(finallyBlock)
        assertEquals(true, finallyBlock.isImplicit)
        assertEquals(1, finallyBlock.statements.size)

        val exitCallAssign = finallyBlock.statements.first()
        assertIs<MemberCallExpression>(exitCallAssign)
        assertLocalName("__exit__", exitCallAssign)
    }

    @Test
    fun testWithWithoutVar() {
        // Test: with open("file.txt", "r"):
        val blockStmts =
            result.statements.filterIsInstance<Block>().filter {
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

        val tryStatement = blockStmt.statements.filterIsInstance<TryStatement>().firstOrNull()
        assertNotNull(tryStatement)

        val tryBlock = tryStatement.tryBlock
        assertNotNull(tryBlock)

        val enterCall = tryBlock.statements.firstOrNull()
        assertIs<MemberCallExpression>(enterCall)
        assertLocalName("__enter__", enterCall)

        val finallyBlock = tryStatement.finallyBlock
        assertNotNull(finallyBlock)
        assertEquals(true, finallyBlock.isImplicit)
        assertEquals(1, finallyBlock.statements.size)

        val exitCallAssign = finallyBlock.statements.first()
        assertIs<MemberCallExpression>(exitCallAssign)
        assertLocalName("__exit__", exitCallAssign)
    }

    @Test
    fun testWithContextManager() {
        val testFunction = result.functions.firstOrNull { it -> it.name.contains("test_function") }
        assertNotNull(testFunction)

        val withBlock =
            testFunction.body.statements
                .filterIsInstance<Block>()
                .flatMap { it.statements.filterIsInstance<Block>() }
                .firstOrNull()
        assertNotNull(withBlock)

        val withBlockStmts = withBlock.statements
        assertEquals(2, withBlockStmts.size)

        val ctxManagerAssign = withBlockStmts.filterIsInstance<AssignExpression>().firstOrNull()
        assertNotNull(ctxManagerAssign)
        assertEquals(true, ctxManagerAssign.isImplicit)

        val ctxManagerAssignLhs = ctxManagerAssign.lhs.firstOrNull()
        assertIs<Reference>(ctxManagerAssignLhs)
        assertTrue(ctxManagerAssignLhs.name.contains("contextManager"))

        val ctxManagerAssignRhs = ctxManagerAssign.rhs.firstOrNull()
        assertLocalName("TestContextManager", ctxManagerAssignRhs)

        val tryStatement = withBlockStmts.filterIsInstance<TryStatement>().firstOrNull()
        assertNotNull(tryStatement)

        val tryBlock = tryStatement.tryBlock
        assertNotNull(tryBlock)

        val enterCall = tryBlock.statements.firstOrNull()
        assertIs<AssignExpression>(enterCall)

        val enterCallAssignLhs = enterCall.lhs.firstOrNull()
        assertIs<Reference>(enterCallAssignLhs)

        val enterCallAssignRhs = enterCall.rhs.firstOrNull()
        assertIs<MemberCallExpression>(enterCallAssignRhs)
        assertLocalName("__enter__", enterCallAssignRhs)

        val parentNameOfEnterCall = enterCallAssignRhs.name.parent
        assertEquals("TestContextManager", parentNameOfEnterCall?.localName)

        val withBodyStatement = tryBlock.statements[1]
        assertIs<CallExpression>(withBodyStatement)

        val finallyBlock = tryStatement.finallyBlock
        assertNotNull(finallyBlock)
        assertEquals(1, finallyBlock.statements.size)

        val exitCallAssign = finallyBlock.statements.firstOrNull()
        assertIs<MemberCallExpression>(exitCallAssign)
        assertLocalName("__exit__", exitCallAssign)

        val parentNameOfExitCall = exitCallAssign.name.parent
        assertEquals("TestContextManager", parentNameOfExitCall?.localName)
    }
}
