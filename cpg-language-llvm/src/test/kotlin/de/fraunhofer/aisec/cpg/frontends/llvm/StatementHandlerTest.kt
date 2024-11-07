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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLocalName
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StatementHandlerTest {

    @Test
    fun testIntegerOps() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("integer_ops.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(2, tu.declarations.size)

        val main = tu.functions["main"]
        assertNotNull(main)
        assertLocalName("i32", main.type)

        val rand = tu.functions["rand"]
        assertNotNull(rand)
        assertNull(rand.body)

        val xDeclaration = tu.variables["x"]
        assertNotNull(xDeclaration)

        val call = xDeclaration.initializer
        assertIs<CallExpression>(call)
        assertLocalName("rand", call)
        assertContains(call.invokes, rand)
        assertEquals(0, call.arguments.size)

        val mulStatement = main.bodyOrNull<DeclarationStatement>(2)
        assertNotNull(mulStatement)

        val mulDeclaration = mulStatement.singleDeclaration
        assertIs<VariableDeclaration>(mulDeclaration)
        assertLocalName("a", mulDeclaration)
        assertEquals("i32", mulDeclaration.type.typeName)

        val mul = mulDeclaration.initializer
        assertIs<BinaryOperator>(mul)
        assertEquals("*", mul.operatorCode)

        val addStatement = main.bodyOrNull<DeclarationStatement>(3)
        assertNotNull(addStatement)

        val addDeclaration = addStatement.singleDeclaration
        assertIs<VariableDeclaration>(addDeclaration)
        assertLocalName("b", addDeclaration)
        assertEquals("i32", addDeclaration.type.typeName)

        val add = addDeclaration.initializer
        assertIs<BinaryOperator>(add)
        assertEquals("+", add.operatorCode)

        val subStatement = main.bodyOrNull<DeclarationStatement>(4)
        assertNotNull(subStatement)

        val subDeclaration = subStatement.singleDeclaration
        assertIs<VariableDeclaration>(subDeclaration)
        assertLocalName("c", subDeclaration)
        assertEquals("i32", subDeclaration.type.typeName)

        val sub = subDeclaration.initializer
        assertIs<BinaryOperator>(sub)
        assertEquals("-", sub.operatorCode)

        val divStatement = main.bodyOrNull<DeclarationStatement>(5)
        assertNotNull(divStatement)

        val divDeclaration = divStatement.singleDeclaration
        assertIs<VariableDeclaration>(divDeclaration)
        assertLocalName("d", divDeclaration)
        assertEquals("i32", divDeclaration.type.typeName)

        val div = divDeclaration.initializer
        assertIs<BinaryOperator>(div)
        assertEquals("/", div.operatorCode)

        val remStatement = main.bodyOrNull<DeclarationStatement>(6)
        assertNotNull(remStatement)

        val remDeclaration = remStatement.singleDeclaration
        assertIs<VariableDeclaration>(remDeclaration)
        assertLocalName("e", remDeclaration)
        assertEquals("i32", remDeclaration.type.typeName)

        val rem = remDeclaration.initializer
        assertIs<BinaryOperator>(rem)
        assertEquals("%", rem.operatorCode)

        val xorStatement = main.bodyOrNull<DeclarationStatement>(7)
        assertNotNull(xorStatement)

        val xorDeclaration = xorStatement.singleDeclaration
        assertIs<VariableDeclaration>(xorDeclaration)
        assertLocalName("f", xorDeclaration)
        assertEquals("i32", xorDeclaration.type.typeName)

        val xor = xorDeclaration.initializer
        assertIs<BinaryOperator>(xor)
        assertEquals("^", xor.operatorCode)

        val udivStatement = main.bodyOrNull<DeclarationStatement>(8)
        assertNotNull(udivStatement)

        val udivDeclaration = udivStatement.singleDeclaration
        assertIs<VariableDeclaration>(udivDeclaration)
        assertLocalName("g", udivDeclaration)
        assertEquals("i32", udivDeclaration.type.typeName)

        val udiv = udivDeclaration.initializer
        assertIs<BinaryOperator>(udiv)
        assertEquals("/", udiv.operatorCode)

        val uremStatement = main.bodyOrNull<DeclarationStatement>(9)
        assertNotNull(uremStatement)

        val uremDeclaration = uremStatement.singleDeclaration
        assertIs<VariableDeclaration>(uremDeclaration)
        assertLocalName("h", uremDeclaration)
        assertEquals("i32", uremDeclaration.type.typeName)

        val urem = uremDeclaration.initializer
        assertIs<BinaryOperator>(urem)
        assertEquals("%", urem.operatorCode)

        val shlStatement = main.bodyOrNull<DeclarationStatement>(10)
        assertNotNull(shlStatement)

        val shlDeclaration = shlStatement.singleDeclaration
        assertIs<VariableDeclaration>(shlDeclaration)
        assertLocalName("i", shlDeclaration)
        assertEquals("i32", shlDeclaration.type.typeName)

        val shl = shlDeclaration.initializer
        assertIs<BinaryOperator>(shl)
        assertEquals("<<", shl.operatorCode)

        val lshrStatement = main.bodyOrNull<DeclarationStatement>(11)
        assertNotNull(lshrStatement)

        val lshrDeclaration = lshrStatement.singleDeclaration
        assertIs<VariableDeclaration>(lshrDeclaration)
        assertLocalName("j", lshrDeclaration)
        assertEquals("i32", lshrDeclaration.type.typeName)

        val lshr = lshrDeclaration.initializer
        assertIs<BinaryOperator>(lshr)
        assertEquals(">>", lshr.operatorCode)

        val ashrStatement = main.bodyOrNull<DeclarationStatement>(12)
        assertNotNull(ashrStatement)

        val ashrDeclaration = ashrStatement.singleDeclaration
        assertIs<VariableDeclaration>(ashrDeclaration)
        assertLocalName("k", ashrDeclaration)
        assertEquals("i32", ashrDeclaration.type.typeName)

        val ashr = ashrDeclaration.initializer
        assertIs<BinaryOperator>(ashr)
        assertEquals(">>", ashr.operatorCode)
    }

    @Test
    fun testFloatingpoingOps() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("floatingpoint_ops.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(2, tu.declarations.size)

        val main = tu.functions["main"]
        assertNotNull(main)
        assertLocalName("half", main.type)

        val rand = tu.functions["rand"]
        assertNotNull(rand)
        assertNull(rand.body)

        val xDeclaration = tu.variables["x"]
        assertNotNull(xDeclaration)

        val call = xDeclaration.initializer
        assertIs<CallExpression>(call)
        assertLocalName("rand", call)
        assertContains(call.invokes, rand)
        assertEquals(0, call.arguments.size)

        val fmulStatement = main.bodyOrNull<DeclarationStatement>(2)
        assertNotNull(fmulStatement)

        val fmulDeclaration = fmulStatement.singleDeclaration
        assertIs<VariableDeclaration>(fmulDeclaration)
        assertLocalName("a", fmulDeclaration)
        assertEquals("half", fmulDeclaration.type.typeName)

        val fmul = fmulDeclaration.initializer
        assertIs<BinaryOperator>(fmul)
        assertEquals("*", fmul.operatorCode)

        val faddStatement = main.bodyOrNull<DeclarationStatement>(3)
        assertNotNull(faddStatement)

        val faddDeclaration = faddStatement.singleDeclaration
        assertIs<VariableDeclaration>(faddDeclaration)
        assertLocalName("b", faddDeclaration)
        assertEquals("half", faddDeclaration.type.typeName)

        val fadd = faddDeclaration.initializer
        assertIs<BinaryOperator>(fadd)
        assertEquals("+", fadd.operatorCode)

        val fsubStatement = main.bodyOrNull<DeclarationStatement>(4)
        assertNotNull(fsubStatement)

        val fsubDeclaration = fsubStatement.singleDeclaration
        assertIs<VariableDeclaration>(fsubDeclaration)
        assertLocalName("c", fsubDeclaration)
        assertEquals("half", fsubDeclaration.type.typeName)

        val fsub = fsubDeclaration.initializer
        assertIs<BinaryOperator>(fsub)
        assertEquals("-", fsub.operatorCode)

        val fdivStatement = main.bodyOrNull<DeclarationStatement>(5)
        assertNotNull(fdivStatement)

        val fdivDeclaration = fdivStatement.singleDeclaration
        assertIs<VariableDeclaration>(fdivDeclaration)
        assertLocalName("d", fdivDeclaration)
        assertEquals("half", fdivDeclaration.type.typeName)

        val fdiv = fdivDeclaration.initializer
        assertIs<BinaryOperator>(fdiv)
        assertEquals("/", fdiv.operatorCode)

        val fremStatement = main.bodyOrNull<DeclarationStatement>(6)
        assertNotNull(fremStatement)

        val fremDeclaration = fremStatement.singleDeclaration
        assertIs<VariableDeclaration>(fremDeclaration)
        assertLocalName("e", fremDeclaration)
        assertEquals("half", fremDeclaration.type.typeName)

        val frem = fremDeclaration.initializer
        assertIs<BinaryOperator>(frem)
        assertEquals("%", frem.operatorCode)
    }
}
