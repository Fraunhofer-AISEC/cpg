/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class StatementTest {

    @Test
    fun testBranchStatement() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("branch.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
                it.configureFrontend<GoLanguageFrontend>(GoFrontendConfiguration())
            }

        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val main = p.functions["main"]
        assertNotNull(main)

        val start = main.allChildren<LabelStatement>().firstOrNull { it.label == "start" }
        assertNotNull(start)

        val cases = start.allChildren<CaseStatement>()
        assertEquals(4, cases.size)

        val case0 = cases.firstOrNull { (it.caseExpression as? Literal<*>)?.value == 0 }
        assertNotNull(case0)

        var stmt = case0.nextEOG.firstOrNull()
        assertIs<ContinueStatement>(stmt)

        val case1 = cases.firstOrNull { (it.caseExpression as? Literal<*>)?.value == 1 }
        assertNotNull(case1)

        stmt = case1.nextEOG.firstOrNull()
        val breakStatement = assertIs<BreakStatement>(stmt)
        assertEquals("start", breakStatement.label)

        val default = start.allChildren<DefaultStatement>().firstOrNull()
        assertNotNull(default)

        val end = main.allChildren<LabelStatement>().firstOrNull { it.label == "end" }
        assertNotNull(end)
    }

    @Test
    fun testDeferStatement() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("defer.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
                it.configureFrontend<GoLanguageFrontend>(GoFrontendConfiguration())
            }
        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val `do` = p.methods["Do"]
        assertNotNull(`do`)

        val op = `do`.allChildren<UnaryOperator> { it.name.localName == "defer" }.firstOrNull()
        assertNotNull(op)

        // The EOG for the defer statement itself should be in the regular EOG path
        op.prevEOG.any { it is CallExpression && it.name.localName == "do" }
        op.nextEOG.any { it is Reference && it.name.localName == "that" }

        // It should NOT connect to the call expression
        op.nextEOG.none { it is CallExpression }

        // Its call expression should connect to the return statement
        op.input.prevEOG.all { it is ReturnStatement }
    }

    @Test
    fun testTypeSwitch() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("type_assert.go").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
                it.configureFrontend<GoLanguageFrontend>(GoFrontendConfiguration())
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val body = main.body
        assertIs<Block>(body)
        assertNotNull(body)

        val switch = body.statements<SwitchStatement>(6)
        assertNotNull(switch)

        val block = switch.statement
        assertIs<Block>(block)
        assertNotNull(block)

        val vs = main.variables("v")
        assertNotNull(vs)
        assertEquals(
            listOf("main.MyStruct", "main.MyStruct*", "main.MyInterface"),
            vs.map { it.type.name.toString() },
        )
        vs.forEach {
            assertLocalName("v", it)
            assertLocalName("f", it.initializer)
        }
    }
}
