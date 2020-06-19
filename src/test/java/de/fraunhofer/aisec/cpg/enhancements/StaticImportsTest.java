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

package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StaticImportsTest {

  /**
   * {@link TypeParser} and {@link TypeManager} hold static state. This needs to be cleared before
   * all tests in order to avoid strange errors
   */
  @BeforeEach
  void resetPersistentState() {
    TypeParser.reset();
    TypeManager.reset();
  }

  private Path topLevel = Path.of("src", "test", "resources", "staticImports");

  @Test
  void testSingleStaticImport() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel.resolve("single"));
    List<MethodDeclaration> methods = Util.subnodesOfType(result, MethodDeclaration.class);
    MethodDeclaration test = TestUtils.findByUniqueName(methods, "test");
    MethodDeclaration main = TestUtils.findByUniqueName(methods, "main");

    CallExpression call = Util.subnodesOfType(main, CallExpression.class).get(0);
    assertEquals(List.of(test), call.getInvokes());

    List<FieldDeclaration> testFields =
        Util.subnodesOfType(result, FieldDeclaration.class).stream()
            .filter(f -> f.getName().equals("test"))
            .collect(Collectors.toList());
    assertEquals(1, testFields.size());
    FieldDeclaration staticField = testFields.get(0);
    assertTrue(staticField.getModifiers().contains("static"));

    List<MemberExpression> memberExpressions = Util.subnodesOfType(main, MemberExpression.class);
    MemberExpression usage = TestUtils.findByUniqueName(memberExpressions, "A.test");
    assertEquals(staticField, usage.getMember());
  }

  @Test
  void testAsteriskImport() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze("java", topLevel.resolve("asterisk"));
    List<MethodDeclaration> methods = Util.subnodesOfType(result, MethodDeclaration.class);
    MethodDeclaration main = TestUtils.findByUniqueName(methods, "main");
    for (CallExpression call : Util.subnodesOfType(main, CallExpression.class)) {
      switch (call.getName()) {
        case "a":
          assertEquals(List.of(TestUtils.findByUniqueName(methods, "a")), call.getInvokes());
          assertTrue(((MethodDeclaration) call.getInvokes().get(0)).isStatic());
          break;
        case "b":
          List<MethodDeclaration> bs =
              methods.stream()
                  .filter(m -> m.getName().equals("b") && m.isStatic())
                  .collect(Collectors.toList());
          assertEquals(
              call.getInvokes(),
              bs.stream()
                  .filter(b -> b.hasSignature(call.getSignature()))
                  .collect(Collectors.toList()));
          break;
        case "nonStatic":
          assertTrue(call.getInvokes().isEmpty());
      }
    }

    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);
    RecordDeclaration A = TestUtils.findByUniqueName(records, "A");
    List<FieldDeclaration> testFields = Util.subnodesOfType(A, FieldDeclaration.class);
    FieldDeclaration staticField = TestUtils.findByUniqueName(testFields, "staticField");
    FieldDeclaration nonStaticField = TestUtils.findByUniqueName(testFields, "nonStaticField");
    assertTrue(staticField.getModifiers().contains("static"));
    assertFalse(nonStaticField.getModifiers().contains("static"));

    List<MemberExpression> declaredReferences = Util.subnodesOfType(main, MemberExpression.class);
    MemberExpression usage = TestUtils.findByUniqueName(declaredReferences, "A.staticField");
    assertEquals(usage.getMember(), staticField);

    MemberExpression nonStatic =
        TestUtils.findByUniqueName(declaredReferences, "this.nonStaticField");
    assertNotEquals(nonStatic.getMember(), nonStaticField);
    assertTrue(nonStatic.getMember().isImplicit());
  }

  @Test
  void testDummyGeneration() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze("java", topLevel.resolve("dummies"));
    assertEquals(
        1, result.stream().filter(t -> t.getName().equals("unknown declarations")).count());
    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration dummyRecord = TestUtils.findByUniqueName(records, "a.b.c.SomeClass");
    assertEquals(
        1, dummyRecord.getFields().stream().filter(f -> f.getName().equals("someMethod")).count());
    List<MethodDeclaration> dummyMethods =
        dummyRecord.getMethods().stream()
            .filter(m -> m.getName().equals("someMethod"))
            .collect(Collectors.toList());
    Map<Integer, List<MethodDeclaration>> dummiesByNumberOfParams =
        dummyMethods.stream().collect(Collectors.groupingBy(m -> m.getParameters().size()));

    assertTrue(dummiesByNumberOfParams.containsKey(1));
    assertEquals(1, dummiesByNumberOfParams.get(1).size());
    MethodDeclaration dummy1 = dummiesByNumberOfParams.get(1).get(0);

    assertTrue(dummiesByNumberOfParams.containsKey(4));
    assertEquals(1, dummiesByNumberOfParams.get(4).size());
    MethodDeclaration dummy4 = dummiesByNumberOfParams.get(4).get(0);

    RecordDeclaration mainRecord = TestUtils.findByUniqueName(records, "GenerateDummies");
    MethodDeclaration main = TestUtils.findByUniqueName(mainRecord.getMethods(), "main");
    for (CallExpression call : Util.subnodesOfType(main, CallExpression.class)) {
      switch (call.getSignature().size()) {
        case 1:
          assertEquals(call.getInvokes(), List.of(dummy1));
          break;
        case 4:
          assertEquals(call.getInvokes(), List.of(dummy4));
          break;
      }
    }
  }
}
