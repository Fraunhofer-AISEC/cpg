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
package de.fraunhofer.aisec.cpg.enhancements.variable_resolution;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.CatchClause;
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement;
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VariableResolverCppTest extends BaseTest {

  private RecordDeclaration externalClass;
  private FieldDeclaration externVarName;
  private FieldDeclaration externStaticVarName;

  private RecordDeclaration outerClass;
  private FieldDeclaration outerVarName;
  private FieldDeclaration outerStaticVarName;
  private FieldDeclaration outerImpThis;

  private RecordDeclaration innerClass;
  private FieldDeclaration innerVarName;
  private FieldDeclaration innerStaticVarName;
  private FieldDeclaration innerImpThis;
  private FieldDeclaration innerImpOuter;

  private FunctionDeclaration main;

  private MethodDeclaration outerFunction1;
  private List<ForStatement> forStatements;
  private MethodDeclaration outerFunction2;
  private MethodDeclaration outerFunction3;
  private MethodDeclaration outerFunction4;
  private MethodDeclaration outerFunction5;

  private MethodDeclaration innerFunction1;
  private MethodDeclaration innerFunction2;

  private Map<String, Expression> callParamMap = new HashMap<>();

  @BeforeAll
  void initTests() throws ExecutionException, InterruptedException {
    final String topLevelPath = "src/test/resources/variables_extended/cpp/";
    List<String> fileNames = Arrays.asList("scope_variables.cpp", "external_class.cpp");
    List<File> fileLocations =
        fileNames.stream()
            .map(fileName -> new File(topLevelPath + fileName))
            .collect(Collectors.toList());
    TranslationConfiguration config =
        TranslationConfiguration.builder()
            .sourceLocations(fileLocations.toArray(new File[fileNames.size()]))
            .topLevel(new File(topLevelPath))
            .defaultPasses()
            .debugParser(true)
            .defaultLanguages()
            .failOnError(true)
            .loadIncludes(true)
            .build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();
    List<TranslationUnitDeclaration> tu = analyzer.analyze().get().getTranslationUnits();

    List<Node> nodes =
        tu.stream()
            .flatMap(tUnit -> SubgraphWalker.flattenAST(tUnit).stream())
            .collect(Collectors.toList());
    List<CallExpression> calls =
        TestUtils.findByName(Util.filterCast(nodes, CallExpression.class), "printLog");

    calls.sort(new NodeComparator());

    List<RecordDeclaration> records = Util.filterCast(nodes, RecordDeclaration.class);

    // Extract all Variable declarations and field declarations for matching
    externalClass = TestUtils.getOfTypeWithName(nodes, RecordDeclaration.class, "ExternalClass");
    externVarName =
        TestUtils.getSubnodeOfTypeWithName(externalClass, FieldDeclaration.class, "varName");
    externStaticVarName =
        TestUtils.getSubnodeOfTypeWithName(externalClass, FieldDeclaration.class, "staticVarName");
    outerClass = TestUtils.getOfTypeWithName(nodes, RecordDeclaration.class, "ScopeVariables");
    outerVarName =
        outerClass.getFields().stream()
            .filter(n -> n.getName().equals("varName"))
            .findFirst()
            .get();
    outerStaticVarName =
        outerClass.getFields().stream()
            .filter(n -> n.getName().equals("staticVarName"))
            .findFirst()
            .get();
    outerImpThis =
        outerClass.getFields().stream().filter(n -> n.getName().equals("this")).findFirst().get();

    List<RecordDeclaration> classes = Util.filterCast(nodes, RecordDeclaration.class);

    // Inner class and its fields
    innerClass =
        TestUtils.getOfTypeWithName(nodes, RecordDeclaration.class, "ScopeVariables::InnerClass");
    innerVarName =
        innerClass.getFields().stream()
            .filter(n -> n.getName().equals("varName"))
            .findFirst()
            .get();
    innerStaticVarName =
        innerClass.getFields().stream()
            .filter(n -> n.getName().equals("staticVarName"))
            .findFirst()
            .get();
    innerImpThis =
        innerClass.getFields().stream().filter(n -> n.getName().equals("this")).findFirst().get();

    main = TestUtils.getOfTypeWithName(nodes, FunctionDeclaration.class, "main");

    // Functions in the outer and inner object
    outerFunction1 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function1"))
            .collect(Collectors.toList())
            .get(0);
    forStatements = Util.filterCast(SubgraphWalker.flattenAST(outerFunction1), ForStatement.class);
    outerFunction2 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function2"))
            .collect(Collectors.toList())
            .get(0);
    outerFunction3 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function3"))
            .collect(Collectors.toList())
            .get(0);
    outerFunction4 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function4"))
            .collect(Collectors.toList())
            .get(0);
    outerFunction5 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function5"))
            .collect(Collectors.toList())
            .get(0);
    innerFunction1 =
        innerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function1"))
            .collect(Collectors.toList())
            .get(0);
    innerFunction2 =
        innerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function2"))
            .collect(Collectors.toList())
            .get(0);

    for (CallExpression call : calls) {
      Expression first = call.getArguments().get(0);
      String logId = ((Literal) first).getValue().toString();

      Expression second = call.getArguments().get(1);
      callParamMap.put(logId, second);
    }
  }

  DeclaredReferenceExpression getCallWithReference(String literal) {
    Expression exp = callParamMap.get(literal);
    if (exp instanceof DeclaredReferenceExpression) return (DeclaredReferenceExpression) exp;
    return null;
  }

  MemberExpression getCallWithMemberExpression(String literal) {
    Expression exp = callParamMap.get(literal);
    if (exp instanceof MemberExpression) return (MemberExpression) exp;
    return null;
  }

  @Test
  void testOuterVarNameAccessedImplicitThis() {
    VRUtil.assertUsageOf(callParamMap.get("func1_impl_this_varName"), outerVarName);
  }

  @Test
  void testStaticFieldAccessedImplicitly() {
    VRUtil.assertUsageOf(callParamMap.get("func1_static_staticVarName"), outerStaticVarName);
  }

  @Test
  void testVarNameOfFirstLoopAccessed() {
    DeclaredReferenceExpression asReference = getCallWithReference("func1_first_loop_varName");
    assertNotNull(asReference);
    VariableDeclaration vDeclaration =
        TestUtils.getSubnodeOfTypeWithName(
            forStatements.get(0), VariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(callParamMap.get("func1_first_loop_varName"), vDeclaration);
  }

  @Test
  void testAccessLocalVarNameInNestedBlock() {
    CompoundStatement innerBlock =
        TestUtils.getSubnodeOfTypeWithName(forStatements.get(1), CompoundStatement.class, "");
    VariableDeclaration nestedDeclaration =
        TestUtils.getSubnodeOfTypeWithName(innerBlock, VariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(
        callParamMap.get("func1_nested_block_shadowed_local_varName"), nestedDeclaration);
  }

  @Test
  void testVarNameOfSecondLoopAccessed() {
    VariableDeclaration vDeclaration =
        TestUtils.getSubnodeOfTypeWithName(
            forStatements.get(1), VariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(callParamMap.get("func1_second_loop_varName"), vDeclaration);
  }

  @Test
  void testParamVarNameAccessed() {
    ParamVariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(
            outerFunction2, ParamVariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(callParamMap.get("func2_param_varName"), declaration);
  }

  @Test
  void testMemberVarNameOverExplicitThis() {
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_this_varName"), outerImpThis, outerVarName);
  }

  @Test
  void testVarNameDeclaredInIfClause() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(
            TestUtils.getSubnodeOfTypeWithName(outerFunction2, IfStatement.class, Node.EMPTY_NAME),
            VariableDeclaration.class,
            "varName");
    VRUtil.assertUsageOf(callParamMap.get("func2_if_varName"), declaration);
  }

  @Test
  void testVarNameCoughtAsException() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(
            TestUtils.getSubnodeOfTypeWithName(outerFunction2, CatchClause.class, Node.EMPTY_NAME),
            VariableDeclaration.class,
            "varName");
    VRUtil.assertUsageOf(callParamMap.get("func2_catch_varName"), declaration);
  }

  @Test
  void testMemberAccessedOverInstance() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(
            outerFunction2, VariableDeclaration.class, "scopeVariables");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_instance_varName"), declaration, outerVarName);
  }

  @Test
  void testMemberAccessedOverInstanceAfterParamDeclaration() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(
            outerFunction3, VariableDeclaration.class, "scopeVariables");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func3_instance_varName"), declaration, outerVarName);
  }

  @Test
  void testAccessExternalClassMemberVarnameOverInstance() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(
            outerFunction3, VariableDeclaration.class, "externalClass");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func3_external_instance_varName"), declaration, externVarName);
  }

  @Test
  void testExplicitlyReferenceStaticMemberInInternalClass() {
    VRUtil.assertUsageOf(
        callParamMap.get("func4_static_staticVarName"), outerStaticVarName.getDefinition());
  }

  @Test
  void testExplicitlyReferenceStaticMemberInExternalClass() {
    VRUtil.assertUsageOf(
        callParamMap.get("func4_external_staticVarName"), externStaticVarName.getDefinition());
  }

  @Test
  void testAccessExternalMemberOverInstance() {
    VariableDeclaration externalInstance =
        TestUtils.getSubnodeOfTypeWithName(
            outerFunction4, VariableDeclaration.class, "externalClass");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func4_external_instance_varName"), externalInstance, externVarName);
  }

  @Test
  void testAccessExternalStaticMemberAfterInstanceCreation() {
    VRUtil.assertUsageOf(
        callParamMap.get("func4_second_external_staticVarName"),
        externStaticVarName.getDefinition());
  }

  @Test
  void testAccessStaticMemberThroughInstanceFirst() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(outerFunction5, VariableDeclaration.class, "first");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func5_staticVarName_throughInstance_first"),
        declaration,
        outerStaticVarName.getDefinition());
  }

  @Test
  void testAccessStaticMemberThroughInstanceSecond() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(outerFunction5, VariableDeclaration.class, "second");

    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func5_staticVarName_throughInstance_second"),
        declaration,
        outerStaticVarName.getDefinition());
  }

  @Test
  void testImplicitThisAccessOfInnerClassMember() {
    VRUtil.assertUsageOf(callParamMap.get("func1_inner_imp_this_varName"), innerVarName);
  }

  @Test
  void testAccessOfInnerClassMemberOverInstance() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(innerFunction1, VariableDeclaration.class, "inner");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func1_inner_instance_varName"), declaration, innerVarName);
  }

  @Test
  void testAccessOfOuterMemberOverInstance() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(
            innerFunction1, VariableDeclaration.class, "scopeVariables");

    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func1_outer_instance_varName"), declaration, outerVarName);
  }

  @Test
  void testAccessOfOuterStaticMember() {
    VRUtil.assertUsageOf(
        callParamMap.get("func1_outer_static_staticVarName"), outerStaticVarName.getDefinition());
  }

  @Test
  void testAccessOfInnerStaticMember() {
    VRUtil.assertUsageOf(
        callParamMap.get("func1_inner_static_staticVarName"), innerStaticVarName.getDefinition());
  }

  @Test
  void testAccessOfInnerClassMemberOverInstanceWithSameNamedVariable() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(innerFunction2, VariableDeclaration.class, "inner");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_inner_instance_varName_with_shadows"), declaration, innerVarName);
  }

  @Test
  void testAccessOfOuterMemberOverInstanceWithSameNamedVariable() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(
            innerFunction2, VariableDeclaration.class, "scopeVariables");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_outer_instance_varName_with_shadows"), declaration, outerVarName);
  }

  @Test
  void testAccessOfOuterStaticMembertWithSameNamedVariable() {
    VRUtil.assertUsageOf(
        callParamMap.get("func2_outer_static_staticVarName_with_shadows"),
        outerStaticVarName.getDefinition());
  }

  @Test
  void testAccessOfInnerStaticMemberWithSameNamedVariable() {
    VRUtil.assertUsageOf(
        callParamMap.get("func2_inner_static_staticVarName_with_shadows"),
        innerStaticVarName.getDefinition());
  }

  @Test
  void testLocalVariableUsedAsParameter() {
    VariableDeclaration declaration =
        TestUtils.getSubnodeOfTypeWithName(main, VariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(callParamMap.get("main_local_varName"), declaration);
  }
}
