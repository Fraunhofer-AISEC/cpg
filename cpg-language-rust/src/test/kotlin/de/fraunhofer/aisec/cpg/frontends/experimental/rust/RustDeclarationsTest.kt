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

class RustDeclarationsTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    @Test
    fun testConstItem() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val maxSize =
            tu.allChildren<VariableDeclaration>().firstOrNull { it.name.localName == "MAX_SIZE" }
        assertNotNull(maxSize, "Should have const MAX_SIZE")
        assertNotNull(maxSize.initializer, "Const should have initializer")
    }

    @Test
    fun testStaticItem() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val counter =
            tu.allChildren<VariableDeclaration>().firstOrNull {
                it.name.localName == "GLOBAL_COUNTER"
            }
        assertNotNull(counter, "Should have static GLOBAL_COUNTER")
    }

    @Test
    fun testUseDeclaration() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val imports = tu.allChildren<IncludeDeclaration>()
        assertTrue(imports.isNotEmpty(), "Should have use/import declarations")
    }

    @Test
    fun testFieldVisibility() {
        val tu = parseTU("adt/field_visibility.rs")
        assertNotNull(tu)

        val config = tu.records["Config"]
        assertNotNull(config)

        // "name" field should have pub modifier
        val nameField = config.fields.firstOrNull { it.name.localName == "name" }
        assertNotNull(nameField)
        assertTrue(nameField.modifiers.any { it == "pub" }, "name field should have pub modifier")

        // "version" field should have pub(crate) modifier
        val versionField = config.fields.firstOrNull { it.name.localName == "version" }
        assertNotNull(versionField)
        assertTrue(
            versionField.modifiers.any { it.contains("pub") },
            "version field should have pub(crate) modifier",
        )

        // "secret" field should NOT have visibility modifier
        val secretField = config.fields.firstOrNull { it.name.localName == "secret" }
        assertNotNull(secretField)
        assertTrue(
            secretField.modifiers.none { it.contains("pub") },
            "secret field should not have pub modifier",
        )
    }

    @Test
    fun testEnumDeclaration() {
        val tu = parseTU("adt/enums.rs")
        assertNotNull(tu)

        // Color enum with unit variants
        val color = tu.records["Color"]
        assertNotNull(color, "Should find Color enum")
        assertIs<EnumDeclaration>(color)
        assertEquals("enum", color.kind)
        assertEquals(3, color.entries.size)
        assertEquals("Red", color.entries.getOrNull(0)?.name?.localName)
        assertEquals("Green", color.entries.getOrNull(1)?.name?.localName)
        assertEquals("Blue", color.entries.getOrNull(2)?.name?.localName)

        // Shape enum with tuple variants
        val shape = tu.records["Shape"]
        assertNotNull(shape, "Should find Shape enum")
        assertIs<EnumDeclaration>(shape)
        assertEquals(2, shape.entries.size)
        assertEquals("Circle", shape.entries.getOrNull(0)?.name?.localName)
        assertEquals("Rectangle", shape.entries.getOrNull(1)?.name?.localName)

        // Message enum with mixed variants
        val message = tu.records["Message"]
        assertNotNull(message, "Should find Message enum")
        assertIs<EnumDeclaration>(message)
        assertEquals(3, message.entries.size)
        assertEquals("Quit", message.entries.getOrNull(0)?.name?.localName)
        assertEquals("Move", message.entries.getOrNull(1)?.name?.localName)
        assertEquals("Write", message.entries.getOrNull(2)?.name?.localName)
    }

    @Test
    fun testUnionDeclaration() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val records = tu.records
        val union = records["MyUnion"]
        assertNotNull(union, "Should find MyUnion")
        assertEquals("union", union.kind, "MyUnion should have kind 'union'")

        val fields = union.fields
        assertTrue(fields.isNotEmpty(), "Union should have fields")
        assertTrue(fields.any { it.name.localName == "i" }, "Union should have field 'i'")
        assertTrue(fields.any { it.name.localName == "f" }, "Union should have field 'f'")
    }

    @Test
    fun testExternBlock() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val namespaces = tu.allChildren<NamespaceDeclaration>()
        val extern = namespaces.firstOrNull { it.name.localName == "extern" }
        assertNotNull(extern, "Should find extern namespace")

        // Extern block should not produce a ProblemDeclaration
        val problems = tu.allChildren<ProblemDeclaration>()
        assertTrue(
            problems.none { it.problem.contains("foreign_mod_item") },
            "foreign_mod_item should not produce ProblemDeclaration",
        )
    }

    @Test
    fun testExternCrate() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val includes = tu.allChildren<IncludeDeclaration>()
        assertTrue(
            includes.any { it.name.localName == "alloc" },
            "Should have extern crate alloc: ${includes.map { it.name }}",
        )
    }

    @Test
    fun testInnerAttribute() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        // Inner attribute should produce a declaration with annotation
        val allAnnotations = tu.declarations.flatMap { it.annotations }.map { it.name.localName }
        assertTrue(
            allAnnotations.any { it.contains("allow") },
            "Should have #![allow(dead_code)] attribute: $allAnnotations",
        )
    }

    @Test
    fun testEmptyStatement() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val func = tu.functions["test_empty_statement"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val emptyStmts = body.allChildren<EmptyStatement>()
        assertTrue(emptyStmts.isNotEmpty(), "Should have empty statement for ';'")
    }

    @Test
    fun testSelfParameterRef() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val point = tu.records["Point"]
        assertNotNull(point, "Should find Point struct")

        val pointType = point.toType()
        val methods = pointType.methods

        // &self
        val distance = methods["distance"]
        assertNotNull(distance, "Should find distance method")
        assertNotNull(distance.receiver, "&self method should have receiver")

        // &mut self
        val reset = methods["reset"]
        assertNotNull(reset, "Should find reset method")
        assertNotNull(reset.receiver, "&mut self should have receiver")

        // self (by value)
        val consume = methods["consume"]
        assertNotNull(consume, "Should find consume method")
        assertNotNull(consume.receiver, "self method should have receiver")

        // static (no self)
        val new = methods["new"]
        assertNotNull(new, "Should find new method")
        assertNull(new.receiver, "Static method should not have receiver")
    }

    @Test
    fun testTraitWithAssociatedType() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val shape = tu.records["Shape"]
        assertNotNull(shape, "Should find Shape trait")
        assertEquals("trait", shape.kind, "Shape should be a trait")

        // Should have methods
        val shapeType = shape.toType()
        val methods = shapeType.methods
        assertTrue(methods.any { it.name.localName == "area" }, "Trait should have area method")
        assertTrue(
            methods.any { it.name.localName == "name" },
            "Trait should have name method with default impl",
        )
    }

    @Test
    fun testWhereClauseWithMultipleBounds() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)

        val templates = tu.allChildren<FunctionTemplateDeclaration>()
        val process = templates.firstOrNull { it.name.localName == "process" }
        assertNotNull(process, "Should find process template function")

        // Should have type parameters
        val typeParams = process.allChildren<TypeParameterDeclaration>()
        assertTrue(typeParams.size >= 2, "Should have at least T and U type params")
    }

    @Test
    fun testBranchLetMutPattern() {
        val tu = parseTU("control_flow/branch_coverage_statements.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_mut_pattern"]
        assertNotNull(func, "Should have test_let_mut_pattern function")
        val body = func.body as? Block
        assertNotNull(body)
        val decls = body.allChildren<VariableDeclaration>()
        assertTrue(decls.any { it.name.localName == "x" }, "Should have variable x")
    }

    @Test
    fun testBranchTupleStructs() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)
        val pair = tu.records["Pair"]
        assertNotNull(pair, "Should have Pair tuple struct")
        val triple = tu.records["Triple"]
        assertNotNull(triple, "Should have Triple tuple struct")
        val wrapper = tu.records["Wrapper"]
        assertNotNull(wrapper, "Should have Wrapper tuple struct")
    }

    @Test
    fun testBranchMutParam() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)
        val func = tu.functions["test_mut_param"]
        assertNotNull(func, "Should have test_mut_param function")
        assertTrue(func.parameters.isNotEmpty(), "Should have parameters")
    }

    @Test
    fun testBranchAnnotatedImpl() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)
        val record = tu.records["MyType"]
        assertNotNull(record, "Should have MyType record")
        val recordType = record.toType()
        val methods = recordType.methods
        assertTrue(
            methods.any { it.name.localName == "get_value" },
            "MyType should have get_value method",
        )
    }

    @Test
    fun testBranchAnnotatedModule() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)
        val namespaces = tu.allChildren<NamespaceDeclaration>()
        assertTrue(
            namespaces.any { it.name.localName == "annotated_mod" },
            "Should have annotated_mod namespace",
        )
    }

    @Test
    fun testBranchExternCBlock() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)
        val funcs = tu.allChildren<FunctionDeclaration>()
        assertTrue(
            funcs.any { it.name.localName == "c_abs" },
            "Should have extern C function c_abs",
        )
    }

    @Test
    fun testBranchEmptyStatementAndMacroDecl() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)
        val allDecls = tu.allChildren<Declaration>()
        assertTrue(allDecls.isNotEmpty(), "Should have declarations")
    }

    @Test
    fun testDeepLetMut() {
        val tu = parseTU("patterns/let_declarations_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_mut"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val assigns = body.allChildren<AssignExpression>()
        assertTrue(assigns.any { it.operatorCode == "+=" }, "Should have compound assignment")
    }

    @Test
    fun testDeepLetTypeAnnotation() {
        val tu = parseTU("patterns/let_declarations_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_type_annotation"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val decls = body.allChildren<VariableDeclaration>()
        assertTrue(decls.size >= 4, "Should have 4+ variable declarations")
    }

    @Test
    fun testDeepLetNoValue() {
        val tu = parseTU("patterns/let_declarations_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_no_value"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val decls = body.allChildren<VariableDeclaration>()
        assertTrue(decls.isNotEmpty(), "Should have variable declaration")
    }

    @Test
    fun testDeepLetDestructureStruct() {
        val tu = parseTU("patterns/let_declarations_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_destructure_struct"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Struct destructure should not produce unknown problems",
        )
    }

    @Test
    fun testDeepModuleNamespace() {
        val tu = parseTU("integration/comprehensive.rs")
        assertNotNull(tu)
        val namespaces = tu.allChildren<NamespaceDeclaration>()
        assertTrue(
            namespaces.any { it.name.localName == "inner_module" },
            "Should have inner module namespace",
        )
    }

    @Test
    fun testDeepExternBlockFunctions() {
        val tu = parseTU("declarations/declarations.rs")
        assertNotNull(tu)
        val allFuncs = tu.allChildren<FunctionDeclaration>()
        assertTrue(allFuncs.any { it.name.localName == "printf" }, "Should have extern fn printf")
    }

    @Test
    fun testDeepConstStatic() {
        val tu = parseTU("integration/comprehensive.rs")
        assertNotNull(tu)
        val decls = tu.allChildren<VariableDeclaration>()
        assertTrue(decls.any { it.name.localName == "MAX_SIZE" }, "Should have const MAX_SIZE")
        assertTrue(decls.any { it.name.localName == "GLOBAL" }, "Should have static GLOBAL")
    }

    @Test
    fun testDeepTypeAlias() {
        val tu = parseTU("integration/comprehensive.rs")
        assertNotNull(tu)
        val allDecls = tu.allChildren<Declaration>()
        assertTrue(
            allDecls.any { it.name.localName == "Coordinate" },
            "Should have Coordinate type alias",
        )
    }

    @Test
    fun testDeepUseDecl() {
        val tu = parseTU("integration/comprehensive.rs")
        assertNotNull(tu)
        val includes = tu.allChildren<IncludeDeclaration>()
        assertTrue(includes.isNotEmpty(), "Should have use/include declarations")
    }

    @Test
    fun testDeepNestedFn() {
        val tu = parseTU("types/advanced_features.rs")
        assertNotNull(tu)
        val func = tu.functions["test_nested_fn"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val innerFuncs = body.allChildren<FunctionDeclaration>()
        assertTrue(
            innerFuncs.any { it.name.localName == "inner_add" },
            "Should have nested inner_add function",
        )
    }

    @Test
    fun testDeepEnumVariants() {
        val tu = parseTU("adt/enums_advanced.rs")
        assertNotNull(tu)
        val msg = tu.records["Message"]
        assertNotNull(msg, "Should have Message enum")
    }

    @Test
    fun testDeepGenericStruct() {
        val tu = parseTU("types/generics_deep.rs")
        assertNotNull(tu)
        val templates = tu.allChildren<TemplateDeclaration>()
        assertTrue(
            templates.any { it.name.localName.contains("Container") },
            "Should have Container template",
        )
    }
}
