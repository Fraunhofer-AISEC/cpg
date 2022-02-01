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
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class FunctionTemplateTest extends BaseTest {

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
    BinaryOperator dependentOperation =
        TestUtils.findByUniquePredicate(
            binaryOperators, b -> Objects.equals(b.getCode(), "val * N"));

    assertEquals(UnknownType.getUnknownType(), dependentOperation.getType());
  }

  void testFunctionTemplateArguments(
      CallExpression callFloat3, ObjectType floatType, Literal<Integer> int3) {
    assertEquals(2, callFloat3.getTemplateParameters().size());

    assertEquals(floatType, ((TypeExpression) callFloat3.getTemplateParameters().get(0)).getType());
    assertEquals(
        0, callFloat3.getTemplateParametersPropertyEdge().get(0).getProperty(Properties.INDEX));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        callFloat3
            .getTemplateParametersPropertyEdge()
            .get(0)
            .getProperty(Properties.INSTANTIATION));

    assertEquals(int3, callFloat3.getTemplateParameters().get(1));
    assertEquals(
        1, callFloat3.getTemplateParametersPropertyEdge().get(1).getProperty(Properties.INDEX));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        callFloat3
            .getTemplateParametersPropertyEdge()
            .get(1)
            .getProperty(Properties.INSTANTIATION));
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
    List<TypeParamDeclaration> typeParamDeclarations =
        TestUtils.subnodesOfType(result, TypeParamDeclaration.class);
    assertEquals(1, typeParamDeclarations.size());
    TypeParamDeclaration typeParamDeclaration = typeParamDeclarations.get(0);
    assertEquals(typeParamDeclaration, functionTemplateDeclaration.getParameters().get(0));

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

    assertEquals(T, typeParamDeclaration.getType());
    assertEquals(intType, typeParamDeclaration.getDefault());

    ParamVariableDeclaration N =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, ParamVariableDeclaration.class), "N");
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
    assertTrue(N.getPrevDFG().contains(int5));
    assertTrue(N.getPrevDFG().contains(int3));
    assertTrue(N.getPrevDFG().contains(int2));

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

    // Check template arguments
    testFunctionTemplateArguments(callFloat3, floatType, int3);
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
            t -> t.getName().equals("fixed_multiply"));

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
    assertEquals(1, call.getInvokes().size());
    assertEquals(fixedMultiply, call.getInvokes().get(0));

    // Check template parameters
    ObjectType doubleType =
        new ObjectType(
            "double",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    Literal<?> literal5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));

    assertEquals(2, call.getTemplateParameters().size());
    assertEquals(doubleType, ((TypeExpression) call.getTemplateParameters().get(0)).getType());
    assertEquals(literal5, call.getTemplateParameters().get(1));

    // Check return value
    assertEquals(doubleType, call.getType());
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

    CallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
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

    CallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
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
    assertEquals(intType, ((TypeExpression) call.getTemplateParameters().get(0)).getType());
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

    CallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getName().equals("fixed_multiply"));

    // Check invocation target
    assertEquals(1, call.getInvokes().size());
    assertEquals(fixedMultiply, call.getInvokes().get(0));

    // Check template parameters
    ObjectType doubleType =
        new ObjectType(
            "double",
            Type.Storage.AUTO,
            new Type.Qualifier(),
            new ArrayList<>(),
            ObjectType.Modifier.SIGNED,
            true);
    Literal<?> literal5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));

    assertEquals(2, call.getTemplateParameters().size());
    assertEquals(doubleType, ((TypeExpression) call.getTemplateParameters().get(0)).getType());
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

    CallExpression call =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
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
    assertEquals(intType, ((TypeExpression) call.getTemplateParameters().get(0)).getType());
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
            t -> t.getName().equals("f") && !t.isInferred());

    FunctionDeclaration f =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            func ->
                func.getName().equals("f")
                    && !templateDeclaration.getRealization().contains(func)
                    && !func.isInferred());

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
    assertTrue(f4.getInvokes().get(0).isInferred());
  }

  @Test
  void testFunctionTemplateInMethod() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateMethod.cpp").toFile()),
            topLevel,
            true);

    RecordDeclaration recordDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, RecordDeclaration.class),
            c -> c.getName().equals("MyClass"));

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getName().equals("fixed_multiply") && !t.isImplicit());

    assertEquals(2, templateDeclaration.getParameters().size());

    assertEquals(1, recordDeclaration.getTemplates().size());
    assertTrue(recordDeclaration.getTemplates().contains(templateDeclaration));

    MethodDeclaration methodDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, MethodDeclaration.class),
            m -> !(m.isImplicit()) && m.getName().equals("fixed_multiply"));
    assertEquals(1, templateDeclaration.getRealization().size());
    assertTrue(templateDeclaration.getRealization().contains(methodDeclaration));

    // Test callexpression to invoke the realization
    CallExpression callExpression =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getCode() != null && c.getCode().equals("myObj.fixed_multiply<int>(3);"));
    assertEquals(1, callExpression.getInvokes().size());
    assertEquals(methodDeclaration, callExpression.getInvokes().get(0));

    assertEquals(templateDeclaration, callExpression.getTemplateInstantiation());

    assertEquals(2, callExpression.getTemplateParameters().size());

    assertEquals("int", callExpression.getTemplateParameters().get(0).getName());
    assertEquals(
        TemplateDeclaration.TemplateInitialization.EXPLICIT,
        callExpression
            .getTemplateParametersPropertyEdge()
            .get(0)
            .getProperty(Properties.INSTANTIATION));
    assertEquals(
        0, callExpression.getTemplateParametersPropertyEdge().get(0).getProperty(Properties.INDEX));

    Literal<Integer> int5 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(5));

    assertEquals(int5, callExpression.getTemplateParameters().get(1));
    assertEquals(
        1, callExpression.getTemplateParametersPropertyEdge().get(1).getProperty(Properties.INDEX));
    assertEquals(
        TemplateDeclaration.TemplateInitialization.DEFAULT,
        callExpression
            .getTemplateParametersPropertyEdge()
            .get(1)
            .getProperty(Properties.INSTANTIATION));
  }

  @Test
  void testCreateInferred() throws Exception {
    // test invocation target when template parameter produces a cast in an argument
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "functionTemplateInvocation8.cpp").toFile()),
            topLevel,
            true);

    // Check inferred for first fixed_division call

    FunctionTemplateDeclaration templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getCode().equals("fixed_division<int,2>(10)"));

    FunctionDeclaration fixedDivision =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getCode().equals("fixed_division<int,2>(10)") && f.isInferred());

    assertEquals(1, templateDeclaration.getRealization().size());
    assertEquals(fixedDivision, templateDeclaration.getRealization().get(0));

    assertEquals(2, templateDeclaration.getParameters().size());
    assertTrue(templateDeclaration.getParameters().get(0) instanceof TypeParamDeclaration);
    assertTrue(templateDeclaration.getParameters().get(1) instanceof ParamVariableDeclaration);

    assertEquals(1, fixedDivision.getParameters().size());

    CallExpression callInt2 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 12);

    assertEquals(1, callInt2.getInvokes().size());
    assertEquals(fixedDivision, callInt2.getInvokes().get(0));
    assertTrue(
        callInt2
            .getTemplateParameters()
            .get(1)
            .getNextDFG()
            .contains(templateDeclaration.getParameters().get(1)));

    // Check inferred for second fixed_division call

    templateDeclaration =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionTemplateDeclaration.class),
            t -> t.getCode().equals("fixed_division<double,3>(10.0)"));

    fixedDivision =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getCode().equals("fixed_division<double,3>(10.0)") && f.isInferred());

    assertEquals(1, templateDeclaration.getRealization().size());
    assertEquals(fixedDivision, templateDeclaration.getRealization().get(0));

    assertEquals(2, templateDeclaration.getParameters().size());
    assertTrue(templateDeclaration.getParameters().get(0) instanceof TypeParamDeclaration);
    assertTrue(templateDeclaration.getParameters().get(1) instanceof ParamVariableDeclaration);

    assertEquals(1, fixedDivision.getParameters().size());

    CallExpression callDouble3 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getLocation().getRegion().getStartLine() == 13);

    assertEquals(1, callDouble3.getInvokes().size());
    assertEquals(fixedDivision, callDouble3.getInvokes().get(0));
    assertTrue(
        callDouble3
            .getTemplateParameters()
            .get(1)
            .getNextDFG()
            .contains(templateDeclaration.getParameters().get(1)));

    // Check return values
    assertEquals(UnknownType.getUnknownType(), callInt2.getType());
    assertEquals(UnknownType.getUnknownType(), callDouble3.getType());
  }
}
