/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements.types

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*

internal class TypeTests : BaseTest() {
    @Test
    fun reference() {
        val language = CPPLanguage(TranslationContext(TranslationConfiguration.builder().build()))
        val ctx = language.ctx
        assertNotNull(ctx)

        val objectType: Type = IntegerType(ctx, "int", 32, language, NumericType.Modifier.SIGNED)
        val pointerType: Type = PointerType(ctx, objectType, PointerType.PointerOrigin.POINTER)
        val unknownType: Type = UnknownType.getUnknownType(language)
        val incompleteType: Type = IncompleteType(ctx, language)
        val parameterList =
            listOf<Type>(IntegerType(ctx, "int", 32, language, NumericType.Modifier.SIGNED))
        val functionPointerType: Type =
            FunctionPointerType(ctx, parameterList, language, IncompleteType(ctx, language))

        // Test 1: ObjectType becomes PointerType containing the original ObjectType as ElementType
        assertEquals(
            PointerType(ctx, objectType, PointerType.PointerOrigin.POINTER),
            objectType.reference(PointerType.PointerOrigin.POINTER),
        )

        // Test 2: Existing PointerType adds one level more of references as ElementType
        assertEquals(
            PointerType(ctx, pointerType, PointerType.PointerOrigin.POINTER),
            pointerType.reference(PointerType.PointerOrigin.POINTER),
        )

        // Test 3: UnknownType cannot be referenced
        assertEquals(unknownType, unknownType.reference(null))

        // Test 4: IncompleteType can be referenced e.g. void*
        assertEquals(
            PointerType(ctx, incompleteType, PointerType.PointerOrigin.POINTER),
            incompleteType.reference(PointerType.PointerOrigin.POINTER),
        )

        // Test 5: Create reference to function pointer = pointer to function pointer
        assertEquals(
            PointerType(ctx, functionPointerType, PointerType.PointerOrigin.POINTER),
            functionPointerType.reference(PointerType.PointerOrigin.POINTER),
        )
    }

    @Test
    fun dereference() {
        val language = CPPLanguage(TranslationContext(TranslationConfiguration.builder().build()))
        val ctx = language.ctx
        assertNotNull(ctx)

        val objectType: Type = IntegerType(ctx, "int", 32, language, NumericType.Modifier.SIGNED)
        val pointerType: Type = PointerType(ctx, objectType, PointerType.PointerOrigin.POINTER)
        val unknownType: Type = UnknownType.getUnknownType(language)
        val incompleteType: Type = IncompleteType(ctx, language)
        val parameterList =
            listOf<Type>(IntegerType(ctx, "int", 32, language, NumericType.Modifier.SIGNED))
        val functionPointerType: Type =
            FunctionPointerType(ctx, parameterList, language, IncompleteType(ctx, language))

        // Test 1: Dereferencing an ObjectType results in an UnknownType, since we cannot track the
        // type
        // of the corresponding memory
        assertEquals(UnknownType.getUnknownType(language), objectType.dereference())

        // Test 2: Dereferencing a PointerType results in the corresponding elementType of the
        // PointerType (can also be another PointerType)
        assertEquals(objectType, pointerType.dereference())

        // Test 3: Dereferencing unknown or incomplete type results in the same type
        assertEquals(unknownType, unknownType.dereference())
        assertEquals(incompleteType, incompleteType.dereference())

        // Test 5: Due to the definition in the C-Standard dereferencing function pointer yields the
        // same function pointer
        assertEquals(functionPointerType, functionPointerType.dereference())
    }

    /**
     * Test for usage of getTypeStringFromDeclarator to determine function pointer raw type string
     *
     * @throws Exception Any exception thrown during the analysis process
     */
    @Test
    @Throws(Exception::class)
    fun testFunctionPointerTypes() {
        val topLevel = Path.of("src", "test", "resources", "types")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("fptr_type.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val language = tu.ctx?.availableLanguage<CPPLanguage>()
        assertNotNull(language)

        val ctx = language.ctx
        assertNotNull(ctx)

        val noParamType =
            FunctionPointerType(ctx, emptyList(), language, IncompleteType(ctx, language))
        val oneParamType =
            FunctionPointerType(
                ctx,
                listOf<Type>(tu.primitiveType("int")),
                language,
                IncompleteType(ctx, language),
            )
        val twoParamType =
            FunctionPointerType(
                ctx,
                listOf(tu.primitiveType("int"), tu.primitiveType("unsigned long int")),
                language,
                IntegerType(ctx, "int", 32, language, NumericType.Modifier.SIGNED),
            )
        val variables = tu.variables
        val localTwoParam = findByUniqueName(variables, "local_two_param")
        assertNotNull(localTwoParam)
        assertEquals(twoParamType, localTwoParam.type)

        val localOneParam = findByUniqueName(variables, "local_one_param")
        assertNotNull(localOneParam)
        assertEquals(oneParamType, localOneParam.type)

        val globalNoParam = findByUniqueName(variables, "global_no_param")
        assertNotNull(globalNoParam)
        assertEquals(noParamType, globalNoParam.type)

        val globalNoParamVoid = findByUniqueName(variables, "global_no_param_void")
        assertNotNull(globalNoParamVoid)
        assertEquals(noParamType, globalNoParamVoid.type)

        val globalTwoParam = findByUniqueName(variables, "global_two_param")
        assertNotNull(globalTwoParam)
        assertEquals(twoParamType, globalTwoParam.type)

        val globalOneParam = findByUniqueName(variables, "global_one_param")
        assertNotNull(globalOneParam)
        assertEquals(oneParamType, globalOneParam.type)
    }

    @Throws(Exception::class)
    @Test
    fun testCommonTypeTestCpp() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())

