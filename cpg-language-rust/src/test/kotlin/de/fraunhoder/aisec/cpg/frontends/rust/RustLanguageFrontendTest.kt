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
package de.fraunhoder.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.frontends.rust.RustLanguage
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.collectAllPrevDFGPaths
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Switch
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RustLanguageFrontendTest {
    @Test
    fun testFunctionResolutionWithUse() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("use.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Assert functions exist and are not inferred
        val add = tu.functions["math::basic::add"]
        assertNotNull(add)
        assert(!add.isInferred)

        val sub = tu.functions["math::basic::sub"]
        assertNotNull(sub)
        assert(!sub.isInferred)

        val mul = tu.functions["math::advanced::mul"]
        assertNotNull(mul)
        assert(!mul.isInferred)

        val div = tu.functions["math::advanced::div"]
        assertNotNull(div)
        assert(!div.isInferred)

        val utilsHelper = tu.functions["utils::helper"]
        assertNotNull(utilsHelper)
        assert(!utilsHelper.isInferred)

        val innerHelper = tu.functions["utils::inner::helper"]
        assertNotNull(innerHelper)
        assert(!innerHelper.isInferred)

        val foo = tu.functions["extra::foo"]
        assertNotNull(foo)
        assert(!foo.isInferred)

        val bar = tu.functions["extra::bar"]
        assertNotNull(bar)
        assert(!bar.isInferred)

        val local = tu.functions["nested::local"]
        assertNotNull(local)
        assert(!local.isInferred)

        val nestedChildCall = tu.functions["nested::child::call"]
        assertNotNull(nestedChildCall)
        assert(!nestedChildCall.isInferred)

        // Assert calls exist and invoke the correct functions
        val addFnCall = tu.calls["add_fn"]
        assertNotNull(addFnCall)
        assert(addFnCall.invokes.contains(add))

        val subFnCall = tu.calls["sub_fn"]
        assertNotNull(subFnCall)
        assert(subFnCall.invokes.contains(sub))

        val mulFnCall = tu.calls["mul_fn"]
        assertNotNull(mulFnCall)
        assert(mulFnCall.invokes.contains(mul))

        val divFnCall = tu.calls["div_fn"]
        assertNotNull(divFnCall)
        assert(divFnCall.invokes.contains(div))

        val fooCall = tu.calls["foo"]
        assertNotNull(fooCall)
        assert(fooCall.invokes.contains(foo))

        val barCall = tu.calls["bar"]
        assertNotNull(barCall)
        assert(barCall.invokes.contains(bar))

        val rootHelperCall = tu.calls["root_helper"]
        assertNotNull(rootHelperCall)
        assert(rootHelperCall.invokes.contains(utilsHelper))

        val innerHelperFnCall = tu.calls["inner_helper_fn"]
        assertNotNull(innerHelperFnCall)
        assert(innerHelperFnCall.invokes.contains(innerHelper))

        val addLocalCall = tu.calls["add_local"]
        assertNotNull(addLocalCall)
        assert(addLocalCall.invokes.contains(add))

        val subLocalCall = tu.calls["sub_local"]
        assertNotNull(subLocalCall)
        assert(subLocalCall.invokes.contains(sub))

        val mulLocalCall = tu.calls["mul_local"]
        assertNotNull(mulLocalCall)
        assert(mulLocalCall.invokes.contains(mul))

        val parentLocal = tu.calls["parent_local"]
        assertNotNull(parentLocal)
        assert(parentLocal.invokes.contains(local))

        // For helper calls, since multiple with same name, filter and check
        val helperCalls = tu.calls.filter { it.name.localName == "helper" }
        assert(helperCalls.size == 5)

        // Assuming order: line 101, 106, 109, 117, 121
        assert(helperCalls[0].invokes.contains(utilsHelper)) // line 101
        assert(helperCalls[1].invokes.contains(innerHelper)) // line 106
        assert(helperCalls[2].invokes.contains(utilsHelper)) // line 109
        assert(helperCalls[3].invokes.contains(utilsHelper)) // line 117
        assert(helperCalls[4].invokes.contains(innerHelper)) // line 121

        // For the call to nested::child::call
        val callCall = tu.calls["call"]
        assertNotNull(callCall)
        assert(callCall.invokes.contains(nestedChildCall))
    }

    @Test
    fun testDFandMatchDeconstruction() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("match.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Test match in handle_wrap function
        // 1st match: possible results: 1,2,3
        testMatchStatement(tu, "handle_wrap", 0, listOf("1", "2", "3"))

        // Test match in handle_tuple function
        // 1st match: possible results: 4,5
        testMatchStatement(tu, "handle_tuple", 0, listOf("4", "5"))

        // Test match in handle_alternative function
        // 1st match: possible results: 4,5
        testMatchStatement(tu, "handle_alternative", 0, listOf("4", "5"))

        // Test match in handle_deep function
        // 1st match: possible results: 7,8,9
        testMatchStatement(tu, "handle_deep", 0, listOf("7", "8"))

        // 2nd match in handle_deep: possible results: 7,9
        testMatchStatement(tu, "handle_deep", 1, listOf("7", "9"))

        // Test match in process_all function
        // 1st match: possible results: 10,11,13,14,15
        testMatchStatement(tu, "process_all", 0, listOf("10", "11", "13", "14", "15"))
    }

    private fun testMatchStatement(
        tu: TranslationUnit,
        functionName: String,
        matchIndex: Int,
        expectedLiterals: List<String>,
    ) {
        // Get the translation unit as a proper type to access functions
        val function = tu.functions[functionName]

        assertNotNull(function, "Function '$functionName' should exist")

        // Get all switch statements in the function
        val switchStatements = SubgraphWalker.flattenAST(function).filterIsInstance<Switch>()
        assertTrue(
            switchStatements.size > matchIndex,
            "Function '$functionName' should have at least ${matchIndex + 1} match statement(s). Found: ${switchStatements.size}",
        )

        val switchStatement = switchStatements[matchIndex]

        // Collect all literals reachable from the switch through DFG using built-in function
        val reachableLiterals =
            switchStatement
                .collectAllPrevDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value.toString() }
                .toSet()

        // Verify all expected literals are reachable
        for (expectedLiteral in expectedLiterals) {
            assertTrue(
                reachableLiterals.contains(expectedLiteral),
                "Match #$matchIndex in function '$functionName' should have DFG path to literal '$expectedLiteral'. Found: $reachableLiterals",
            )
        }
    }

    @Ignore
    @Test
    fun testDFInLetStatements() {
        val topLevel = Path.of("src", "test", "resources")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("let.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val mainFunction = tu.functions["main"]
        assertNotNull(mainFunction, "main function should exist")

        // Test simple binding: let alpha = 77;
        testLetVariableDFG(tu, "alpha", listOf("77"))

        // Test tuple destructuring: let (beta, gamma) = (11, 22);

        testLetVariableDFG(tu, "beta", listOf("11"))
        testLetVariableDFG(tu, "gamma", listOf("22"))

        // Test nested tuple destructuring: let ((delta, epsilon), zeta) = ((33, 44), 55);
        testLetVariableDFG(tu, "delta", listOf("33"))
        testLetVariableDFG(tu, "epsilon", listOf("44"))
        testLetVariableDFG(tu, "zeta", listOf("55"))

        // Test struct destructuring: let Coord { a1, b1 } = c1; (where c1 = Coord { a1: 100, b1:
        // 200 })
        testLetVariableDFG(tu, "a1", listOf("100"))
        testLetVariableDFG(tu, "b1", listOf("200"))

        // Test struct destructuring with renaming: let Coord { a1: left_val, b1: _ } = c2; (where
        // c2 = Coord { a1: 300, b1: 400 })
        testLetVariableDFG(tu, "left_val", listOf("300"))

        // Test array destructuring: let [first_val, second_val, ..] = data_arr; (where data_arr =
        // [9, 8, 7, 6])
        testLetVariableDFG(tu, "first_val", listOf("9"))
        testLetVariableDFG(tu, "second_val", listOf("8"))

        // Test ref binding: let ref greet_ref = greeting; (where greeting = "world")
        testLetVariableDFG(tu, "greet_ref", listOf("\"world\""))

        // Test ref mut binding: let ref mut counter_ref = counter; (where counter = 12)
        testLetVariableDFG(tu, "counter_ref", listOf("12"))

        // Test enum destructuring: let Signal::Shift { dx, dy } = sig; (where sig = Signal::Shift {
        // dx: 70, dy: 90 })
        testLetVariableDFG(tu, "dx", listOf("70"))
        testLetVariableDFG(tu, "dy", listOf("90"))

        // Test if let: if let Some(found) = maybe_num { ... } (where maybe_num = Some(999))
        testLetVariableDFG(tu, "found", listOf("999"))

        // Test while let: while let Some(elem) = series[idx] { ... } (where series = [Some(5),
        // Some(6), Some(7), None])
        testLetVariableDFG(tu, "elem", listOf("5", "6", "7"))

        // Test let-else: let Some(extracted) = maybe_text else { ... } (where maybe_text =
        // Some("Pattern"))
        testLetVariableDFG(tu, "extracted", listOf("\"Pattern\""))

        // Test @ binding: let bound_val @ 5..=15 = value_check; (where value_check = 8)
        testLetVariableDFG(tu, "bound_val", listOf("8"))

        // Test OR pattern: let 3 | 4 | 5 = choice; (where choice = 3)
        testLetVariableDFG(tu, "choice", listOf("3"))

        // Test reference destructuring: let &(left_side, right_side) = ref_tuple; (where ref_tuple
        // = &(111, 222))
        testLetVariableDFG(tu, "left_side", listOf("111"))
        testLetVariableDFG(tu, "right_side", listOf("222"))

        // Test ignoring values: let (keep_a, _, keep_c) = (13, 14, 15);
        testLetVariableDFG(tu, "keep_a", listOf("13"))
        testLetVariableDFG(tu, "keep_c", listOf("15"))

        // Test slice pattern: let (start_val, .., end_val) = large_tuple; (where large_tuple = (21,
        // 22, 23, 24, 25))
        testLetVariableDFG(tu, "start_val", listOf("21"))
        testLetVariableDFG(tu, "end_val", listOf("25"))
    }

    private fun testLetVariableDFG(
        tu: TranslationUnit,
        variableName: String,
        expectedLiterals: List<String>,
    ) {
        // Find the variable declaration in the main function
        val mainFunction = tu.functions["main"]
        assertNotNull(mainFunction, "main function should exist")

        // Find all variable declarations with the given name
        val variables =
            SubgraphWalker.flattenAST(mainFunction).filterIsInstance<Variable>().filter {
                it.name.localName == variableName
            }

        assertTrue(variables.isNotEmpty(), "Variable '$variableName' should exist in main function")

        // For simplicity, take the first one (in case of multiple declarations with same name)
        val variable = variables.first()

        // Collect all literals reachable to this variable through DFG
        val reachableLiterals =
            variable
                .collectAllPrevDFGPaths()
                .flatMap { it.nodes }
                .filterIsInstance<Literal<*>>()
                .map { it.value.toString() }
                .toSet()

        // Verify all expected literals are reachable
        for (expectedLiteral in expectedLiterals) {
            assertTrue(
                reachableLiterals.contains(expectedLiteral),
                "Variable '$variableName' should have DFG path to literal '$expectedLiteral'. Found: $reachableLiterals",
            )
        }
    }
}
