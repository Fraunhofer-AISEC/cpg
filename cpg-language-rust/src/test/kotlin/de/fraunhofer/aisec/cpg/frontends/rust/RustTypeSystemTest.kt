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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustTypeSystemTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    @Test
    fun testGenericTypesPreserveArguments() {
        val tu = parseTU("type_system.rs")
        assertNotNull(tu)

        val func = tu.functions["test_generic_types"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // Vec<i32> should have "Vec" as base name with i32 as generic arg
        val decls = body.allChildren<VariableDeclaration>()
        val vecDecl = decls.firstOrNull { it.name.localName == "v" }
        assertNotNull(vecDecl, "Should find variable 'v'")
        val vecType = vecDecl.type
        assertIs<ObjectType>(vecType)
        assertEquals("Vec", vecType.name.localName)
        assertTrue(vecType.generics.isNotEmpty(), "Vec<i32> should preserve generic args")
        assertEquals("i32", vecType.generics.first().name.localName)

        // Option<bool>
        val optDecl = decls.firstOrNull { it.name.localName == "o" }
        assertNotNull(optDecl, "Should find variable 'o'")
        val optType = optDecl.type
        assertIs<ObjectType>(optType)
        assertEquals("Option", optType.name.localName)
        assertTrue(optType.generics.isNotEmpty(), "Option<bool> should preserve generic args")
    }

    @Test
    fun testTupleType() {
        val tu = parseTU("type_system.rs")
        assertNotNull(tu)

        val func = tu.functions["test_tuple_types"]
        assertNotNull(func)

        // Return type should be a TupleType with 3 elements
        val returnType = func.returnTypes.firstOrNull()
        assertIs<TupleType>(returnType, "Return type (i32, String, bool) should be TupleType")
        assertEquals(3, returnType.types.size)
    }

    @Test
    fun testFunctionType() {
        val tu = parseTU("type_system.rs")
        assertNotNull(tu)

        val func = tu.functions["test_function_types"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val decls = body.allChildren<VariableDeclaration>()
        val fDecl = decls.firstOrNull { it.name.localName == "f" }
        assertNotNull(fDecl, "Should find variable 'f'")
        val fnType = fDecl.type
        assertIs<FunctionType>(fnType, "fn(i32) -> bool should be FunctionType")
        assertEquals(1, fnType.parameters.size)
        assertEquals(1, fnType.returnTypes.size)
    }

    @Test
    fun testNeverType() {
        val tu = parseTU("type_system.rs")
        assertNotNull(tu)

        val func = tu.functions["test_never_type"]
        assertNotNull(func)
        val returnType = func.returnTypes.firstOrNull()
        assertNotNull(returnType)
        assertEquals("!", returnType.name.localName)
    }

    @Test
    fun testUnitType() {
        val tu = parseTU("type_system.rs")
        assertNotNull(tu)

        val func = tu.functions["test_unit_type"]
        assertNotNull(func)
        val returnType = func.returnTypes.firstOrNull()
        assertNotNull(returnType)
        assertEquals("()", returnType.name.localName)
    }

    @Test
    fun testPointerTypes() {
        val tu = parseTU("type_system.rs")
        assertNotNull(tu)

        val func = tu.functions["test_pointer_types"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val decls = body.allChildren<VariableDeclaration>()
        val pDecl = decls.firstOrNull { it.name.localName == "p" }
        assertNotNull(pDecl, "Should find const pointer variable 'p'")
        assertIs<PointerType>(pDecl.type, "*const i32 should be PointerType")

        val qDecl = decls.firstOrNull { it.name.localName == "q" }
        assertNotNull(qDecl, "Should find mut pointer variable 'q'")
    }

    @Test
    fun testArrayType() {
        val tu = parseTU("type_system.rs")
        assertNotNull(tu)

        val func = tu.functions["test_array_type"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // The function should parse without ProblemExpressions
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Array type test should not produce unknown problems: ${problems.map { it.problem }}",
        )

        val decls = body.allChildren<VariableDeclaration>()
        val arrDecl = decls.firstOrNull { it.name.localName == "arr" }
        assertNotNull(arrDecl, "Should find array variable 'arr'")
    }

    @Test
    fun testLifetimeRef() {
        val tu = parseTU("type_system.rs")
        assertNotNull(tu)

        val func = tu.functions["test_lifetime_ref"]
        assertNotNull(func)
        // Function should parse without problems
        val body = func.body as? Block
        assertNotNull(body)
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Lifetime refs should not produce problems: ${problems.map { it.problem }}",
        )
    }
}
