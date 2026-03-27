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
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.expressions.ExpressionList
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class LoopsTest : BaseTest() {

    @Test
    fun testWhileLoop() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Loops.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["Foo"]?.methods["whileLoop"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // int i = 0;
        val decl = body.statements.getOrNull(0)
        assertIs<DeclarationStatement>(decl)
        val iVar = decl.declarations.firstOrNull()
        assertIs<Variable>(iVar)

        // while (i < 10) { ... }
        val whileStmt = body.statements.getOrNull(1)
        assertIs<While>(whileStmt)
        assertIs<Block>(whileStmt.statement)

        val condition = whileStmt.condition
        assertIs<BinaryOperator>(condition)

        val lhsRef = condition.lhs
        assertIs<Reference>(lhsRef)
        assertNotNull(lhsRef.refersTo)
        assertEquals(iVar, lhsRef.refersTo)
    }

    @Test
    fun testDoWhileLoop() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Loops.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["Foo"]?.methods["doWhileLoop"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // do {..} while (i < 10);
        val doStmt = body.statements.getOrNull(1)
        assertIs<DoWhile>(doStmt)

        val condition = doStmt.condition
        assertIs<BinaryOperator>(condition)

        assertIs<Block>(doStmt.statement)
    }

    @Test
    fun testForLoop() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Loops.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["Foo"]?.methods["forLoop"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // for (int i = 0; i < 10; i += 1)
        val forStmt = body.statements.getOrNull(0)
        assertNotNull(forStmt)
        assertIs<For>(forStmt)

        // initializer: int i = 0
        val initializer = forStmt.initializerStatement
        assertIs<DeclarationStatement>(initializer)
        val iVar = initializer.declarations.firstOrNull()
        assertIs<Variable>(iVar)
        assertIs<IntegerType>(iVar.type)

        // condition: i < 10
        val condition = forStmt.condition
        assertIs<BinaryOperator>(condition)

        // iterator: i += 1
        val iterator = forStmt.iterationStatement
        assertIs<Assign>(iterator)

        assertIs<Block>(forStmt.statement)
    }

    @Test
    fun testForLoopMultipleIncrementors() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Loops.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["Foo"]?.methods["forLoopMultipleIncrementors"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // for (int i = 0, j = 10; i < j; i += 1, j -= 1)
        val forStmt = body.statements.getOrNull(0)
        assertIs<For>(forStmt)

        // initializer: int i = 0, j = 10
        val initializer = forStmt.initializerStatement
        assertIs<DeclarationStatement>(initializer)
        assertEquals(2, initializer.declarations.size)

        assertIs<Block>(forStmt.statement)

        // iterator: i += 1, j -= 1
        val iterator = forStmt.iterationStatement
        assertIs<ExpressionList>(iterator)
        assertEquals(2, iterator.expressions.size)

        val firstIncr = iterator.expressions.getOrNull(0)
        assertNotNull(firstIncr)
        assertIs<Assign>(firstIncr)

        val secondIncr = iterator.expressions.getOrNull(1)
        assertNotNull(secondIncr)
        assertIs<Assign>(secondIncr)
    }

    @Test
    fun testForEachLoop() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Loops.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["Foo"]?.methods["forEachLoop"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // foreach (int n in numbers)
        val forEachStmt = body.statements.getOrNull(1)
        assertIs<ForEach>(forEachStmt)
        assertIs<Block>(forEachStmt.statement)

        val variable = forEachStmt.variable
        assertIs<DeclarationStatement>(variable)
        val nVar = variable.declarations.firstOrNull()
        assertIs<Variable>(nVar)
        assertIs<IntegerType>(nVar.type)

        val iterable = forEachStmt.iterable
        assertIs<Reference>(iterable)
        assertEquals("numbers", iterable.name.localName)
        val numbersVar = iterable.refersTo
        assertIs<Variable>(numbersVar)
        val numbersType = numbersVar.type
        assertIs<PointerType>(numbersType)
        assertEquals(PointerType.PointerOrigin.ARRAY, numbersType.pointerOrigin)
        assertIs<IntegerType>(numbersType.elementType)

        val loopBody = forEachStmt.statement
        assertIs<Block>(loopBody)
        val xDecl = loopBody.statements.getOrNull(0)
        assertNotNull(xDecl)
        assertIs<DeclarationStatement>(xDecl)
        val xVar = xDecl.declarations.firstOrNull()
        assertNotNull(xVar)
        assertIs<Variable>(xVar)
        val nRef = xVar.initializer
        assertNotNull(nRef)
        assertIs<Reference>(nRef)
        assertEquals(nVar, nRef.refersTo)
    }
}
