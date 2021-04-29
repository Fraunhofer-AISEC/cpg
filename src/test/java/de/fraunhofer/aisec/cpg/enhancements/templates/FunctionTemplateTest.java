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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FunctionTemplateTest extends BaseTest {

  private final Path topLevel = Path.of("src", "test", "resources", "templates");

  @Test
  void testDependentType() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplate.cpp").toFile()), topLevel, true);

    List<VariableDeclaration> variableDeclarations =
        TestUtils.subnodesOfType(result, VariableDeclaration.class);
    VariableDeclaration x = TestUtils.findByUniqueName(variableDeclarations, "x");

    assertEquals(UnknownType.getUnknownType(), x.getType());

    List<DeclaredReferenceExpression> declaredReferenceExpressions =
        TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class);
    DeclaredReferenceExpression xDeclaredReferenceExpression =
        TestUtils.findByUniqueName(declaredReferenceExpressions, "x");

    assertEquals(UnknownType.getUnknownType(), xDeclaredReferenceExpression.getType());

    List<BinaryOperator> binaryOperators = TestUtils.subnodesOfType(result, BinaryOperator.class);
    BinaryOperator dependetOperation =
        TestUtils.findByUniquePredicate(binaryOperators, b -> b.getCode().equals("= val * N"));

    assertEquals(UnknownType.getUnknownType(), dependetOperation.getType());
  }

  @Test
  void testFunctionTemplateStructure() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplate.cpp").toFile()), topLevel, true);
    // This test checks the structure of FunctionTemplates without the TemplateExpansionPass
    FunctionTemplateDeclaration functionTemplateDeclaration =
        TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class).get(0);

    // Check FunctionTemplate Parameters
    List<TypeTemplateParamDeclaration> typeTemplateParamDeclarations =
        TestUtils.subnodesOfType(result, TypeTemplateParamDeclaration.class);
    assertEquals(1, typeTemplateParamDeclarations.size());
    TypeTemplateParamDeclaration typeTemplateParamDeclaration =
        typeTemplateParamDeclarations.get(0);
    assertEquals(typeTemplateParamDeclaration, functionTemplateDeclaration.getParameters().get(0));

    ParameterizedType T = new ParameterizedType("T");
    ObjectType intType =
        new ObjectType(
            "int",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    ObjectType floatType =
        new ObjectType(
            "float",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);

    assertEquals(T, typeTemplateParamDeclaration.getType());
    assertEquals(intType, typeTemplateParamDeclaration.getDefault());
    assertTrue(typeTemplateParamDeclaration.getPossibleInitializations().contains(intType));
    // assertTrue(typeTemplateParamDeclaration.getPossibleInitializations().contains(floatType));
    // //TODO

    NonTypeTemplateParamDeclaration N =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, NonTypeTemplateParamDeclaration.class), "N");
    Literal<Integer> int2 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(2));
    Literal<Integer> int3 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(3));
    Literal<Integer> int5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));
    assertEquals(N, functionTemplateDeclaration.getParameters().get(1));
    assertEquals(intType, N.getType());
    assertEquals(5, ((Literal) N.getDefault()).getValue());
    assertTrue(N.getPossibleInitializations().contains(int5));
    // assertTrue(N.getPossibleInitializations().contains(int3)); // TODO
    // assertTrue(N.getPossibleInitializations().contains(int2)); // TODO

    // Check the realization
    assertEquals(1, functionTemplateDeclaration.getRealization().size());
    FunctionDeclaration fixed_multiply = functionTemplateDeclaration.getRealization().get(0);
    assertEquals(T, fixed_multiply.getType());
    ParamVariableDeclaration val = fixed_multiply.getParameters().get(0);
    assertEquals(T, val.getType());

    // Check the invokes
    CallExpression callInt2 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 12);

    assertEquals(1, callInt2.getInvokes().size());
    // assertEquals(fixed_multiply, callInt2.getInvokes().get(0)); // TODO

    CallExpression callFloat3 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 13);

    assertEquals(1, callFloat3.getInvokes().size());
    // assertEquals(fixed_multiply, callFloat3.getInvokes().get(0)); // TODO

    // TODO tests for arguments and initializers of the TemplateCallExpressions

  }
}
