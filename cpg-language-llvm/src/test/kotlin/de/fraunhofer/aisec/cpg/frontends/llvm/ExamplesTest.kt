/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.graph.all
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.passes.CompressLLVMPass
import java.nio.file.Path
import kotlin.test.Ignore
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("llvm-examples")
class ExamplesTest {
    @Test
    fun testRust() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("rust_sample.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    @Ignore
    fun testRustPerformance() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "rust_deps")
        val allFiles =
            listOf(
                "addr2line-afac5bae8fec16b6.ll",
                "adler-7ed5b10d0bcaf3c3.ll",
                "alloc-04ee48fceb10d7c0.ll",
                "cfg_if-5eaf120bd4404621.ll",
                "compiler_builtins-f8373ef78ecdac9a.ll",
                "core-e4cbdb9a079d6d85.ll",
                "gimli-42844e93de3eb724.ll",
                "hashbrown-a8f963e8367825e2.ll",
                "libc-887d7ac704a6f1c9.ll",
                "memchr-f368e2194464f0ec.ll",
                "miniz_oxide-bdfdbcfc5f7f7f1b.ll",
                "object-fbe95894d6e4ba98.ll",
                "panic_abort-3f2cdfc2f6526910.ll",
                "panic_unwind-819ee3fd9119ecd4.ll",
                "proc_macro-ad55da585703b268.ll",
                "rustc_demangle-0523e76fb0a24ded.ll",
                "rustc_std_workspace_alloc-16b7c4c0daaa10d9.ll",
                "rustc_std_workspace_core-a0483d65bbef920e.ll",
                "std-b98b422506f4d0f3.ll",
                "std_detect-fecce841f4b381cf.ll",
                "unwind-89fbc502220cd1f2.ll"
            )

        val lines = mutableMapOf<String, Int>()
        lines["addr2line-afac5bae8fec16b6.ll"] = 879
        lines["adler-7ed5b10d0bcaf3c3.ll"] = 488
        lines["alloc-04ee48fceb10d7c0.ll"] = 4925
        lines["cfg_if-5eaf120bd4404621.ll"] = 9
        lines["compiler_builtins-f8373ef78ecdac9a.ll"] = 9990
        lines["core-e4cbdb9a079d6d85.ll"] = 80193
        lines["gimli-42844e93de3eb724.ll"] = 23702
        lines["hashbrown-a8f963e8367825e2.ll"] = 276
        lines["libc-887d7ac704a6f1c9.ll"] = 1477
        lines["memchr-f368e2194464f0ec.ll"] = 11063
        lines["miniz_oxide-bdfdbcfc5f7f7f1b.ll"] = 15760
        lines["object-fbe95894d6e4ba98.ll"] = 14174
        lines["panic_abort-3f2cdfc2f6526910.ll"] = 71
        lines["panic_unwind-819ee3fd9119ecd4.ll"] = 927
        lines["proc_macro-ad55da585703b268.ll"] = 92115
        lines["rustc_demangle-0523e76fb0a24ded.ll"] = 14669
        lines["rustc_std_workspace_alloc-16b7c4c0daaa10d9.ll"] = 9
        lines["rustc_std_workspace_core-a0483d65bbef920e.ll"] = 9
        lines["std-b98b422506f4d0f3.ll"] = 157377
        lines["std_detect-fecce841f4b381cf.ll"] = 558
        lines["unwind-89fbc502220cd1f2.ll"] = 106

        var atloc = ""
        var atnodes = ""
        var table = ""

        for (file in allFiles) {
            val start = System.currentTimeMillis()
            val tu =
                TestUtils.analyzeAndGetFirstTU(
                    listOf(topLevel.resolve(file).toFile()),
                    topLevel,
                    true
                ) {
                    it.registerLanguage(
                            LLVMIRLanguageFrontend::class.java,
                            LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                        )
                        .registerPass(CompressLLVMPass())
                }
            val time = System.currentTimeMillis() - start
            val problemNodes = tu.all().filterIsInstance<ProblemNode>().size
            val functionNodes = tu.all().filterIsInstance<FunctionDeclaration>().size
            val loc = lines[file]
            val nodes = tu.all().size

            val str = "$file & $loc & $nodes & $functionNodes & $problemNodes & $time \\\\\\hline\n"
            println(str)
            table += str
            atloc += "($loc,$time) "
            atnodes += "($nodes,$time) "
            println(table)
            println(atloc)
            println(atnodes)
        }

        println(table)
        println(atloc)
        println(atnodes)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "llvm")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("client.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedClient() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("client.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedIf() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("if.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedMain() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("main.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }
}
