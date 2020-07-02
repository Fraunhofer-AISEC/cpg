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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class VariableResolverTest extends BaseTest {

  private final Path topLevel = Path.of("src", "test", "resources", "variables");

  @Test
  void testFields() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<MethodDeclaration> methods = Util.subnodesOfType(result, MethodDeclaration.class);
    List<FieldDeclaration> fields = Util.subnodesOfType(result, FieldDeclaration.class);
    FieldDeclaration field = TestUtils.findByUniqueName(fields, "field");

    MethodDeclaration getField = TestUtils.findByUniqueName(methods, "getField");
    ReturnStatement returnStatement = Util.subnodesOfType(getField, ReturnStatement.class).get(0);
    assertEquals(field, ((MemberExpression) returnStatement.getReturnValue()).getMember());

    MethodDeclaration noShadow = TestUtils.findByUniqueName(methods, "getField");
    returnStatement = Util.subnodesOfType(noShadow, ReturnStatement.class).get(0);
    assertEquals(field, ((MemberExpression) returnStatement.getReturnValue()).getMember());
  }

  @Test
  void testLocalVars() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<MethodDeclaration> methods = Util.subnodesOfType(result, MethodDeclaration.class);
    List<FieldDeclaration> fields = Util.subnodesOfType(result, FieldDeclaration.class);
    FieldDeclaration field = TestUtils.findByUniqueName(fields, "field");

    MethodDeclaration getLocal = TestUtils.findByUniqueName(methods, "getLocal");
    ReturnStatement returnStatement = Util.subnodesOfType(getLocal, ReturnStatement.class).get(0);
    VariableDeclaration local = Util.subnodesOfType(getLocal, VariableDeclaration.class).get(0);
    DeclaredReferenceExpression returnValue =
        (DeclaredReferenceExpression) returnStatement.getReturnValue();
    assertNotEquals(Set.of(field), returnValue.getRefersTo());
    assertEquals(Set.of(local), returnValue.getRefersTo());

    MethodDeclaration getShadow = TestUtils.findByUniqueName(methods, "getShadow");
    returnStatement = Util.subnodesOfType(getShadow, ReturnStatement.class).get(0);
    local = Util.subnodesOfType(getShadow, VariableDeclaration.class).get(0);
    returnValue = (DeclaredReferenceExpression) returnStatement.getReturnValue();
    assertNotEquals(Set.of(field), returnValue.getRefersTo());
    assertEquals(Set.of(local), returnValue.getRefersTo());
  }

  @Test
  void testLocalVarsCpp() throws Exception {
    List<TranslationUnitDeclaration> tu = TestUtils.analyze("cpp", topLevel, true);
    FunctionDeclaration function = tu.get(0).getDeclarationAs(2, FunctionDeclaration.class);

    assertEquals("testExpressionInExpressionList()int", function.getSignature());

    List<VariableDeclaration> locals = function.getBody().getLocals();
    // Expecting x, foo, t
    Set<String> localNames = locals.stream().map(l -> l.getName()).collect(Collectors.toSet());
    assertTrue(localNames.contains("x"));
    assertTrue(localNames.contains("foo"));
    assertTrue(localNames.contains("t"));

    // ... and nothing else
    assertEquals(3, localNames.size());

    // Class "Test" has only one (virtual) field "this"
    RecordDeclaration clazz = tu.get(0).getDeclarationAs(0, RecordDeclaration.class);
    for (FieldDeclaration f : clazz.getFields()) {
      if (f == null) {
        System.out.println("NULL");
        continue;
      }
      System.out.println(f.getName() + " " + f.getInitializer());
    }
    // FIXME Fails. Actually has "this", "a" and "foo"
    assertEquals(1, clazz.getFields().size());
  }
}
