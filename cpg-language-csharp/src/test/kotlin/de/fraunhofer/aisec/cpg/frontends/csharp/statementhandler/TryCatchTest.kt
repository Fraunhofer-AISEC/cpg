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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
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

    private fun analyze(): TranslationUnit {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("TryCatch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)
        return tu
    }

    /** Returns the single [Try] in the body of the method called [name]. */
    private fun tryOf(tu: TranslationUnit, name: String): Try {
        val method = tu.methods[name]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val tryStmt = body.statements.firstOrNull()
        assertIs<Try>(tryStmt)
        return tryStmt
    }

    @Test
    fun tryCatchTest() {
        val tryStmt = tryOf(analyze(), "TryCatchStmt")

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
    fun tryCatchFinallyTest() {
        val tryStmt = tryOf(analyze(), "TryCatchFinally")

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
    fun tryFinallyTest() {
        val tryStmt = tryOf(analyze(), "TryFinally")

        assertNotNull(tryStmt.tryBlock)
        assertTrue(tryStmt.catchClauses.isEmpty())
        assertNotNull(tryStmt.finallyBlock)
    }

    @Test
    fun multipleCatchesTest() {
        val tryStmt = tryOf(analyze(), "MultipleCatches")
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
    fun catchFilterTest() {
        val tryStmt = tryOf(analyze(), "CatchFilter")

        // catch (IOException e) when (e.Message == "retry") { Retry(); }
        val catchClause = tryStmt.catchClauses.single()
        val parameter = catchClause.parameter
        assertNotNull(parameter)
        assertLocalName("e", parameter)

        // The filter has no direct equivalent in the CPG, so the body is wrapped in an implicit
        // if-else guarded by the filter expression.
        val body = catchClause.body
        assertIs<Block>(body)
        val guard = body.statements.single()
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
    fun throwStatementTest() {
        val tu = analyze()
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
    fun rethrowTest() {
        val tryStmt = tryOf(analyze(), "Rethrow")

        // catch (IOException) { throw; }
        val catchBlock = tryStmt.catchClauses.single().body
        assertIs<Block>(catchBlock)
        val throwStmt = catchBlock.statements.single()
        assertIs<Throw>(throwStmt)
        assertNull(throwStmt.exception)
    }

    @Test
    fun throwExpressionTest() {
        val tu = analyze()
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
