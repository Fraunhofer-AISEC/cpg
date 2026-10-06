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
package de.fraunhofer.aisec.cpg.frontends.golang.verified

import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage
import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import java.math.BigInteger
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Runs the verified translation of `cpg-verified` on the Go test files and compares its output with
 * the CPG that the Go frontend produces (without passes). The translation runs in-process via
 * [LeanTranslator]; the test is skipped if the native library has not been built
 * (`cpg-verified/native/build.sh`).
 */
class VerifiedTranslationTest {

    companion object {
        /** The native library built by `cpg-verified/native/build.sh`. */
        val libraryFile: File =
            listOf("dylib", "so")
                .map { File("../cpg-verified/.lake/build/native/libcpgverified.$it") }
                .firstOrNull { it.exists() }
                ?: File("../cpg-verified/.lake/build/native/libcpgverified.so")
    }

    /** Statistics about the comparison of one file. */
    private class Stats {
        var requests = 0
        var outsideSubset = 0
        var compared = 0
        var matched = 0
        var partial = 0
        val notFound = mutableListOf<String>()
        val mismatches = mutableListOf<String>()
    }

    @Test
    fun testGoFiles() {
        assumeTrue(libraryFile.exists(), "native library not found at $libraryFile")

        val topLevel = Path.of("src", "test", "resources", "golang")
        val files =
            topLevel
                .toFile()
                .walk()
                .filter { it.isFile && it.extension == "go" }
                .sortedBy { it.path }
                .toList()

        val total = Stats()
        for (file in files) {
            val stats = compareFile(file, topLevel)
            if (stats.requests == 0) continue
            println(
                "%-45s %4d requests, %4d outside subset, %4d compared, %4d matched (%d with problem subtrees), %d not found, %d mismatches"
                    .format(
                        file.relativeTo(topLevel.toFile()).path,
                        stats.requests,
                        stats.outsideSubset,
                        stats.compared,
                        stats.matched,
                        stats.partial,
                        stats.notFound.size,
                        stats.mismatches.size,
                    )
            )
            stats.notFound.forEach { println("    not found: $it") }
            stats.mismatches.forEach { println("    MISMATCH: $it") }

            total.requests += stats.requests
            total.outsideSubset += stats.outsideSubset
            total.compared += stats.compared
            total.matched += stats.matched
            total.partial += stats.partial
            total.notFound += stats.notFound
            total.mismatches += stats.mismatches
        }

        println(
            "TOTAL: ${total.requests} requests, ${total.outsideSubset} outside subset, " +
                "${total.compared} compared, ${total.matched} matched " +
                "(${total.partial} with problem subtrees), ${total.notFound.size} not found, " +
                "${total.mismatches.size} mismatches"
        )
        assertTrue(total.mismatches.isEmpty(), "${total.mismatches.size} mismatches")
    }

    @Test
    fun testConcurrentTranslation() {
        assumeTrue(libraryFile.exists(), "native library not found")

        // x + 1 at package level of package main
        val request =
            sexpOf(
                atom("expr"),
                sexpOf(atom("main")),
                sexpOf(),
                sexpOf(),
                sexpOf(),
                sexpOf(
                    atom("binary"),
                    atom(0),
                    atom(5),
                    atom("+"),
                    sexpOf(atom("ident"), atom(0), atom(1), atom("x")),
                    sexpOf(atom("lit"), atom(4), atom(5), atom("int"), atom("1")),
                ),
            )
        val expected = LeanTranslator.translate(libraryFile, listOf(request)).single()
        assertEquals(
            "(binary 0 5 + (reference 0 1 main.x) (literal 4 5 (int 1) (primitive int) ()))",
            expected.toString(),
        )

        // Every thread registers itself with the Lean runtime on first use
        val pool = Executors.newFixedThreadPool(8)
        try {
            val results =
                (1..64)
                    .map {
                        pool.submit<List<Sexp>> {
                            LeanTranslator.translate(libraryFile, List(100) { request })
                        }
                    }
                    .flatMap { it.get() }
            assertEquals(6400, results.size)
            assertTrue(results.all { it == expected })
        } finally {
            pool.shutdown()
        }
    }

