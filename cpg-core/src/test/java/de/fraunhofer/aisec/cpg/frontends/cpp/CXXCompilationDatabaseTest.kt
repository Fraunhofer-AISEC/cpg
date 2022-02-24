/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class CXXCompilationDatabaseTest {
    @Test
    fun testCompilationDatabaseWithIncludes() {
        val ccs =
            listOf(
                "src/test/resources/cxxCompilationDatabase/compile_commands_arguments.json",
                "src/test/resources/cxxCompilationDatabase/compile_commands_commands.json"
            )
        for (path in ccs) {
            val cc = File(path)
            val tus = TestUtils.analyzeWithCompilationDatabase(cc, true)
            val tu = tus.stream().findFirst().orElseThrow()
            assertNotNull(tu)

            val mainFunc = tu.byNameOrNull<FunctionDeclaration>("main")
            assertNotNull(mainFunc)

            val func1 = tu.byNameOrNull<FunctionDeclaration>("func1")
            assertNotNull(func1)
            assertEquals(func1.isInferred, false)
            val func2 = tu.byNameOrNull<FunctionDeclaration>("func2")
            assertNotNull(func2)
            assertEquals(func2.isInferred, false)
            val sysFunc = tu.byNameOrNull<FunctionDeclaration>("sys_func")
            assertNotNull(sysFunc)
            assertEquals(sysFunc.isInferred, false)

            val s0 = mainFunc.getBodyStatementAs(0, CallExpression::class.java)
            assertNotNull(s0)
            val arg1 = s0.arguments[1]
            assert(arg1 is Literal<*>)
            val lit = arg1 as Literal<*>
            assertEquals(lit.value, "hi")

            val s1 = mainFunc.getBodyStatementAs(1, CallExpression::class.java)
            assertNotNull(s1)
            assertEquals(s1.invokes.iterator().next(), func1)

            val s2 = mainFunc.getBodyStatementAs(2, CallExpression::class.java)
            assertNotNull(s2)
            assertEquals(s2.invokes.iterator().next(), func2)

            val s3 = mainFunc.getBodyStatementAs(3, CallExpression::class.java)
            assertNotNull(s3)
            assertEquals(s3.invokes.iterator().next(), sysFunc)

            val s4 = mainFunc.getBodyStatementAs(4, Literal::class.java)
            assertNotNull(s4)
            assertEquals(s4.value, 1)

            val s5 = mainFunc.getBodyStatementAs(5, Literal::class.java)
            assertNotNull(s5)
            assertEquals(s5.value, 2)
        }
    }

    @Test
    fun testCompilationDatabaseSimple() {
        val cc = File("src/test/resources/cxxCompilationDatabase/compile_commands_simple.json")
        val tus = TestUtils.analyzeWithCompilationDatabase(cc, true)
        val tu = tus.stream().findFirst().orElseThrow()
        assertNotNull(tu)
        assertNotNull(tu)

        val mainFunc = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(mainFunc)

        val s0 = mainFunc.getBodyStatementAs(0, ReturnStatement::class.java)
        assertNotNull(s0)
        val retVal = s0.returnValue as Literal<*>
        assertEquals(retVal.value, 1)
    }

    @Test
    fun testCompilationDatabaseMultiTUs() {
        val cc = File("src/test/resources/cxxCompilationDatabase/compile_commands_multi_tus.json")
        val tus = TestUtils.analyzeWithCompilationDatabase(cc, true)
        assertEquals(tus.size, 2)

        val ref = mapOf("main_tu_1.c" to 1, "main_tu_2.c" to 2)

        for (i in tus.indices) {
            val tu = tus[i]
            val value = ref[File(tu.name).name]
            val mainFunc = tu.byNameOrNull<FunctionDeclaration>("main")
            assertNotNull(mainFunc)

            val s0 = mainFunc.getBodyStatementAs(0, Literal::class.java)
            assertNotNull(s0)
            assertEquals(s0.value, value)

            val s1 = mainFunc.getBodyStatementAs(1, Literal::class.java)
            assertNotNull(s1)
            assertEquals(s1.value, value)
        }
    }
}
