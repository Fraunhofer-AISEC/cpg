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
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ClassTemplateTest {
  private final Path topLevel = Path.of("src", "test", "resources", "templates", "classtemplates");

  @Test
  void testClassTemplateStructure() throws Exception {
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


}
