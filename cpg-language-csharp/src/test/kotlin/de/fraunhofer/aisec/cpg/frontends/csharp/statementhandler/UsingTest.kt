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
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UsingTest : BaseTest() {

    private fun analyze(): TranslationUnit {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("UsingStatements.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)
        return tu
    }

    @Test
    fun testUsingDeclaration() {
        val body = analyze().methods["UsingDeclaration"]?.body
        assertIs<Block>(body)
        val tryStmt = body.statements.single()
        assertIs<Try>(tryStmt)

        // The resource is acquired outside the try block, so a failing acquisition skips both the
        // body and the disposal.
        val resource = tryStmt.resources.single()
        assertIs<DeclarationStatement>(resource)
        val w = resource.declarations.single()
        assertIs<Variable>(w)
        assertLocalName("w", w)
        assertLocalName("TextWriter", w.type)
        val createText = w.initializer
        assertIs<MemberCall>(createText)
        assertLocalName("CreateText", createText)

        // The body of the using becomes the try block.
        val tryBlock = tryStmt.tryBlock
        assertIs<Block>(tryBlock)
        assertEquals(2, tryBlock.statements.size)

        // finally { w.Dispose(); }
        val finallyBlock = tryStmt.finallyBlock
        assertIs<Block>(finallyBlock)
        val dispose = finallyBlock.statements.single()
        assertIs<MemberCall>(dispose)
        assertLocalName("Dispose", dispose)
        assertTrue(dispose.isImplicit, "the Dispose() call is not in the source code")
        val base = dispose.base
        assertIs<Reference>(base)
        assertUsageOf(base, w)

        // C# has no else block, and there are no catch clauses here.
        assertTrue(tryStmt.catchClauses.isEmpty())
        assertNull(tryStmt.elseBlock)
    }

    @Test
    fun testUsingExpression() {
        val tu = analyze()
        val body = tu.methods["UsingExpression"]?.body
        assertIs<Block>(body)
        val tryStmt = body.statements.single()
        assertIs<Try>(tryStmt)

        // `using (w)` has no variable to dispose, so the resource is bound to an implicit temporary
        // that is initialized with the parameter.
        val resource = tryStmt.resources.single()
        assertIs<DeclarationStatement>(resource)
        val tmp = resource.declarations.single()
        assertIs<Variable>(tmp)
        assertTrue(tmp.isImplicit, "the temporary is not in the source code")

        val parameter = tu.methods["UsingExpression"]?.parameters?.single()
        assertNotNull(parameter)
        val initializer = tmp.initializer
        assertIs<Reference>(initializer)
        assertUsageOf(initializer, parameter)

        // The temporary is what gets disposed.
        val finallyBlock = tryStmt.finallyBlock
        assertIs<Block>(finallyBlock)
        val dispose = finallyBlock.statements.single()
        assertIs<MemberCall>(dispose)
        assertLocalName("Dispose", dispose)
        val base = dispose.base
        assertIs<Reference>(base)
        assertUsageOf(base, tmp)
    }

    @Test
    fun testUsingWithoutBlock() {
        val body = analyze().methods["UsingWithoutBlock"]?.body
        assertIs<Block>(body)
        val tryStmt = body.statements.single()
        assertIs<Try>(tryStmt)

        // The body is a single statement, which we wrap in an implicit block.
        val tryBlock = tryStmt.tryBlock
        assertIs<Block>(tryBlock)
        assertTrue(tryBlock.isImplicit, "the block around the single statement is synthesized")
        assertIs<MemberCall>(tryBlock.statements.single())

        val resource = tryStmt.resources.single()
        assertIs<DeclarationStatement>(resource)
        val w = resource.declarations.single()
        assertIs<Variable>(w)

        val finallyBlock = tryStmt.finallyBlock
        assertIs<Block>(finallyBlock)
        val dispose = finallyBlock.statements.single()
        assertIs<MemberCall>(dispose)
        assertLocalName("Dispose", dispose)
        val base = dispose.base
        assertIs<Reference>(base)
        assertUsageOf(base, w)
    }

    @Test
    fun testMultipleResources() {
        val body = analyze().methods["MultipleResources"]?.body
        assertIs<Block>(body)
        val outer = body.statements.single()
        assertIs<Try>(outer)

        // `using (a = ..., b = ...)` is defined as nested using statements, so it nests two try
        // statements.
        val outerResource = outer.resources.single()
        assertIs<DeclarationStatement>(outerResource)
        val a = outerResource.declarations.single()
        assertIs<Variable>(a)
        assertLocalName("a", a)

        val outerBlock = outer.tryBlock
        assertIs<Block>(outerBlock)
        val inner = outerBlock.statements.single()
        assertIs<Try>(inner)

        val innerResource = inner.resources.single()
        assertIs<DeclarationStatement>(innerResource)
        val b = innerResource.declarations.single()
        assertIs<Variable>(b)
        assertLocalName("b", b)

        // b is disposed by the inner try, i.e. before a.
        val innerFinally = inner.finallyBlock
        assertIs<Block>(innerFinally)
        val disposeB = innerFinally.statements.single()
        assertIs<MemberCall>(disposeB)
        val baseB = disposeB.base
        assertIs<Reference>(baseB)
        assertUsageOf(baseB, b)

        val outerFinally = outer.finallyBlock
        assertIs<Block>(outerFinally)
        val disposeA = outerFinally.statements.single()
        assertIs<MemberCall>(disposeA)
        val baseA = disposeA.base
        assertIs<Reference>(baseA)
        assertUsageOf(baseA, a)

        // Both resources are visible in the innermost body.
        val innerBlock = inner.tryBlock
        assertIs<Block>(innerBlock)
        assertEquals(2, innerBlock.statements.size)
    }

    @Test
    fun testUsingDeclarationStatement() {
        val body = analyze().methods["UsingDeclarationStatement"]?.body
        assertIs<Block>(body)

        // A using declaration only covers the statements that follow it, so the declaration of
        // `path` stays outside the try statement.
        assertEquals(2, body.statements.size)
        val path = body.statements[0]
        assertIs<DeclarationStatement>(path)
        assertLocalName("path", path.declarations.single())

        val tryStmt = body.statements[1]
        assertIs<Try>(tryStmt)
        val resource = tryStmt.resources.single()
        assertIs<DeclarationStatement>(resource)
        val w = resource.declarations.single()
        assertIs<Variable>(w)
        assertLocalName("w", w)

        // The rest of the block became the try block.
        val tryBlock = tryStmt.tryBlock
        assertIs<Block>(tryBlock)
        assertIs<MemberCall>(tryBlock.statements.single())

        val finallyBlock = tryStmt.finallyBlock
        assertIs<Block>(finallyBlock)
        val dispose = finallyBlock.statements.single()
        assertIs<MemberCall>(dispose)
        assertLocalName("Dispose", dispose)
        val base = dispose.base
        assertIs<Reference>(base)
        assertUsageOf(base, w)
    }

    @Test
    fun testMultipleUsingDeclarations() {
        val body = analyze().methods["MultipleUsingDeclarations"]?.body
        assertIs<Block>(body)
        val outer = body.statements.single()
        assertIs<Try>(outer)

        // The second using declaration is part of the body of the first one, so the two nest and b
        // is disposed before a.
        val outerResource = outer.resources.single()
        assertIs<DeclarationStatement>(outerResource)
        val a = outerResource.declarations.single()
        assertIs<Variable>(a)
        assertLocalName("a", a)

        val outerBlock = outer.tryBlock
        assertIs<Block>(outerBlock)
        val inner = outerBlock.statements.single()
        assertIs<Try>(inner)

        val innerResource = inner.resources.single()
        assertIs<DeclarationStatement>(innerResource)
        val b = innerResource.declarations.single()
        assertIs<Variable>(b)
        assertLocalName("b", b)

        val innerFinally = inner.finallyBlock
        assertIs<Block>(innerFinally)
        val disposeB = innerFinally.statements.single()
        assertIs<MemberCall>(disposeB)
        val baseB = disposeB.base
        assertIs<Reference>(baseB)
        assertUsageOf(baseB, b)

        val outerFinally = outer.finallyBlock
        assertIs<Block>(outerFinally)
        val disposeA = outerFinally.statements.single()
        assertIs<MemberCall>(disposeA)
        val baseA = disposeA.base
        assertIs<Reference>(baseA)
        assertUsageOf(baseA, a)
    }

    @Test
    fun testUsingDeclarationInTry() {
        // A using declaration is lowered in every kind of block, not just in a method body.
        val body = analyze().methods["UsingDeclarationInTry"]?.body
        assertIs<Block>(body)
        val outer = body.statements.single()
        assertIs<Try>(outer)
        assertEquals(1, outer.catchClauses.size)

        val outerBlock = outer.tryBlock
        assertIs<Block>(outerBlock)
        val inner = outerBlock.statements.single()
        assertIs<Try>(inner)

        val resource = inner.resources.single()
        assertIs<DeclarationStatement>(resource)
        val w = resource.declarations.single()
        assertIs<Variable>(w)
        assertLocalName("w", w)

        val finallyBlock = inner.finallyBlock
        assertIs<Block>(finallyBlock)
        val dispose = finallyBlock.statements.single()
        assertIs<MemberCall>(dispose)
        assertLocalName("Dispose", dispose)
        val base = dispose.base
        assertIs<Reference>(base)
        assertUsageOf(base, w)
    }

    @Test
    fun testNestedUsings() {
        val body = analyze().methods["NestedUsings"]?.body
        assertIs<Block>(body)
        val outer = body.statements.single()
        assertIs<Try>(outer)

        val outerResource = outer.resources.single()
        assertIs<DeclarationStatement>(outerResource)
        val outerWriter = outerResource.declarations.single()
        assertIs<Variable>(outerWriter)
        assertLocalName("outer", outerWriter)

        // The inner using is a statement of the outer body, followed by a use of the outer
        // resource.
        val outerBlock = outer.tryBlock
        assertIs<Block>(outerBlock)
        assertEquals(2, outerBlock.statements.size)

        val inner = outerBlock.statements[0]
        assertIs<Try>(inner)
        val innerResource = inner.resources.single()
        assertIs<DeclarationStatement>(innerResource)
        val innerWriter = innerResource.declarations.single()
        assertIs<Variable>(innerWriter)
        assertLocalName("inner", innerWriter)

        val innerFinally = inner.finallyBlock
        assertIs<Block>(innerFinally)
        val disposeInner = innerFinally.statements.single()
        assertIs<MemberCall>(disposeInner)
        val innerBase = disposeInner.base
        assertIs<Reference>(innerBase)
        assertUsageOf(innerBase, innerWriter)

        val outerFinally = outer.finallyBlock
        assertIs<Block>(outerFinally)
        val disposeOuter = outerFinally.statements.single()
        assertIs<MemberCall>(disposeOuter)
        val outerBase = disposeOuter.base
        assertIs<Reference>(outerBase)
        assertUsageOf(outerBase, outerWriter)
    }
}
