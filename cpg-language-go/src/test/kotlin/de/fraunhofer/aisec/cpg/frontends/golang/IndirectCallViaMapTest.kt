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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Lambda
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.functional.getFunctionPointerType
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.isNonLocal
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

/**
 * A function value that is only ever invoked indirectly -- read back out of a map, held in a local
 * variable, or received as a parameter -- still has to produce a call edge.
 *
 * [de.fraunhofer.aisec.cpg.passes.DynamicInvokeResolver] resolves these by walking *backwards*
 * along the DFG from the callee until it finds a function or a variable initialized with a lambda.
 * That only works if the DFG actually connects the function literal to the place it is called, so
 * any gap in the over-approximating DFG shows up here as a call without targets.
 *
 * This is the pattern behind miekg/dns' `TypeToRR`, a package-level map of constructors, which
 * guards the methods of every resource record type.
 */
class IndirectCallViaMapTest {

    private fun analyzeFixture() =
        analyze(
            listOf(
                Path.of("src", "test", "resources", "golang")
                    .resolve("indirect_call_via_map.go")
                    .toFile()
            ),
            Path.of("src", "test", "resources", "golang"),
            true,
        ) {
            it.registerLanguage<GoLanguage>()
            it.configurePass<SymbolResolver>(
                SymbolResolver.Configuration(includeAllOverrides = true)
            )
        }

    /** Reports the state of every link the resolver depends on, so the first gap is visible. */
    @Test
    fun testDiagnoseIndirectCallChain() {
        val result = analyzeFixture()
        assertNotNull(result)

        println("=== functions named like our indirect callees ===")
        for (f in result.functions) {
            if (f.name.localName in setOf("fn", "newFn", "f")) {
                println(
                    "  ${f.name} inferred=${f.isInferred} body=${f.body != null} type=${f.type}"
                )
            }
        }

        println("=== lambdas in the graph ===")
        for (l in result.allChildren<Lambda>()) {
            val f = l.function
            println(
                "  lambda at ${l.location?.region?.startLine} function=${f?.name} " +
                    "type=${f?.type} pointer=${f?.type?.pointer()} " +
                    "returnTypes=${f?.returnTypes} params=${f?.parameters?.map { it.type }}"
            )
        }

        println("=== the global map ===")
        val global = result.variables["TypeToRR"]
        println("  TypeToRR = ${global?.name} type=${global?.type}")
        println("  isGlobal=${global?.isGlobal} isNonLocal=${global?.isNonLocal}")
        println("  scope=${global?.scope?.javaClass?.simpleName}")
        for (r in result.allChildren<Reference>().filter { it.name.localName == "TypeToRR" }) {
            println(
                "  read@${r.location?.region?.startLine} access=${r.access} " +
                    "refersTo=${r.refersTo?.javaClass?.simpleName} " +
                    "prevDFG=${r.prevDFG.map { it.javaClass.simpleName }}"
            )
        }
        println(
            "  variable prevDFG = ${global?.prevDFGEdges?.map { it.granularity.javaClass.simpleName + "<-" + it.start.javaClass.simpleName }}"
        )
        for (s in result.allChildren<de.fraunhofer.aisec.cpg.graph.expressions.Subscription>()) {
            println(
                "  subscription on ${s.arrayExpression.name}: prevDFG edges = " +
                    "${s.prevDFGEdges.map { it.granularity.javaClass.simpleName + "<-" + it.start.javaClass.simpleName }}"
            )
        }
        println("  initializer = ${global?.initializer?.javaClass?.simpleName}")
        println(
            "  initializer prevDFG = ${global?.initializer?.prevDFG?.map { it.javaClass.simpleName }}"
        )

        for (fname in listOf("unpack", "direct", "viaParam", "apply")) {
            val f = result.functions[fname] ?: continue
            for (call in f.allChildren<Call>()) {
                val callee = call.callee
                if (callee !is Reference) continue
                // Only the indirect ones are interesting; a named call is resolved by the
                // SymbolResolver.
                println(
                    "  [$fname] call '${callee.name}' type=${callee.type} " +
                        "fptr=${getFunctionPointerType(callee) != null} " +
                        "refersTo=${callee.refersTo?.javaClass?.simpleName} " +
                        "invokes=${call.invokes.map { it.name.ifEmpty { "<anon@" + it.location?.region?.startLine + ">" } }}"
                )
                var cur: Node? = callee
                val chain = mutableListOf<String>()
                val seen = mutableSetOf<Node>()
                while (cur != null && seen.add(cur)) {
                    chain += "${cur.javaClass.simpleName}(${cur.name})"
                    cur =
                        cur.prevDFGEdges
                            .filter { it.granularity is FullDataflowGranularity }
                            .map { it.start }
                            .firstOrNull()
                }
                println("      backward DFG: ${chain.joinToString(" <- ")}")
            }
        }
    }

    /** The simplest case: a literal in a local variable, invoked through that variable. */
    @Test
    fun testCallThroughLocalVariableResolves() {
        val result = analyzeFixture()
        assertNotNull(result)

        val call =
            result.functions["direct"]?.allChildren<Call>()?.singleOrNull {
                (it.callee as? Reference)?.name?.localName == "fn"
            }
        assertNotNull(call, "direct should contain the fn() call")
        assertTrue(
            call.invokes.isNotEmpty(),
            "calling a local variable holding a function literal must produce a target",
        )
    }

    /** The case that matters for the corpus: a constructor read out of a package-level map. */
    @Test
    fun testCallThroughMapValueResolves() {
        val result = analyzeFixture()
        assertNotNull(result)

        val call =
            result.functions["unpack"]?.allChildren<Call>()?.singleOrNull {
                (it.callee as? Reference)?.name?.localName == "newFn"
            }
        assertNotNull(call, "unpack should contain the newFn() call")
        assertTrue(
            call.invokes.isNotEmpty(),
            "calling a function value read from a map must produce a target",
        )
    }
}
