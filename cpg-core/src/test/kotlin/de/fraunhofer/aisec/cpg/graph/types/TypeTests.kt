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
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.disableTypeManagerCleanup
import de.fraunhofer.aisec.cpg.TestUtils.findByName
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.flattenIsInstance
import de.fraunhofer.aisec.cpg.TestUtils.flattenListIsInstance
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import java.nio.file.Path
import java.util.*
import kotlin.test.*

internal class TypeTests : BaseTest() {
    @Test
    fun reference() {
        TypeParser.setLanguageSupplier { TypeManager.Language.CXX }
        val objectType: Type =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        val pointerType: Type = PointerType(objectType, PointerType.PointerOrigin.POINTER)
        val unknownType: Type = UnknownType.getUnknownType()
        val incompleteType: Type = IncompleteType()
        val parameterList =
            listOf<Type>(
                ObjectType(
                    "int",
                    Type.Storage.AUTO,
                    Type.Qualifier(),
                    emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true
                )
            )
        val functionPointerType: Type =
            FunctionPointerType(
                Type.Qualifier(),
                Type.Storage.AUTO,
                parameterList,
                IncompleteType()
            )

        // Test 1: ObjectType becomes PointerType containing the original ObjectType as ElementType
        assertEquals(
            PointerType(objectType, PointerType.PointerOrigin.POINTER),
            objectType.reference(PointerType.PointerOrigin.POINTER)
        )

        // Test 2: Existing PointerType adds one level more of references as ElementType
        assertEquals(
            PointerType(pointerType, PointerType.PointerOrigin.POINTER),
            pointerType.reference(PointerType.PointerOrigin.POINTER)
        )

        // Test 3: UnknownType cannot be referenced
        assertEquals(unknownType, unknownType.reference(null))

        // Test 4: IncompleteType can be refereced e.g. void*
        assertEquals(
            PointerType(incompleteType, PointerType.PointerOrigin.POINTER),
            incompleteType.reference(PointerType.PointerOrigin.POINTER)
        )

        // Test 5: Create reference to function pointer = pointer to function pointer
        assertEquals(
            PointerType(functionPointerType, PointerType.PointerOrigin.POINTER),
            functionPointerType.reference(PointerType.PointerOrigin.POINTER)
        )
    }

    @Test
    fun dereference() {
        TypeParser.setLanguageSupplier { TypeManager.Language.CXX }
        val objectType: Type =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        val pointerType: Type = PointerType(objectType, PointerType.PointerOrigin.POINTER)
        val unknownType: Type = UnknownType.getUnknownType()
        val incompleteType: Type = IncompleteType()
        val parameterList =
            listOf<Type>(
                ObjectType(
                    "int",
                    Type.Storage.AUTO,
                    Type.Qualifier(),
                    emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true
                )
            )
        val functionPointerType: Type =
            FunctionPointerType(
                Type.Qualifier(),
                Type.Storage.AUTO,
                parameterList,
                IncompleteType()
            )

        // Test 1: Dereferencing an ObjectType results in an UnknownType, since we cannot track the
        // type
        // of the corresponding memory
        assertEquals(UnknownType.getUnknownType(), objectType.dereference())

        // Test 2: Dereferencing a PointerType results in the corresponding elementType of the
        // PointerType (can also be another PointerType)
        assertEquals(objectType, pointerType.dereference())

        // Test 3: Dereferecing unknown or incomplete type results in the same type
        assertEquals(unknownType, unknownType.dereference())
        assertEquals(incompleteType, incompleteType.dereference())

        // Test 5: Due to the definition in the C-Standard dereferencing function pointer yields the
        // same function pointer
        assertEquals(functionPointerType, functionPointerType.dereference())
    }

