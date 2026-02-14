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

class RustDeclarationsExtendedTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    @Test
    fun testUnionDeclaration() {
        val tu = parseTU("declarations_extended.rs")
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
        val tu = parseTU("declarations_extended.rs")
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
        val tu = parseTU("declarations_extended.rs")
        assertNotNull(tu)

        val includes = tu.allChildren<IncludeDeclaration>()
        assertTrue(
            includes.any { it.name.localName == "alloc" },
            "Should have extern crate alloc: ${includes.map { it.name }}",
        )
    }

    @Test
    fun testInnerAttribute() {
        val tu = parseTU("declarations_extended.rs")
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
        val tu = parseTU("declarations_extended.rs")
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
        val tu = parseTU("declarations_extended.rs")
        assertNotNull(tu)

        val point = tu.records["Point"]
        assertNotNull(point, "Should find Point struct")

        val methods = point.methods

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
        val tu = parseTU("declarations_extended.rs")
        assertNotNull(tu)

        val shape = tu.records["Shape"]
        assertNotNull(shape, "Should find Shape trait")
        assertEquals("trait", shape.kind, "Shape should be a trait")

        // Should have methods
        val methods = shape.methods
        assertTrue(methods.any { it.name.localName == "area" }, "Trait should have area method")
        assertTrue(
            methods.any { it.name.localName == "name" },
            "Trait should have name method with default impl",
        )
    }

    @Test
    fun testWhereClauseWithMultipleBounds() {
        val tu = parseTU("declarations_extended.rs")
        assertNotNull(tu)

        val templates = tu.allChildren<FunctionTemplateDeclaration>()
        val process = templates.firstOrNull { it.name.localName == "process" }
        assertNotNull(process, "Should find process template function")

        // Should have type parameters
        val typeParams = process.allChildren<TypeParameterDeclaration>()
        assertTrue(typeParams.size >= 2, "Should have at least T and U type params")
    }
}
