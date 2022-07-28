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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.graph.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.all
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.passes.*
import java.io.File
import java.nio.file.Path
import java.util.*
import org.junit.jupiter.api.Test

class SASTests {
    private fun prettyTable(table: List<List<String>>): String {
        var res = ""
        val maxWidth =
            Collections.max(
                table.filter { it.size > 1 }.map { l -> Collections.max(l.map { c -> c.length }) }
            )

        val separator = ("+" + "-".repeat(maxWidth + 2)).repeat(table[0].size) + "+\n"
        res += separator
        for (row in table) {
            if (row.size == 1) {
                res +=
                    "| ${row[0]} " +
                        " ".repeat(separator.length - 4 - row[0].length) +
                        "|\n" +
                        separator
            } else {
                for (cell in row) {
                    res += "| $cell " + " ".repeat(maxWidth - cell.length)
                }
                res += "|\n"
                res += separator
            }
        }
        return res
    }

    fun setupConfig(topLevel: File, fileName: List<String>): TranslationConfiguration? {
        for (filename in fileName) {
            if (topLevel.resolve(filename).exists()) {
                val translationConfiguration =
                    TranslationConfiguration.builder()
                        .sourceLocations(fileName.map { topLevel.resolve(it) })
                        .topLevel(topLevel)
                        .loadIncludes(true)
                        .debugParser(true)
                        .typeSystemActiveInFrontend(false)
                        .useParallelFrontends(true)
                        .defaultLanguages()
                        .registerLanguage(
                            LLVMIRLanguageFrontend::class.java,
                            LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                        )
                        .registerPass(CompressLLVMPass())
                        .defaultPasses()
                        .registerPass(StubRemoverPass())
                return translationConfiguration.build()
            }
        }
        return null
    }

    class StubRemoverPass : Pass() {
        override fun accept(t: TranslationResult?) {
            if (t == null) {
                return
            }

            val stubs = mutableMapOf<FunctionDeclaration, FunctionDeclaration>()

            for (funcDecl in t.all().filterIsInstance<FunctionDeclaration>()) {
                var funcStatements =
                    if (funcDecl.body == null && funcDecl.definition != null) {
                        (funcDecl.definition?.body as? CompoundStatement)?.statements
                    } else {
                        (funcDecl.body as? CompoundStatement)?.statements
                    }
                if (
                    funcStatements?.size == 1 &&
                        (funcStatements[0] as? LabelStatement)?.subStatement is CompoundStatement
                ) {
                    // There's only one label statement, so we can simply get this one. Sometimes,
                    // lifters or clang give a label to a BB even if there's nothing else...
                    funcStatements =
                        ((funcStatements[0] as LabelStatement).subStatement as CompoundStatement)
                            .statements
                }
                if (funcStatements?.size == 1 && funcStatements[0] is CallExpression) {
                    stubs[funcDecl] = (funcStatements[0] as CallExpression).invokes.first()
                } else if (
                    funcStatements?.size == 1 &&
                        (funcStatements[0] as? ReturnStatement)?.returnValue is CallExpression
                ) {
                    stubs[funcDecl] =
                        ((funcStatements[0] as? ReturnStatement)?.returnValue as CallExpression)
                            .invokes
                            .first()
                } else if (
                    funcStatements?.size == 2 &&
                        ((funcStatements[0] as? DeclarationStatement)?.singleDeclaration
                                as? VariableDeclaration)
                            ?.initializer is CallExpression
                ) {
                    val func =
                        (((funcStatements[0] as DeclarationStatement).singleDeclaration
                                    as VariableDeclaration)
                                .initializer as CallExpression)
                            .invokes
                            .firstOrNull()
                    if (func != null) stubs[funcDecl] = func
                }
            }

            val toReplace =
                t.all().filter { n ->
                    n is CallExpression && n.invokes.any { c -> stubs.keys.contains(c) }
                }
            for (callExpr in toReplace) {
                val mutableInvokes = (callExpr as CallExpression).invokes.toMutableList()
                mutableInvokes.replaceAll { old ->
                    if (stubs.keys.contains(old)) {
                        stubs[old]!!
                    } else {
                        old
                    }
                }
                callExpr.invokes = mutableInvokes
                callExpr.fqn = mutableInvokes.first().name
                callExpr.name = mutableInvokes.first().name
            }
            return
        }

        override fun cleanup() {
            // Nothing to do
            return
        }
    }

    class CPGQuery {
        fun querySourcecodeForViolation(tr: TranslationResult): Boolean {
            val callExprs =
                tr.all().filter { n ->
                    n is CallExpression &&
                        (n.fqn == "SSL_CTX_set_cipher_list" || n.fqn == "_SSL_CTX_set_cipher_list")
                }
            var violation = false
            println("Calls to SSL_CTX_set_cipher_list: ${callExprs.size}")
            for (expr in callExprs) {
                if ((expr as? CallExpression)?.arguments != null && expr.arguments.size > 1) {
                    val arg = expr.arguments[1]
                    val evalArg = ValueEvaluator().evaluate(arg)
                    println("Arg: $evalArg")
                    violation = violation || evalArg == "MD5"
                }
            }
            return violation
        }
    }