    @Test
    fun createFromJava() {
        var result: Type
        var expected: Type
        TypeParser.setLanguageSupplier { TypeManager.Language.JAVA }

        // Test 1: Ignore Access Modifier Keyword (public, private, protected)
        var typeString = "private int a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        assertEquals(expected, result)

        // Test 2: constant type using final
        typeString = "final int a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(true, false, false, false),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        assertEquals(expected, result)

        // Test 3: static type
        typeString = "static int a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "int",
                Type.Storage.STATIC,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        assertEquals(expected, result)

        // Test 4: volatile type
        typeString = "public volatile int a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(false, true, false, false),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        assertEquals(expected, result)

        // Test 5: combining a storage type and a qualifier
        typeString = "private static final String a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "String",
                Type.Storage.STATIC,
                Type.Qualifier(true, false, false, false),
                ArrayList(),
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        assertEquals(expected, result)

        // Test 6: using two different qualifiers
        typeString = "public final volatile int a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(true, true, false, false),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        assertEquals(expected, result)

        // Test 7: Reference level using arrays
        typeString = "int[] a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            PointerType(
                ObjectType(
                    "int",
                    Type.Storage.AUTO,
                    Type.Qualifier(),
                    ArrayList(),
                    ObjectType.Modifier.SIGNED,
                    true
                ),
                PointerType.PointerOrigin.ARRAY
            )
        assertEquals(expected, result)