    private fun compareFile(file: File, topLevel: Path): Stats {
        val stats = Stats()

        // The CPG of the Go frontend, without passes so that we see the frontend's output
        val tu =
            analyzeAndGetFirstTU(listOf(file), topLevel, false) {
                it.registerLanguage<GoLanguage>()
            }
        val byRegion = tu.allChildren<Expression>().groupBy { it.location?.region?.toString() }

        // The same file, parsed again, as input for the verified translation
        val fset = GoStandardLibrary.INSTANCE.NewFileSet()
        val raw = GoStandardLibrary.Parser.parseFile(fset, file.absolutePath)
        val requests = GoAstEncoder(raw).encode().requests
        stats.requests = requests.size

        val results = LeanTranslator.translate(libraryFile, requests.map { it.record })
        check(results.size == requests.size) {
            "expected ${requests.size} results, got ${results.size}"
        }

        fun region(start: Sexp, end: Sexp): String {
            val s = fset.position(start.toString().toInt())
            val e = fset.position(end.toString().toInt())
            return "${s.line}:${s.column}-${e.line}:${e.column}"
        }

        for ((request, result) in requests.zip(results)) {
            val items = (result as Sexp.SList).items
            when (items[0].toString()) {
                "error" -> {
                    stats.mismatches += "${file.name}: decoding failed: ${items[1]}"
                    continue
                }
                "problem" -> {
                    stats.outsideSubset++
                    continue
                }
            }

            val where = "${file.name}:${region(items[1], items[2])}"
            val candidates = byRegion[region(items[1], items[2])].orEmpty()
            if (candidates.isEmpty()) {
                stats.notFound += "$where ${fset.code(request.expr)}"
                continue
            }

            stats.compared++
            val diffs = candidates.map { compare(result, it, ::region) }
            val best = diffs.minBy { it.size }
            if (best.isEmpty()) {
                stats.matched++
                if (result.containsProblem()) stats.partial++
            } else {
                stats.mismatches += "$where ${fset.code(request.expr)}: ${best.joinToString("; ")}"
            }
        }

        return stats
    }

    private fun Sexp.containsProblem(): Boolean =
        this is Sexp.SList &&
            (items.firstOrNull()?.toString() == "problem" || items.any { it.containsProblem() })

    /**
     * Compares the translated expression [lean] with the [kotlin] node and returns the differences.
     * Sub-expressions that the verified translation does not support (problems) are not compared.
     */
    private fun compare(
        lean: Sexp,
        kotlin: Expression?,
        region: (Sexp, Sexp) -> String,
    ): List<String> {
        val items = (lean as Sexp.SList).items
        val kind = items[0].toString()
        if (kind == "problem") return emptyList()
        if (kotlin == null) return listOf("missing $kind")

        val diffs = mutableListOf<String>()
        val expected = region(items[1], items[2])
        val actual = kotlin.location?.region?.toString()
        if (expected != actual) diffs += "$kind at $expected but Kotlin node at $actual"

        when (kind) {
            "literal" -> {
                if (kotlin !is Literal<*>)
                    return diffs + "expected Literal, got ${kotlin::class.simpleName}"
                val value = items[3] as Sexp.SList
                val valueMatches =
                    when (value.items[0].toString()) {
                        "int" ->
                            (kotlin.value as? Number)?.let { BigInteger(it.toString()) } ==
                                BigInteger(value.items[1].toString())
                        "bool" -> kotlin.value == value.items[1].toString().toBoolean()
                        "str" -> kotlin.value == value.items[1].toString()
                        else -> kotlin.value == null
                    }
                if (!valueMatches) diffs += "value $value vs ${kotlin.value}"

                val type = items[4] as Sexp.SList
                val typeMatches =
                    when (type.items[0].toString()) {
                        "primitive" -> {
                            // The language resolves aliases, e.g., rune is int32
                            val name = type.items[1].toString()
                            val resolved = kotlin.language.builtInTypes[name]?.name?.toString()
                            kotlin.type.name.toString() == (resolved ?: name)
                        }
                        else -> kotlin.type is UnknownType
                    }
                if (!typeMatches) diffs += "type $type vs ${kotlin.type.name}"

                val name = (items[5] as Sexp.SList).items.firstOrNull()?.toString() ?: ""
                if (kotlin.name.toString() != name) diffs += "name '$name' vs '${kotlin.name}'"
            }
            "reference" -> {
                if (kotlin !is Reference || kotlin is MemberAccess) {
                    return diffs + "expected Reference, got ${kotlin::class.simpleName}"
                }
                if (kotlin.name.toString() != items[3].toString()) {
                    diffs += "name ${items[3]} vs ${kotlin.name}"
                }
            }
            "binary" -> {
                if (kotlin !is BinaryOperator) {
                    return diffs + "expected BinaryOperator, got ${kotlin::class.simpleName}"
                }
                if (kotlin.operatorCode != items[3].toString()) {
                    diffs += "operator ${items[3]} vs ${kotlin.operatorCode}"
                }
                diffs += compare(items[4], kotlin.lhs, region)
                diffs += compare(items[5], kotlin.rhs, region)
            }
            "unary" -> {
                if (kotlin !is UnaryOperator) {
                    return diffs + "expected UnaryOperator, got ${kotlin::class.simpleName}"
                }
                if (kotlin.operatorCode != items[3].toString()) {
                    diffs += "operator ${items[3]} vs ${kotlin.operatorCode}"
                }
                diffs += compare(items[4], kotlin.input, region)
            }
            "call" -> {
                if (kotlin !is Call || kotlin is MemberCall) {
                    return diffs + "expected Call, got ${kotlin::class.simpleName}"
                }
                diffs += compare(items[3], kotlin.callee, region)
                val args = (items[4] as Sexp.SList).items
                if (args.size != kotlin.arguments.size) {
                    diffs += "${args.size} arguments vs ${kotlin.arguments.size}"
                } else {
                    args.zip(kotlin.arguments).forEach { (a, k) -> diffs += compare(a, k, region) }
                }
            }
            else -> diffs += "unknown kind $kind"
        }
        return diffs
    }
}
