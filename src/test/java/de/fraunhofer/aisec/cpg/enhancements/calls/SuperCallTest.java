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
package de.fraunhofer.aisec.cpg.enhancements.calls;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SuperCallTest extends BaseTest {

  private final Path topLevel = Path.of("src", "test", "resources", "superCalls");

  @Test
  void testSimpleCall() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration superClass = TestUtils.findByUniqueName(records, "SuperClass");
    List<MethodDeclaration> superMethods =
        TestUtils.subnodesOfType(superClass, MethodDeclaration.class);
    MethodDeclaration superTarget = TestUtils.findByUniqueName(superMethods, "target");

    RecordDeclaration subClass = TestUtils.findByUniqueName(records, "SubClass");
    List<MethodDeclaration> methods = TestUtils.subnodesOfType(subClass, MethodDeclaration.class);
    MethodDeclaration target = TestUtils.findByUniqueName(methods, "target");
    List<CallExpression> calls = TestUtils.subnodesOfType(target, CallExpression.class);
    CallExpression superCall =
        TestUtils.findByUniquePredicate(calls, c -> "super.target();".equals(c.getCode()));

    assertEquals(List.of(superTarget), superCall.getInvokes());
  }

  @Test
  void testInterfaceCall() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration interface1 = TestUtils.findByUniqueName(records, "Interface1");
    List<MethodDeclaration> interface1Methods =
        TestUtils.subnodesOfType(interface1, MethodDeclaration.class);
    MethodDeclaration interface1Target = TestUtils.findByUniqueName(interface1Methods, "target");

    RecordDeclaration interface2 = TestUtils.findByUniqueName(records, "Interface2");
    List<MethodDeclaration> interface2Methods =
        TestUtils.subnodesOfType(interface2, MethodDeclaration.class);
    MethodDeclaration interface2Target = TestUtils.findByUniqueName(interface2Methods, "target");

    RecordDeclaration subClass = TestUtils.findByUniqueName(records, "SubClass");
    List<MethodDeclaration> methods = TestUtils.subnodesOfType(subClass, MethodDeclaration.class);
    MethodDeclaration target = TestUtils.findByUniqueName(methods, "target");
    List<CallExpression> calls = TestUtils.subnodesOfType(target, CallExpression.class);
    CallExpression interface1Call =
        TestUtils.findByUniquePredicate(
            calls, c -> "Interface1.super.target();".equals(c.getCode()));
    CallExpression interface2Call =
        TestUtils.findByUniquePredicate(
            calls, c -> "Interface2.super.target();".equals(c.getCode()));

    assertEquals(List.of(interface1Target), interface1Call.getInvokes());
    assertEquals(List.of(interface2Target), interface2Call.getInvokes());
  }

  @Test
  void testSuperField() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration superClass = TestUtils.findByUniqueName(records, "SuperClass");
    FieldDeclaration superField = TestUtils.findByUniqueName(superClass.getFields(), "field");

    RecordDeclaration subClass = TestUtils.findByUniqueName(records, "SubClass");
    List<MethodDeclaration> methods = TestUtils.subnodesOfType(subClass, MethodDeclaration.class);
    FieldDeclaration field = TestUtils.findByUniqueName(subClass.getFields(), "field");

    MethodDeclaration getField = TestUtils.findByUniqueName(methods, "getField");
    List<MemberExpression> refs = TestUtils.subnodesOfType(getField, MemberExpression.class);
    MemberExpression fieldRef =
        TestUtils.findByUniquePredicate(refs, r -> "field".equals(r.getCode()));

    MethodDeclaration getSuperField = TestUtils.findByUniqueName(methods, "getSuperField");
    refs = TestUtils.subnodesOfType(getSuperField, MemberExpression.class);
    MemberExpression superFieldRef =
        TestUtils.findByUniquePredicate(refs, r -> "super.field".equals(r.getCode()));

    assertTrue(fieldRef.getBase() instanceof DeclaredReferenceExpression);
    assertEquals(
        subClass.getThis(), ((DeclaredReferenceExpression) fieldRef.getBase()).getRefersTo());
    assertEquals(field, fieldRef.getRefersTo());

    assertTrue(superFieldRef.getBase() instanceof DeclaredReferenceExpression);
    assertEquals(
        superClass.getThis(),
        ((DeclaredReferenceExpression) superFieldRef.getBase()).getRefersTo());
    assertEquals(superField, superFieldRef.getRefersTo());
  }

  @Test
  void testInnerCall() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration superClass = TestUtils.findByUniqueName(records, "SuperClass");
    List<MethodDeclaration> superMethods =
        TestUtils.subnodesOfType(superClass, MethodDeclaration.class);
    MethodDeclaration superTarget = TestUtils.findByUniqueName(superMethods, "target");

    RecordDeclaration innerClass = TestUtils.findByUniqueName(records, "SubClass.Inner");
    List<MethodDeclaration> methods = TestUtils.subnodesOfType(innerClass, MethodDeclaration.class);
    MethodDeclaration target = TestUtils.findByUniqueName(methods, "inner");
    List<CallExpression> calls = TestUtils.subnodesOfType(target, CallExpression.class);
    CallExpression superCall =
        TestUtils.findByUniquePredicate(calls, c -> "SubClass.super.target();".equals(c.getCode()));

    assertEquals(List.of(superTarget), superCall.getInvokes());
  }

  @Test
  void testNoExcessFields() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<RecordDeclaration> records = TestUtils.subnodesOfType(result, RecordDeclaration.class);

    RecordDeclaration superClass = TestUtils.findByUniqueName(records, "SuperClass");
    assertEquals(2, superClass.getFields().size());
    assertEquals(
        Set.of("this", "field"),
        superClass.getFields().stream().map(Node::getName).collect(Collectors.toSet()));

    RecordDeclaration subClass = TestUtils.findByUniqueName(records, "SubClass");
    assertEquals(2, subClass.getFields().size());
    assertEquals(
        Set.of("this", "field"),
        subClass.getFields().stream().map(Node::getName).collect(Collectors.toSet()));

    RecordDeclaration inner = TestUtils.findByUniqueName(records, "SubClass.Inner");
    assertEquals(1, inner.getFields().size());
    assertEquals(
        Set.of("this"), inner.getFields().stream().map(Node::getName).collect(Collectors.toSet()));
  }
}
