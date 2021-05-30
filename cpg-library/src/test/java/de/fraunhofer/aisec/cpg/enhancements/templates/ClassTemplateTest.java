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

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ClassTemplateTest {
  private final Path topLevel = Path.of("src", "test", "resources", "templates", "classtemplates");

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

    // Test Template Structure
    assertEquals(2, template.getParameters().size());
    assertEquals(type1, template.getParameters().get(0));
    assertEquals(type2, template.getParameters().get(1));

    assertEquals(1, template.getRealization().size());
    assertEquals(pair, template.getRealization().get(0));

    // Test Fields
    assertTrue(pair.getFields().contains(thisField));
    assertTrue(pair.getFields().contains(first));
    assertTrue(pair.getFields().contains(second));

    // Test Types
    assertEquals("Pair", thisField.getType().getName());
    assertTrue(thisField.getType() instanceof ObjectType);
    ObjectType pairType = (ObjectType) thisField.getType();

    assertEquals("Type1", type1.getType().getName());
    assertEquals("Type2", type2.getType().getName());

    assertEquals(type1.getType(), pairType.getGenerics().get(0));
    assertEquals(type2.getType(), pairType.getGenerics().get(1));

    assertEquals(pair, pairType.getRecordDeclaration());

    // Test Constructor
    assertEquals(pair, pairConstructorDeclaration.getRecordDeclaration());
    assertTrue(pair.getConstructors().contains(pairConstructorDeclaration));
    assertEquals(pairType, pairConstructorDeclaration.getType());

    // Test Invocation
    assertEquals(pairConstructorDeclaration, constructExpression.getConstructor());
    assertTrue(constructExpression.getInvokes().contains(pairConstructorDeclaration));
    assertEquals(pair, constructExpression.getInstantiates());
    assertEquals(template, constructExpression.getTemplateInstantiation());

    assertEquals("Pair", constructExpression.getType().getName());
    assertNotEquals(pairType, constructExpression.getType());

    ObjectType instantiatedType = (ObjectType) constructExpression.getType();

    assertEquals(2, instantiatedType.getGenerics().size());
    assertEquals("int", instantiatedType.getGenerics().get(0).getName());
    assertEquals("int", instantiatedType.getGenerics().get(1).getName());

    assertEquals(2, constructExpression.getTemplateParameters().size());
    assertEquals("int", constructExpression.getTemplateParameters().get(0).getName());
    assertEquals("int", constructExpression.getTemplateParameters().get(1).getName());
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
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(3));

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

    // Test Invocation
    assertEquals(3, constructExpression.getTemplateParameters().size());
    assertEquals(literal3, constructExpression.getTemplateParameters().get(2));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(2)
            .getProperty(Properties.INSTANTIATION));

    assertEquals(pair, constructExpression.getInstantiates());
    assertEquals(template, constructExpression.getTemplateInstantiation());
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

    assertEquals(2, pair.getFields().size());
    assertEquals(first, pair.getFields().get(0));
    assertEquals(second, pair.getFields().get(1));

    assertEquals(type1ParameterizedType, first.getType());
    assertEquals(type2ParameterizedType, second.getType());

    ConstructExpression constructExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ConstructExpression.class),
            c -> c.getCode().equals("()"));

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
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(2));

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

    assertEquals(literal2, constructExpression.getTemplateParameters().get(2));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        constructExpression
            .getTemplateParametersPropertyEdge()
            .get(2)
            .getProperty(Properties.INSTANTIATION));

    assertEquals(literal2, constructExpression.getTemplateParameters().get(3));
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

    assertEquals("int", constructExpression.getTemplateParameters().get(0).getName());
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

    assertEquals("int", constructExpression.getTemplateParameters().get(1).getName());
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
}
