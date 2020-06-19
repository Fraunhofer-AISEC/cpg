/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

package de.fraunhofer.aisec.cpg;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.*;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TypeTests {

  /**
   * {@link TypeParser} and {@link TypeManager} hold static state. This needs to be cleared before
   * all tests in order to avoid strange errors
   */
  @BeforeEach
  void resetPersistentState() {
    TypeParser.reset();
    TypeManager.reset();
  }

  @Test
  void reference() {

    TypeParser.setLanguageSupplier(() -> TypeManager.Language.CXX);

    Type objectType =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    Type pointerType = new PointerType(objectType, PointerType.PointerOrigin.POINTER);
    Type unknownType = UnknownType.getUnknownType();
    Type incompleteType = new IncompleteType();

    List<Type> parameterList =
        List.of(
            new ObjectType(
                "int",
                Type.Storage.AUTO,
                new Type.Qualifier(),
                Collections.emptyList(),
                ObjectType.Modifier.SIGNED,
                true));
    Type functionPointerType =
        new FunctionPointerType(
            new Type.Qualifier(), Type.Storage.AUTO, parameterList, new IncompleteType());

    // Test 1: ObjectType becomes PointerType containing the original ObjectType as ElementType
    assertEquals(
        new PointerType(objectType, PointerType.PointerOrigin.POINTER),
        objectType.reference(PointerType.PointerOrigin.POINTER));

    // Test 2: Existing PointerType adds one level more of references as ElementType
    assertEquals(
        new PointerType(pointerType, PointerType.PointerOrigin.POINTER),
        pointerType.reference(PointerType.PointerOrigin.POINTER));

    // Test 3: UnknownType cannot be referenced
    assertEquals(unknownType, unknownType.reference(null));

    // Test 4: IncompleteType can be refereced e.g. void*
    assertEquals(
        new PointerType(incompleteType, PointerType.PointerOrigin.POINTER),
        incompleteType.reference(PointerType.PointerOrigin.POINTER));

    // Test 5: Create reference to function pointer = pointer to function pointer
    assertEquals(
        new PointerType(functionPointerType, PointerType.PointerOrigin.POINTER),
        functionPointerType.reference(PointerType.PointerOrigin.POINTER));
  }

  @Test
  void dereference() {

    TypeParser.setLanguageSupplier(() -> TypeManager.Language.CXX);

    Type objectType =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    Type pointerType = new PointerType(objectType, PointerType.PointerOrigin.POINTER);
    Type unknownType = UnknownType.getUnknownType();
    Type incompleteType = new IncompleteType();

    List<Type> parameterList =
        List.of(
            new ObjectType(
                "int",
                Type.Storage.AUTO,
                new Type.Qualifier(),
                Collections.emptyList(),
                ObjectType.Modifier.SIGNED,
                true));
    Type functionPointerType =
        new FunctionPointerType(
            new Type.Qualifier(), Type.Storage.AUTO, parameterList, new IncompleteType());

    // Test 1: Dereferencing an ObjectType results in an UnknownType, since we cannot track the type
    // of the corresponding memory
    assertEquals(UnknownType.getUnknownType(), objectType.dereference());

    // Test 2: Dereferencing a PointerType results in the corresponding elementType of the
    // PointerType (can also be another PointerType)
    assertEquals(objectType, pointerType.dereference());

    // Test 3: Dereferecing unknown or incomplete type results in the same type
    assertEquals(unknownType, unknownType.dereference());
    assertEquals(incompleteType, incompleteType.dereference());

    // Test 5: Due to the definition in the C-Standard dereferencing function pointer yields the
    // same function pointer
    assertEquals(functionPointerType, functionPointerType.dereference());
  }

  @Test
  void createFromJava() {
    String typeString;
    Type result;
    Type expected;

    TypeParser.setLanguageSupplier(() -> TypeManager.Language.JAVA);

    // Test 1: Ignore Access Modifier Keyword (public, private, protected)
    typeString = "private int a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    assertEquals(expected, result);

    // Test 2: constant type using final
    typeString = "final int a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(true, false, false, false),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    assertEquals(expected, result);

    // Test 3: static type
    typeString = "static int a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "int",
            Type.Storage.STATIC,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    assertEquals(expected, result);

    // Test 4: volatile type
    typeString = "public volatile int a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(false, true, false, false),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    assertEquals(expected, result);

    // Test 5: combining a storage type and a qualifier
    typeString = "private static final String a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "String",
            Type.Storage.STATIC,
            new Type.Qualifier(true, false, false, false),
            new ArrayList<>(),
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    assertEquals(expected, result);

    // Test 6: using two different qualifiers
    typeString = "public final volatile int a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(true, true, false, false),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    assertEquals(expected, result);

    // Test 7: Reference level using arrays
    typeString = "int[] a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new PointerType(
            new ObjectType(
                "int",
                Type.Storage.AUTO,
                new Type.Qualifier(),
                new ArrayList<>(),
                ObjectType.Modifier.SIGNED,
                true),
            PointerType.PointerOrigin.ARRAY);
    assertEquals(expected, result);

    // Test 8: generics
    typeString = "List<String> list";
    result = TypeParser.createFrom(typeString, true);
    List<Type> generics = new ArrayList<>();
    generics.add(
        new ObjectType(
            "String",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.NOT_APPLICABLE,
            false));
    expected =
        new ObjectType(
            "List",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            generics,
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    assertEquals(expected, result);

    // Test 9: more generics
    typeString = "List<List<List<String>>, List<String>> data";
    result = TypeParser.createFrom(typeString, true);

    ObjectType genericStringType =
        new ObjectType(
            "String",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    List<Type> generics3 = new ArrayList<>();
    generics3.add(genericStringType);

    ObjectType genericElement3 =
        new ObjectType(
            "List",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            generics3,
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    List<Type> generics2a = new ArrayList<>();
    generics2a.add(genericElement3);

    List<Type> generics2b = new ArrayList<>();
    generics2b.add(genericStringType);

    ObjectType genericElement1 =
        new ObjectType(
            "List",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            generics2a,
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    ObjectType genericElement2 =
        new ObjectType(
            "List",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            generics2b,
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    generics = new ArrayList<>();
    generics.add(genericElement1);
    generics.add(genericElement2);
    expected =
        new ObjectType(
            "List",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            generics,
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    assertEquals(expected, result);
  }

  @Test
  void createFromC() {
    String typeString;
    Type result;

    TypeParser.setLanguageSupplier(() -> TypeManager.Language.CXX);

    // Test 1: Function pointer
    typeString = "void (*single_param)(int)";
    result = TypeParser.createFrom(typeString, true);
    List<Type> parameterList =
        List.of(
            new ObjectType(
                "int",
                Type.Storage.AUTO,
                new Type.Qualifier(),
                Collections.emptyList(),
                ObjectType.Modifier.SIGNED,
                true));
    Type expected =
        new FunctionPointerType(
            new Type.Qualifier(), Type.Storage.AUTO, parameterList, new IncompleteType());
    assertEquals(expected, result);

    // Test 1.1: interleaved brackets in function pointer
    typeString = "void ((*single_param)(int))";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(result, expected);

    // Test 2: Stronger binding of brackets and pointer
    typeString = "char (* const a)[]";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new PointerType(
            new PointerType(
                new ObjectType(
                    "char",
                    Type.Storage.AUTO,
                    new Type.Qualifier(),
                    Collections.emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true),
                PointerType.PointerOrigin.ARRAY),
            PointerType.PointerOrigin.POINTER);
    expected.setQualifier(new Type.Qualifier(true, false, false, false));
    assertEquals(expected, result);

    // Test 3: Mutable pointer to a mutable char
    typeString = "char *p";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new PointerType(
            new ObjectType(
                "char",
                Type.Storage.AUTO,
                new Type.Qualifier(),
                Collections.emptyList(),
                ObjectType.Modifier.SIGNED,
                true),
            PointerType.PointerOrigin.POINTER);
    assertEquals(expected, result);

    // Test 3.1: Different Whitespaces
    typeString = "char* p";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(expected, result);

    // Test 3.2: Different Whitespaces
    typeString = "char * p";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(expected, result);

    // Test 4: Mutable pointer to a constant char
    typeString = "const char *p;";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new PointerType(
            new ObjectType(
                "char",
                Type.Storage.AUTO,
                new Type.Qualifier(true, false, false, false),
                Collections.emptyList(),
                ObjectType.Modifier.SIGNED,
                true),
            PointerType.PointerOrigin.POINTER);
    assertEquals(expected, result);

    // Test 5: Constant pointer to a mutable char
    typeString = "char * const p;";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new PointerType(
            new ObjectType(
                "char",
                Type.Storage.AUTO,
                new Type.Qualifier(false, false, false, false),
                Collections.emptyList(),
                ObjectType.Modifier.SIGNED,
                true),
            PointerType.PointerOrigin.POINTER);
    expected.setQualifier(new Type.Qualifier(true, false, false, false));
    assertEquals(expected, result);

    // Test 6: Constant pointer to a constant char
    typeString = "const char * const p;";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new PointerType(
            new ObjectType(
                "char",
                Type.Storage.AUTO,
                new Type.Qualifier(true, false, false, false),
                Collections.emptyList(),
                ObjectType.Modifier.SIGNED,
                true),
            PointerType.PointerOrigin.POINTER);
    expected.setQualifier(new Type.Qualifier(true, false, false, false));
    assertEquals(expected, result);

    // Test 7: Array of const pointer to static const char
    typeString = "static const char * const somearray []";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new PointerType(
            new PointerType(
                new ObjectType(
                    "char",
                    Type.Storage.STATIC,
                    new Type.Qualifier(true, false, false, false),
                    Collections.emptyList(),
                    ObjectType.Modifier.SIGNED,
                    true),
                PointerType.PointerOrigin.POINTER),
            PointerType.PointerOrigin.ARRAY);
    ((PointerType) expected)
        .getElementType()
        .setQualifier(new Type.Qualifier(true, false, false, false));
    assertEquals(expected, result);

    // Test 7.1: Array of array of pointer to static const char
    typeString = "static const char * somearray[][]";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new PointerType(
            new PointerType(
                new PointerType(
                    new ObjectType(
                        "char",
                        Type.Storage.STATIC,
                        new Type.Qualifier(true, false, false, false),
                        Collections.emptyList(),
                        ObjectType.Modifier.SIGNED,
                        true),
                    PointerType.PointerOrigin.POINTER),
                PointerType.PointerOrigin.ARRAY),
            PointerType.PointerOrigin.ARRAY);
    assertEquals(expected, result);

    // Test 8: Generics
    typeString = "Array<int> array";
    result = TypeParser.createFrom(typeString, true);
    List<Type> generics = new ArrayList<>();
    generics.add(
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            Collections.emptyList(),
            ObjectType.Modifier.SIGNED,
            true));
    expected =
        new ObjectType(
            "Array",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            generics,
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    assertEquals(expected, result);

    // Test 9: Compound Primitive Types
    typeString = "long long int";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "long long int",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    assertEquals(expected, result);

    // Test 10: Unsigned/Signed Types
    typeString = "unsigned int";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.UNSIGNED,
            true);
    assertEquals(expected, result);

    typeString = "signed int";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    assertEquals(expected, result);

    typeString = "A a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ObjectType(
            "A",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.NOT_APPLICABLE,
            false);
    assertEquals(expected, result);

    // Test 11: Unsigned + const + compound primitive Types
    expected =
        new ObjectType(
            "long long int",
            Type.Storage.AUTO,
            new Type.Qualifier(true, false, false, false),
            new ArrayList<>(),
            ObjectType.Modifier.UNSIGNED,
            true);

    typeString = "const unsigned long long int a = 1";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(expected, result);

    typeString = "unsigned const long long int b = 1";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(expected, result);

    typeString = "unsigned long const long int c = 1";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(expected, result);

    typeString = "unsigned long long const int d = 1";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(expected, result);

    typeString = "unsigned long long int const e = 1";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(expected, result);

    // Test 12: C++ Reference Types
    typeString = "const int& ref = a";
    result = TypeParser.createFrom(typeString, true);
    expected =
        new ReferenceType(
            Type.Storage.AUTO,
            new Type.Qualifier(true, false, false, false),
            new ObjectType(
                "int",
                Type.Storage.AUTO,
                new Type.Qualifier(),
                new ArrayList<>(),
                ObjectType.Modifier.SIGNED,
                true));
    assertEquals(expected, result);

    typeString = "int const &ref2 = a";
    result = TypeParser.createFrom(typeString, true);
    assertEquals(expected, result);
  }

  // Tests on the resulting graph

  @Test
  void graphTest() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "types");
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel);

    List<ObjectType> variables = Util.subnodesOfType(result, ObjectType.class);
    List<RecordDeclaration> recordDeclarations =
        Util.subnodesOfType(result, RecordDeclaration.class);

    // Test RecordDeclaration relationship
    List<ObjectType> objectTypes = TestUtils.findByName(variables, "A");
    RecordDeclaration recordDeclarationA = TestUtils.findByUniqueName(recordDeclarations, "A");
    for (ObjectType objectType : objectTypes) {
      assertEquals(recordDeclarationA, objectType.getRecordDeclaration());
    }

    // Test uniqueness of types x and y have same type
    List<FieldDeclaration> fieldDeclarations = Util.subnodesOfType(result, FieldDeclaration.class);
    FieldDeclaration x = TestUtils.findByUniqueName(fieldDeclarations, "x");
    FieldDeclaration z = TestUtils.findByUniqueName(fieldDeclarations, "z");
    assertSame(x.getType(), z.getType());

    // Test propagation of specifiers in primitive fields (final int y)
    FieldDeclaration y = TestUtils.findByUniqueName(fieldDeclarations, "y");
    assertTrue(y.getType().getQualifier().isConst());

    // Test propagation of specifiers in non-primitive fields (final A a)
    List<VariableDeclaration> variableDeclarations =
        Util.subnodesOfType(result, VariableDeclaration.class);
    VariableDeclaration aA = TestUtils.findByUniqueName(variableDeclarations, "a");
    assertTrue(aA.getType().getQualifier().isConst());

    // Test propagation of specifiers in variables (final String s)
    VariableDeclaration sString = TestUtils.findByUniqueName(variableDeclarations, "s");
    assertTrue(sString.getType().getQualifier().isConst());

    // Test PointerType chain with array
    VariableDeclaration array = TestUtils.findByUniqueName(variableDeclarations, "array");
    assertTrue(array.getType() instanceof PointerType);
    assertEquals(((PointerType) array.getType()).getElementType(), x.getType());

    topLevel = Path.of("src", "test", "resources", "types");
    result = TestUtils.analyze("cpp", topLevel);

    variableDeclarations = Util.subnodesOfType(result, VariableDeclaration.class);

    // Test PointerType chain with pointer
    VariableDeclaration regularInt = TestUtils.findByUniqueName(variableDeclarations, "regularInt");
    VariableDeclaration ptr = TestUtils.findByUniqueName(variableDeclarations, "ptr");
    assertTrue(ptr.getType() instanceof PointerType);
    assertEquals(((PointerType) ptr.getType()).getElementType(), regularInt.getType());

    // Test type Propagation (auto) UnknownType
    VariableDeclaration unknown = TestUtils.findByUniqueName(variableDeclarations, "unknown");
    assertEquals(UnknownType.getUnknownType(), unknown.getType());

    // Test type Propagation auto
    VariableDeclaration propagated = TestUtils.findByUniqueName(variableDeclarations, "propagated");
    assertEquals(regularInt.getType(), propagated.getType());
  }
}
