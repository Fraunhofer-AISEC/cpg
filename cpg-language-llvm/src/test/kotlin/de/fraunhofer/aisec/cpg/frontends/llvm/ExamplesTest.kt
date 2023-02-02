/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.assertFullName
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.junit.jupiter.api.Tag

@Tag("llvm-examples")
class ExamplesTest {

    @Test
    fun testExceptions() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("exceptions.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val funcF = tu.byNameOrNull<FunctionDeclaration>("f")
        assertNotNull(funcF)

        val tryStatement =
            (funcF.bodyOrNull<LabelStatement>(0)?.subStatement as? CompoundStatement)
                ?.statements
                ?.firstOrNull { s -> s is TryStatement } as? TryStatement
        assertNotNull(tryStatement)
        assertEquals(2, tryStatement.tryBlock?.statements?.size)
        assertFullName(
            "_CxxThrowException",
            tryStatement.tryBlock?.statements?.get(0) as? CallExpression
        )
        assertEquals(
            "end",
            (tryStatement.tryBlock?.statements?.get(1) as? GotoStatement)
                ?.targetLabel
                ?.name
                ?.localName
        )

        assertEquals(1, tryStatement.catchClauses.size)
        val catchSwitchExpr =
            tryStatement.catchClauses[0].body?.statements?.get(0) as? DeclarationStatement
        assertNotNull(catchSwitchExpr)
        val catchswitchCall =
            (catchSwitchExpr.singleDeclaration as? VariableDeclaration)?.initializer
                as? CallExpression
        assertNotNull(catchswitchCall)
        assertFullName("llvm.catchswitch", catchswitchCall)
        val ifExceptionMatches =
            tryStatement.catchClauses[0].body?.statements?.get(1) as? IfStatement
        val matchesExceptionCall = ifExceptionMatches?.condition as? CallExpression
        assertNotNull(matchesExceptionCall)
        assertFullName("llvm.matchesCatchpad", matchesExceptionCall)
        assertEquals(
            catchSwitchExpr.singleDeclaration,
            (matchesExceptionCall.arguments[0] as DeclaredReferenceExpression).refersTo
        )
        assertEquals(null, (matchesExceptionCall.arguments[1] as Literal<*>).value)
        assertEquals(64L, (matchesExceptionCall.arguments[2] as Literal<*>).value as Long)
        assertEquals(null, (matchesExceptionCall.arguments[3] as Literal<*>).value)

        val catchBlock = ifExceptionMatches.thenStatement as? CompoundStatement
        assertNotNull(catchBlock)
        assertFullName(
            "llvm.catchpad",
            ((catchBlock.statements[0] as? DeclarationStatement)?.singleDeclaration
                    as? VariableDeclaration)
                ?.initializer as? CallExpression
        )

        val innerTry = catchBlock.statements[1] as? TryStatement
        assertNotNull(innerTry)
        assertFullName(
            "_CxxThrowException",
            innerTry.tryBlock?.statements?.get(0) as? CallExpression
        )
        assertLocalName(
            "try.cont",
            (innerTry.tryBlock?.statements?.get(1) as? GotoStatement)?.targetLabel
        )

        val innerCatchClause =
            (innerTry.catchClauses[0].body?.statements?.get(1) as? IfStatement)?.thenStatement
                as? CompoundStatement
        assertNotNull(innerCatchClause)
        assertFullName(
            "llvm.catchpad",
            ((innerCatchClause.statements[0] as? DeclarationStatement)?.singleDeclaration
                    as? VariableDeclaration)
                ?.initializer as? CallExpression
        )
        assertLocalName("try.cont", (innerCatchClause.statements[1] as? GotoStatement)?.targetLabel)

        val innerCatchThrows =
            (innerTry.catchClauses[0].body?.statements?.get(1) as? IfStatement)?.elseStatement
                as? UnaryOperator
        assertNotNull(innerCatchThrows)
        assertNotNull(innerCatchThrows.input)
        assertSame(
            innerTry.catchClauses[0].parameter,
            (innerCatchThrows.input as? DeclaredReferenceExpression)?.refersTo
        )
    }

    @Test
    fun testRust() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("rust_sample.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "llvm")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("client.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedClient() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("client.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedIf() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("if.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedMain() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("main.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }
}
