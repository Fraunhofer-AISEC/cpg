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

    @Test
    fun testIntegerComparisons() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("integer_comparisons.ll").toFile()),
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

        val cmpEqStatement = main.bodyOrNull<DeclarationStatement>(1)
        assertNotNull(cmpEqStatement)

        val cmpEqDeclaration = cmpEqStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpEqDeclaration)
        assertLocalName("a", cmpEqDeclaration)
        assertEquals("i1", cmpEqDeclaration.type.typeName)

        val cmpEq = cmpEqDeclaration.initializer
        assertIs<BinaryOperator>(cmpEq)
        assertEquals("==", cmpEq.operatorCode)

        val cmpNeqStatement = main.bodyOrNull<DeclarationStatement>(2)
        assertNotNull(cmpNeqStatement)

        val cmpNeqDeclaration = cmpNeqStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpNeqDeclaration)
        assertLocalName("b", cmpNeqDeclaration)
        assertEquals("i1", cmpNeqDeclaration.type.typeName)

        val cmpNeq = cmpNeqDeclaration.initializer
        assertIs<BinaryOperator>(cmpNeq)
        assertEquals("!=", cmpNeq.operatorCode)

        val cmpUgtStatement = main.bodyOrNull<DeclarationStatement>(3)
        assertNotNull(cmpUgtStatement)

        val cmpUgtDeclaration = cmpUgtStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUgtDeclaration)
        assertLocalName("c", cmpUgtDeclaration)
        assertEquals("i1", cmpUgtDeclaration.type.typeName)

        val cmpUgt = cmpUgtDeclaration.initializer
        assertIs<BinaryOperator>(cmpUgt)
        assertEquals(">", cmpUgt.operatorCode)

        val cmpUltStatement = main.bodyOrNull<DeclarationStatement>(4)
        assertNotNull(cmpUltStatement)

        val cmpUltDeclaration = cmpUltStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUltDeclaration)
        assertLocalName("d", cmpUltDeclaration)
        assertEquals("i1", cmpUltDeclaration.type.typeName)

        val cmpUlt = cmpUltDeclaration.initializer
        assertIs<BinaryOperator>(cmpUlt)
        assertEquals("<", cmpUlt.operatorCode)

        val cmpUgeStatement = main.bodyOrNull<DeclarationStatement>(5)
        assertNotNull(cmpUgeStatement)

        val cmpUgeDeclaration = cmpUgeStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUgeDeclaration)
        assertLocalName("e", cmpUgeDeclaration)
        assertEquals("i1", cmpUgeDeclaration.type.typeName)

        val cmpUge = cmpUgeDeclaration.initializer
        assertIs<BinaryOperator>(cmpUge)
        assertEquals(">=", cmpUge.operatorCode)

        val cmpUleStatement = main.bodyOrNull<DeclarationStatement>(6)
        assertNotNull(cmpUleStatement)

        val cmpUleDeclaration = cmpUleStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUleDeclaration)
        assertLocalName("f", cmpUleDeclaration)
        assertEquals("i1", cmpUleDeclaration.type.typeName)

        val cmpUle = cmpUleDeclaration.initializer
        assertIs<BinaryOperator>(cmpUle)
        assertEquals("<=", cmpUle.operatorCode)

        val cmpSgtStatement = main.bodyOrNull<DeclarationStatement>(7)
        assertNotNull(cmpSgtStatement)

        val cmpSgtDeclaration = cmpSgtStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpSgtDeclaration)
        assertLocalName("g", cmpSgtDeclaration)
        assertEquals("i1", cmpSgtDeclaration.type.typeName)

        val cmpSgt = cmpSgtDeclaration.initializer
        assertIs<BinaryOperator>(cmpSgt)
        assertEquals(">", cmpSgt.operatorCode)

        val cmpSltStatement = main.bodyOrNull<DeclarationStatement>(8)
        assertNotNull(cmpSltStatement)

        val cmpSltDeclaration = cmpSltStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpSltDeclaration)
        assertLocalName("h", cmpSltDeclaration)
        assertEquals("i1", cmpSltDeclaration.type.typeName)

        val cmpSlt = cmpSltDeclaration.initializer
        assertIs<BinaryOperator>(cmpSlt)
        assertEquals("<", cmpSlt.operatorCode)

        val cmpSgeStatement = main.bodyOrNull<DeclarationStatement>(9)
        assertNotNull(cmpSgeStatement)

        val cmpSgeDeclaration = cmpSgeStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpSgeDeclaration)
        assertLocalName("i", cmpSgeDeclaration)
        assertEquals("i1", cmpSgeDeclaration.type.typeName)

        val cmpSge = cmpSgeDeclaration.initializer
        assertIs<BinaryOperator>(cmpSge)
        assertEquals(">=", cmpSge.operatorCode)

        val cmpSleStatement = main.bodyOrNull<DeclarationStatement>(10)
        assertNotNull(cmpSleStatement)

        val cmpSleDeclaration = cmpSleStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpSleDeclaration)
        assertLocalName("j", cmpSleDeclaration)
        assertEquals("i1", cmpSleDeclaration.type.typeName)

        val cmpSle = cmpSleDeclaration.initializer
        assertIs<BinaryOperator>(cmpSle)
        assertEquals("<=", cmpSle.operatorCode)
    }

    @Test
    fun testFloatingpointComparisons() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("floatingpoint_comparisons.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        // main, rand and inferred dummy function "isunordered"
        assertEquals(3, tu.declarations.size)

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

        val cmpOeqStatement = main.bodyOrNull<DeclarationStatement>(2)
        assertNotNull(cmpOeqStatement)

        val cmpOeqDeclaration = cmpOeqStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpOeqDeclaration)
        assertLocalName("a", cmpOeqDeclaration)
        assertEquals("i1", cmpOeqDeclaration.type.typeName)

        val cmpOeq = cmpOeqDeclaration.initializer
        assertIs<BinaryOperator>(cmpOeq)
        assertEquals("==", cmpOeq.operatorCode)

        val cmpOneStatement = main.bodyOrNull<DeclarationStatement>(3)
        assertNotNull(cmpOneStatement)

        val cmpOneDeclaration = cmpOneStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpOneDeclaration)
        assertLocalName("b", cmpOneDeclaration)
        assertEquals("i1", cmpOneDeclaration.type.typeName)

        val cmpOne = cmpOneDeclaration.initializer
        assertIs<BinaryOperator>(cmpOne)
        assertEquals("!=", cmpOne.operatorCode)

        val cmpOgtStatement = main.bodyOrNull<DeclarationStatement>(4)
        assertNotNull(cmpOgtStatement)

        val cmpOgtDeclaration = cmpOgtStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpOgtDeclaration)
        assertLocalName("c", cmpOgtDeclaration)
        assertEquals("i1", cmpOgtDeclaration.type.typeName)

        val cmpOgt = cmpOgtDeclaration.initializer
        assertIs<BinaryOperator>(cmpOgt)
        assertEquals(">", cmpOgt.operatorCode)

        val cmpOltStatement = main.bodyOrNull<DeclarationStatement>(5)
        assertNotNull(cmpOltStatement)

        val cmpOltDeclaration = cmpOltStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpOltDeclaration)
        assertLocalName("d", cmpOltDeclaration)
        assertEquals("i1", cmpOltDeclaration.type.typeName)

        val cmpOlt = cmpOltDeclaration.initializer
        assertIs<BinaryOperator>(cmpOlt)
        assertEquals("<", cmpOlt.operatorCode)

        val cmpOgeStatement = main.bodyOrNull<DeclarationStatement>(6)
        assertNotNull(cmpOgeStatement)

        val cmpOgeDeclaration = cmpOgeStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpOgeDeclaration)
        assertLocalName("e", cmpOgeDeclaration)
        assertEquals("i1", cmpOgeDeclaration.type.typeName)

        val cmpOge = cmpOgeDeclaration.initializer
        assertIs<BinaryOperator>(cmpOge)
        assertEquals(">=", cmpOge.operatorCode)

        val cmpOleStatement = main.bodyOrNull<DeclarationStatement>(7)
        assertNotNull(cmpOleStatement)

        val cmpOleDeclaration = cmpOleStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpOleDeclaration)
        assertLocalName("f", cmpOleDeclaration)
        assertEquals("i1", cmpOleDeclaration.type.typeName)

        val cmpOle = cmpOleDeclaration.initializer
        assertIs<BinaryOperator>(cmpOle)
        assertEquals("<=", cmpOle.operatorCode)

        val cmpUgtStatement = main.bodyOrNull<DeclarationStatement>(8)
        assertNotNull(cmpUgtStatement)

        val cmpUgtDeclaration = cmpUgtStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUgtDeclaration)
        assertLocalName("g", cmpUgtDeclaration)
        assertEquals("i1", cmpUgtDeclaration.type.typeName)

        val cmpUgtOr = cmpUgtDeclaration.initializer
        assertIs<BinaryOperator>(cmpUgtOr)
        assertEquals("||", cmpUgtOr.operatorCode)
        val cmpUgt = cmpUgtOr.rhs
        assertIs<BinaryOperator>(cmpUgt)
        assertEquals(">", cmpUgt.operatorCode)

        val cmpUltStatement = main.bodyOrNull<DeclarationStatement>(9)
        assertNotNull(cmpUltStatement)

        val cmpUltDeclaration = cmpUltStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUltDeclaration)
        assertLocalName("h", cmpUltDeclaration)
        assertEquals("i1", cmpUltDeclaration.type.typeName)

        val cmpUltOr = cmpUltDeclaration.initializer
        assertIs<BinaryOperator>(cmpUltOr)
        assertEquals("||", cmpUltOr.operatorCode)
        val cmpUlt = cmpUltOr.rhs
        assertIs<BinaryOperator>(cmpUlt)
        assertEquals("<", cmpUlt.operatorCode)

        val cmpUgeStatement = main.bodyOrNull<DeclarationStatement>(10)
        assertNotNull(cmpUgeStatement)

        val cmpUgeDeclaration = cmpUgeStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUgeDeclaration)
        assertLocalName("i", cmpUgeDeclaration)
        assertEquals("i1", cmpUgeDeclaration.type.typeName)

        val cmpUgeOr = cmpUgeDeclaration.initializer
        assertIs<BinaryOperator>(cmpUgeOr)
        assertEquals("||", cmpUgeOr.operatorCode)
        val cmpUge = cmpUgeOr.rhs
        assertIs<BinaryOperator>(cmpUge)
        assertEquals(">=", cmpUge.operatorCode)

        val cmpUleStatement = main.bodyOrNull<DeclarationStatement>(11)
        assertNotNull(cmpUleStatement)

        val cmpUleDeclaration = cmpUleStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUleDeclaration)
        assertLocalName("j", cmpUleDeclaration)
        assertEquals("i1", cmpUleDeclaration.type.typeName)

        val cmpUleOr = cmpUleDeclaration.initializer
        assertIs<BinaryOperator>(cmpUleOr)
        assertEquals("||", cmpUleOr.operatorCode)
        val cmpUle = cmpUleOr.rhs
        assertIs<BinaryOperator>(cmpUle)
        assertEquals("<=", cmpUle.operatorCode)

        val cmpUeqStatement = main.bodyOrNull<DeclarationStatement>(12)
        assertNotNull(cmpUeqStatement)

        val cmpUeqDeclaration = cmpUeqStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUeqDeclaration)
        assertLocalName("k", cmpUeqDeclaration)
        assertEquals("i1", cmpUeqDeclaration.type.typeName)

        val cmpUeqOr = cmpUeqDeclaration.initializer
        assertIs<BinaryOperator>(cmpUeqOr)
        assertEquals("||", cmpUeqOr.operatorCode)
        val cmpUeq = cmpUeqOr.rhs
        assertIs<BinaryOperator>(cmpUeq)
        assertEquals("==", cmpUeq.operatorCode)

        val cmpUneStatement = main.bodyOrNull<DeclarationStatement>(13)
        assertNotNull(cmpUneStatement)

        val cmpUneDeclaration = cmpUneStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUneDeclaration)
        assertLocalName("l", cmpUneDeclaration)
        assertEquals("i1", cmpUneDeclaration.type.typeName)

        val cmpUneOr = cmpUneDeclaration.initializer
        assertIs<BinaryOperator>(cmpUneOr)
        assertEquals("||", cmpUneOr.operatorCode)
        val cmpUne = cmpUneOr.rhs
        assertIs<BinaryOperator>(cmpUne)
        assertEquals("!=", cmpUne.operatorCode)
    }
}
