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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class FunctionTemplateTest extends BaseTest {

  private final Path topLevel =
      Path.of("src", "test", "resources", "templates", "functiontemplates");

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
    assertTrue(typeTemplateParamDeclaration.getPossibleInitializations().contains(floatType));

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
    assertTrue(N.getPossibleInitializations().contains(int3));
    assertTrue(N.getPossibleInitializations().contains(int2));

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
    assertEquals(fixed_multiply, callInt2.getInvokes().get(0));

    CallExpression callFloat3 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 13);

    assertEquals(1, callFloat3.getInvokes().size());
    assertEquals(fixed_multiply, callFloat3.getInvokes().get(0));

    // Check return values
    assertEquals(intType, callInt2.getType());
    assertEquals(floatType, callFloat3.getType());
  }

  @Test
  void testInvocationWithCallTarget() throws Exception {
    // Check invocation target with specialized function alongside template with same name
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation1.cpp").toFile()),
            topLevel,
            true);

    FunctionDeclaration doubleFixedMultiply =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getName().equals("fixed_multiply") && f.getType().getName().equals("double"));

    CallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getName().equals("fixed_multiply"));

    // Check invocation
    assertEquals(1, call.getInvokes().size());
    assertEquals(doubleFixedMultiply, call.getInvokes().get(0));

    // Check return value
    assertEquals("double", call.getType().getName());
  }

  @Disabled
  @Test
  void testInvocationWithoutCallTarget() throws Exception {
    // Check if a CallExpression is converted to a TemplateCallExpression if a compatible target
    // exists
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation2.cpp").toFile()),
            topLevel,
            true);

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getName().equals("template <class T=int, int N=5> T fixed_multiply (T val)"));

    FunctionDeclaration fixedMultiply =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getName().equals("fixed_multiply") && f.getType().getName().equals("T"));

    // Check realization of template maps to our target function
    assertEquals(1, templateDeclaration.getRealization().size());
    assertEquals(fixedMultiply, templateDeclaration.getRealization().get(0));

    CallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getName().equals("fixed_multiply"));

    // Check invocation target
    assertTrue(call instanceof TemplateCallExpression);
    assertEquals(1, call.getInvokes().size());
    assertEquals(fixedMultiply, call.getInvokes().get(0));

    // Check template parameters
    ObjectType doubleType =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ObjectType.class), t -> t.getName().equals("double"));
    Literal<?> literal5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));

    assertEquals(2, ((TemplateCallExpression) call).getTemplateParameters().size());
    assertEquals(doubleType, ((TemplateCallExpression) call).getTemplateParameters().get(0));
    assertEquals(literal5, ((TemplateCallExpression) call).getTemplateParameters().get(1));

    // Check return value
    assertEquals(doubleType, ((TemplateCallExpression) call).getType());
  }

  @Test
  void testInvocationWithAutoDeduction() throws Exception {
    // Check if a TemplateCallExpression without template parameters performs autodeduction
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation3.cpp").toFile()),
            topLevel,
            true);

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getName().equals("fixed_multiply"));

    FunctionDeclaration fixedMultiply =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getName().equals("fixed_multiply") && f.getType().getName().equals("T"));

    // Check realization of template maps to our target function
    assertEquals(1, templateDeclaration.getRealization().size());
    assertEquals(fixedMultiply, templateDeclaration.getRealization().get(0));

    TemplateCallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, TemplateCallExpression.class),
            c -> c.getName().equals("fixed_multiply"));

    // Check invocation target
    assertEquals(1, call.getInvokes().size());
    assertEquals(fixedMultiply, call.getInvokes().get(0));

    // Check template parameters
    Literal<?> literal5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));

    assertEquals(2, call.getTemplateParameters().size());
    assertEquals("double", call.getTemplateParameters().get(0).getName());
    assertEquals(literal5, call.getTemplateParameters().get(1));

    // Check return value
    assertEquals("double", call.getType().getName());
  }

  @Test
  void testInvocationWithDefaults() throws Exception {
    // test invocation target when no autodeduction is possible, but defaults are provided
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation4.cpp").toFile()),
            topLevel,
            true);

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getName().equals("fixed_multiply"));

    FunctionDeclaration fixedMultiply =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getName().equals("fixed_multiply") && f.getType().getName().equals("T"));

    // Check realization of template maps to our target function
    assertEquals(1, templateDeclaration.getRealization().size());
    assertEquals(fixedMultiply, templateDeclaration.getRealization().get(0));

    TemplateCallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, TemplateCallExpression.class),
            c -> c.getName().equals("fixed_multiply"));

    // Check invocation target
    assertEquals(1, call.getInvokes().size());
    assertEquals(fixedMultiply, call.getInvokes().get(0));

    // Check template parameters
    ObjectType intType =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ObjectType.class), t -> t.getName().equals("int"));
    Literal<?> literal5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));

    assertEquals(2, call.getTemplateParameters().size());
    assertEquals(intType, call.getTemplateParameters().get(0));
    assertEquals(literal5, call.getTemplateParameters().get(1));

    // Check return value
    assertEquals(intType, call.getType());
  }

  @Test
  void testInvocationWithPartialDefaults() throws Exception {
    // test invocation target when no autodeduction is possible, but defaults are partially used
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation5.cpp").toFile()),
            topLevel,
            true);

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getName().equals("fixed_multiply"));

    FunctionDeclaration fixedMultiply =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getName().equals("fixed_multiply") && f.getType().getName().equals("T"));

    // Check realization of template maps to our target function
    assertEquals(1, templateDeclaration.getRealization().size());
    assertEquals(fixedMultiply, templateDeclaration.getRealization().get(0));

    TemplateCallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, TemplateCallExpression.class),
            c -> c.getName().equals("fixed_multiply"));

    // Check invocation target
    assertEquals(1, call.getInvokes().size());
    assertEquals(fixedMultiply, call.getInvokes().get(0));

    // Check template parameters
    ObjectType doubleType =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ObjectType.class), t -> t.getName().equals("double"));
    Literal<?> literal5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));

    assertEquals(2, call.getTemplateParameters().size());
    assertEquals(doubleType, call.getTemplateParameters().get(0));
    assertEquals(literal5, call.getTemplateParameters().get(1));

    // Check return value
    assertEquals(doubleType, call.getType());
  }

  @Test
  void testInvocationWithImplicitCastToOverridenTemplateParameter() throws Exception {
    // test invocation target when template parameter produces a cast in an argument
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation6.cpp").toFile()),
            topLevel,
            true);

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getName().equals("fixed_multiply"));

    FunctionDeclaration fixedMultiply =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getName().equals("fixed_multiply") && f.getType().getName().equals("T"));

    // Check realization of template maps to our target function
    assertEquals(1, templateDeclaration.getRealization().size());
    assertEquals(fixedMultiply, templateDeclaration.getRealization().get(0));

    TemplateCallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, TemplateCallExpression.class),
            c -> c.getName().equals("fixed_multiply"));

    // Check invocation target
    assertEquals(1, call.getInvokes().size());
    assertEquals(fixedMultiply, call.getInvokes().get(0));

    // Check template parameters
    ObjectType intType =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, ObjectType.class), t -> t.getName().equals("int"));
    Literal<?> literal5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));

    assertEquals(2, call.getTemplateParameters().size());
    assertEquals(intType, call.getTemplateParameters().get(0));
    assertEquals(literal5, call.getTemplateParameters().get(1));

    // Check return value
    assertEquals(intType, call.getType());

    // Check cast
    assertEquals(1, call.getArguments().size());
    assertTrue(call.getArguments().get(0) instanceof CastExpression);
    CastExpression arg = (CastExpression) call.getArguments().get(0);
    assertEquals(intType, arg.getCastType());
    assertEquals(20.3, ((Literal) arg.getExpression()).getValue());
  }

  @Disabled
  @Test
  void testInvocationWithImplicitCast() throws Exception {
    // test invocation target when signature does not match but implicitcast can be applied
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation7.cpp").toFile()),
            topLevel,
            true);

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getName().equals("f"));

    FunctionDeclaration f =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            func ->
                func.getName().equals("f") && !templateDeclaration.getRealization().contains(func));

    CallExpression f1 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 9);

    CallExpression f2 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 10);

    CallExpression f3 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 11);

    CallExpression f4 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 12);

    assertEquals(1, f1.getInvokes().size());
    assertEquals(f, f1.getInvokes().get(0));

    assertEquals(1, f2.getInvokes().size());
    assertEquals(templateDeclaration.getRealization().get(0), f2.getInvokes().get(0));

    assertEquals(1, f3.getInvokes().size());
    assertEquals(f, f3.getInvokes().get(0));
    assertEquals(2, f3.getArguments().size());
    assertEquals("int", f3.getArguments().get(0).getType().getName());
    assertEquals("int", f3.getArguments().get(1).getType().getName());
    assertTrue(f3.getArguments().get(1) instanceof CastExpression);
    CastExpression castExpression = (CastExpression) f3.getArguments().get(1);
    assertEquals('b', ((Literal) castExpression.getExpression()).getValue());

    assertEquals(1, f4.getInvokes().size());
    assertTrue(f4.getInvokes().get(0).isImplicit());
  }

  @Disabled
  @Test
  void testCreateDummy() throws Exception {
    // test invocation target when template parameter produces a cast in an argument
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation8.cpp").toFile()),
            topLevel,
            true);

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.isImplicit());

    FunctionDeclaration fixedDivision =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getName().equals("fixed_division") && f.isImplicit());

    assertEquals(1, templateDeclaration.getRealization().size());
    assertEquals(fixedDivision, templateDeclaration.getRealization().get(0));

    assertEquals(2, templateDeclaration.getParameters().size());
    assertTrue(templateDeclaration.getParameters().get(0) instanceof TypeTemplateParamDeclaration);
    assertTrue(
        templateDeclaration.getParameters().get(1) instanceof NonTypeTemplateParamDeclaration);

    assertEquals(1, fixedDivision.getParameters().size());
    assertEquals(
        templateDeclaration.getParameters().get(0), fixedDivision.getParameters().get(0).getType());

    // Check invocation targets

    CallExpression callInt2 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 12);

    assertEquals(1, callInt2.getInvokes().size());
    assertEquals(fixedDivision, callInt2.getInvokes().get(0));

    CallExpression callDouble3 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 13);

    assertEquals(1, callDouble3.getInvokes().size());
    assertEquals(fixedDivision, callDouble3.getInvokes().get(0));

    // Check return values
    assertEquals(UnknownType.getUnknownType(), callInt2.getType());
    assertEquals(UnknownType.getUnknownType(), callDouble3.getType());
  }
}
