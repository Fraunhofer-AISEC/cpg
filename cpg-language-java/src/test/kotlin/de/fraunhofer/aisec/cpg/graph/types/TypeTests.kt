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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.findByName
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import java.nio.file.Path
import java.util.*
import kotlin.test.*

internal class TypeTests : BaseTest() {

    // Tests on the resulting graph
    @Test
    @Throws(Exception::class)
    fun testParameterizedTypes() {
        val topLevel = Path.of("src", "test", "resources", "types")
        val result = analyze("java", topLevel, true) { it.registerLanguage(JavaLanguage()) }

        // Check Parameterized
        val recordDeclarations = result.records
        val recordDeclarationBox = findByUniqueName(recordDeclarations, "Box")
        val typeT = result.finalCtx.typeManager.getTypeParameter(recordDeclarationBox, "T")
        assertNotNull(typeT)
        assertIs<ParameterizedType>(typeT)
        assertLocalName("T", typeT)
        assertEquals(typeT, result.finalCtx.typeManager.getTypeParameter(recordDeclarationBox, "T"))

        // Type of field t
        val fieldDeclarations = result.fields
        val fieldDeclarationT = findByUniqueName(fieldDeclarations, "t")
        // assertTrue(fieldDeclarationT.possibleSubTypes.contains(typeT))

        // Parameter of set Method
        val methodDeclarations = result.methods
        val methodDeclarationSet = findByUniqueName(methodDeclarations, "set")
        val t = methodDeclarationSet.parameters[0]
        assertEquals(typeT, t.type)
        // assertTrue(t.possibleSubTypes.contains(typeT))

        // Return Type of get Method
        val methodDeclarationGet = findByUniqueName(methodDeclarations, "get")
        assertEquals(
            FunctionType("get()T", listOf(), listOf(typeT), JavaLanguage()),
            methodDeclarationGet.type
        )
    }

    @Test
    @Throws(Exception::class)
    fun graphTest() {
        val topLevel = Path.of("src", "test", "resources", "types")
        val result = analyze("java", topLevel, true) { it.registerLanguage(JavaLanguage()) }
        val variables = result.allChildren<ObjectType>()
        val recordDeclarations = result.records

        // Test RecordDeclaration relationship
        val objectTypes = findByName(variables, "A")
        val recordDeclarationA = findByUniqueName(recordDeclarations, "A")
        for (objectType in objectTypes) {
            assertEquals(recordDeclarationA, objectType.recordDeclaration)
        }

        // Test uniqueness of types x and y have same type
        val fieldDeclarations = result.fields
        val x = findByUniqueName(fieldDeclarations, "x")
        val z = findByUniqueName(fieldDeclarations, "z")
        assertSame(x.type, z.type)

        // Test propagation of specifiers in primitive fields (final int y)
        val y = findByUniqueName(fieldDeclarations, "y")

        // Test propagation of specifiers in non-primitive fields (final A a)
        val variableDeclarations = result.variables
        val aA = findByUniqueName(variableDeclarations, "a")

        // Test propagation of specifiers in variables (final String s)
        val sString = findByUniqueName(variableDeclarations, "s")

        // Test PointerType chain with array
        val array = findByUniqueName(variableDeclarations, "array")
        assertTrue(array.type is PointerType)
        assertEquals((array.type as PointerType).elementType, x.type)

        // Test java generics
        val map = findByUniqueName(variableDeclarations, "map")
        assertTrue(map.type is ObjectType)
        assertLocalName("C", map.type)
        assertEquals(2, (map.type as ObjectType).generics.size)
        assertLocalName("D", (map.type as ObjectType).generics[0])
        assertLocalName("E", (map.type as ObjectType).generics[1])
    }

    @Throws(Exception::class)
    @Test
    fun testCommonTypeTestJava() {
        with(
            JavaLanguageFrontend(
                JavaLanguage(),
                TranslationContext(
                    TranslationConfiguration.builder().build(),
                    ScopeManager(),
                    TypeManager()
                )
            )
        ) {
            val topLevel = Path.of("src", "test", "resources", "compiling", "hierarchy")
            val result = analyze("java", topLevel, true) { it.registerLanguage(JavaLanguage()) }
            val root = objectType("multistep.Root")
            val level0 = objectType("multistep.Level0")
            val level1 = objectType("multistep.Level1")
            val level1b = objectType("multistep.Level1B")
            val level2 = objectType("multistep.Level2")
            val unrelated = objectType("multistep.Unrelated")
            getCommonTypeTestGeneral(
                root,
                level0,
                level1,
                level1b,
                level2,
                unrelated,
                result.finalCtx
            )
        }
    }

    private fun getCommonTypeTestGeneral(
        root: Type,
        level0: Type,
        level1: Type,
        level1b: Type,
        level2: Type,
        unrelated: Type,
        ctx: TranslationContext
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
        val provider = ctx.scopeManager

        // A single type is its own least common ancestor
        for (t in listOf(root, level0, level1, level1b, level2)) {
            assertEquals(Optional.of(t), ctx.typeManager.getCommonType(listOf(t), ctx))
        }

        // Root is the root of all types
        for (t in listOf(level0, level1, level1b, level2)) {
            assertEquals(Optional.of(root), ctx.typeManager.getCommonType(listOf(t, root), ctx))
        }

        // Level0 is above all types but Root
        for (t in listOf(level1, level1b, level2)) {
            assertEquals(Optional.of(level0), ctx.typeManager.getCommonType(listOf(t, level0), ctx))
        }

        // Level1 and Level1B have Level0 as common ancestor
        assertEquals(
            Optional.of(level0),
            ctx.typeManager.getCommonType(listOf(level1, level1b), ctx)
        )

        // Level2 and Level1B have Level0 as common ancestor
        assertEquals(
            Optional.of(level0),
            ctx.typeManager.getCommonType(listOf(level2, level1b), ctx)
        )

        // Level1 and Level2 have Level1 as common ancestor
        assertEquals(
            Optional.of(level1),
            ctx.typeManager.getCommonType(listOf(level1, level2), ctx)
        )

        // Check unrelated type behavior: No common root class
        for (t in listOf(root, level0, level1, level1b, level2)) {
            assertEquals(Optional.empty(), ctx.typeManager.getCommonType(listOf(unrelated, t), ctx))
        }
    }
}
