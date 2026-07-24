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
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SwitchTest : BaseTest() {

    @Test
    fun switchStatementTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Switch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["SwitchStmt"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // int counter = 0;
        val decl = body.statements[0]
        assertIs<DeclarationStatement>(decl)

        // switch (x) {}
        val switchStmt = body.statements[1]
        assertIs<Switch>(switchStmt)

        val selector = switchStmt.selector
        assertIs<Reference>(selector)
        assertEquals("x", selector.name.localName)

        val switchBody = switchStmt.statement
        assertIs<Block>(switchBody)

        // case 1:
        val case1 = switchBody.statements[0]
        assertIs<Case>(case1)
        val case1Expr = case1.caseExpression
        assertIs<Literal<*>>(case1Expr)
        assertEquals(1, case1Expr.value)

        // counter = 10;
        val assign1 = switchBody.statements[1]
        assertIs<Assign>(assign1)

        // break;
        val break1 = switchBody.statements[2]
        assertIs<Break>(break1)

        // case 2:
        val case2 = switchBody.statements[3]
        assertIs<Case>(case2)

        // case 3:
        val case3 = switchBody.statements[4]
        assertIs<Case>(case3)

        // counter = 20;
        val assign2 = switchBody.statements[5]
        assertIs<Assign>(assign2)

        // break;
        val break2 = switchBody.statements[6]
        assertIs<Break>(break2)

        // default:
        val defaultStmt = switchBody.statements[7]
        assertIs<Default>(defaultStmt)

        // counter = -1;
        val assign3 = switchBody.statements[8]
        assertIs<Assign>(assign3)

        // break;
        val break3 = switchBody.statements[9]
        assertIs<Break>(break3)

        // return counter;
        val returnStmt = body.statements[2]
        assertIs<Return>(returnStmt)
    }

    @Test
    fun caseWithDefaultTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Switch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["CaseWithDefault"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        val switchStmt = body.statements[0]
        assertIs<Switch>(switchStmt)

        val switchBody = switchStmt.statement
        assertIs<Block>(switchBody)

        // case 0: CaseZero(); break;
        assertIs<Case>(switchBody.statements[0])
        assertIs<Break>(switchBody.statements[2])

        // case 1: CaseOne(); break;
        assertIs<Case>(switchBody.statements[3])
        assertIs<Break>(switchBody.statements[5])

        // case 2: and default: (same section)
        val case2 = switchBody.statements[6]
        assertIs<Case>(case2)
        assertEquals(2, (case2.caseExpression as Literal<*>).value)
        val defaultStmt = switchBody.statements[7]
        assertIs<Default>(defaultStmt)

        // CaseTwo(); break;
        assertIs<Break>(switchBody.statements[9])
    }

    @Test
    fun stringSwitchTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Switch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["StringSwitch"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        val switchStmt = body.statements[0]
        assertIs<Switch>(switchStmt)

        val selector = switchStmt.selector
        assertIs<Reference>(selector)
        assertEquals("command", selector.name.localName)

        val switchBody = switchStmt.statement
        assertIs<Block>(switchBody)

        // case "run": return "running";
        val case1 = switchBody.statements[0]
        assertIs<Case>(case1)
        val case1Expr = case1.caseExpression
        assertIs<Literal<*>>(case1Expr)
        assertEquals("run", case1Expr.value)

        val return1 = switchBody.statements[1]
        assertIs<Return>(return1)

        // case "stop": return "stopped";
        val case2 = switchBody.statements[2]
        assertIs<Case>(case2)
        assertEquals("stop", (case2.caseExpression as Literal<*>).value)

        // default: return "unknown";
        val defaultStmt = switchBody.statements[4]
        assertIs<Default>(defaultStmt)
    }

    @Test
    fun patternMatchSwitchTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Switch.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["PatternMatchSwitch"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        val switchStmt = body.statements[0]
        assertIs<Switch>(switchStmt)

        val switchBody = switchStmt.statement
        assertIs<Block>(switchBody)

        val firstCase = switchBody.statements[0]
        assertIs<Case>(firstCase)
        assertEquals("first case", (firstCase.caseExpression as Literal<*>).value)

        // case var a when a.Length > 10: return a;
        // This is modeled as a Case whose caseExpression is an implicit `and` BinaryOperator.
        // The lhs is the declaration of variable `o`, the rhs is the condition `o.Length > 10`.
        val patternCase = switchBody.statements[2]
        assertIs<Case>(patternCase)
        val andOp = patternCase.caseExpression
        assertIs<BinaryOperator>(andOp)
        assertEquals("and", andOp.operatorCode)

        // lhs: variable `a`
        val patternDecl = andOp.lhs
        assertIs<DeclarationStatement>(patternDecl)
        assertEquals(1, patternDecl.declarations.size)
        val aVariable = patternDecl.declarations.single()
        assertIs<Variable>(aVariable)
        assertEquals("a", aVariable.name.localName)

        // rhs: condition `a.Length > 10`
        val condition = andOp.rhs
        assertIs<BinaryOperator>(condition)

        val returnStmt = switchBody.statements[3]
        assertIs<Return>(returnStmt)
        assertUsageOf(returnStmt.returnValue, aVariable)

        // default: return "unknown";
        val defaultStmt = switchBody.statements[4]
        assertIs<Default>(defaultStmt)
    }
}
