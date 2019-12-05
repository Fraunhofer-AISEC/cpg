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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class StaticImportsTest {

  private List<TranslationUnitDeclaration> analyze(String path) throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "staticImports", path);
    File[] files =
        Files.walk(topLevel, Integer.MAX_VALUE)
            .map(Path::toFile)
            .filter(File::isFile)
            .filter(f -> f.getName().endsWith(".java"))
            .sorted()
            .toArray(File[]::new);

    TranslationConfiguration config =
        TranslationConfiguration.builder()
            .sourceFiles(files)
            .topLevel(topLevel.toFile())
            .defaultPasses()
            .debugParser(true)
            .failOnError(true)
            .build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();

    return analyzer.analyze().get().getTranslationUnits();
  }

  @Test
  void testSingleStaticImport() throws Exception {
    List<TranslationUnitDeclaration> result = analyze("single");
    List<MethodDeclaration> methods = Util.subnodesOfType(result, MethodDeclaration.class);
    MethodDeclaration test = TestUtils.findByName(methods, "test");
    MethodDeclaration main = TestUtils.findByName(methods, "main");

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
    MemberExpression usage = TestUtils.findByName(memberExpressions, "A.test");
    assertEquals(staticField, usage.getMember());
  }

  @Test
  void testAsteriskImport() throws Exception {
    List<TranslationUnitDeclaration> result = analyze("asterisk");
    List<MethodDeclaration> methods = Util.subnodesOfType(result, MethodDeclaration.class);
    MethodDeclaration main = TestUtils.findByName(methods, "main");
    for (CallExpression call : Util.subnodesOfType(main, CallExpression.class)) {
      switch (call.getName()) {
        case "a":
          assertEquals(List.of(TestUtils.findByName(methods, "a")), call.getInvokes());
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
    RecordDeclaration A = TestUtils.findByName(records, "A");
    List<FieldDeclaration> testFields = Util.subnodesOfType(A, FieldDeclaration.class);
    FieldDeclaration staticField = TestUtils.findByName(testFields, "staticField");
    FieldDeclaration nonStaticField = TestUtils.findByName(testFields, "nonStaticField");
    assertTrue(staticField.getModifiers().contains("static"));
    assertFalse(nonStaticField.getModifiers().contains("static"));

    List<MemberExpression> declaredReferences = Util.subnodesOfType(main, MemberExpression.class);
    MemberExpression usage = TestUtils.findByName(declaredReferences, "A.staticField");
    assertEquals(usage.getMember(), staticField);

    MemberExpression nonStatic = TestUtils.findByName(declaredReferences, "this.nonStaticField");
    assertNotEquals(nonStatic.getMember(), nonStaticField);
    assertTrue(nonStatic.getMember().isDummy());
  }

  @Test
  void testDummyGeneration() throws Exception {
    List<TranslationUnitDeclaration> result = analyze("dummies");
    List<RecordDeclaration> records = Util.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration dummyRecord = TestUtils.findByName(records, "a.b.c.SomeClass");
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

    RecordDeclaration mainRecord = TestUtils.findByName(records, "GenerateDummies");
    MethodDeclaration main = TestUtils.findByName(mainRecord.getMethods(), "main");
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
