/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StaticImportsTest extends BaseTest {

  private final Path topLevel = Path.of("src", "test", "resources", "staticImports");

  @Test
  void testSingleStaticImport() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze("java", topLevel.resolve("single"), true);
    List<MethodDeclaration> methods = TestUtils.subnodesOfType(result, MethodDeclaration.class);
    MethodDeclaration test = TestUtils.findByUniqueName(methods, "test");
    MethodDeclaration main = TestUtils.findByUniqueName(methods, "main");

    CallExpression call = TestUtils.subnodesOfType(main, CallExpression.class).get(0);
    assertEquals(List.of(test), call.getInvokes());

    List<FieldDeclaration> testFields =
        TestUtils.subnodesOfType(result, FieldDeclaration.class).stream()
            .filter(f -> f.getName().equals("test"))
            .collect(Collectors.toList());
    assertEquals(1, testFields.size());
    FieldDeclaration staticField = testFields.get(0);
    assertTrue(staticField.getModifiers().contains("static"));

    List<MemberExpression> memberExpressions =
        TestUtils.subnodesOfType(main, MemberExpression.class);
    MemberExpression usage = TestUtils.findByUniqueName(memberExpressions, "test");
    assertEquals(staticField, usage.getRefersTo());
  }

  @Test
  void testAsteriskImport() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze("java", topLevel.resolve("asterisk"), true);
    List<MethodDeclaration> methods = TestUtils.subnodesOfType(result, MethodDeclaration.class);
    MethodDeclaration main = TestUtils.findByUniqueName(methods, "main");
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);
    RecordDeclaration A = TestUtils.findByUniqueName(records, "A");
    RecordDeclaration B = TestUtils.findByUniqueName(records, "B");

    for (CallExpression call : TestUtils.subnodesOfType(main, CallExpression.class)) {
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
          MethodDeclaration nonStatic = TestUtils.findByUniqueName(B.getMethods(), "nonStatic");
          assertTrue(nonStatic.isImplicit());
          assertEquals(List.of(nonStatic), call.getInvokes());
      }
    }

    List<FieldDeclaration> testFields = TestUtils.subnodesOfType(A, FieldDeclaration.class);
    FieldDeclaration staticField = TestUtils.findByUniqueName(testFields, "staticField");
    FieldDeclaration nonStaticField = TestUtils.findByUniqueName(testFields, "nonStaticField");
    assertTrue(staticField.getModifiers().contains("static"));
    assertFalse(nonStaticField.getModifiers().contains("static"));

    List<MemberExpression> declaredReferences =
        TestUtils.subnodesOfType(main, MemberExpression.class);
    MemberExpression usage = TestUtils.findByUniqueName(declaredReferences, "staticField");
    assertEquals(staticField, usage.getRefersTo());

    MemberExpression nonStatic = TestUtils.findByUniqueName(declaredReferences, "nonStaticField");
    assertNotEquals(nonStaticField, nonStatic.getRefersTo());
    assertTrue(nonStatic.getRefersTo().isImplicit());
  }
}
