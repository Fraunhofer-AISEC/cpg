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
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
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

  @Test
  void testJava() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);
    Type intType = TypeParser.createFrom("int", true);
    Type stringType = TypeParser.createFrom("java.lang.String", true);

    testMethods(records, intType, stringType);
    ensureNoUnknownClassDummies(records);
  }

  @Test
  void testCpp() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);
    Type intType = TypeParser.createFrom("int", true);
    Type stringType = TypeParser.createFrom("char*", true);

    testMethods(records, intType, stringType);

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
  }
}
