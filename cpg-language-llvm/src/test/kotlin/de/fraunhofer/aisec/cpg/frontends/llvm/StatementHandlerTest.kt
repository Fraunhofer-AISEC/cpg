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
import de.fraunhofer.aisec.cpg.graph.dFunctions
import de.fraunhofer.aisec.cpg.graph.dVariables
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.statements.BreakStatement
import de.fraunhofer.aisec.cpg.graph.statements.CaseStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.GotoStatement
import de.fraunhofer.aisec.cpg.graph.statements.SwitchStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConditionalExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertFullName
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertRefersTo
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
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(2, tu.declarations.size)

        val main = tu.dFunctions["main"]
        assertNotNull(main)
        assertLocalName("i32", main.type)

        val rand = tu.dFunctions["rand"]
        assertNotNull(rand)
        assertNull(rand.body)

        val xDeclaration = tu.dVariables["x"]
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
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(2, tu.declarations.size)

        val main = tu.dFunctions["main"]
        assertNotNull(main)
        assertLocalName("half", main.type)

        val rand = tu.dFunctions["rand"]
        assertNotNull(rand)
        assertNull(rand.body)

        val xDeclaration = tu.dVariables["x"]
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

        val fnegStatement = main.bodyOrNull<DeclarationStatement>(7)
        assertNotNull(fnegStatement)

        val fnegDeclaration = fnegStatement.singleDeclaration
        assertIs<VariableDeclaration>(fnegDeclaration)
        assertLocalName("f", fnegDeclaration)
        assertEquals("half", fnegDeclaration.type.typeName)

        val fneg = fnegDeclaration.initializer
        assertIs<UnaryOperator>(fneg)
        assertEquals("-", fneg.operatorCode)
    }

    @Test
    fun testIntegerComparisons() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("integer_comparisons.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(2, tu.declarations.size)

        val main = tu.dFunctions["main"]
        assertNotNull(main)
        assertLocalName("i32", main.type)

        val rand = tu.dFunctions["rand"]
        assertNotNull(rand)
        assertNull(rand.body)

        val xDeclaration = tu.dVariables["x"]
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
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        // main, rand and inferred dummy function "isunordered"
        assertEquals(3, tu.declarations.size)

        val main = tu.dFunctions["main"]
        assertNotNull(main)
        assertLocalName("half", main.type)

        val rand = tu.dFunctions["rand"]
        assertNotNull(rand)
        assertNull(rand.body)

        val xDeclaration = tu.dVariables["x"]
        assertNotNull(xDeclaration)
        val yDeclaration = tu.dVariables["y"]
        assertNotNull(yDeclaration)

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

        val cmpOrdStatement = main.bodyOrNull<DeclarationStatement>(14)
        assertNotNull(cmpOrdStatement)

        val cmpOrdDeclaration = cmpOrdStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpOrdDeclaration)
        assertLocalName("m", cmpOrdDeclaration)
        assertEquals("i1", cmpOrdDeclaration.type.typeName)

        val cmpOrdNeg = cmpOrdDeclaration.initializer
        assertIs<UnaryOperator>(cmpOrdNeg)
        assertEquals("!", cmpOrdNeg.operatorCode)
        val cmpOrd = cmpOrdNeg.input
        assertIs<CallExpression>(cmpOrd)
        assertLocalName("isunordered", cmpOrd)
        assertRefersTo(cmpOrd.arguments[0], xDeclaration)
        assertRefersTo(cmpOrd.arguments[1], yDeclaration)

        val cmpUnoStatement = main.bodyOrNull<DeclarationStatement>(15)
        assertNotNull(cmpUnoStatement)

        val cmpUnoDeclaration = cmpUnoStatement.singleDeclaration
        assertIs<VariableDeclaration>(cmpUnoDeclaration)
        assertLocalName("n", cmpUnoDeclaration)
        assertEquals("i1", cmpUnoDeclaration.type.typeName)

        val cmpUno = cmpUnoDeclaration.initializer
        assertIs<CallExpression>(cmpUno)
        assertLocalName("isunordered", cmpUno)
        assertRefersTo(cmpUno.arguments[0], xDeclaration)
        assertRefersTo(cmpUno.arguments[1], yDeclaration)
    }

    @Test
    fun testFreeze() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("freeze.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val main = tu.dFunctions["main"]
        assertNotNull(main)

        val mainBody = main.body
        assertIs<Block>(mainBody)
        val wDeclaration = main.dVariables["w"]
        assertNotNull(wDeclaration)

        val freezeInstructionDeclaration = mainBody.statements[3]
        // We expect something like this: x = (w != undef && w != poison) ? w : llvm.freeze(w)
        assertIs<DeclarationStatement>(freezeInstructionDeclaration)
        val xDeclaration = freezeInstructionDeclaration.singleDeclaration
        assertIs<VariableDeclaration>(xDeclaration)
        assertLocalName("x", xDeclaration)
        assertEquals("i32", xDeclaration.type.typeName)

        val freezeInstruction = xDeclaration.initializer
        assertIs<ConditionalExpression>(freezeInstruction)
        val condition = freezeInstruction.condition
        assertIs<BinaryOperator>(condition)
        assertEquals("&&", condition.operatorCode)

        val undefCheck = condition.lhs
        assertIs<BinaryOperator>(undefCheck)
        assertEquals("!=", undefCheck.operatorCode)
        assertRefersTo(undefCheck.lhs, wDeclaration)
        // undef is modeled as null
        assertLiteralValue(null, undefCheck.rhs)

        val poisonCheck = condition.rhs
        assertIs<BinaryOperator>(poisonCheck)
        assertEquals("!=", poisonCheck.operatorCode)
        assertRefersTo(poisonCheck.lhs, wDeclaration)
        // poison is modeled as a reference "poison"
        assertLocalName("poison", poisonCheck.rhs)

        assertRefersTo(freezeInstruction.thenExpression, wDeclaration)

        val elseExpression = freezeInstruction.elseExpression
        assertIs<CallExpression>(elseExpression)
        assertFullName("llvm.freeze", elseExpression)
        assertEquals(1, elseExpression.arguments.size)
        assertRefersTo(elseExpression.arguments.firstOrNull(), wDeclaration)
    }

    @Test
    fun testAtomicrmw() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("atomicrmw.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val foo = tu.dFunctions["foo"]
        assertNotNull(foo)

        val fooBody = foo.body
        assertIs<Block>(fooBody)

        val atomicrmwAddStatement = fooBody[0]
        assertIs<Block>(atomicrmwAddStatement)
        checkAtomicRmwBinaryOpReplacement(atomicrmwAddStatement, "+", "old1")

        val atomicrmwSubStatement = fooBody[1]
        assertIs<Block>(atomicrmwSubStatement)
        checkAtomicRmwBinaryOpReplacement(atomicrmwSubStatement, "-", "old2")

        val atomicrmwAndStatement = fooBody[2]
        assertIs<Block>(atomicrmwAndStatement)
        checkAtomicRmwBinaryOpReplacement(atomicrmwAndStatement, "&", "old3")

        val atomicrmwOrStatement = fooBody[3]
        assertIs<Block>(atomicrmwOrStatement)
        checkAtomicRmwBinaryOpReplacement(atomicrmwOrStatement, "|", "old4")

        val atomicrmwXorStatement = fooBody[4]
        assertIs<Block>(atomicrmwXorStatement)
        checkAtomicRmwBinaryOpReplacement(atomicrmwXorStatement, "^", "old5")

        // This one is not wrapped in a block and does not have the declaration statement!
        // Check that the replacement equals *ptr = ~(*ptr | 1)
        val replacementNand = fooBody[5]
        assertIs<AssignExpression>(replacementNand)
        assertEquals(1, replacementNand.lhs.size)
        assertEquals(1, replacementNand.rhs.size)
        assertEquals("=", replacementNand.operatorCode)
        val replacementNandLhs = replacementNand.lhs.first()
        assertIs<UnaryOperator>(replacementNandLhs)
        assertEquals("*", replacementNandLhs.operatorCode)
        assertLocalName("ptr", replacementNandLhs.input)
        // Check that the rhs is equal to ~(*ptr | 1)
        val unaryOp = replacementNand.rhs.first()
        assertIs<UnaryOperator>(unaryOp)
        assertEquals("~", unaryOp.operatorCode)
        val binOp = unaryOp.input
        assertIs<BinaryOperator>(binOp)
        assertEquals("|", binOp.operatorCode)
        val binOpLhs = binOp.lhs
        assertIs<UnaryOperator>(binOpLhs)
        assertEquals("*", binOpLhs.operatorCode)
        assertLocalName("ptr", binOpLhs.input)
        assertLiteralValue(1L, binOp.rhs)

        val atomicrmwMinStatement = fooBody[6]
        assertIs<Block>(atomicrmwMinStatement)
        checkAtomicRmwMinMax(atomicrmwMinStatement, "<", "old7", false)

        val atomicrmwMaxStatement = fooBody[7]
        assertIs<Block>(atomicrmwMaxStatement)
        checkAtomicRmwMinMax(atomicrmwMaxStatement, ">", "old8", false)

        val atomicrmwUminStatement = fooBody[8]
        assertIs<Block>(atomicrmwUminStatement)
        checkAtomicRmwMinMax(atomicrmwUminStatement, "<", "old9", true)

        val atomicrmwUmaxStatement = fooBody[9]
        assertIs<Block>(atomicrmwUmaxStatement)
        checkAtomicRmwMinMax(atomicrmwUmaxStatement, ">", "old10", true)
    }

    // We expect *ptr = (*ptr <cmp> 1) ? *ptr : 1
    private fun checkAtomicRmwMinMax(
        atomicrmwStatement: Block,
        cmp: String,
        variableName: String,
        requiresUintCast: Boolean,
    ) {
        // Check that the value is assigned to
        val declaration = atomicrmwStatement.statements[0].declarations[0]
        assertIs<VariableDeclaration>(declaration)
        assertLocalName(variableName, declaration)
        assertLocalName("i32", declaration.type)
        val initializer = declaration.initializer
        assertIs<UnaryOperator>(initializer)
        assertEquals("*", initializer.operatorCode)
        assertLocalName("ptr", initializer.input)

        // Check that the replacement equals *ptr = (*ptr <cmp> 1) ? *ptr : 1
        val replacement = atomicrmwStatement.statements[1]
        assertIs<AssignExpression>(replacement)
        assertEquals(1, replacement.lhs.size)
        assertEquals(1, replacement.rhs.size)
        assertEquals("=", replacement.operatorCode)
        val replacementLhs = replacement.lhs.first()
        assertIs<UnaryOperator>(replacementLhs)
        assertEquals("*", replacementLhs.operatorCode)
        assertLocalName("ptr", replacementLhs.input)

        // Check that the rhs is equal to (*ptr <cmp> 1) ? *ptr : 1
        val conditionalExpression = replacement.rhs.first()
        assertIs<ConditionalExpression>(conditionalExpression)
        val condition = conditionalExpression.condition
        assertIs<BinaryOperator>(condition)
        assertEquals(cmp, condition.operatorCode)
        var cmpLhs = condition.lhs
        if (requiresUintCast) {
            assertIs<CastExpression>(cmpLhs)
            assertEquals("ui32", cmpLhs.castType.typeName)
            cmpLhs = cmpLhs.expression
        }
        assertIs<UnaryOperator>(cmpLhs)
        assertEquals("*", cmpLhs.operatorCode)
        assertLocalName("ptr", cmpLhs.input)
        var cmpRhs = condition.rhs
        if (requiresUintCast) {
            assertIs<CastExpression>(cmpRhs)
            assertEquals("ui32", cmpRhs.castType.typeName)
            cmpRhs = cmpRhs.expression
        }
        assertLiteralValue(1L, cmpRhs)
        val thenExpression = conditionalExpression.thenExpression
        assertIs<UnaryOperator>(thenExpression)
        assertEquals("*", thenExpression.operatorCode)
        assertLocalName("ptr", thenExpression.input)
        assertLiteralValue(1L, conditionalExpression.elseExpression)
    }

    private fun checkAtomicRmwBinaryOpReplacement(
        atomicrmwStatement: Block,
        operator: String,
        variableName: String,
    ) {
        // Check that the value is assigned to
        val declaration = atomicrmwStatement.statements[0].declarations[0]
        assertIs<VariableDeclaration>(declaration)
        assertLocalName(variableName, declaration)
        assertLocalName("i32", declaration.type)
        val initializer = declaration.initializer
        assertIs<UnaryOperator>(initializer)
        assertEquals("*", initializer.operatorCode)
        assertLocalName("ptr", initializer.input)

        // Check that the replacement equals *ptr = *ptr <operator> 1
        val replacement = atomicrmwStatement.statements[1]

        assertIs<AssignExpression>(replacement)
        assertEquals(1, replacement.lhs.size)
        assertEquals(1, replacement.rhs.size)
        assertEquals("=", replacement.operatorCode)
        val replacementLhs = replacement.lhs.first()
        assertIs<UnaryOperator>(replacementLhs)
        assertEquals("*", replacementLhs.operatorCode)
        assertLocalName("ptr", replacementLhs.input)
        // Check that the rhs is equal to *ptr + 1
        val binOp = replacement.rhs.first()
        assertIs<BinaryOperator>(binOp)
        assertEquals(operator, binOp.operatorCode)
        val binOpLhs = binOp.lhs
        assertIs<UnaryOperator>(binOpLhs)
        assertEquals("*", binOpLhs.operatorCode)
        assertLocalName("ptr", binOpLhs.input)
        assertLiteralValue(1L, binOp.rhs)
    }

    @Test
    fun testCallBr() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("callbr.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val main = tu.dFunctions["main"]
        assertNotNull(main)

        val mainBody = main.body
        assertIs<Block>(mainBody)
        val callBrInstruction = mainBody.statements[3]
        assertIs<ProblemExpression>(callBrInstruction)
    }

    @Test
    fun testIndirectBr() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("indirectbr.ll").toFile()),
                topLevel,
                false,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val foo = tu.dFunctions["foo"]
        assertNotNull(foo)

        val fooBody = foo.body
        assertIs<Block>(fooBody)
        val indirectbrInstruction = fooBody.statements[0]
        assertIs<SwitchStatement>(indirectbrInstruction)
        assertRefersTo(indirectbrInstruction.selector, foo.parameters.single())
        val jumps = indirectbrInstruction.statement
        assertIs<Block>(jumps)
        val caseBB1 = jumps.statements[0]
        assertIs<CaseStatement>(caseBB1)
        assertIs<Literal<*>>(caseBB1.caseExpression)
        val jumpBB1 = jumps.statements[1]
        assertIs<GotoStatement>(jumpBB1)
        assertEquals("bb1", jumpBB1.targetLabel?.label)
        assertIs<BreakStatement>(jumps.statements[2])

        val caseBB2 = jumps.statements[3]
        assertIs<CaseStatement>(caseBB2)
        assertIs<Literal<*>>(caseBB2.caseExpression)
        val jumpBB2 = jumps.statements[4]
        assertIs<GotoStatement>(jumpBB2)
        assertEquals("bb2", jumpBB2.targetLabel?.label)
        assertIs<BreakStatement>(jumps.statements[5])
    }
}
