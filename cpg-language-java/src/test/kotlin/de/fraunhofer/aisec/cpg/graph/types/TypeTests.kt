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

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.disableTypeManagerCleanup
import de.fraunhofer.aisec.cpg.TestUtils.findByName
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.passes.JavaExternalTypeHierarchyResolver
import java.nio.file.Path
import java.util.*
import kotlin.test.*

internal class TypeTests : BaseTest() {

    @Test
    fun createFromJava() {
        var result: Type
        var expected: Type

        // Test 1: Ignore Access Modifier Keyword (public, private, protected)
        var typeString = "private int a"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        expected = IntegerType("int", 32, JavaLanguage(), NumericType.Modifier.SIGNED)
        assertEquals(expected, result)

        // Test 2: constant type using final
        typeString = "final int a"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        expected = IntegerType("int", 32, JavaLanguage(), NumericType.Modifier.SIGNED)
        assertEquals(expected, result)

        // Test 3: static type
        typeString = "static int a"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        expected = IntegerType("int", 32, JavaLanguage(), NumericType.Modifier.SIGNED)
        assertEquals(expected, result)

        // Test 4: volatile type
        typeString = "public volatile int a"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        expected = IntegerType("int", 32, JavaLanguage(), NumericType.Modifier.SIGNED)
        assertEquals(expected, result)

        // Test 5: combining a storage type and a qualifier
        typeString = "private static final String a"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        expected = StringType("java.lang.String", JavaLanguage())
        assertEquals(expected, result)

        // Test 6: using two different qualifiers
        typeString = "public final volatile int a"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        expected = IntegerType("int", 32, JavaLanguage(), NumericType.Modifier.SIGNED)
        assertEquals(expected, result)

        // Test 7: Reference level using arrays
        typeString = "int[] a"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        expected =
            PointerType(
                IntegerType("int", 32, JavaLanguage(), NumericType.Modifier.SIGNED),
                PointerType.PointerOrigin.ARRAY
            )
        assertEquals(expected, result)

        // Test 8: generics
        typeString = "List<String> list"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        var generics: MutableList<Type?> = ArrayList()
        generics.add(StringType("java.lang.String", JavaLanguage()))
        expected = ObjectType("List", generics, false, JavaLanguage())
        assertEquals(expected, result)

        // Test 9: more generics
        typeString = "List<List<List<String>>, List<String>> data"
        result = TypeParser.createFrom(typeString, JavaLanguage())
        val genericStringType = StringType("java.lang.String", JavaLanguage())
        val generics3: MutableList<Type> = ArrayList()
        generics3.add(genericStringType)
        val genericElement3 = ObjectType("List", generics3, false, JavaLanguage())
        val generics2a: MutableList<Type> = ArrayList()
        generics2a.add(genericElement3)
        val generics2b: MutableList<Type> = ArrayList()
        generics2b.add(genericStringType)
        val genericElement1 = ObjectType("List", generics2a, false, JavaLanguage())
        val genericElement2 = ObjectType("List", generics2b, false, JavaLanguage())
        generics = ArrayList()
        generics.add(genericElement1)
        generics.add(genericElement2)
        expected = ObjectType("List", generics, false, JavaLanguage())
        assertEquals(expected, result)
    }

    // Tests on the resulting graph
    @Test
    @Throws(Exception::class)
    fun testParameterizedTypes() {
        val topLevel = Path.of("src", "test", "resources", "types")
        val result =
            analyze("java", topLevel, true) {
                it.registerLanguage(JavaLanguage())
                    .registerPass(JavaExternalTypeHierarchyResolver())
            }

        // Check Parameterized
        val recordDeclarations = result.records
        val recordDeclarationBox = findByUniqueName(recordDeclarations, "Box")
        val typeT = TypeManager.getInstance().getTypeParameter(recordDeclarationBox, "T")
        assertNotNull(typeT)
        assertEquals(typeT, TypeManager.getInstance().getTypeParameter(recordDeclarationBox, "T"))

        // Type of field t
        val fieldDeclarations = result.fields
        val fieldDeclarationT = findByUniqueName(fieldDeclarations, "t")
        assertEquals(typeT, fieldDeclarationT.type)
        assertTrue(fieldDeclarationT.possibleSubTypes.contains(typeT))

        // Parameter of set Method
        val methodDeclarations = result.methods
        val methodDeclarationSet = findByUniqueName(methodDeclarations, "set")
        val t = methodDeclarationSet.parameters[0]
        assertEquals(typeT, t.type)
        assertTrue(t.possibleSubTypes.contains(typeT))

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
        val result =
            analyze("java", topLevel, true) {
                it.registerLanguage(JavaLanguage())
                    .registerPass(JavaExternalTypeHierarchyResolver())
            }
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
        disableTypeManagerCleanup()
        val topLevel = Path.of("src", "test", "resources", "compiling", "hierarchy")
        val result =
            analyze("java", topLevel, true) {
                it.registerLanguage(JavaLanguage())
                    .registerPass(JavaExternalTypeHierarchyResolver())
            }
        val root = TypeParser.createFrom("multistep.Root", JavaLanguage())
        val level0 = TypeParser.createFrom("multistep.Level0", JavaLanguage())
        val level1 = TypeParser.createFrom("multistep.Level1", JavaLanguage())
        val level1b = TypeParser.createFrom("multistep.Level1B", JavaLanguage())
        val level2 = TypeParser.createFrom("multistep.Level2", JavaLanguage())
        val unrelated = TypeParser.createFrom("multistep.Unrelated", JavaLanguage())
        getCommonTypeTestGeneral(root, level0, level1, level1b, level2, unrelated, result)
    }

    private fun getCommonTypeTestGeneral(
        root: Type,
        level0: Type,
        level1: Type,
        level1b: Type,
        level2: Type,
        unrelated: Type,
        result: TranslationResult
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
        val provider = result.scopeManager

        // A single type is its own least common ancestor
        for (t in listOf(root, level0, level1, level1b, level2)) {
            assertEquals(
                Optional.of(t),
                TypeManager.getInstance().getCommonType(listOf(t), provider)
            )
        }

        // Root is the root of all types
        for (t in listOf(level0, level1, level1b, level2)) {
            assertEquals(
                Optional.of(root),
                TypeManager.getInstance().getCommonType(listOf(t, root), provider)
            )
        }

        // Level0 is above all types but Root
        for (t in listOf(level1, level1b, level2)) {
            assertEquals(
                Optional.of(level0),
                TypeManager.getInstance().getCommonType(listOf(t, level0), provider)
            )
        }

        // Level1 and Level1B have Level0 as common ancestor
        assertEquals(
            Optional.of(level0),
            TypeManager.getInstance().getCommonType(listOf(level1, level1b), provider)
        )

        // Level2 and Level1B have Level0 as common ancestor
        assertEquals(
            Optional.of(level0),
            TypeManager.getInstance().getCommonType(listOf(level2, level1b), provider)
        )

        // Level1 and Level2 have Level1 as common ancestor
        assertEquals(
            Optional.of(level1),
            TypeManager.getInstance().getCommonType(listOf(level1, level2), provider)
        )

        // Check unrelated type behavior: No common root class
        for (t in listOf(root, level0, level1, level1b, level2)) {
            assertEquals(
                Optional.empty(),
                TypeManager.getInstance().getCommonType(listOf(unrelated, t), provider)
            )
        }
    }
}
