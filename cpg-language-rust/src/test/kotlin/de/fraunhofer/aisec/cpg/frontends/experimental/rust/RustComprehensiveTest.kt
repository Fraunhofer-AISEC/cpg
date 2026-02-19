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
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustComprehensiveTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust", "integration")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    // === Literal Tests ===

    @Test
    fun testCharLiterals() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_char_literals"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val literals = body.allChildren<Literal<String>>()
        assertTrue(literals.any { it.value == "a" }, "Should have char literal 'a'")
        assertTrue(literals.any { it.value == "\\n" }, "Should have char literal '\\n'")
    }

    @Test
    fun testFloatLiterals() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_float_literals"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        @Suppress("UNCHECKED_CAST")
        val floats = body.allChildren<Literal<*>>().filter { it.value is Double }
        assertTrue(floats.isNotEmpty(), "Should have float literals")
        assertTrue(
            floats.any { (it.value as Double) == 3.14 },
            "Should have 3.14: ${floats.map { it.value }}",
        )
    }

    @Test
    fun testUnitExpression() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_unit_expression"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val unitLits = body.allChildren<Literal<*>>().filter { it.value == null }
        assertTrue(unitLits.isNotEmpty(), "Should have unit expression literal (null value)")
    }

    @Test
    fun testParenthesizedExpression() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_parenthesized"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // (1 + 2) * 3 — should NOT produce a ProblemExpression
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Parenthesized expression should not be unknown: ${problems.map { it.problem }}",
        )

        val binOps = body.allChildren<BinaryOperator>()
        assertTrue(binOps.any { it.operatorCode == "*" }, "Should have multiplication operator")
        assertTrue(binOps.any { it.operatorCode == "+" }, "Should have addition operator")
    }

    // === Operator Tests ===

    @Test
    fun testTryOperator() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_try_operator"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val tryOps = body.allChildren<UnaryOperator>().filter { it.operatorCode == "?" }
        assertTrue(tryOps.isNotEmpty(), "Should have ? try operator")
        assertTrue(tryOps.first().isPostfix, "? should be postfix")
    }

    @Test
    fun testAwaitExpression() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_await"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val awaits = body.allChildren<UnaryOperator>().filter { it.operatorCode == "await" }
        assertTrue(awaits.isNotEmpty(), "Should have await operator")
    }

    @Test
    fun testCompoundAssignments() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_compound_assignments"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val assigns = body.allChildren<AssignExpression>()
        val opCodes = assigns.map { it.operatorCode }.toSet()
        assertTrue("+=" in opCodes, "Should have +=")
        assertTrue("-=" in opCodes, "Should have -=")
        assertTrue("*=" in opCodes, "Should have *=")
        assertTrue("/=" in opCodes, "Should have /=")
        assertTrue("%=" in opCodes, "Should have %=")
        assertTrue("&=" in opCodes, "Should have &=")
        assertTrue("|=" in opCodes, "Should have |=")
        assertTrue("^=" in opCodes, "Should have ^=")
        assertTrue("<<=" in opCodes, "Should have <<=")
        assertTrue(">>=" in opCodes, "Should have >>=")
    }

    // === Declaration Tests ===

    @Test
    fun testSelfParameterVariations() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        val records = tu.records
        val myStruct = records["MyStruct"]
        assertNotNull(myStruct, "Should find MyStruct")

        val myStructType = myStruct.toType()
        val methods = myStructType.methods
        val byRef = methods["by_ref"]
        assertNotNull(byRef, "Should find by_ref method")
        assertNotNull(byRef.receiver, "&self method should have receiver")

        val byMutRef = methods["by_mut_ref"]
        assertNotNull(byMutRef, "Should find by_mut_ref method")
        assertNotNull(byMutRef.receiver, "&mut self method should have receiver")

        val byValue = methods["by_value"]
        assertNotNull(byValue, "Should find by_value method")
        assertNotNull(byValue.receiver, "self method should have receiver")

        val staticMethod = methods["static_method"]
        assertNotNull(staticMethod, "Should find static_method")
        assertNull(staticMethod.receiver, "Static method should not have receiver")
    }

    @Test
    fun testTraitWithAssociatedType() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        val records = tu.records
        val processor = records["Processor"]
        assertNotNull(processor, "Should find Processor trait")
        assertEquals("trait", processor.kind, "Processor should be a trait")

        // Should have process and default_method
        val methods = processor.methods
        assertTrue(methods.isNotEmpty(), "Trait should have methods")
    }

    @Test
    fun testWhereClause() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        // complex_where should be a template with type parameters
        val templates = tu.allChildren<FunctionTemplateDeclaration>()
        val whereFunc = templates.firstOrNull { it.name.localName == "complex_where" }
        assertNotNull(whereFunc, "Should find complex_where template")
    }

    @Test
    fun testTypeAlias() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        val typedefs = tu.allChildren<TypedefDeclaration>()
        val coord = typedefs.firstOrNull { it.alias.name.localName == "Coordinate" }
        assertNotNull(coord, "Should find Coordinate type alias")
    }

    @Test
    fun testUseDeclaration() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        val includes = tu.allChildren<IncludeDeclaration>()
        assertTrue(includes.isNotEmpty(), "Should have use declarations")
        assertTrue(
            includes.any { it.name.localName.contains("HashMap") },
            "Should include HashMap: ${includes.map { it.name }}",
        )
    }

    @Test
    fun testConstAndStatic() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        val vars = tu.allChildren<VariableDeclaration>()
        val maxSize = vars.firstOrNull { it.name.localName == "MAX_SIZE" }
        assertNotNull(maxSize, "Should find MAX_SIZE const")
        assertTrue("const" in maxSize.modifiers, "MAX_SIZE should have const modifier")

        val global = vars.firstOrNull { it.name.localName == "GLOBAL" }
        assertNotNull(global, "Should find GLOBAL static")
        assertTrue("static" in global.modifiers, "GLOBAL should have static modifier")

        val mutGlobal = vars.firstOrNull { it.name.localName == "MUTABLE_GLOBAL" }
        assertNotNull(mutGlobal, "Should find MUTABLE_GLOBAL")
        assertTrue("mut" in mutGlobal.modifiers, "MUTABLE_GLOBAL should have mut modifier")
    }

    @Test
    fun testMacroDefinition() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        val macros =
            tu.allChildren<FunctionDeclaration>().filter { decl ->
                decl.annotations.any { it.name.localName == "macro_rules" }
            }
        assertTrue(macros.isNotEmpty(), "Should find macro_rules! definition")
        assertTrue(macros.any { it.name.localName == "my_macro" }, "Should find my_macro")
    }

    @Test
    fun testEnumWithVariants() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        val enums = tu.allChildren<EnumDeclaration>()
        val message = enums.firstOrNull { it.name.localName == "Message" }
        assertNotNull(message, "Should find Message enum")
        assertEquals(4, message.entries.size, "Message should have 4 variants")

        val variantNames = message.entries.map { it.name.localName }.toSet()
        assertTrue("Quit" in variantNames, "Should have Quit variant")
        assertTrue("Move" in variantNames, "Should have Move variant")
        assertTrue("Write" in variantNames, "Should have Write variant")
        assertTrue("Color" in variantNames, "Should have Color variant")
    }

    // === Expression Tests ===

    @Test
    fun testStructExpression() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_struct_spread"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val constructs = body.allChildren<ConstructExpression>()
        assertTrue(constructs.size >= 2, "Should have at least 2 struct expressions")
    }

    @Test
    fun testMatchWithGuard() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_match_guard"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val switches = body.allChildren<SwitchStatement>()
        assertTrue(switches.isNotEmpty(), "Should have match/switch expression")

        val cases = body.allChildren<CaseStatement>()
        assertTrue(cases.size >= 3, "Should have at least 3 match arms")
    }

    @Test
    fun testReferenceExpressions() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_references"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val refs = body.allChildren<UnaryOperator>()
        assertTrue(refs.any { it.operatorCode == "&" }, "Should have & reference")
        assertTrue(refs.any { it.operatorCode == "&mut" }, "Should have &mut reference")
    }

    @Test
    fun testUnsafeBlock() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_unsafe"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val blocks = body.allChildren<Block>()
        val unsafeBlock =
            blocks.firstOrNull { block -> block.annotations.any { it.name.localName == "unsafe" } }
        assertNotNull(unsafeBlock, "Should have unsafe block")
    }

    @Test
    fun testAsyncBlock() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_async_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val blocks = body.allChildren<Block>()
        val asyncBlock =
            blocks.firstOrNull { block -> block.annotations.any { it.name.localName == "async" } }
        assertNotNull(asyncBlock, "Should have async block")
    }

    @Test
    fun testRawStringLiterals() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_raw_strings"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val literals = body.allChildren<Literal<String>>()
        assertTrue(
            literals.any { it.value == "hello\\nworld" },
            "Should have raw string without escape processing: ${literals.map { it.value }}",
        )
    }

    @Test
    fun testRangeExpressions() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_ranges"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val ranges = body.allChildren<RangeExpression>()
        assertTrue(ranges.size >= 2, "Should have at least 2 range expressions")
    }

    // === Control Flow Tests ===

    @Test
    fun testForLoop() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_for_loop"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val forStmts = body.allChildren<ForEachStatement>()
        assertTrue(forStmts.isNotEmpty(), "Should have for loop")
    }

    @Test
    fun testWhileAndLoop() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_loops"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val whileStmts = body.allChildren<WhileStatement>()
        assertTrue(whileStmts.isNotEmpty(), "Should have while loop")
    }

    @Test
    fun testIfLet() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val ifStmts = body.allChildren<IfStatement>()
        assertTrue(ifStmts.isNotEmpty(), "Should have if-let statement")
    }

    @Test
    fun testClosureWithTypes() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_typed_closure"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val lambdas = body.allChildren<LambdaExpression>()
        assertTrue(lambdas.isNotEmpty(), "Should have closures")
        val typedClosure = lambdas.first()
        assertNotNull(typedClosure.function, "Closure should have inner function")
        assertTrue(
            typedClosure.function!!.parameters.isNotEmpty(),
            "Typed closure should have parameters",
        )
    }

    @Test
    fun testNegativeIntegers() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_negative_ints"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // In expression context, -42 is parsed as unary_expression (- 42), not negative_literal
        val negOps = body.allChildren<UnaryOperator>().filter { it.operatorCode == "-" }
        assertTrue(negOps.isNotEmpty(), "Should have unary negation operators")
    }

    @Test
    fun testModule() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)

        val namespaces = tu.allChildren<NamespaceDeclaration>()
        val innerMod = namespaces.firstOrNull { it.name.localName == "inner_module" }
        assertNotNull(innerMod, "Should find inner_module")

        val innerFuncs = innerMod.allChildren<FunctionDeclaration>()
        assertTrue(
            innerFuncs.any { it.name.localName == "inner_fn" },
            "Module should contain inner_fn",
        )
    }

    @Test
    fun testScopedIdentifiers() {
        val tu = parseTU("comprehensive.rs")
        assertNotNull(tu)
        val func = tu.functions["test_scoped_ids"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val refs = body.allChildren<Reference>()
        assertTrue(
            refs.any { it.name.toString().contains("MAX") },
            "Should have scoped identifier: ${refs.map { it.name }}",
        )
    }
}