        with(CXXLanguageFrontend(ctx, CPPLanguage(ctx))) {
            val topLevel =
                Path.of("src", "test", "resources", "compiling", "hierarchy", "multistep")
            val result =
                analyze("simple_inheritance.cpp", topLevel, true) {
                    it.registerLanguage<CPPLanguage>()
                }
            val root = assertNotNull(result.records["Root"]).toType()
            val level0 = assertNotNull(result.records["Level0"]).toType()
            val level1 = assertNotNull(result.records["Level1"]).toType()
            val level1b = assertNotNull(result.records["Level1B"]).toType()
            val level2 = assertNotNull(result.records["Level2"]).toType()
            val unrelated = assertNotNull(result.records["Unrelated"]).toType()
            getCommonTypeTestGeneral(root, level0, level1, level1b, level2, unrelated)
        }
    }

    // level2 and level2b have two intersections, both root and level0 -> level0 is lower
    @Throws(Exception::class)
    @Test
    fun testCommonTypeTestCppMultiInheritance() {
        val topLevel = Path.of("src", "test", "resources", "compiling", "hierarchy", "multistep")
        val result =
            analyze("multi_inheritance.cpp", topLevel, true) { it.registerLanguage<CPPLanguage>() }

        val root = assertNotNull(result.records["Root"]).toType()
        val level0 = assertNotNull(result.records["Level0"]).toType()
        val level0b = assertNotNull(result.records["Level0B"]).toType()
        val level1 = assertNotNull(result.records["Level1"]).toType()
        val level1b = assertNotNull(result.records["Level1B"]).toType()
        val level1c = assertNotNull(result.records["Level1C"]).toType()
        val level2 = assertNotNull(result.records["Level2"]).toType()
        val level2b = assertNotNull(result.records["Level2B"]).toType()

        /*
        Type hierarchy:
                  Root------------
                   |             |
                 Level0  Level0B |
                  / \     /  \   |
             Level1 Level1B  Level1C
               |       \       /
             Level2     Level2B
         */
        // Root is the top, but unrelated to Level0B
        for (t in listOf(root, level0, level1, level1b, level1c, level2, level2b)) {
            assertEquals(t, setOf(t).commonType)
        }
        /*assertEquals(null, setOf(root, level0b).commonType)
        for (t in listOf(level0, level1, level2)) {
            assertEquals(null, setOf(t, level0b).commonType)
        }*/
        assertEquals(level0b, setOf(level1b, level1c).commonType)
        assertEquals(level0, setOf(level1, level1b, level2, level2b).commonType)
        assertEquals(root, setOf(level1, level1c).commonType)

        // level2 and level2b have two intersections, both root and level0 -> level0 is lower
        assertEquals(level0, setOf(level2, level2b).commonType)
    }

    @Test
    @Throws(Exception::class)
    fun graphTest() {
        val topLevel = Path.of("src", "test", "resources", "types")
        val result = analyze("cpp", topLevel, true) { it.registerLanguage<CPPLanguage>() }
        val variableDeclarations = result.variables

        // Test PointerType chain with pointer
        val regularInt = findByUniqueName(variableDeclarations, "regularInt")
        val ptr = findByUniqueName(variableDeclarations, "ptr")
        assertTrue(ptr.type is PointerType)
        assertEquals((ptr.type as PointerType).elementType, regularInt.type)

        // Unresolved auto type propagation
        val unknown = findByUniqueName(variableDeclarations, "unknown")
        assertIs<AutoType>(unknown.type)

        // Resolved auto type propagation
        val propagated = findByUniqueName(variableDeclarations, "propagated")
        assertEquals(regularInt.type, propagated.type)
    }

    private fun getCommonTypeTestGeneral(
        root: Type,
        level0: Type,
        level1: Type,
        level1b: Type,
        level2: Type,
        unrelated: Type,
    ) {
        /*
        Type hierarchy:
                  Root
                   |
                 Level0
                  / \
             Level1 Level1B
               |
             Level2
         */
        // A single type is its own least common ancestor
        for (t in listOf(root, level0, level1, level1b, level2)) {
            assertEquals(t, setOf(t).commonType)
        }

        // Root is the root of all types
        for (t in listOf(level0, level1, level1b, level2)) {
            assertEquals(root, setOf(t, root).commonType)
        }

        // Level0 is above all types but Root
        for (t in listOf(level1, level1b, level2)) {
            assertEquals(level0, setOf(t, level0).commonType)
        }

        // Level1 and Level1B have Level0 as common ancestor
        assertEquals(level0, setOf(level1, level1b).commonType)

        // Level2 and Level1B have Level0 as common ancestor
        assertEquals(level0, setOf(level2, level1b).commonType)

        // Level1 and Level2 have Level1 as common ancestor
        assertEquals(level1, setOf(level1, level2).commonType)

        // Check unrelated type behavior: No common root class
        for (t in listOf(root, level0, level1, level1b, level2)) {
            assertEquals(null, setOf(unrelated, t).commonType)
        }
    }
}
