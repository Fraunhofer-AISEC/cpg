/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements.templates;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import de.fraunhofer.aisec.cpg.graph.types.PointerType;
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassTemplateTest extends BaseTest {
  private final Path topLevel = Path.of("src", "test", "resources", "templates", "classtemplates");

  void testTemplateStructure(
      ClassTemplateDeclaration template,
      RecordDeclaration pair,
      TypeParamDeclaration type1,
      TypeParamDeclaration type2) {
    assertEquals(2, template.getParameters().size());
    assertEquals(type1, template.getParameters().get(0));
    assertEquals(type2, template.getParameters().get(1));

    assertEquals(1, template.getRealization().size());
    assertEquals(pair, template.getRealization().get(0));
  }

  void testClassTemplateFields(
      RecordDeclaration pair,
      FieldDeclaration thisField,
      FieldDeclaration first,
      FieldDeclaration second) {
    assertTrue(pair.getFields().contains(thisField));
    assertTrue(pair.getFields().contains(first));
    assertTrue(pair.getFields().contains(second));
  }

  ObjectType testClassTemplatesTypes(
      RecordDeclaration pair,
      FieldDeclaration thisField,
      TypeParamDeclaration type1,
      TypeParamDeclaration type2) {
    assertEquals("Pair", thisField.getType().getName());
    assertTrue(thisField.getType() instanceof ObjectType);
    ObjectType pairType = (ObjectType) thisField.getType();

    assertEquals("Type1", type1.getType().getName());
    assertEquals("Type2", type2.getType().getName());

    assertEquals(type1.getType(), pairType.getGenerics().get(0));
    assertEquals(type2.getType(), pairType.getGenerics().get(1));

    assertEquals(pair, pairType.getRecordDeclaration());
    return pairType;
  }

  void testClassTemplateConstructor(
      RecordDeclaration pair,
      ObjectType pairType,
      ConstructorDeclaration pairConstructorDeclaration) {
    assertEquals(pair, pairConstructorDeclaration.getRecordDeclaration());
    assertTrue(pair.getConstructors().contains(pairConstructorDeclaration));
    assertEquals(pairType, pairConstructorDeclaration.getType());
  }

  void testClassTemplateInvocation(
      ConstructorDeclaration pairConstructorDeclaration,
      ConstructExpression constructExpression,
      RecordDeclaration pair,
      ObjectType pairType,
      ClassTemplateDeclaration template,
      VariableDeclaration point1) {
    assertEquals(pairConstructorDeclaration, constructExpression.getConstructor());
    assertTrue(constructExpression.getInvokes().contains(pairConstructorDeclaration));
    assertEquals(pair, constructExpression.getInstantiates());
    assertEquals(template, constructExpression.getTemplateInstantiation());

    assertEquals("Pair", constructExpression.getType().getName());
    assertEquals(constructExpression.getType(), point1.getType());
    assertNotEquals(pairType, constructExpression.getType());

    ObjectType instantiatedType = (ObjectType) constructExpression.getType();

    assertEquals(2, instantiatedType.getGenerics().size());
    assertEquals("int", instantiatedType.getGenerics().get(0).getName());
    assertEquals("int", instantiatedType.getGenerics().get(1).getName());

    assertEquals(2, constructExpression.getTemplateParameters().size());
    assertEquals(
        "int",
        ((TypeExpression) constructExpression.getTemplateParameters().get(0)).getType().getName());
    assertEquals(
        "int",
        ((TypeExpression) constructExpression.getTemplateParameters().get(1)).getType().getName());
    assertTrue(constructExpression.getTemplateParameters().get(0).isImplicit());
    assertTrue(constructExpression.getTemplateParameters().get(1).isImplicit());

    assertEquals(2, point1.getTemplateParameters().size());
    assertEquals(
        "int", ((TypeExpression) point1.getTemplateParameters().get(0)).getType().getName());
    assertEquals(
        "int", ((TypeExpression) point1.getTemplateParameters().get(1)).getType().getName());
    assertFalse(point1.getTemplateParameters().get(0).isImplicit());
    assertFalse(point1.getTemplateParameters().get(1).isImplicit());
  }

  @Test
  void testClassTemplateStructure() throws Exception {
    TypeManager.reset();
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "pair.cpp").toFile()), topLevel, true);

    List<ClassTemplateDeclaration> classTemplateDeclarations =
        TestUtils.subnodesOfType(result, ClassTemplateDeclaration.class);
    ClassTemplateDeclaration template =
        TestUtils.findByUniqueName(
            classTemplateDeclarations, "template<class Type1, class Type2> class Pair");

    RecordDeclaration pair =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, RecordDeclaration.class), "Pair");

    TypeParamDeclaration type1 =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, TypeParamDeclaration.class), "class Type1");
    TypeParamDeclaration type2 =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, TypeParamDeclaration.class), "class Type2");

    FieldDeclaration first =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, FieldDeclaration.class), "first");
    FieldDeclaration second =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, FieldDeclaration.class), "second");
    FieldDeclaration thisField =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, FieldDeclaration.class), "this");

    ConstructorDeclaration pairConstructorDeclaration =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ConstructorDeclaration.class), "Pair");

    ConstructExpression constructExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ConstructExpression.class),
            c -> c.getCode().equals("()"));

    VariableDeclaration point1 =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "point1");

    // Test Template Structure
    testTemplateStructure(template, pair, type1, type2);

    // Test Fields
    testClassTemplateFields(pair, thisField, first, second);

    // Test Types
    ObjectType pairType = testClassTemplatesTypes(pair, thisField, type1, type2);

    // Test Constructor
    testClassTemplateConstructor(pair, pairType, pairConstructorDeclaration);

    // Test Invocation
    testClassTemplateInvocation(
        pairConstructorDeclaration, constructExpression, pair, pairType, template, point1);
  }

  @Test
  void testClassTemplateWithValueParameter() throws Exception {
    // Test pair2.cpp: Add Value Parameter to Template Instantiation
    TypeManager.reset();
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "pair2.cpp").toFile()), topLevel, true);

    List<ClassTemplateDeclaration> classTemplateDeclarations =
        TestUtils.subnodesOfType(result, ClassTemplateDeclaration.class);
    ClassTemplateDeclaration template =
        TestUtils.findByUniqueName(
            classTemplateDeclarations, "template<class Type1, class Type2, int N> class Pair");

    RecordDeclaration pair =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, RecordDeclaration.class), "Pair");

    ParamVariableDeclaration N =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ParamVariableDeclaration.class), "N");

    FieldDeclaration n =
        TestUtils.findByUniqueName(TestUtils.subnodesOfType(result, FieldDeclaration.class), "n");

    FieldDeclaration thisField =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, FieldDeclaration.class), "this");

    ConstructorDeclaration pairConstructorDeclaration =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ConstructorDeclaration.class), "Pair");

    ConstructExpression constructExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ConstructExpression.class),
            c -> c.getCode().equals("()"));

    Literal<?> literal3 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class),
            l -> l.getValue().equals(3) && !l.isImplicit());

    Literal<?> literal3Implicit =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class),
            l -> l.getValue().equals(3) && l.isImplicit());

    VariableDeclaration point1 =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "point1");

    assertEquals(3, template.getParameters().size());
    assertEquals(N, template.getParameters().get(2));

    assertTrue(pair.getFields().contains(n));
    assertEquals(N, ((DeclaredReferenceExpression) n.getInitializer()).getRefersTo());

    // Test Type
    ObjectType type = (ObjectType) thisField.getType();
    assertEquals(pairConstructorDeclaration.getType(), type);
    assertEquals(pair, type.getRecordDeclaration());

    assertEquals(2, type.getGenerics().size());
    assertEquals("Type1", type.getGenerics().get(0).getName());
    assertEquals("Type2", type.getGenerics().get(1).getName());

    ObjectType instantiatedType = (ObjectType) constructExpression.getType();
    assertEquals(instantiatedType, point1.getType());

    assertEquals(2, instantiatedType.getGenerics().size());
    assertEquals("int", instantiatedType.getGenerics().get(0).getName());
    assertEquals("int", instantiatedType.getGenerics().get(1).getName());

    // Test TemplateParameter of VariableDeclaration
    assertEquals(3, point1.getTemplateParameters().size());
    assertEquals(literal3, point1.getTemplateParameters().get(2));

    // Test Invocation
    assertEquals(3, constructExpression.getTemplateParameters().size());
    assertEquals(literal3Implicit, constructExpression.getTemplateParameters().get(2));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(2)
            .getProperty(Properties.INSTANTIATION));

    assertEquals(pair, constructExpression.getInstantiates());
    assertEquals(template, constructExpression.getTemplateInstantiation());
  }

  void testStructTemplateWithSameDefaultTypeInvocation(
      ClassTemplateDeclaration template,
      RecordDeclaration pair,
      ConstructorDeclaration pairConstructorDeclaration,
      ConstructExpression constructExpression,
      VariableDeclaration point1) {
    assertEquals(pair, constructExpression.getInstantiates());
    assertEquals(template, constructExpression.getTemplateInstantiation());
    assertEquals(pairConstructorDeclaration, constructExpression.getConstructor());

    assertEquals(2, constructExpression.getTemplateParameters().size());
    assertEquals("int", constructExpression.getTemplateParameters().get(0).getName());
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(0)
            .getProperty(Properties.INSTANTIATION));
    assertEquals("int", constructExpression.getTemplateParameters().get(1).getName());
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(1)
            .getProperty(Properties.INSTANTIATION));

    ObjectType pairTypeInstantiated = (ObjectType) constructExpression.getType();

    assertEquals(pair, pairTypeInstantiated.getRecordDeclaration());
    assertEquals(2, pairTypeInstantiated.getGenerics().size());
    assertEquals("int", pairTypeInstantiated.getGenerics().get(0).getName());
    assertEquals("int", pairTypeInstantiated.getGenerics().get(1).getName());

    assertEquals(pairTypeInstantiated, point1.getType());
  }

  @Test
  void testStructTemplateWithSameDefaultType() throws Exception {
    // Test pair3.cpp: Template a struct instead of a class and use a Type1 as default of Type2
    TypeManager.reset();
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "pair3.cpp").toFile()), topLevel, true);

    ClassTemplateDeclaration template =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ClassTemplateDeclaration.class),
            "template<class Type1, class Type2 = Type1> struct Pair");

    RecordDeclaration pair =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, RecordDeclaration.class), "Pair");

    ConstructorDeclaration pairConstructorDeclaration =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ConstructorDeclaration.class), "Pair");

    TypeParamDeclaration type1 =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, TypeParamDeclaration.class), "class Type1");
    TypeParamDeclaration type2 =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, TypeParamDeclaration.class), "class Type2 = Type1");

    FieldDeclaration first =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, FieldDeclaration.class), "first");
    FieldDeclaration second =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, FieldDeclaration.class), "second");

    VariableDeclaration point1 =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "point1");

    ConstructExpression constructExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ConstructExpression.class),
            c -> c.getCode().equals("()"));

    assertEquals(1, template.getRealization().size());
    assertEquals(pair, template.getRealization().get(0));

    assertEquals(2, template.getParameters().size());
    assertEquals(type1, template.getParameters().get(0));
    assertEquals(type2, template.getParameters().get(1));

    assertEquals("Type1", type1.getType().getName());
    ParameterizedType type1ParameterizedType = (ParameterizedType) type1.getType();

    assertEquals("Type2", type2.getType().getName());
    ParameterizedType type2ParameterizedType = (ParameterizedType) type2.getType();

    assertEquals(type1ParameterizedType, type2.getDefault());

    ObjectType pairType = (ObjectType) pairConstructorDeclaration.getType();

    assertEquals(2, pairType.getGenerics().size());
    assertEquals(type1ParameterizedType, pairType.getGenerics().get(0));
    assertEquals(type2ParameterizedType, pairType.getGenerics().get(1));

    assertEquals(3, pair.getFields().size()); // cpp has implicit `this` field
    assertEquals(first, pair.getFields().get(1));
    assertEquals(second, pair.getFields().get(2));

    assertEquals(type1ParameterizedType, first.getType());
    assertEquals(type2ParameterizedType, second.getType());

    testStructTemplateWithSameDefaultTypeInvocation(
        template, pair, pairConstructorDeclaration, constructExpression, point1);
  }

  @Test
  void testTemplateOverrindingDefaults() throws Exception {
    // Test pair3-1.cpp: Override defaults of template
    TypeManager.reset();
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "pair3-1.cpp").toFile()), topLevel, true);

    ClassTemplateDeclaration template =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ClassTemplateDeclaration.class),
            "template<class Type1, class Type2 = Type1, int A=1, int B=A> struct Pair");

    RecordDeclaration pair =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, RecordDeclaration.class), "Pair");

    ConstructExpression constructExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ConstructExpression.class),
            c -> c.getCode().equals("()"));

    Literal<?> literal2 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class),
            l -> l.getValue().equals(2) && !l.isImplicit());

    Literal<?> literal2Implicit =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class),
            l -> l.getValue().equals(2) && l.isImplicit());

    assertEquals(pair, constructExpression.getInstantiates());
    assertEquals(template, constructExpression.getTemplateInstantiation());

    assertEquals(4, constructExpression.getTemplateParameters().size());

    assertEquals("int", constructExpression.getTemplateParameters().get(0).getName());
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(0)
            .getProperty(Properties.INSTANTIATION));

    assertEquals("int", constructExpression.getTemplateParameters().get(1).getName());
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(1)
            .getProperty(Properties.INSTANTIATION));

    assertEquals(literal2Implicit, constructExpression.getTemplateParameters().get(2));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(2)
            .getProperty(Properties.INSTANTIATION));

    assertEquals(literal2Implicit, constructExpression.getTemplateParameters().get(3));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.DEFAULT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(3)
            .getProperty(Properties.INSTANTIATION));

    ObjectType type = (ObjectType) constructExpression.getType();

    assertEquals(pair, type.getRecordDeclaration());

    assertEquals(2, type.getGenerics().size());
    assertEquals("int", type.getGenerics().get(0).getName());
    assertEquals("int", type.getGenerics().get(1).getName());
  }

  @Test
  void testTemplateRecursiveDefaults() throws Exception {
    // Test pair3-2.cpp: Use recursive template parameters using defaults
    TypeManager.reset();
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "pair3-2.cpp").toFile()), topLevel, true);

    ClassTemplateDeclaration template =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ClassTemplateDeclaration.class),
            "template<class Type1, class Type2 = Type1, int A=1, int B=A> struct Pair");

    RecordDeclaration pair =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, RecordDeclaration.class), "Pair");

    ParamVariableDeclaration A =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ParamVariableDeclaration.class), "A");
    ParamVariableDeclaration B =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ParamVariableDeclaration.class), "B");

    ConstructExpression constructExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ConstructExpression.class),
            c -> c.getCode().equals("()"));

    Literal<?> literal1 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(1));

    assertEquals(4, template.getParameters().size());

    assertEquals(A, template.getParameters().get(2));
    assertEquals(literal1, A.getDefault());

    assertEquals(B, template.getParameters().get(3));
    assertEquals(A, ((DeclaredReferenceExpression) B.getDefault()).getRefersTo());

    assertEquals(pair, constructExpression.getInstantiates());
    assertEquals(template, constructExpression.getTemplateInstantiation());

    assertEquals(4, constructExpression.getTemplateParameters().size());

    assertEquals(
        "int",
        ((TypeExpression) constructExpression.getTemplateParameters().get(0)).getType().getName());
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(0)
            .getProperty(Properties.INSTANTIATION));
    assertEquals(
        0,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(0)
            .getProperty(Properties.INDEX));

    assertEquals(
        "int",
        ((TypeExpression) constructExpression.getTemplateParameters().get(1)).getType().getName());
    assertEquals(
        TemplateDeclaration.TemplateInitialization.DEFAULT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(1)
            .getProperty(Properties.INSTANTIATION));
    assertEquals(
        1,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(1)
            .getProperty(Properties.INDEX));

    assertEquals(literal1, constructExpression.getTemplateParameters().get(2));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.DEFAULT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(2)
            .getProperty(Properties.INSTANTIATION));
    assertEquals(
        2,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(2)
            .getProperty(Properties.INDEX));

    assertEquals(literal1, constructExpression.getTemplateParameters().get(3));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.DEFAULT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(3)
            .getProperty(Properties.INSTANTIATION));
    assertEquals(
        3,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(3)
            .getProperty(Properties.INDEX));

    // Test Type
    ObjectType type = (ObjectType) constructExpression.getType();
    assertEquals(2, type.getGenerics().size());
    assertEquals("int", type.getGenerics().get(0).getName());
    assertEquals("int", type.getGenerics().get(1).getName());
  }

  @Test
  void testReferenceInTemplates() throws Exception {
    // Test array.cpp: checks usage of referencetype of parameterized type (T[])
    TypeManager.reset();
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "array.cpp").toFile()), topLevel, true);

    ClassTemplateDeclaration template =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ClassTemplateDeclaration.class),
            "template<typename T, int N=10> class Array");

    RecordDeclaration array =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, RecordDeclaration.class), "Array");

    ParamVariableDeclaration N =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ParamVariableDeclaration.class), "N");

    TypeParamDeclaration T =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, TypeParamDeclaration.class), "typename T");

    Literal<?> literal10 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(10));

    FieldDeclaration thisField =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, FieldDeclaration.class), "this");
    FieldDeclaration m_Array =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, FieldDeclaration.class), "m_Array");

    assertEquals(2, template.getParameters().size());
    assertEquals(T, template.getParameters().get(0));
    assertEquals(N, template.getParameters().get(1));
    assertEquals(literal10, N.getDefault());

    assertEquals(2, array.getFields().size());
    assertEquals(thisField, array.getFields().get(0));
    assertEquals(m_Array, array.getFields().get(1));

    ObjectType arrayType = (ObjectType) thisField.getType();

    assertEquals(1, arrayType.getGenerics().size());
    assertEquals("T", arrayType.getGenerics().get(0).getName());

    ParameterizedType typeT = (ParameterizedType) arrayType.getGenerics().get(0);

    assertEquals(typeT, T.getType());

    assertTrue(m_Array.getType() instanceof PointerType);
    PointerType tArray = (PointerType) m_Array.getType();

    assertEquals(typeT, tArray.getElementType());

    ConstructExpression constructExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ConstructExpression.class),
            c -> c.getCode().equals("()"));

    assertEquals(template, constructExpression.getTemplateInstantiation());
    assertEquals(array, constructExpression.getInstantiates());

    assertEquals("int", constructExpression.getTemplateParameters().get(0).getName());
    assertEquals(literal10, constructExpression.getTemplateParameters().get(1));

    assertEquals("Array", constructExpression.getType().getName());

    ObjectType instantiatedType = (ObjectType) constructExpression.getType();

    assertEquals(1, instantiatedType.getGenerics().size());
    assertEquals("int", instantiatedType.getGenerics().get(0).getName());
  }

  @Test
  void testTemplateInstantiationWithNew() throws Exception {
    // Test array2.cpp: Test template usage with new keyword
    TypeManager.reset();

    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "array2.cpp").toFile()), topLevel, true);

    ClassTemplateDeclaration template =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ClassTemplateDeclaration.class),
            "template<typename T, int N=10> class Array");

    RecordDeclaration array =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, RecordDeclaration.class), "Array");

    ConstructExpression constructExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ConstructExpression.class),
            c -> c.getCode().equals("()"));

    Literal<?> literal5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class),
            l ->
                l.getValue().equals(5)
                    && l.getLocation().getRegion().getEndColumn() == 41
                    && !l.isImplicit());

    Literal<?> literal5Declaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class),
            l ->
                l.getValue().equals(5)
                    && l.getLocation().getRegion().getEndColumn() == 14
                    && !l.isImplicit());

    Literal<?> literal5Implicit =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class),
            l ->
                l.getValue().equals(5)
                    && l.getLocation().getRegion().getEndColumn() == 41
                    && l.isImplicit());

    VariableDeclaration arrayVariable =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "array");
    NewExpression newExpression =
        TestUtils.findByUniqueName(TestUtils.subnodesOfType(result, NewExpression.class), "");

    assertEquals(array, constructExpression.getInstantiates());
    assertEquals(template, constructExpression.getTemplateInstantiation());

    assertEquals(2, constructExpression.getTemplateParameters().size());
    assertEquals(
        "int",
        ((TypeExpression) constructExpression.getTemplateParameters().get(0)).getType().getName());
    assertTrue(constructExpression.getTemplateParameters().get(0).isImplicit());
    assertEquals(literal5Implicit, constructExpression.getTemplateParameters().get(1));

    assertEquals(2, arrayVariable.getTemplateParameters().size());
    assertEquals(
        "int", ((TypeExpression) arrayVariable.getTemplateParameters().get(0)).getType().getName());
    assertFalse(arrayVariable.getTemplateParameters().get(0).isImplicit());
    assertEquals(literal5Declaration, arrayVariable.getTemplateParameters().get(1));

    assertEquals("Array", constructExpression.getType().getName());

    ObjectType arrayType = (ObjectType) constructExpression.getType();

    assertEquals(1, arrayType.getGenerics().size());
    assertEquals("int", arrayType.getGenerics().get(0).getName());
    assertEquals(array, arrayType.getRecordDeclaration());

    assertEquals(arrayType.reference(PointerOrigin.POINTER), arrayVariable.getType());
    assertEquals(arrayType.reference(PointerOrigin.POINTER), newExpression.getType());
  }
}