    @Test
    fun testVulnCheck() {
        val noIterations = 1
        // val filePath1 = Path.of("src", "test", "resources", "llvm", "vulnchecks").toFile()
        // val tc1 = setupConfig(filePath1, listOf("client.cpp"))
        // var resultTest = TranslationManager.builder().config(tc1!!).build().analyze().get()

        val architectures =
            listOf("macAArch64", "linuxx86", "linuxx86gcc", "linuxAArch64", "linuxArm")
        var filePath = Path.of("src", "test", "resources", "llvm", "vulnchecks").toFile()

        var startTime = System.currentTimeMillis()
        var tc = setupConfig(filePath, listOf("client.cpp"))
        var resultOrig = TranslationManager.builder().config(tc!!).build().analyze().get()
        var sourceViolation = CPGQuery().querySourcecodeForViolation(resultOrig)
        for (i in 1..noIterations) {
            resultOrig = TranslationManager.builder().config(tc).build().analyze().get()
            sourceViolation = CPGQuery().querySourcecodeForViolation(resultOrig)
        }
        val timeOrig = (System.currentTimeMillis() - startTime) / noIterations

        val outputList = mutableListOf<List<String>>()
        outputList.add(listOf("", "Analysis time [ms]", "# Nodes", "# Functions", "Problem found"))
        outputList.add(listOf("Source Code"))
        outputList.add(
            listOf(
                "Original file",
                "$timeOrig",
                "${resultOrig.all().size}",
                "${resultOrig.all().filterIsInstance<FunctionDeclaration>().size}",
                "$sourceViolation"
            )
        )

        for (arch in architectures) {
            filePath = Path.of("src", "test", "resources", "llvm", "vulnchecks", arch).toFile()
            startTime = System.currentTimeMillis()
            tc = setupConfig(filePath, listOf("client.ll"))
            var resultComp =
                if (tc != null) TranslationManager.builder().config(tc).build().analyze().get()
                else null
            var compiledViolation =
                if (resultComp != null) CPGQuery().querySourcecodeForViolation(resultComp)
                else "N/A"
            for (i in 1..noIterations) {
                resultComp =
                    if (tc != null) TranslationManager.builder().config(tc).build().analyze().get()
                    else null
                compiledViolation =
                    if (resultComp != null) CPGQuery().querySourcecodeForViolation(resultComp)
                    else "N/A"
            }
            val timeComp = (System.currentTimeMillis() - startTime) / noIterations

            filePath =
                Path.of("src", "test", "resources", "llvm", "vulnchecks", arch, "lifted").toFile()
            startTime = System.currentTimeMillis()
            tc = setupConfig(filePath, listOf("client.ll")) // , "if.ll", "main.ll"))
            var resultLifted = TranslationManager.builder().config(tc!!).build().analyze().get()
            var liftedViolation = CPGQuery().querySourcecodeForViolation(resultLifted)
            for (i in 1..noIterations) {
                resultLifted = TranslationManager.builder().config(tc).build().analyze().get()
                liftedViolation = CPGQuery().querySourcecodeForViolation(resultLifted)
            }
            val timeLifted = (System.currentTimeMillis() - startTime) / noIterations

            /*
             * For the mac example, the CPG fails to resolve the variable g3 in the decompiled code.
             * The CDT parser does not identify g3 as the name in the declaration of the variable.
             */
            startTime = System.currentTimeMillis()
            tc = setupConfig(filePath, listOf("client.c")) // , "if.c", "main.c"))
            var resultDecomp = TranslationManager.builder().config(tc!!).build().analyze().get()
            var decompViolation = CPGQuery().querySourcecodeForViolation(resultDecomp)
            for (i in 1..noIterations) {
                resultDecomp = TranslationManager.builder().config(tc).build().analyze().get()
                decompViolation = CPGQuery().querySourcecodeForViolation(resultDecomp)
            }
            val timeDecomp = (System.currentTimeMillis() - startTime) / noIterations

            outputList.add(
                listOf(
                    if ("gcc" in arch) {
                        "Results for g++ on $arch\n"
                    } else {
                        "Results for clang on $arch\n"
                    }
                )
            )

            if (resultComp != null) {
                outputList.add(
                    listOf(
                        "Compiled ll",
                        "$timeComp",
                        "${resultComp.all().size}",
                        "${resultComp.all().filterIsInstance<FunctionDeclaration>().size}",
                        "$compiledViolation"
                    )
                )
            }
            outputList.add(
                listOf(
                    "Lifted ll",
                    "$timeLifted",
                    "${resultLifted.all().size}",
                    "${resultLifted.all().filterIsInstance<FunctionDeclaration>().size}",
                    "$liftedViolation"
                )
            )
            outputList.add(
                listOf(
                    "Decompiled",
                    "$timeDecomp",
                    "${resultDecomp.all().size}",
                    "${resultDecomp.all().filterIsInstance<FunctionDeclaration>().size}",
                    "$decompViolation"
                )
            )
        }

        println(prettyTable(outputList))
    }

    @Test
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
                    false
                ) {
                    it.registerLanguage(
                            LLVMIRLanguageFrontend::class.java,
                            LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                        )
                        .registerPass(CompressLLVMPass())
                        .registerPass(TypeHierarchyResolver())
                        .registerPass(JavaExternalTypeHierarchyResolver())
                        .registerPass(ImportResolver())
                        .registerPass(VariableUsageResolver())
                        .registerPass(CallResolver())
                        .registerPass(EvaluationOrderGraphPass())
                        .registerPass(TypeResolver())
                        .registerPass(FilenameMapper())
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
}
