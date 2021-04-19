/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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

package de.fraunhofer.aisec.cpg.enhancements.calls;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CallResolverTest extends BaseTest {

  private static final Path topLevel = Path.of("src", "test", "resources", "calls");

  private void testMethods(List<RecordDeclaration> records, Type intType, Type stringType) {
    RecordDeclaration callsRecord = TestUtils.findByUniqueName(records, "Calls");
    RecordDeclaration externalRecord = TestUtils.findByUniqueName(records, "External");
    RecordDeclaration superClassRecord = TestUtils.findByUniqueName(records, "SuperClass");

    List<MethodDeclaration> innerMethods =
        TestUtils.findByName(
            TestUtils.subnodesOfType(callsRecord, MethodDeclaration.class), "innerTarget");
    List<CallExpression> innerCalls =
        TestUtils.findByName(
            TestUtils.subnodesOfType(callsRecord, CallExpression.class), "innerTarget");
    checkCalls(intType, stringType, innerMethods, innerCalls);

    List<MethodDeclaration> superMethods =
        TestUtils.findByName(
            TestUtils.subnodesOfType(superClassRecord, MethodDeclaration.class), "superTarget");
    // We can't infer that a call to superTarget(int, int, int) is intended to be part of the
    // superclass. It looks like a call to a member of Calls.java, thus we need to add these
    // methods to the lookup
    superMethods.addAll(
        TestUtils.findByName(
            TestUtils.subnodesOfType(callsRecord, MethodDeclaration.class), "superTarget"));
    List<CallExpression> superCalls =
        TestUtils.findByName(
            TestUtils.subnodesOfType(callsRecord, CallExpression.class), "superTarget");
    checkCalls(intType, stringType, superMethods, superCalls);

    List<MethodDeclaration> externalMethods =
        TestUtils.findByName(
            TestUtils.subnodesOfType(externalRecord, MethodDeclaration.class), "externalTarget");
    List<CallExpression> externalCalls =
        TestUtils.findByName(
            TestUtils.subnodesOfType(callsRecord, CallExpression.class), "externalTarget");
    checkCalls(intType, stringType, externalMethods, externalCalls);
  }

  private void ensureNoUnknownClassDummies(List<RecordDeclaration> records) {
    RecordDeclaration callsRecord = TestUtils.findByUniqueName(records, "Calls");
    assertTrue(records.stream().noneMatch(r -> r.getName().equals("Unknown")));
    CallExpression unknownCall =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(callsRecord, CallExpression.class), "unknownTarget");
    assertEquals(List.of(), unknownCall.getInvokes());
  }

  /**
   * Checks that method calls from a function outside a class are correctly resolved to the
   * MethodDeclaration
   *
   * @param result
   */
  private void ensureInvocationOfMethodsInFunction(List<TranslationUnitDeclaration> result) {
    assertEquals(1, result.size());
    TranslationUnitDeclaration tu = result.get(0);

    for (Declaration declaration : tu.getDeclarations()) {
      assertNotEquals("invoke", declaration.getName());
    }
    List<CallExpression> callExpressions = TestUtils.subnodesOfType(result, CallExpression.class);
    CallExpression invoke = TestUtils.findByUniqueName(callExpressions, "invoke");
    assertEquals(1, invoke.getInvokes().size());
    assertTrue(invoke.getInvokes().get(0) instanceof MethodDeclaration);
  }

  private void checkCalls(
      Type intType,
      Type stringType,
      List<? extends FunctionDeclaration> methods,
      List<CallExpression> calls) {
    List<List<Type>> signatures =
        List.of(List.of(), List.of(intType, intType), List.of(intType, stringType));
    for (List<Type> signature : signatures) {
      for (CallExpression call :
          TestUtils.findByPredicate(calls, c -> c.getSignature().equals(signature))) {
        FunctionDeclaration target =
            TestUtils.findByUniquePredicate(methods, m -> m.hasSignature(signature));
        assertEquals(List.of(target), call.getInvokes());
      }
    }

    // Check for dummies
    List<Type> dummySignature = List.of(intType, intType, intType);
    for (CallExpression dummyCall :
        TestUtils.findByPredicate(calls, c -> c.getSignature().equals(dummySignature))) {
      FunctionDeclaration dummyTarget =
          TestUtils.findByUniquePredicate(methods, m -> m.hasSignature(dummySignature));
      assertEquals(List.of(dummyTarget), dummyCall.getInvokes());
      assertTrue(dummyTarget.isImplicit());
    }
  }

  private void testOverriding(List<RecordDeclaration> records) {
    RecordDeclaration callsRecord = TestUtils.findByUniqueName(records, "Calls");
    RecordDeclaration externalRecord = TestUtils.findByUniqueName(records, "External");
    RecordDeclaration superClassRecord = TestUtils.findByUniqueName(records, "SuperClass");

    MethodDeclaration originalMethod =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(superClassRecord, MethodDeclaration.class),
            "overridingTarget");
    MethodDeclaration overridingMethod =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(externalRecord, MethodDeclaration.class), "overridingTarget");
    CallExpression call =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(callsRecord, CallExpression.class), "overridingTarget");

    // TODO related to #204: Currently we have both the original and the overriding method in the
    //  invokes list. This check needs to be adjusted to the choice we make on solving #204
    assertTrue(call.getInvokes().contains(overridingMethod));
    assertEquals(List.of(originalMethod), overridingMethod.getOverrides());
    assertEquals(List.of(overridingMethod), originalMethod.getOverriddenBy());
  }

  @Test
  void testJava() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);
    Type intType = TypeParser.createFrom("int", true);
    Type stringType = TypeParser.createFrom("java.lang.String", true);

    testMethods(records, intType, stringType);
    testOverriding(records);
    ensureNoUnknownClassDummies(records);
  }

  @Test
  void testCpp() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "calls.cpp").toFile()), topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);
    Type intType = TypeParser.createFrom("int", true);
    Type stringType = TypeParser.createFrom("char*", true);

    testMethods(records, intType, stringType);
    testOverriding(records);

    // Test functions (not methods!)
    List<FunctionDeclaration> functions =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, FunctionDeclaration.class),
            f -> f.getName().equals("functionTarget") && !(f instanceof MethodDeclaration));
    List<CallExpression> calls =
        TestUtils.findByName(
            TestUtils.subnodesOfType(result, CallExpression.class), "functionTarget");
    checkCalls(intType, stringType, functions, calls);

    ensureNoUnknownClassDummies(records);
    ensureInvocationOfMethodsInFunction(result);
  }

  @Test
  void testImplicitCastCallResolution() throws Exception {
    Path filePath = Path.of(topLevel.toString(), "implicitcast");
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", filePath, true);
    List<CallExpression> callExpressions = TestUtils.subnodesOfType(result, CallExpression.class);

    // Check resolution of implicit cast
    CallExpression multiply = TestUtils.findByUniqueName(callExpressions, "multiply");

    assertEquals(1, multiply.getInvokes().size());
    FunctionDeclaration functionDeclaration = multiply.getInvokes().get(0);
    assertFalse(functionDeclaration.isImplicit());
    assertEquals("int", functionDeclaration.getSignatureTypes().get(0).getTypeName());

    assertTrue(multiply.getArguments().get(0) instanceof CastExpression);
    CastExpression implicitCast = (CastExpression) multiply.getArguments().get(0);
    assertEquals("int", implicitCast.getCastType().getTypeName());
    assertEquals("10.0", implicitCast.getExpression().getCode());

    // Check implicit cast in case of ambiguous call
    CallExpression ambiguousCall =
        TestUtils.findByUniqueName(callExpressions, "ambiguous_multiply");

    // Check invokes
    List<FunctionDeclaration> functionDeclarations = ambiguousCall.getInvokes();
    assertEquals(2, functionDeclarations.size());
    for (FunctionDeclaration func : functionDeclarations) {
      assertFalse(func.isImplicit());
      assertTrue(
          (func.getParameters().get(0).getType().getName().equals("int"))
              || (func.getParameters().get(0).getType().getName().equals("float")));
    }

    // Check Cast
    assertTrue(ambiguousCall.getArguments().get(0) instanceof CastExpression);
    CastExpression castExpression = (CastExpression) ambiguousCall.getArguments().get(0);
    assertEquals(UnknownType.getUnknownType(), castExpression.getType());
    assertEquals("10.0", castExpression.getExpression().getCode());
  }

  private void testDefaultArgumentsInDeclaration() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(
                Path.of(topLevel.toString(), "defaultargs", "defaultInDeclaration.cpp").toFile()),
            topLevel,
            true);

    List<CallExpression> calls = TestUtils.subnodesOfType(result, CallExpression.class);
    List<FunctionDeclaration> functionDeclarations =
        TestUtils.subnodesOfType(result, FunctionDeclaration.class);
    FunctionDeclaration displayDeclaration =
        TestUtils.findByUniquePredicate(
            functionDeclarations,
            f -> f.getName().equals("display") && !f.isDefinition() && !f.isImplicit());
    FunctionDeclaration displayDefinition =
        TestUtils.findByUniquePredicate(
            functionDeclarations,
            f -> f.getName().equals("display") && f.isDefinition() && !f.isImplicit());

    // Check defines edge
    assertEquals(displayDefinition, displayDeclaration.getDefinition());

    // Check defaults edge of ParamVariableDeclaration
    assertEquals(
        displayDeclaration.getDefaultParameters(), displayDefinition.getDefaultParameters());

    // Check call display(1);
    CallExpression display1 =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("display(1);");
            });

    assertEquals(1, display1.getInvokes().size());
    assertTrue(display1.getInvokes().contains(displayDeclaration));

    assertEquals("1", display1.getArguments().get(0).getCode());
    assertEquals(displayDeclaration.getDefaultParameters().get(1), display1.getArguments().get(1));

    CallExpression display =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("display();");
            });

    assertEquals(2, display.getInvokes().size());
    assertTrue(display.getInvokes().contains(displayDeclaration));
    assertTrue(display.getInvokes().contains(displayDefinition));

    assertEquals(displayDeclaration.getDefaultParameters().get(0), display.getArguments().get(0));
    assertEquals(displayDeclaration.getDefaultParameters().get(1), display.getArguments().get(1));

    CallExpression displayCount$ =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("display(count, '$');");
            });

    assertEquals(2, display.getInvokes().size());
    assertTrue(display.getInvokes().contains(displayDeclaration));
    assertTrue(display.getInvokes().contains(displayDefinition));

    assertEquals("count", displayCount$.getArguments().get(0).getName());
    assertEquals("'$'", displayCount$.getArguments().get(1).getCode());

    CallExpression display10 =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("display(10.0);");
            });

    assertEquals(2, display10.getInvokes().size());
    assertTrue(display.getInvokes().contains(displayDeclaration));
    assertTrue(display.getInvokes().contains(displayDefinition));

    assertTrue(display10.getArguments().get(0) instanceof CastExpression);
    assertEquals(
        "10.0", ((CastExpression) display10.getArguments().get(0)).getExpression().getCode());
    assertEquals("int", ((CastExpression) display10.getArguments().get(0)).getCastType().getName());
    assertEquals(displayDeclaration.getDefaultParameters().get(1), display10.getArguments().get(1));
  }

  private void testDefaultArgumentsInDefinition() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(
                Path.of(topLevel.toString(), "defaultargs", "defaultInDefinition.cpp").toFile()),
            topLevel,
            true);

    List<CallExpression> calls = TestUtils.subnodesOfType(result, CallExpression.class);
    List<FunctionDeclaration> functionDeclarations =
        TestUtils.subnodesOfType(result, FunctionDeclaration.class);
    FunctionDeclaration displayFunction =
        TestUtils.findByUniquePredicate(
            functionDeclarations, f -> f.getName().equals("display") && !f.isImplicit());

    // Check defaults edge of ParamVariableDeclaration
    assertTrue(displayFunction.getDefaultParameters().get(0) instanceof Literal);
    assertTrue(displayFunction.getDefaultParameters().get(1) instanceof Literal);
    assertEquals("*", ((Literal) displayFunction.getDefaultParameters().get(0)).getValueString());
    assertEquals("3", ((Literal) displayFunction.getDefaultParameters().get(1)).getValueString());

    // Check call display();
    CallExpression display =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("display();");
            });
    assertEquals(1, display.getInvokes().size());
    assertEquals(displayFunction, display.getInvokes().get(0));

    assertTrue(display.getArguments().get(0) instanceof Literal);
    assertTrue(display.getArguments().get(1) instanceof Literal);
    assertEquals("*", ((Literal) display.getArguments().get(0)).getValueString());
    assertEquals("3", ((Literal) display.getArguments().get(1)).getValueString());

    // Check call display('#');
    CallExpression displayHash =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("display('#');");
            });

    assertEquals(1, displayHash.getInvokes().size());
    assertEquals(displayFunction, displayHash.getInvokes().get(0));

    assertTrue(displayHash.getArguments().get(0) instanceof Literal);
    assertTrue(displayHash.getArguments().get(1) instanceof Literal);
    assertEquals("#", ((Literal) displayHash.getArguments().get(0)).getValueString());
    assertEquals("3", ((Literal) displayHash.getArguments().get(1)).getValueString());

    // Check call display('#');
    CallExpression display$Count =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("display('$', count);");
            });

    assertEquals(1, display$Count.getInvokes().size());
    assertEquals(displayFunction, display$Count.getInvokes().get(0));

    assertTrue(display$Count.getArguments().get(0) instanceof Literal);
    assertEquals("$", ((Literal) display$Count.getArguments().get(0)).getValueString());
    assertEquals("count", display$Count.getArguments().get(1).getName());
  }

  private void testPartialDefaultArguments() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "defaultargs", "partialDefaults.cpp").toFile()),
            topLevel,
            true);

    List<CallExpression> calls = TestUtils.subnodesOfType(result, CallExpression.class);
    List<FunctionDeclaration> functionDeclarations =
        TestUtils.subnodesOfType(result, FunctionDeclaration.class);
    FunctionDeclaration addFunction =
        TestUtils.findByUniquePredicate(
            functionDeclarations, f -> f.getName().equals("add") && !f.isImplicit());
    FunctionDeclaration addFunctionImplicit =
        TestUtils.findByUniquePredicate(
            functionDeclarations, f -> f.getName().equals("add") && f.isImplicit());

    // Check call add();
    CallExpression add =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("add();");
            });

    assertEquals(1, add.getInvokes().size());
    assertEquals(addFunctionImplicit, add.getInvokes().get(0));

    // Check call add(1, 2);
    CallExpression add12 =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("add(1,2);");
            });

    assertEquals(1, add12.getInvokes().size());
    assertEquals(addFunction, add12.getInvokes().get(0));

    assertEquals(4, add12.getArguments().size());
    assertEquals("1", add12.getArguments().get(0).getCode());
    assertEquals("2", add12.getArguments().get(1).getCode());
    assertEquals(addFunction.getDefaultParameters().get(2), add12.getArguments().get(2));
    assertEquals(addFunction.getDefaultParameters().get(3), add12.getArguments().get(3));

    // Check call add(1, 2, 5, 6);
    CallExpression add1256 =
        TestUtils.findByUniquePredicate(
            calls,
            c -> {
              assert c.getCode() != null;
              return c.getCode().equals("add(1,2,5,6);");
            });

    assertEquals(1, add1256.getInvokes().size());
    assertEquals(addFunction, add1256.getInvokes().get(0));

    assertEquals(4, add1256.getArguments().size());
    assertEquals("1", add1256.getArguments().get(0).getCode());
    assertEquals("2", add1256.getArguments().get(1).getCode());
    assertEquals("5", add1256.getArguments().get(2).getCode());
    assertEquals("6", add1256.getArguments().get(3).getCode());
  }

  @Test
  void testDefaultArgumentsCallResolution() throws Exception {
    testDefaultArgumentsInDeclaration();
    testDefaultArgumentsInDefinition();
    testPartialDefaultArguments();
  }

  @Test
  void testScopedFunctionResolutionUndefined() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "scopedresolution", "undefined.cpp").toFile()),
            topLevel,
            true);

    List<CallExpression> calls = TestUtils.subnodesOfType(result, CallExpression.class);
    assertEquals(1, calls.size());
    List<FunctionDeclaration> functionDeclarations =
        TestUtils.subnodesOfType(result, FunctionDeclaration.class);
    assertEquals(3, functionDeclarations.size());

    assertEquals(1, calls.get(0).getInvokes().size());
    assertTrue(calls.get(0).getInvokes().get(0).isImplicit());
    assertEquals("f", calls.get(0).getInvokes().get(0).getName());
  }

  @Test
  void testScopedFunctionResolutionDefined() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "scopedresolution", "defined.cpp").toFile()),
            topLevel,
            true);
    List<CallExpression> calls = TestUtils.subnodesOfType(result, CallExpression.class);
    assertEquals(1, calls.size());
    List<FunctionDeclaration> functionDeclarations =
        TestUtils.subnodesOfType(result, FunctionDeclaration.class);
    assertEquals(2, functionDeclarations.size());

    assertEquals(1, calls.get(0).getInvokes().size());
    assertFalse(calls.get(0).getInvokes().get(0).isImplicit());
    assertEquals("g", calls.get(0).getInvokes().get(0).getName());
  }

  @Test
  void testScopedFunctionResolutionWithDefaults() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(
                Path.of(topLevel.toString(), "scopedresolution", "scopedResolutionWithDefaults.cpp")
                    .toFile()),
            topLevel,
            true);
    List<CallExpression> calls = TestUtils.subnodesOfType(result, CallExpression.class);

    CallExpression fh =
        TestUtils.findByUniquePredicate(
            calls, c -> c.getLocation().getRegion().getStartLine() == 4);

    assertEquals(1, fh.getInvokes().size());
    assertFalse(fh.getInvokes().get(0).isImplicit());
    assertEquals(2, fh.getInvokes().get(0).getLocation().getRegion().getStartLine());
    assertEquals(2, fh.getArguments().size());
    assertEquals(3, ((Literal) fh.getArguments().get(0)).getValue());
    assertEquals(7, ((Literal) fh.getArguments().get(1)).getValue());
    assertFalse((Boolean) fh.getArgumentsPropertyEdge().get(0).getProperty(Properties.DEFAULT));
    assertTrue((Boolean) fh.getArgumentsPropertyEdge().get(1).getProperty(Properties.DEFAULT));

    CallExpression fm1 =
        TestUtils.findByUniquePredicate(
            calls, c -> c.getLocation().getRegion().getStartLine() == 8);

    assertEquals(1, fm1.getInvokes().size());
    assertTrue(fm1.getInvokes().get(0).isImplicit());
    assertEquals(1, fm1.getArguments().size());
    assertEquals(4, ((Literal) fm1.getArguments().get(0)).getValue());
    assertFalse((Boolean) fm1.getArgumentsPropertyEdge().get(0).getProperty(Properties.DEFAULT));

    CallExpression fm2 =
        TestUtils.findByUniquePredicate(
            calls, c -> c.getLocation().getRegion().getStartLine() == 10);

    assertEquals(1, fm2.getInvokes().size());
    assertFalse(fm2.getInvokes().get(0).isImplicit());
    assertEquals(9, fm2.getInvokes().get(0).getLocation().getRegion().getStartLine());
    assertEquals(2, fm2.getArguments().size());
    assertEquals(4, ((Literal) fm2.getArguments().get(0)).getValue());
    assertEquals(5, ((Literal) fm2.getArguments().get(1)).getValue());
    assertFalse((Boolean) fm2.getArgumentsPropertyEdge().get(0).getProperty(Properties.DEFAULT));
    assertTrue((Boolean) fm2.getArgumentsPropertyEdge().get(1).getProperty(Properties.DEFAULT));

    CallExpression fn =
        TestUtils.findByUniquePredicate(
            calls, c -> c.getLocation().getRegion().getStartLine() == 13);

    assertEquals(1, fn.getInvokes().size());
    assertFalse(fn.getInvokes().get(0).isImplicit());
    assertEquals(2, fn.getInvokes().get(0).getLocation().getRegion().getStartLine());
    assertEquals(2, fn.getArguments().size());
    assertEquals(6, ((Literal) fn.getArguments().get(0)).getValue());
    assertEquals(7, ((Literal) fn.getArguments().get(1)).getValue());
    assertFalse((Boolean) fn.getArgumentsPropertyEdge().get(0).getProperty(Properties.DEFAULT));
    assertTrue((Boolean) fn.getArgumentsPropertyEdge().get(1).getProperty(Properties.DEFAULT));
  }
}
