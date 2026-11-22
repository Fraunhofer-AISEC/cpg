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
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class IsPatternExpressionTest : BaseTest() {

    @Test
    fun testDeclarationPattern() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["DeclarationPattern"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o is Foo f;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)

        // var tmp = o;
        val tmpDecl = outer.expressions[0]
        assertIs<DeclarationStatement>(tmpDecl)
        val tmpVar = tmpDecl.singleDeclaration
        assertIs<Variable>(tmpVar)

        val inner = outer.expressions[1]
        assertIs<ExpressionList>(inner)

        // Foo f = tmp as Foo;
        val fDecl = inner.expressions[0]
        assertIs<DeclarationStatement>(fDecl)
        val fVar = fDecl.singleDeclaration
        assertIs<Variable>(fVar)
        assertLocalName("f", fVar)
        assertLocalName("Foo", fVar.type)
        val cast = fVar.initializer
        assertIs<Cast>(cast)
        assertLocalName("Foo", cast.castType)
        val castInput = cast.expression
        assertIs<Reference>(castInput)
        assertRefersTo(castInput, tmpVar)

        // tmp is Foo
        val check = inner.expressions[1]
        assertIs<BinaryOperator>(check)
        assertEquals("is", check.operatorCode)
        val checkLhs = check.lhs
        assertIs<Reference>(checkLhs)
        assertRefersTo(checkLhs, tmpVar)
        val checkRhs = check.rhs
        assertIs<TypeExpression>(checkRhs)
        assertLocalName("Foo", checkRhs.type)
    }

    @Test
    fun testDeclarationPatternWithValueType() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["DeclarationPatternWithValueType"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o is int i;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val inner = outer.expressions[1]
        assertIs<ExpressionList>(inner)

        // int i = tmp as int;
        val iDecl = inner.expressions[0]
        assertIs<DeclarationStatement>(iDecl)
        val iVar = iDecl.singleDeclaration
        assertIs<Variable>(iVar)
        assertLocalName("i", iVar)
        assertLocalName("int", iVar.type)
        val cast = iVar.initializer
        assertIs<Cast>(cast)
        assertLocalName("int", cast.castType)

        // tmp is int
        val check = inner.expressions[1]
        assertIs<BinaryOperator>(check)
        val checkRhs = check.rhs
        assertIs<TypeExpression>(checkRhs)
        assertLocalName("int", checkRhs.type)
    }

    @Test
    fun testDeclarationPatternWithSideEffect() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["DeclarationPatternWithSideEffect"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return GetObject() is Foo f;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)

        // var tmp = GetObject();
        val tmpDecl = outer.expressions[0]
        assertIs<DeclarationStatement>(tmpDecl)
        val tmpVar = tmpDecl.singleDeclaration
        assertIs<Variable>(tmpVar)
        val call = tmpVar.initializer
        assertIs<Call>(call)
        assertLocalName("GetObject", call)

        val inner = outer.expressions[1]
        assertIs<ExpressionList>(inner)

        val fDecl = inner.expressions[0]
        assertIs<DeclarationStatement>(fDecl)
        val fVar = fDecl.singleDeclaration
        assertIs<Variable>(fVar)
        val cast = fVar.initializer
        assertIs<Cast>(cast)
        val castInput = cast.expression
        assertIs<Reference>(castInput)
        assertRefersTo(castInput, tmpVar)

        val check = inner.expressions[1]
        assertIs<BinaryOperator>(check)
        val checkLhs = check.lhs
        assertIs<Reference>(checkLhs)
        assertRefersTo(checkLhs, tmpVar)
    }

    @Test
    fun testDeclarationPatternInCondition() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["DeclarationPatternInCondition"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // if (o is Foo f) { Handle(f); }
        val ifStmt = body.statements.single()
        assertIs<IfElse>(ifStmt)
        val outer = ifStmt.condition
        assertIs<ExpressionList>(outer)
        val inner = outer.expressions[1]
        assertIs<ExpressionList>(inner)
        val fDecl = inner.expressions[0]
        assertIs<DeclarationStatement>(fDecl)
        val fVar = fDecl.singleDeclaration
        assertIs<Variable>(fVar)

        val thenBlock = ifStmt.thenStatement
        assertIs<Block>(thenBlock)
        val call = thenBlock.statements.single()
        assertIs<Call>(call)
        val arg = call.arguments.single()
        assertIs<Reference>(arg)
        assertRefersTo(arg, fVar)
    }

    @Test
    fun testConstantPatternNull() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["ConstantPatternNull"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o is null;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val tmpVar = (outer.expressions[0] as DeclarationStatement).singleDeclaration
        assertIs<Variable>(tmpVar)

        // tmp == null
        val check = outer.expressions[1]
        assertIs<BinaryOperator>(check)
        assertEquals("==", check.operatorCode)
        val lhs = check.lhs
        assertIs<Reference>(lhs)
        assertRefersTo(lhs, tmpVar)
        val rhs = check.rhs
        assertIs<Literal<*>>(rhs)
        assertEquals(null, rhs.value)
    }

    @Test
    fun testVarPattern() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["VarPattern"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o is var y;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val tmpVar = (outer.expressions[0] as DeclarationStatement).singleDeclaration
        assertIs<Variable>(tmpVar)

        val inner = outer.expressions[1]
        assertIs<ExpressionList>(inner)

        // var y = tmp;
        val yDecl = inner.expressions[0]
        assertIs<DeclarationStatement>(yDecl)
        val yVar = yDecl.singleDeclaration
        assertIs<Variable>(yVar)
        assertLocalName("y", yVar)
        val init = yVar.initializer
        assertIs<Reference>(init)
        assertRefersTo(init, tmpVar)

        // always true
        val alwaysTrue = inner.expressions[1]
        assertIs<Literal<*>>(alwaysTrue)
        assertEquals(true, alwaysTrue.value)
    }

    @Test
    fun testNotPattern() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["NotPattern"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o is not null;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val tmpVar = (outer.expressions[0] as DeclarationStatement).singleDeclaration
        assertIs<Variable>(tmpVar)

        // !(tmp is null)
        val not = outer.expressions[1]
        assertIs<UnaryOperator>(not)
        assertEquals("!", not.operatorCode)
        val check = not.input
        assertIs<BinaryOperator>(check)
        assertEquals("==", check.operatorCode)
        val lhs = check.lhs
        assertIs<Reference>(lhs)
        assertRefersTo(lhs, tmpVar)
    }

    @Test
    fun testRelationalPattern() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["RelationalPattern"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return x is > 0;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val tmpVar = (outer.expressions[0] as DeclarationStatement).singleDeclaration
        assertIs<Variable>(tmpVar)

        val check = outer.expressions[1]
        assertIs<BinaryOperator>(check)
        assertEquals(">", check.operatorCode)
        val lhs = check.lhs
        assertIs<Reference>(lhs)
        assertRefersTo(lhs, tmpVar)
        val rhs = check.rhs
        assertIs<Literal<*>>(rhs)
        assertEquals(0, rhs.value)
    }

    @Test
    fun testAndPattern() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["AndPattern"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return x is > 0 and < 100;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val tmpVar = (outer.expressions[0] as DeclarationStatement).singleDeclaration
        assertIs<Variable>(tmpVar)

        val and = outer.expressions[1]
        assertIs<ShortCircuitOperator>(and)
        assertEquals("and", and.operatorCode)
        assertLocalName("bool", and.type)

        val left = and.lhs
        assertIs<BinaryOperator>(left)
        assertEquals(">", left.operatorCode)
        val leftLhs = left.lhs
        assertIs<Reference>(leftLhs)
        assertRefersTo(leftLhs, tmpVar)

        val right = and.rhs
        assertIs<BinaryOperator>(right)
        assertEquals("<", right.operatorCode)
        val rightLhs = right.lhs
        assertIs<Reference>(rightLhs)
        assertRefersTo(rightLhs, tmpVar)
    }

    @Test
    fun testOrPattern() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val record = tu.records["IsPatternExpression"]
        assertNotNull(record)
        val method = record.methods["OrPattern"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return o is int or string;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val tmpVar = (outer.expressions[0] as DeclarationStatement).singleDeclaration
        assertIs<Variable>(tmpVar)

        val or = outer.expressions[1]
        assertIs<ShortCircuitOperator>(or)
        assertEquals("or", or.operatorCode)
        assertLocalName("bool", or.type)

        val left = or.lhs
        assertIs<BinaryOperator>(left)
        assertEquals("is", left.operatorCode)
        val leftRhs = left.rhs
        assertIs<TypeExpression>(leftRhs)
        assertLocalName("int", leftRhs.type)

        val right = or.rhs
        assertIs<BinaryOperator>(right)
        assertEquals("is", right.operatorCode)
        val rightRhs = right.rhs
        assertIs<TypeExpression>(rightRhs)
        assertLocalName("string", rightRhs.type)
    }

    @Test
    fun testNotTypeCheck() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["IsPatternExpression"]?.methods["NotTypeCheck"]
        assertNotNull(method)

        // !(tmp is Foo)
        val body = method.body
        assertIs<Block>(body)
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val not = outer.expressions[1]
        assertIs<UnaryOperator>(not)
        assertEquals("!", not.operatorCode)
        val check = not.input
        assertIs<BinaryOperator>(check)
        assertEquals("is", check.operatorCode)
        val rhs = check.rhs
        assertIs<TypeExpression>(rhs)
        assertLocalName("Foo", rhs.type)
    }

    @Test
    fun testOrTypeCheck() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["IsPatternExpression"]?.methods["OrTypeCheck"]
        assertNotNull(method)

        // (tmp is Foo) or (tmp is Bar)
        val body = method.body
        assertIs<Block>(body)
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val or = outer.expressions[1]
        assertIs<ShortCircuitOperator>(or)
        assertEquals("or", or.operatorCode)
        assertLocalName("bool", or.type)

        for ((side, expected) in listOf(or.lhs to "Foo", or.rhs to "Bar")) {
            assertIs<BinaryOperator>(side)
            assertEquals("is", side.operatorCode)
            val rhs = side.rhs
            assertIs<TypeExpression>(rhs)
            assertLocalName(expected, rhs.type)
        }
    }

    @Test
    fun testTwoPatternsDoNotShareTemporary() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["IsPatternExpression"]?.methods["TwoPatterns"]
        assertNotNull(method)
        val temporaries =
            method.body?.nodes.orEmpty().filterIsInstance<Variable>().filter {
                it.name.localName.startsWith("isPattern")
            }

        assertEquals(2, temporaries.size)
        assertEquals(2, temporaries.map { it.name.localName }.toSet().size)
        assertEquals(2, temporaries.map { it.id }.toSet().size)
    }

    @Test
    fun testDiscardDesignation() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["IsPatternExpression"]?.methods["DiscardDesignation"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val check = outer.expressions[1]
        assertIs<BinaryOperator>(check)
        assertEquals("is", check.operatorCode)
        val rhs = check.rhs
        assertIs<TypeExpression>(rhs)
        assertLocalName("Foo", rhs.type)
    }

    @Test
    fun testParenthesizedPattern() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["IsPatternExpression"]?.methods["ParenthesizedPattern"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val and = outer.expressions[1]
        assertIs<BinaryOperator>(and)
        assertEquals("and", and.operatorCode)
        // The parentheses leave no trace: the left side is the `or` directly.
        val or = and.lhs
        assertIs<BinaryOperator>(or)
        assertEquals("or", or.operatorCode)
        val not = and.rhs
        assertIs<UnaryOperator>(not)
    }

    @Test
    fun testUnsupportedPatternIsProblem() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["IsPatternExpression"]?.methods["RecursivePattern"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val outer = ret.returnValue
        assertIs<ExpressionList>(outer)
        val problem = outer.expressions[1]
        assertIs<ProblemExpression>(problem)
    }

    @Test
    fun testSameVariableNameInTwoIfs() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("IsPatternExpression.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.records["IsPatternExpression"]?.methods["TwoIfsSameName"]
        assertNotNull(method)

        val ifs = method.ifs
        assertEquals(2, ifs.size)
        for (ifStmt in ifs) {
            val outer = ifStmt.condition
            assertIs<ExpressionList>(outer)
            val inner = outer.expressions[1]
            assertIs<ExpressionList>(inner)
            val fVar = (inner.expressions[0] as DeclarationStatement).singleDeclaration
            assertIs<Variable>(fVar)

            val thenBlock = ifStmt.thenStatement
            assertIs<Block>(thenBlock)
            val arg = (thenBlock.statements.single() as Call).arguments.single()
            assertIs<Reference>(arg)
            assertRefersTo(arg, fVar)
        }
    }
}
