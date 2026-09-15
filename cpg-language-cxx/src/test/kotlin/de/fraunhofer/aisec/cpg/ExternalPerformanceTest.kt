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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.IfElse
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.measureTimeMillis
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@Ignore
class ExternalPerformanceTest {

    @Test
    fun testPointstoloop() {

        val file = File("src/test/resources/pointstoloop.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }

        assertNotNull(tu)
    }

    @Ignore
    @Test
    fun testHugeFunctionHashCode() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "huge-Function.c").toFile()),
                Path.of("src", "test", "resources"),
                usePasses = true,
            ) {
                // it.registerPass<ControlDependenceGraphPass>()
                it.registerPass<UnreachableEOGPass>()
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(result)
        val time = measureTimeMillis {
            for (i in 0..1000) {
                println("$i " + result.functions["FUN_001077c0"]?.body.hashCode())
            }
        }
        println("Time taken: $time ms")
    }

    @Ignore
    @Test
    fun testCDGSlowness() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "slow-cdg.c").toFile()),
                Path.of("src", "test", "resources"),
                usePasses = true,
            ) {
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
    }

    @Ignore
    @Test
    fun testCDGSlowness2() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "y8_ippsGcd_BN.cpp").toFile()),
                Path.of("src", "test", "resources"),
                usePasses = true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
    }

    @Ignore
    @Test
    fun testDefinitionsAndDeclaration() {
        val file = File("src/test/resources/dowhile-cdg.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }

        val prevCdg = tu.calls["__fprintf_chk"]?.prevCDG?.singleOrNull()
        assertIs<IfElse>(prevCdg)
        assertEquals(10, prevCdg.location?.region?.startLine)
        assertIs<Function>(prevCdg.prevCDG.singleOrNull())

        val param1 =
            tu.refs.single {
                it.name.localName == "param_1" && it.location?.region?.startLine == 10
            }
        assertNotNull(param1)
        assertIs<Function>(param1.prevCDG.singleOrNull())

        val printf =
            tu.calls.single {
                it.name.localName == "printf" && it.location?.region?.startLine == 12
            }
        assertNotNull(printf)
        val innerIf = printf.prevCDG.singleOrNull()
        assertIs<IfElse>(innerIf)
        assertEquals(11, innerIf.location?.region?.startLine)
        assertIs<IfElse>(innerIf.prevCDG.singleOrNull())
        assertEquals(10, innerIf.prevCDG.singleOrNull()?.location?.region?.startLine)
    }

    @Ignore
    @Test
    fun testVerifyLoad() {
        val directory =
            Path.of("..", "..", "bdr", "testfiles", "2024-12-05_dji_verify_load", "exports")
        val result =
            analyze(
                listOf(
                    directory.resolve("unified/dji_fw_verify.c").toFile(),
                    directory.resolve("unified/dji_fw_verify.h").toFile(),
                    directory.resolve("unified/globals.h").toFile(),
                    directory.resolve("unified/libfw_util.so.c").toFile(),
                    directory.resolve("unified/libfw_util.so.h").toFile(),
                    directory.resolve("unified/libml_vcr_acc_drv.so.c").toFile(),
                    directory.resolve("unified/libml_vcr_acc_drv.so.h").toFile(),
                ),
                directory,
                true,
            ) {
                it.registerLanguage<CLanguage>()
                it.registerLanguage<CPPLanguage>()
                // it.registerPass<PointsToPass>()
                it.registerPass<ControlDependenceGraphPass>()
                it.loadIncludes(true)
            }
        print(result.functions)
    }

    @Ignore
    @Test
    fun testDoubleScalarmultVartime() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "double_scalarmult_vartime.cpp").toFile()),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(true)
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
    }

    @Ignore
    @Test
    fun testInc() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "increment.c").toFile()),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CLanguage>()
                it.loadIncludes(true)
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
        val a =
            result.refs.singleOrNull {
                it.name.localName == "a" && it.location?.region?.startLine == 6
            }
        assertEquals(2, a?.prevDFG?.size)
    }

    @Ignore
    @Test
    fun testsgx() {
        val result =
            analyze(
                listOf(
                    Path.of("..", "test-files", "sgx", "export.c").toFile(),
                    Path.of("..", "test-files", "sgx", "export.h").toFile(),
                ),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
                it.loadIncludes(true)
            }
        assertNotNull(result)
        assertEquals(2428, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Ignore
    @Test
    fun testWeirdFile() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "main.pp.cpp").toFile()),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
                it.loadIncludes(true)
            }
        assertNotNull(result)
        assertEquals(2428, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Ignore
    @Test
    fun testMorbi1() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "morbi.c").toFile()),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
                it.loadIncludes(true)
            }
        assertNotNull(result)
        assertEquals(2232, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Test
    @Ignore
    fun testMaxGhidra() {
        val result =
            analyze(
                listOf(
                    Path.of("..", "test-files", "max_ghidra", "export.c").toFile(),
                    Path.of("..", "test-files", "max_ghidra", "export.h").toFile(),
                ),
                Path.of("..", "test-files", "max_ghidra"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
                it.loadIncludes(true)
            }
        assertNotNull(result)
        assertEquals(1366, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Ignore
    @Test
    fun testDB() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "DB.h").toFile()),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
        assertEquals(6, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Ignore
    @Test
    fun testSign() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "sign.c").toFile()),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
        assertEquals(123, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Ignore
    @Test
    fun testAes() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "aes246ctr.c").toFile()),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
        assertEquals(92, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Ignore
    @Test
    fun testTest() {
        val result =
            analyze(
                listOf(Path.of("..", "test-files", "test.cpp").toFile()),
                Path.of("..", "test-files"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
        assertEquals(2, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Ignore
    @Test
    fun testRobocut() {
        val result =
            analyze(
                Files.walk(Path.of("..", "test-files", "robocut-master"), Int.MAX_VALUE)
                    .map(Path::toFile)
                    .filter { it.isFile }
                    .filter { it.name.endsWith(".cpp") || it.name.endsWith(".h") }
                    .toList(),
                Path.of("..", "test-files", "robocut-master"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
        assertEquals(209, result.refs.filter { it.prevDFG.size == 0 }.size)
    }

    @Test
    @Ignore
    fun testOpenVPN() {
        val result =
            analyze(
                Files.walk(Path.of("..", "test-files", "openvpn-master"), Int.MAX_VALUE)
                    .map(Path::toFile)
                    .filter { it.isFile }
                    .filter {
                        it.name.endsWith(".cpp") || it.name.endsWith(".c") || it.name.endsWith(".h")
                    }
                    .toList(),
                Path.of("..", "test-files", "openvpn-master"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
    }

    @Ignore
    @Test
    fun testAbyss() {
        val numbers = mutableListOf<Int>()
        for (file in
            Files.walk(Path.of("..", "test-files", "abyss-master"), Int.MAX_VALUE)
                .map(Path::toFile)
                .filter { it.isFile }
                .filter { it.name.endsWith(".cc") || it.name.endsWith(".h") }
                .toList()) {
            val result =
                analyze(listOf(file), Path.of("..", "test-files", "abyss-master"), true) {
                    it.registerLanguage<CPPLanguage>()
                    it.registerPass<ControlDependenceGraphPass>()
                }
            assertNotNull(result)
            numbers.add(result.refs.filter { it.prevDFG.size == 0 }.size)
        }
    }

    @Ignore
    @Test
    fun testMsiKeyboard() {
        val result =
            analyze(
                Files.walk(Path.of("..", "test-files", "msi-keyboard-1.1"), Int.MAX_VALUE)
                    .map(Path::toFile)
                    .filter { it.isFile }
                    .filter { it.name.endsWith(".cpp") || it.name.endsWith(".h") }
                    .toList(),
                Path.of("..", "test-files", "msi-keyboard-1.1"),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)
        assertEquals(19, result.refs.filter { it.prevDFG.size == 0 }.size)
    }
}
