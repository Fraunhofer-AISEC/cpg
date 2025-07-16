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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ExpressionHandlerTest {
    @Test
    fun testConstantFloat() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("floatingpoint_const.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val globalX = tu.dVariables["x"]
        assertNotNull(globalX)
        assertLiteralValue(1.25, globalX.initializer)

        val a = tu.dVariables["a"]
        assertNotNull(a)
        val aInit = a.initializer
        assertIs<BinaryOperator>(aInit)
        assertLiteralValue(1.25, aInit.lhs)
        assertLiteralValue(1.0, aInit.rhs)
    }

    @Test
    fun testConstantExpr() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("integer_const.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val globalX = tu.dVariables["x"]
        assertNotNull(globalX)

        val aInitCall = tu.dVariables["a"]?.initializer
        assertIs<CallExpression>(aInitCall)
        assertLocalName("foo", aInitCall)
        val argumentA = aInitCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(argumentA)
        assertEquals("+", argumentA.operatorCode)
        val argumentAX = argumentA.lhs
        assertIs<CastExpression>(argumentAX)
        assertRefersTo(argumentAX.expression, globalX)
        assertLiteralValue(5L, argumentA.rhs)

        val bInitCall = tu.dVariables["b"]?.initializer
        assertIs<CallExpression>(bInitCall)
        assertLocalName("foo", bInitCall)
        val argumentB = bInitCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(argumentB)
        assertEquals("-", argumentB.operatorCode)
        val argumenBtX = argumentB.lhs
        assertIs<CastExpression>(argumenBtX)
        assertRefersTo(argumenBtX.expression, globalX)
        assertLiteralValue(5L, argumentB.rhs)

        val cInitCall = tu.dVariables["c"]?.initializer
        assertIs<CallExpression>(cInitCall)
        assertLocalName("foo", cInitCall)
        val argumentC = cInitCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(argumentC)
        assertEquals("*", argumentC.operatorCode)
        val argumentCX = argumentC.lhs
        assertIs<CastExpression>(argumentCX)
        assertRefersTo(argumentCX.expression, globalX)
        assertLiteralValue(5L, argumentC.rhs)

        val dInitCall = tu.dVariables["d"]?.initializer
        assertIs<CallExpression>(dInitCall)
        assertLocalName("foo", dInitCall)
        val argumentD = dInitCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(argumentD)
        assertEquals("<<", argumentD.operatorCode)
        val argumentDX = argumentD.lhs
        assertIs<CastExpression>(argumentDX)
        assertRefersTo(argumentDX.expression, globalX)
        assertLiteralValue(5L, argumentD.rhs)

        val eInitCall = tu.dVariables["e"]?.initializer
        assertIs<CallExpression>(eInitCall)
        assertLocalName("foo", eInitCall)
        val argumentE = eInitCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(argumentE)
        assertEquals(">>", argumentE.operatorCode)
        val argumentEX = argumentE.lhs
        assertIs<CastExpression>(argumentEX)
        assertRefersTo(argumentEX.expression, globalX)
        assertLiteralValue(5L, argumentE.rhs)

        val fInitCall = tu.dVariables["f"]?.initializer
        assertIs<CallExpression>(fInitCall)
        assertLocalName("foo", fInitCall)
        val argumentF = fInitCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(argumentF)
        assertEquals("^", argumentF.operatorCode)
        val argumentFX = argumentF.lhs
        assertIs<CastExpression>(argumentFX)
        assertRefersTo(argumentFX.expression, globalX)
        assertLiteralValue(5L, argumentF.rhs)

        val gInitCall = tu.dVariables["g"]?.initializer
        assertIs<CallExpression>(gInitCall)
        assertLocalName("foo1", gInitCall)
        val argumentG = gInitCall.arguments.singleOrNull()
        assertIs<BinaryOperator>(argumentG)
        assertEquals("==", argumentG.operatorCode)
        val argumentGX = argumentG.lhs
        assertIs<CastExpression>(argumentGX)
        assertRefersTo(argumentGX.expression, globalX)
        assertLiteralValue(5L, argumentG.rhs)
    }
}