        // Test 8: generics
        typeString = "List<String> list"
        result = TypeParser.createFrom(typeString, true)
        var generics: MutableList<Type?> = ArrayList()
        generics.add(
            ObjectType(
                "String",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        )
        expected =
            ObjectType(
                "List",
                Type.Storage.AUTO,
                Type.Qualifier(),
                generics,
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        assertEquals(expected, result)

        // Test 9: more generics
        typeString = "List<List<List<String>>, List<String>> data"
        result = TypeParser.createFrom(typeString, true)
        val genericStringType =
            ObjectType(
                "String",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        val generics3: MutableList<Type> = ArrayList()
        generics3.add(genericStringType)
        val genericElement3 =
            ObjectType(
                "List",
                Type.Storage.AUTO,
                Type.Qualifier(),
                generics3,
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        val generics2a: MutableList<Type> = ArrayList()
        generics2a.add(genericElement3)
        val generics2b: MutableList<Type> = ArrayList()
        generics2b.add(genericStringType)
        val genericElement1 =
            ObjectType(
                "List",
                Type.Storage.AUTO,
                Type.Qualifier(),
                generics2a,
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        val genericElement2 =
            ObjectType(
                "List",
                Type.Storage.AUTO,
                Type.Qualifier(),
                generics2b,
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        generics = ArrayList()
        generics.add(genericElement1)
        generics.add(genericElement2)
        expected =
            ObjectType(
                "List",
                Type.Storage.AUTO,
                Type.Qualifier(),
                generics,
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        assertEquals(expected, result)
    }

    @Test
    fun createFromC() {
        var result: Type
        TypeParser.setLanguageSupplier { TypeManager.Language.CXX }

        // Test 1: Function pointer
        var typeString = "void (*single_param)(int)"
        result = TypeParser.createFrom(typeString, true)
        val parameterList =
            listOf<Type>(
                ObjectType(
                    "int",
                    Type.Storage.AUTO,
                    Type.Qualifier(),
                    emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true
                )
            )
        var expected: Type =
            FunctionPointerType(
                Type.Qualifier(),
                Type.Storage.AUTO,
                parameterList,
                IncompleteType()
            )
        assertEquals(expected, result)

        // Test 1.1: interleaved brackets in function pointer
        typeString = "void ((*single_param)(int))"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(result, expected)

        // Test 2: Stronger binding of brackets and pointer
        typeString = "char (* const a)[]"
        result = TypeParser.createFrom(typeString, true)
        expected =
            PointerType(
                PointerType(
                    ObjectType(
                        "char",
                        Type.Storage.AUTO,
                        Type.Qualifier(),
                        emptyList(),
                        ObjectType.Modifier.SIGNED,
                        true
                    ),
                    PointerType.PointerOrigin.ARRAY
                ),
                PointerType.PointerOrigin.POINTER
            )
        expected.setQualifier(Type.Qualifier(true, false, false, false))
        assertEquals(expected, result)

        // Test 3: Mutable pointer to a mutable char
        typeString = "char *p"
        result = TypeParser.createFrom(typeString, true)
        expected =
            PointerType(
                ObjectType(
                    "char",
                    Type.Storage.AUTO,
                    Type.Qualifier(),
                    emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true
                ),
                PointerType.PointerOrigin.POINTER
            )
        assertEquals(expected, result)

        // Test 3.1: Different Whitespaces
        typeString = "char* p"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(expected, result)

        // Test 3.2: Different Whitespaces
        typeString = "char * p"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(expected, result)

        // Test 4: Mutable pointer to a constant char
        typeString = "const char *p;"
        result = TypeParser.createFrom(typeString, true)
        expected =
            PointerType(
                ObjectType(
                    "char",
                    Type.Storage.AUTO,
                    Type.Qualifier(true, false, false, false),
                    emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true
                ),
                PointerType.PointerOrigin.POINTER
            )
        assertEquals(expected, result)

        // Test 5: Constant pointer to a mutable char
        typeString = "char * const p;"
        result = TypeParser.createFrom(typeString, true)
        expected =
            PointerType(
                ObjectType(
                    "char",
                    Type.Storage.AUTO,
                    Type.Qualifier(false, false, false, false),
                    emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true
                ),
                PointerType.PointerOrigin.POINTER
            )
        expected.setQualifier(Type.Qualifier(true, false, false, false))
        assertEquals(expected, result)

        // Test 6: Constant pointer to a constant char
        typeString = "const char * const p;"
        result = TypeParser.createFrom(typeString, true)
        expected =
            PointerType(
                ObjectType(
                    "char",
                    Type.Storage.AUTO,
                    Type.Qualifier(true, false, false, false),
                    emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true
                ),
                PointerType.PointerOrigin.POINTER
            )
        expected.setQualifier(Type.Qualifier(true, false, false, false))
        assertEquals(expected, result)

        // Test 7: Array of const pointer to static const char
        typeString = "static const char * const somearray []"
        result = TypeParser.createFrom(typeString, true)
        expected =
            PointerType(
                PointerType(
                    ObjectType(
                        "char",
                        Type.Storage.STATIC,
                        Type.Qualifier(true, false, false, false),
                        emptyList(),
                        ObjectType.Modifier.SIGNED,
                        true
                    ),
                    PointerType.PointerOrigin.POINTER
                ),
                PointerType.PointerOrigin.ARRAY
            )
        expected.elementType.qualifier = Type.Qualifier(true, false, false, false)
        assertEquals(expected, result)

        // Test 7.1: Array of array of pointer to static const char
        typeString = "static const char * somearray[][]"
        result = TypeParser.createFrom(typeString, true)
        expected =
            PointerType(
                PointerType(
                    PointerType(
                        ObjectType(
                            "char",
                            Type.Storage.STATIC,
                            Type.Qualifier(true, false, false, false),
                            emptyList(),
                            ObjectType.Modifier.SIGNED,
                            true
                        ),
                        PointerType.PointerOrigin.POINTER
                    ),
                    PointerType.PointerOrigin.ARRAY
                ),
                PointerType.PointerOrigin.ARRAY
            )
        assertEquals(expected, result)

        // Test 8: Generics
        typeString = "Array<int> array"
        result = TypeParser.createFrom(typeString, true)
        var generics: MutableList<Type?> = ArrayList()
        generics.add(
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(),
                emptyList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        )
        expected =
            ObjectType(
                "Array",
                Type.Storage.AUTO,
                Type.Qualifier(),
                generics,
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        assertEquals(expected, result)

        // Test 9: Compound Primitive Types
        typeString = "long long int"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "long long int",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        assertEquals(expected, result)

        // Test 10: Unsigned/Signed Types
        typeString = "unsigned int"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.UNSIGNED,
                true
            )
        assertEquals(expected, result)
        typeString = "signed int"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true
            )
        assertEquals(expected, result)
        typeString = "A a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ObjectType(
                "A",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        assertEquals(expected, result)

        // Test 11: Unsigned + const + compound primitive Types
        expected =
            ObjectType(
                "long long int",
                Type.Storage.AUTO,
                Type.Qualifier(true, false, false, false),
                ArrayList(),
                ObjectType.Modifier.UNSIGNED,
                true
            )
        typeString = "const unsigned long long int a = 1"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(expected, result)

        typeString = "unsigned const long long int b = 1"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(expected, result)

        typeString = "unsigned long const long int c = 1"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(expected, result)

        typeString = "unsigned long long const int d = 1"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(expected, result)

        typeString = "unsigned long long int const e = 1"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(expected, result)

        // Test 12: C++ Reference Types
        typeString = "const int& ref = a"
        result = TypeParser.createFrom(typeString, true)
        expected =
            ReferenceType(
                Type.Storage.AUTO,
                Type.Qualifier(true, false, false, false),
                ObjectType(
                    "int",
                    Type.Storage.AUTO,
                    Type.Qualifier(),
                    ArrayList(),
                    ObjectType.Modifier.SIGNED,
                    true
                )
            )
        assertEquals(expected, result)

        typeString = "int const &ref2 = a"
        result = TypeParser.createFrom(typeString, true)
        assertEquals(expected, result)

        // Test 13: Elaborated Type in Generics
        result = TypeParser.createFrom("Array<struct Node>", true)
        generics = ArrayList()
        var generic =
            ObjectType(
                "Node",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        generics.add(generic)
        expected =
            ObjectType(
                "Array",
                Type.Storage.AUTO,
                Type.Qualifier(),
                generics,
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        assertEquals(expected, result)

        result = TypeParser.createFrom("Array<myclass >", true)
        generics = ArrayList()
        generic =
            ObjectType(
                "myclass",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        generics.add(generic)
        expected =
            ObjectType(
                "Array",
                Type.Storage.AUTO,
                Type.Qualifier(),
                generics,
                ObjectType.Modifier.NOT_APPLICABLE,
                false
            )
        assertEquals(expected, result)
    }

    // Tests on the resulting graph
    @Test
    @Throws(Exception::class)
    fun testParameterizedTypes() {
        val topLevel = Path.of("src", "test", "resources", "types")
        val result = analyze("java", topLevel, true)

        // Check Parameterized
        val recordDeclarations = flattenListIsInstance<RecordDeclaration>(result)
        val recordDeclarationBox = findByUniqueName(recordDeclarations, "Box")
        val typeT = TypeManager.getInstance().getTypeParameter(recordDeclarationBox, "T")
        assertEquals(typeT, TypeManager.getInstance().getTypeParameter(recordDeclarationBox, "T"))

        // Type of field t
        val fieldDeclarations = flattenListIsInstance<FieldDeclaration>(result)
        val fieldDeclarationT = findByUniqueName(fieldDeclarations, "t")
        assertEquals(typeT, fieldDeclarationT.type)
        assertTrue(fieldDeclarationT.possibleSubTypes.contains(typeT))

        // Parameter of set Method
        val methodDeclarations = flattenListIsInstance<MethodDeclaration>(result)
        val methodDeclarationSet = findByUniqueName(methodDeclarations, "set")
        val t = methodDeclarationSet.parameters[0]
        assertEquals(typeT, t.type)
        assertTrue(t.possibleSubTypes.contains(typeT))

        // Return Type of get Method
        val methodDeclarationGet = findByUniqueName(methodDeclarations, "get")
        assertEquals(typeT, methodDeclarationGet.type)
    }

    @Test
    @Throws(Exception::class)
    fun graphTest() {
        var topLevel = Path.of("src", "test", "resources", "types")
        var result = analyze("java", topLevel, true)
        val variables = flattenListIsInstance<ObjectType>(result)
        val recordDeclarations = flattenListIsInstance<RecordDeclaration>(result)

        // Test RecordDeclaration relationship
        val objectTypes = findByName(variables, "A")
        val recordDeclarationA = findByUniqueName(recordDeclarations, "A")
        for (objectType in objectTypes) {
            assertEquals(recordDeclarationA, objectType.recordDeclaration)
        }

        // Test uniqueness of types x and y have same type
        val fieldDeclarations = flattenListIsInstance<FieldDeclaration>(result)
        val x = findByUniqueName(fieldDeclarations, "x")
        val z = findByUniqueName(fieldDeclarations, "z")
        assertSame(x.type, z.type)

        // Test propagation of specifiers in primitive fields (final int y)
        val y = findByUniqueName(fieldDeclarations, "y")
        assertTrue(y.type.qualifier.isConst)

        // Test propagation of specifiers in non-primitive fields (final A a)
        var variableDeclarations = flattenListIsInstance<VariableDeclaration>(result)
        val aA = findByUniqueName(variableDeclarations, "a")
        assertTrue(aA.type.qualifier.isConst)

        // Test propagation of specifiers in variables (final String s)
        val sString = findByUniqueName(variableDeclarations, "s")
        assertTrue(sString.type.qualifier.isConst)

        // Test PointerType chain with array
        val array = findByUniqueName(variableDeclarations, "array")
        assertTrue(array.type is PointerType)
        assertEquals((array.type as PointerType).elementType, x.type)

        // Test java generics
        val map = findByUniqueName(variableDeclarations, "map")
        assertTrue(map.type is ObjectType)
        assertEquals("C", map.type.name)
        assertEquals(2, (map.type as ObjectType).generics.size)
        assertEquals("D", (map.type as ObjectType).generics[0].name)
        assertEquals("E", (map.type as ObjectType).generics[1].name)

        topLevel = Path.of("src", "test", "resources", "types")
        result = analyze("cpp", topLevel, true)
        variableDeclarations = flattenListIsInstance(result)

        // Test PointerType chain with pointer
        val regularInt = findByUniqueName(variableDeclarations, "regularInt")
        val ptr = findByUniqueName(variableDeclarations, "ptr")
        assertTrue(ptr.type is PointerType)
        assertEquals((ptr.type as PointerType).elementType, regularInt.type)

        // Test type Propagation (auto) UnknownType
        val unknown = findByUniqueName(variableDeclarations, "unknown")
        assertEquals(UnknownType.getUnknownType(), unknown.type)

        // Test type Propagation auto
        val propagated = findByUniqueName(variableDeclarations, "propagated")
        assertEquals(regularInt.type, propagated.type)
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
            analyzeAndGetFirstTU(listOf(topLevel.resolve("fptr_type.cpp").toFile()), topLevel, true)
        val noParamType =
            FunctionPointerType(Type.Qualifier(), Type.Storage.AUTO, emptyList(), IncompleteType())
        val oneParamType =
            FunctionPointerType(
                Type.Qualifier(),
                Type.Storage.AUTO,
                listOf<Type>(
                    ObjectType(
                        "int",
                        Type.Storage.AUTO,
                        Type.Qualifier(),
                        ArrayList(),
                        ObjectType.Modifier.SIGNED,
                        true
                    )
                ),
                IncompleteType()
            )
        val twoParamType =
            FunctionPointerType(
                Type.Qualifier(),
                Type.Storage.AUTO,
                listOf<Type>(
                    ObjectType(
                        "int",
                        Type.Storage.AUTO,
                        Type.Qualifier(),
                        ArrayList(),
                        ObjectType.Modifier.SIGNED,
                        true
                    ),
                    ObjectType(
                        "long",
                        Type.Storage.AUTO,
                        Type.Qualifier(),
                        ArrayList(),
                        ObjectType.Modifier.UNSIGNED,
                        true
                    )
                ),
                ObjectType(
                    "int",
                    Type.Storage.AUTO,
                    Type.Qualifier(),
                    ArrayList(),
                    ObjectType.Modifier.SIGNED,
                    true
                )
            )
        val variables = flattenIsInstance<VariableDeclaration>(tu)
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
    fun testCommonTypeTestJava() {
        disableTypeManagerCleanup()
        val topLevel = Path.of("src", "test", "resources", "compiling", "hierarchy")
        analyze("java", topLevel, true)
        val root = TypeParser.createFrom("multistep.Root", true)
        val level0 = TypeParser.createFrom("multistep.Level0", true)
        val level1 = TypeParser.createFrom("multistep.Level1", true)
        val level1b = TypeParser.createFrom("multistep.Level1B", true)
        val level2 = TypeParser.createFrom("multistep.Level2", true)
        val unrelated = TypeParser.createFrom("multistep.Unrelated", true)
        getCommonTypeTestGeneral(root, level0, level1, level1b, level2, unrelated)
    }

    @Throws(Exception::class)
    @Test
    fun testCommonTypeTestCpp() {
        disableTypeManagerCleanup()
        val topLevel = Path.of("src", "test", "resources", "compiling", "hierarchy", "multistep")
        analyze("simple_inheritance.cpp", topLevel, true)
        val root = TypeParser.createFrom("Root", true)
        val level0 = TypeParser.createFrom("Level0", true)
        val level1 = TypeParser.createFrom("Level1", true)
        val level1b = TypeParser.createFrom("Level1B", true)
        val level2 = TypeParser.createFrom("Level2", true)
        val unrelated = TypeParser.createFrom("Unrelated", true)
        getCommonTypeTestGeneral(root, level0, level1, level1b, level2, unrelated)
    }

    // level2 and level2b have two intersections, both root and level0 -> level0 is lower
    @Throws(Exception::class)
    @Test
    fun testCommonTypeTestCppMultiInheritance() {
        disableTypeManagerCleanup()
        val topLevel = Path.of("src", "test", "resources", "compiling", "hierarchy", "multistep")
        analyze("multi_inheritance.cpp", topLevel, true)
        val root = TypeParser.createFrom("Root", true)
        val level0 = TypeParser.createFrom("Level0", true)
        val level0b = TypeParser.createFrom("Level0B", true)
        val level1 = TypeParser.createFrom("Level1", true)
        val level1b = TypeParser.createFrom("Level1B", true)
        val level1c = TypeParser.createFrom("Level1C", true)
        val level2 = TypeParser.createFrom("Level2", true)
        val level2b = TypeParser.createFrom("Level2B", true)

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
            assertEquals(Optional.of(t), TypeManager.getInstance().getCommonType(listOf(t)))
        }
        assertEquals(
            Optional.empty(),
            TypeManager.getInstance().getCommonType(listOf(root, level0b))
        )
        for (t in listOf(level0, level1, level2)) {
            assertEquals(
                Optional.empty(),
                TypeManager.getInstance().getCommonType(listOf(t, level0b))
            )
        }
        assertEquals(
            Optional.of(level0b),
            TypeManager.getInstance().getCommonType(listOf(level1b, level1c))
        )
        assertEquals(
            Optional.of(level0),
            TypeManager.getInstance().getCommonType(listOf(level1, level1b, level2, level2b))
        )
        assertEquals(
            Optional.of(root),
            TypeManager.getInstance().getCommonType(listOf(level1, level1c))
        )

        // level2 and level2b have two intersections, both root and level0 -> level0 is lower
        assertEquals(
            Optional.of(level0),
            TypeManager.getInstance().getCommonType(listOf(level2, level2b))
        )
    }

    private fun getCommonTypeTestGeneral(
        root: Type,
        level0: Type,
        level1: Type,
        level1b: Type,
        level2: Type,
        unrelated: Type
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
            assertEquals(Optional.of(t), TypeManager.getInstance().getCommonType(listOf(t)))
        }

        // Root is the root of all types
        for (t in listOf(level0, level1, level1b, level2)) {
            assertEquals(
                Optional.of(root),
                TypeManager.getInstance().getCommonType(listOf(t, root))
            )
        }

        // Level0 is above all types but Root
        for (t in listOf(level1, level1b, level2)) {
            assertEquals(
                Optional.of(level0),
                TypeManager.getInstance().getCommonType(listOf(t, level0))
            )
        }

        // Level1 and Level1B have Level0 as common ancestor
        assertEquals(
            Optional.of(level0),
            TypeManager.getInstance().getCommonType(listOf(level1, level1b))
        )

        // Level2 and Level1B have Level0 as common ancestor
        assertEquals(
            Optional.of(level0),
            TypeManager.getInstance().getCommonType(listOf(level2, level1b))
        )

        // Level1 and Level2 have Level1 as common ancestor
        assertEquals(
            Optional.of(level1),
            TypeManager.getInstance().getCommonType(listOf(level1, level2))
        )

        // Check unrelated type behavior: No common root class
        for (t in listOf(root, level0, level1, level1b, level2)) {
            assertEquals(
                Optional.empty(),
                TypeManager.getInstance().getCommonType(listOf(unrelated, t))
            )
        }
    }
}
