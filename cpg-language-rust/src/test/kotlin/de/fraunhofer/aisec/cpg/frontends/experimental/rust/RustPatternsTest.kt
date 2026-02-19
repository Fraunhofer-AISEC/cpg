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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustPatternsTest : BaseTest() {
    @Test
    fun testStructPattern() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("patterns.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_struct_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // Struct pattern should not produce ProblemExpressions
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Struct pattern should not produce ProblemExpression: ${problems.map { it.problem }}",
        )
    }

    @Test
    fun testOrPattern() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("patterns.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_or_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // Or pattern should not produce ProblemExpressions
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Or pattern should not produce ProblemExpression: ${problems.map { it.problem }}",
        )
    }

    @Test
    fun testBranchRefPatternBinding() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_statements.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_ref_pattern_binding"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have match/switch with ref pattern")
    }

    @Test
    fun testBranchMutPatternBinding() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_statements.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_mut_pattern_binding"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have match/switch with mut pattern")
    }

    @Test
    fun testBranchMatchWildcard() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_wildcard"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms")
    }

    @Test
    fun testBranchEmptyMatchArm() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_empty_match_arm"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms")
    }

    @Test
    fun testBranchNestedTupleMatch() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_nested_tuple_match"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms")
    }

    @Test
    fun testBranchEnumPatterns() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_edge_cases.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_enum_patterns"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3+ match arms for enum")
    }

    @Test
    fun testBranchMatchWithGuard() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_with_guard"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 4, "Should have 4 match arms")
    }

    @Test
    fun testBranchSingleAlternativePattern() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_single_alternative_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 2, "Should have match arms")
    }

    @Test
    fun testBranchStructPatternMatch() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_struct_pattern_match"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 struct pattern match arms")
    }

    @Test
    fun testBranchSlicePattern() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_slice_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 slice pattern match arms")
    }

    @Test
    fun testBranchFieldPatternBinding() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_field_pattern_binding"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.isNotEmpty(), "Should have field pattern match arm")
    }

    @Test
    fun testBranchMatchEmptyArms() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_empty_arms"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms")
    }

    @Test
    fun testBranchOrPatternComplex() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_or_pattern_complex"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3+ match arms")
    }

    @Test
    fun testBranchNestedMatch() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("branch_coverage_targeted.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_nested_match"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.size >= 2, "Should have nested match")
    }

    @Test
    fun testDeepMatchStructPattern() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_struct_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have switch from match")
    }

    @Test
    fun testDeepMatchStructRename() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_struct_rename"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have switch from match with renamed fields")
    }

    @Test
    fun testDeepMatchTupleStruct() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_tuple_struct"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms")
    }

    @Test
    fun testDeepMatchOrPattern() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_or_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val binOps = body.allChildren<BinaryOperator>()
        assertTrue(binOps.any { it.operatorCode == "|" }, "Should have or-pattern operator")
    }

    @Test
    fun testDeepMatchRefPattern() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_ref_pattern"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have match/switch")
    }

    @Test
    fun testDeepMatchGuard() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_guard"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 4, "Should have 4 match arms")
    }

    @Test
    fun testDeepMatchNestedTuple() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_nested_tuple"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have 3 match arms")
    }

    @Test
    fun testDeepMatchSlice() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_slice"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have switch from slice match")
    }

    @Test
    fun testDeepMatchNegativeLiteral() {
        val topLevel = Path.of("src", "test", "resources", "rust", "patterns")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("match_patterns_deep.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_match_negative_literal"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 4, "Should have 4 match arms")
    }

    @Test
    fun testDeepEnumMatch() {
        val topLevel = Path.of("src", "test", "resources", "rust", "adt")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("enums_advanced.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)
        val func = tu.functions["test_enum_match"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 4, "Should have 4 match arms for Message")
    }
}
