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
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.StringType
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertTypes
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.BeforeAll

class RustTypeSystemTest : BaseTest() {

    companion object {
        private val topLevel = Path.of("src", "test", "resources", "rust", "types")
        private lateinit var tu: TranslationUnitDeclaration

        @JvmStatic
        @BeforeAll
        fun setUpOnce() {
            tu =
                analyzeAndGetFirstTU(
                    listOf(topLevel.resolve("types.rs").toFile()),
                    topLevel,
                    true,
                ) {
                    it.registerLanguage<RustLanguage>()
                }
        }
    }

    @Test
    fun testGenericTypesPreserveArguments() {
        assertNotNull(tu)

        val func = tu.functions["test_generic_types"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // Vec<i32> should have "Vec" as base name with i32 as generic arg
        val vecDecl = body.variables["v"]
        assertNotNull(vecDecl, "Should find variable 'v'")
        val vecType = vecDecl.type
        assertIs<ObjectType>(vecType)
        assertLocalName("Vec", vecType)
        val vecParameterType = vecType.generics.firstOrNull()
        assertNotNull(vecParameterType, "Vec<i32> should preserve generic args")
        assertIs<IntegerType>(vecParameterType)
        assertLocalName("i32", vecParameterType)

        // Option<bool>
        val optDecl = body.variables["o"]
        assertNotNull(optDecl, "Should find variable 'o'")
        val optType = optDecl.type
        assertIs<ObjectType>(optType)
        assertLocalName("Option", optType)
        assertTrue(optType.generics.isNotEmpty(), "Option<bool> should preserve generic args")
        val optParameterType = optType.generics.firstOrNull()
        assertNotNull(optParameterType)
        assertIs<BooleanType>(optParameterType)
        assertLocalName("bool", optParameterType)
    }

    @Test
    fun testTupleType() {
        assertNotNull(tu)

        val func = tu.functions["test_tuple_types"]
        assertNotNull(func)

        // Return type should be a TupleType with 3 elements
        val returnType = func.returnTypes.firstOrNull()
        assertIs<TupleType>(returnType, "Return type (i32, String, bool) should be TupleType")
        assertEquals(3, returnType.types.size)
        assertTypes(
            setOf(
                "i32" to IntegerType::class,
                "String" to StringType::class,
                "bool" to BooleanType::class,
            ),
            returnType.types,
        )
        assertEquals(
            setOf(
                "i32" to IntegerType::class,
                "String" to StringType::class,
                "bool" to BooleanType::class,
            ),
            returnType.types.map { Pair(it.name.toString(), it::class) }.toSet(),
        )
    }

    @Test
    fun testFunctionType() {
        assertNotNull(tu)

        val func = tu.functions["test_function_types"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val fDecl = body.variables["f"]
        assertNotNull(fDecl, "Should find variable 'f'")
        val fnType = fDecl.type
        assertIs<FunctionType>(fnType, "fn(i32) -> bool should be FunctionType")
        assertTypes(setOf("i32" to IntegerType::class), fnType.parameters)
        assertTypes(setOf("bool" to BooleanType::class), fnType.returnTypes)
    }

    @Test
    fun testNeverType() {
        assertNotNull(tu)

        val func = tu.functions["test_never_type"]
        assertNotNull(func)
        val returnType = func.returnTypes.firstOrNull()
        assertNotNull(returnType)
        assertLocalName("!", returnType)
    }

    @Test
    fun testUnitType() {
        assertNotNull(tu)

        val func = tu.functions["test_unit_type"]
        assertNotNull(func)
        val returnType = func.returnTypes.firstOrNull()
        assertNotNull(returnType)
        assertLocalName("()", returnType)
    }

    @Test
    fun testPointerTypes() {
        assertNotNull(tu)

        val func = tu.functions["test_pointer_types"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val pDecl = body.variables["p"]
        assertNotNull(pDecl, "Should find const pointer variable 'p'")
        assertIs<PointerType>(pDecl.type, "*const i32 should be PointerType")

        val qDecl = body.variables["q"]
        assertNotNull(qDecl, "Should find mut pointer variable 'q'")
    }

    @Test
    fun testArrayType() {
        assertNotNull(tu)

        val func = tu.functions["test_array_type"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // The function should parse without ProblemExpressions
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Array type test should not produce unknown problems: ${'$'}{problems.map { it.problem }}",
        )

        val arrDecl = body.variables["arr"]
        assertNotNull(arrDecl, "Should find array variable 'arr'")
    }

    @Test
    fun testLifetimeRef() {
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

    @Test
    fun testBranchBoundedType() {
        assertNotNull(tu)
        val func = tu.functions["test_bounded_type"]
        assertNotNull(func, "Should have test_bounded_type function")
        assertTrue(func.parameters.isNotEmpty(), "Should have parameter with bounded type")
    }

    @Test
    fun testBranchFnTypeNoReturn() {
        assertNotNull(tu)
        val func = tu.functions["test_fn_type_no_return"]
        assertNotNull(func, "Should have test_fn_type_no_return function")
        assertTrue(func.parameters.isNotEmpty(), "Should have fn type parameter")
    }

    @Test
    fun testRawPointers() {
        assertNotNull(tu)
        val func = tu.functions["test_raw_pointers"]
        assertNotNull(func)
        assertTrue(func.parameters.size >= 2, "Should have 2 pointer parameters")
    }

    @Test
    fun testDynTrait() {
        assertNotNull(tu)
        val func = tu.functions["test_dyn_trait"]
        assertNotNull(func)
        assertTrue(func.parameters.isNotEmpty(), "Should have dyn trait parameter")
    }

    @Test
    fun testImplTraitReturn() {
        assertNotNull(tu)
        val func = tu.functions["test_impl_trait"]
        assertNotNull(func, "Should have function returning impl trait")
    }

    @Test
    fun testGenericWithBounds() {
        assertNotNull(tu)
        val func = tu.functions["test_generic_with_bounds"]
        assertNotNull(func, "Should have generic function with bounds")
    }
}
