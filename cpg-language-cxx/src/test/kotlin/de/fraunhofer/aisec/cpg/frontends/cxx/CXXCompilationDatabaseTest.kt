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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CXXCompilationDatabaseTest {
    @Test
    fun testCompilationDatabaseWithIncludes() {
        val ccs =
            listOf(
                "src/test/resources/cxxCompilationDatabase/compile_commands_arguments.json",
                "src/test/resources/cxxCompilationDatabase/compile_commands_commands.json",
            )
        for (path in ccs) {
            val cc = File(path)
            val result =
                analyzeWithCompilationDatabase(cc, true) {
                    it.registerLanguage<CPPLanguage>()
                    it.registerLanguage<CLanguage>()
                }
            val tu = result.components.flatMap { it.translationUnits }.firstOrNull()
            assertNotNull(tu)

            val mainFunc = tu.allFunctions["main"]
            assertNotNull(mainFunc)

            val func1 = tu.allFunctions["func1"]
            assertNotNull(func1)
            assertEquals(func1.isInferred, false)

            val func2 = tu.allFunctions["func2"]
            assertNotNull(func2)
            assertEquals(func2.isInferred, false)

            val sysFunc = tu.allFunctions["sys_func"]
            assertNotNull(sysFunc)
            assertEquals(sysFunc.isInferred, false)

            val s0 = mainFunc.bodyOrNull<CallExpression>(0)
            assertNotNull(s0)

            val arg1 = s0.arguments[1]
            assert(arg1 is Literal<*>)

            val lit = arg1 as Literal<*>
            assertEquals(lit.value, "hi")

            val s1 = mainFunc.bodyOrNull<CallExpression>(1)
            assertNotNull(s1)
            assertInvokes(s1, func1)

            val s2 = mainFunc.bodyOrNull<CallExpression>(2)
            assertNotNull(s2)
            assertInvokes(s2, func2)

            val s3 = mainFunc.bodyOrNull<CallExpression>(3)
            assertNotNull(s3)
            assertInvokes(s3, sysFunc)

            val s4 = mainFunc.bodyOrNull<Literal<*>>(4)
            assertNotNull(s4)
            assertEquals(s4.value, 1)

            val s5 = mainFunc.bodyOrNull<Literal<*>>(5)
            assertNotNull(s5)
            assertEquals(s5.value, 2)
        }
    }

    @Test
    fun testCompilationDatabaseSimple() {
        val cc = File("src/test/resources/cxxCompilationDatabase/compile_commands_simple.json")
        val result =
            analyzeWithCompilationDatabase(cc, true) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
            }
        val tu = result.components.flatMap { it.translationUnits }.firstOrNull()
        assertNotNull(tu)
        assertNotNull(tu)

        val mainFunc = tu.allFunctions["main"]
        assertNotNull(mainFunc)

        val s0 = mainFunc.allReturns.firstOrNull()
        assertNotNull(s0)

        val retVal = s0.returnValue as Literal<*>
        assertEquals(retVal.value, 1)
    }

    @Test
    fun testCompilationDatabaseMultiTUs() {
        val cc = File("src/test/resources/cxxCompilationDatabase/compile_commands_multi_tus.json")
        val result =
            analyzeWithCompilationDatabase(cc, true) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
            }
        val tus = result.components.flatMap { it.translationUnits }
        assertEquals(2, tus.size)
        val ref = mapOf("main_tu_1.c" to 1, "main_tu_2.c" to 2)

        for (tu in tus) {
            val value = ref[File(tu.name.toString()).name]
            val mainFunc = tu.allFunctions["main"]
            assertNotNull(mainFunc)

            val s0 = mainFunc.allLiterals.getOrNull(0)
            assertNotNull(s0)
            assertEquals(value, s0.value)

            val s1 = mainFunc.allLiterals.getOrNull(1)
            assertNotNull(s1)
            assertEquals(value, s1.value)
        }
    }

    @Test
    fun testCompilationDatabaseArch() {
        val cc = File("src/test/resources/cxxCompilationDatabase/compile_commands_arch.json")
        val result =
            analyzeWithCompilationDatabase(cc, true) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
            }

        val main = result.allFunctions["main"]
        assertNotNull(main)
    }

    @Test
    fun testVersion() {
        val versions =
            mapOf(
                "cxx20" to 202002L,
                "cxx17" to 201703L,
                "cxx14" to 201402L,
                "cxx11" to 201103L,
                "cxx98" to 199711L,
                "c23" to 202311L,
                "c2x" to 202000L,
                "c17" to 201710L,
                "c11" to 201112L,
                "c99" to 199901L,
                "c94" to 199409L,
            )

        val cc = File("src/test/resources/cxxCompilationDatabase/compile_commands_cxx_std.json")
        val result =
            analyzeWithCompilationDatabase(cc, true) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(result)

        for ((name, version) in versions) {
            val component = result.components[name]
            assertNotNull(component, "component $name is missing")
            val a = component.allVariables["version"]
            assertNotNull(a)
            assertEquals(version, a.evaluate(), "$name should be version $version")
        }
    }

    @Test
    fun testParseDirectory() {
        val cc = File("src/test/resources/cxxCompilationDatabase/compile_commands_bear.json")
        val result =
            analyzeWithCompilationDatabase(cc, true) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
                it.useUnityBuild(true)
            }
        assertNotNull(result)

        val lib1 = result.components["lib1"]
        assertNotNull(lib1)

        val function = lib1.allFunctions["function"]
        assertNotNull(function)
    }
}
