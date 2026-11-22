/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.csharp.statementhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TryCatchTest : BaseTest() {

    @Test
    fun testTryCatch() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["TryCatchStmt"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val tryStmt = body.statements.firstOrNull()
        assertIs<Try>(tryStmt)

        // try { return Convert(s); }
        val tryBlock = tryStmt.tryBlock
        assertIs<Block>(tryBlock)
        assertIs<Return>(tryBlock.statements.single())

        // catch (FormatException e) { Log(e); return -1; }
        val catchClause = tryStmt.catchClauses.single()
        val parameter = catchClause.parameter
        assertNotNull(parameter)
        assertLocalName("e", parameter)
        assertLocalName("FormatException", parameter.type)

        val catchBlock = catchClause.body
        assertIs<Block>(catchBlock)
        val log = catchBlock.statements[0]
        assertIs<Call>(log)
        assertLocalName("Log", log)
        // The catch parameter is in scope inside the catch block.
        assertUsageOf(log.arguments.single(), parameter)
        assertIs<Return>(catchBlock.statements[1])

        assertNull(tryStmt.finallyBlock)
    }

    @Test
    fun testTryCatchFinally() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["TryCatchFinally"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val tryStmt = body.statements.firstOrNull()
        assertIs<Try>(tryStmt)

        assertNotNull(tryStmt.tryBlock)
        assertEquals(1, tryStmt.catchClauses.size)

        // finally { Close(); }
        val finallyBlock = tryStmt.finallyBlock
        assertIs<Block>(finallyBlock)
        val close = finallyBlock.statements.single()
        assertIs<Call>(close)
        assertLocalName("Close", close)
    }

    @Test
    fun testTryFinally() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["TryFinally"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val tryStmt = body.statements.firstOrNull()
        assertIs<Try>(tryStmt)

        assertNotNull(tryStmt.tryBlock)
        assertTrue(tryStmt.catchClauses.isEmpty())
        assertNotNull(tryStmt.finallyBlock)
    }

    @Test
    fun testMultipleCatches() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["MultipleCatches"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val tryStmt = body.statements.firstOrNull()
        assertIs<Try>(tryStmt)
        assertEquals(3, tryStmt.catchClauses.size)

        // catch (FormatException e)
        val named = tryStmt.catchClauses[0].parameter
        assertNotNull(named)
        assertLocalName("e", named)
        assertLocalName("FormatException", named.type)

        // catch (IOException): the type is declared, but not bound to a variable, so the parameter
        // has no name.
        val unnamed = tryStmt.catchClauses[1].parameter
        assertNotNull(unnamed)
        assertEquals("", unnamed.name.localName)
        assertLocalName("IOException", unnamed.type)

        // catch: a general catch clause has no declaration at all.
        assertNull(tryStmt.catchClauses[2].parameter)
    }

    @Test
    fun testCatchFilter() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["CatchFilter"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val tryStmt = body.statements.firstOrNull()
        assertIs<Try>(tryStmt)

        // catch (IOException e) when (e.Message == "retry") { Retry(); }
        val catchClause = tryStmt.catchClauses.single()
        val parameter = catchClause.parameter
        assertNotNull(parameter)
        assertLocalName("e", parameter)

        // The filter has no direct equivalent in the CPG, so the body is wrapped in an implicit
        // if-else guarded by the filter expression.
        val catchBody = catchClause.body
        assertIs<Block>(catchBody)
        val guard = catchBody.statements.single()
        assertIs<IfElse>(guard)

        val condition = guard.condition
        assertIs<BinaryOperator>(condition)
        assertEquals("==", condition.operatorCode)
        val message = condition.lhs
        assertIs<MemberAccess>(message)
        assertLocalName("Message", message)
        assertUsageOf(message.base, parameter)

        val guarded = guard.thenStatement
        assertIs<Block>(guarded)
        val retry = guarded.statements.single()
        assertIs<Call>(retry)
        assertLocalName("Retry", retry)

        assertNull(guard.elseStatement)
    }

    @Test
    fun testThrowStatement() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)
        val method = tu.methods["ThrowStmt"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // throw new InvalidOperationException("nope");
        val throwStmt = body.statements.single()
        assertIs<Throw>(throwStmt)
        val exception = throwStmt.exception
        assertIs<New>(exception)
        assertLocalName("InvalidOperationException", exception.type)
    }

    @Test
    fun testRethrow() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["Rethrow"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val tryStmt = body.statements.firstOrNull()
        assertIs<Try>(tryStmt)

        // catch (IOException) { throw; }
        val catchBlock = tryStmt.catchClauses.single().body
        assertIs<Block>(catchBlock)
        val throwStmt = catchBlock.statements.single()
        assertIs<Throw>(throwStmt)
        assertNull(throwStmt.exception)
    }

    @Test
    fun testThrowExpression() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)
        val method = tu.methods["ThrowExpr"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return s ?? throw new ArgumentNullException("s");
        val returnStmt = body.statements.single()
        assertIs<Return>(returnStmt)
        val coalesce = returnStmt.returnValue
        assertIs<BinaryOperator>(coalesce)
        assertEquals("??", coalesce.operatorCode)

        val throwExpr = coalesce.rhs
        assertIs<Throw>(throwExpr)
        val exception = throwExpr.exception
        assertIs<New>(exception)
        assertLocalName("ArgumentNullException", exception.type)
    }
}
