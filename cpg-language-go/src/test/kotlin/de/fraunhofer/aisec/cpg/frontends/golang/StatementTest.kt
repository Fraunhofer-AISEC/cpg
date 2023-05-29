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

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
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
}
