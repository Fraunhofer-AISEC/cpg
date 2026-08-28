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
package de.fraunhofer.aisec.cpg.frontends.csharp.expressionhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class IsExpressionTest : BaseTest() {

    @Test
    fun isReferenceTypeTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["IsReferenceType"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o is Base;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val binOp = ret.returnValue
        assertIs<BinaryOperator>(binOp)
        assertEquals("is", binOp.operatorCode)
        // A type test yields a bool, not the type of its operands.
        assertLocalName("bool", binOp.type)

        val lhs = binOp.lhs
        assertIs<Reference>(lhs)
        assertUsageOf(lhs, method.parameters["o"])

        // The right-hand side is a type and not a value.
        val rhs = binOp.rhs
        assertIs<TypeExpression>(rhs)
        assertLocalName("Base", rhs.type)
    }

    @Test
    fun isPredefinedTypeTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["IsPredefinedType"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o is string;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val binOp = ret.returnValue
        assertIs<BinaryOperator>(binOp)
        assertEquals("is", binOp.operatorCode)

        val rhs = binOp.rhs
        assertIs<TypeExpression>(rhs)
        assertLocalName("string", rhs.type)
    }

    @Test
    fun isInConditionTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["IsInCondition"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // if (o is Base) { Handle(o); }
        val ifStmt = body.statements.single()
        assertIs<IfElse>(ifStmt)
        val condition = ifStmt.condition
        assertIs<BinaryOperator>(condition)
        assertEquals("is", condition.operatorCode)

        val rhs = condition.rhs
        assertIs<TypeExpression>(rhs)
        assertLocalName("Base", rhs.type)
    }
}
