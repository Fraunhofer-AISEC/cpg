package de.fraunhofer.aisec.cpg.enhancements.variable_resolution;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.CatchClause;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.ForStatement;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

// Todo VariableResolverPass 23 Failed, 5 Passed

@Disabled(
    "Until changing variable resolution to ScopeManager. Then in detail disable the tests that need specific fixes")
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
    final String topLevelPath = "src/test/resources/variables_extended/";
    List<String> fileNames =
        Arrays.asList("scope_variables.cpp", "external_class.cpp", "external_class.h");
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
            .failOnError(true)
            .build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();
    List<TranslationUnitDeclaration> tu = analyzer.analyze().get().getTranslationUnits();

    List<Node> nodes =
        tu.stream()
            .flatMap(tUnit -> SubgraphWalker.flattenAST(tUnit).stream())
            .collect(Collectors.toList());
    List<CallExpression> calls =
        Util.filterCast(nodes, CallExpression.class).stream()
            .filter(call -> call.getName().equals("printLog"))
            .collect(Collectors.toList());
    calls.sort(new NodeComparator());

    List<RecordDeclaration> records = Util.filterCast(nodes, RecordDeclaration.class);

    // Extract all Variable declarations and field declarations for matching
    externalClass = Util.getOfTypeWithName(nodes, RecordDeclaration.class, "ExternalClass");
    externVarName = Util.getSubnodeOfTypeWithName(externalClass, FieldDeclaration.class, "varName");
    externStaticVarName =
        Util.getSubnodeOfTypeWithName(externalClass, FieldDeclaration.class, "staticVarName");
    outerClass = Util.getOfTypeWithName(nodes, RecordDeclaration.class, "ScopeVariables");
    outerVarName = Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "varName");
    outerStaticVarName =
        Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "staticVarName");
    outerImpThis = Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "this");

    List<RecordDeclaration> classes = Util.filterCast(nodes, RecordDeclaration.class);

    // Inner class and its fields
    innerClass =
        Util.getOfTypeWithName(nodes, RecordDeclaration.class, "ScopeVariables::InnerClass");
    innerVarName = Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "varName");
    innerStaticVarName =
        Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "staticVarName");
    innerImpThis = Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "this");

    main = Util.getOfTypeWithName(nodes, FunctionDeclaration.class, "main");

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
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_first_loop_varName"); // first_loop_local
    assertNotNull(asReference);
    VariableDeclaration vDeclaration =
        Util.getSubnodeOfTypeWithName(forStatements.get(0), VariableDeclaration.class, "varName");
    // Todo Points to the second loop varName local
    VRUtil.assertUsageOf(callParamMap.get("func1_first_loop_varName"), vDeclaration);
  }

  @Test
  void testAccessLocalVarNameInNestedBlock() {
    CompoundStatement innerBlock =
        Util.getSubnodeOfTypeWithName(forStatements.get(1), CompoundStatement.class, "");
    VariableDeclaration nestedDeclaration =
        Util.getSubnodeOfTypeWithName(innerBlock, VariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(
        callParamMap.get("func1_nested_block_shadowed_local_varName"), nestedDeclaration);
  }

  @Test
  void testVarNameOfSecondLoopAccessed() {
    VariableDeclaration vDeclaration =
        Util.getSubnodeOfTypeWithName(forStatements.get(1), VariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(callParamMap.get("func1_second_loop_varName"), vDeclaration);
  }

  @Test
  void testParamVarNameAccessed() {
    ParamVariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(outerFunction2, ParamVariableDeclaration.class, "varName");
    // Todo Points to a variable that is declared in a catch clause below
    VRUtil.assertUsageOf(callParamMap.get("func2_param_varName"), declaration);
  }

  @Test
  void testMemberVarNameOverExplicitThis() {
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_this_varName"), outerImpThis, outerVarName); // instance_field
  }

  @Test
  void testVarNameDeclaredInIfClause() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(
            Util.getSubnodeOfTypeWithName(outerFunction2, IfStatement.class, Node.EMPTY_NAME),
            VariableDeclaration.class,
            "varName");
    // Todo Refers to the variable declare in the catch clause instead of the variable declared in
    // the if-condition
    VRUtil.assertUsageOf(callParamMap.get("func2_if_varName"), declaration);
  }

  @Test
  void testVarNameCoughtAsException() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(
            Util.getSubnodeOfTypeWithName(outerFunction2, CatchClause.class, Node.EMPTY_NAME),
            VariableDeclaration.class,
            "varName");
    VRUtil.assertUsageOf(callParamMap.get("func2_catch_varName"), declaration);
  }

  @Test
  void testMemberAccessedOverInstance() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(outerFunction2, VariableDeclaration.class, "scopeVariables");
    // Todo Points to variable instantiated in if condition
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_instance_varName"), declaration, outerVarName); // instance_field
  }

  @Test
  void testMemberAccessedOverInstanceAfterParamDeclaration() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(outerFunction3, VariableDeclaration.class, "scopeVariables");
    // Todo Points to the parameter declaration of the same name in the same functionf
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func3_instance_varName"), declaration, outerVarName); // instance_field
  }

  @Test
  void testAccessExternalClassMemberVarnameOverInstance() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(outerFunction3, VariableDeclaration.class, "externalClass");
    // Todo Points to the Parameter in the same function instead of the field of the external
    // variable
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func3_external_instance_varName"),
        declaration,
        externVarName); // external_instance_field
  }

  @Test
  void testExplicitlyReferenceStaticMemberInInternalClass() {
    VRUtil.assertUsageOf(callParamMap.get("func4_static_staticVarName"), outerStaticVarName);
    // Todo refers to the definition instead of the declaration
  }

  @Test
  void testExplicitlyReferenceStaticMemberInExternalClass() {
    // Todo Point to the redeclaration that actually is a definition.
    VRUtil.assertUsageOf(callParamMap.get("func4_external_staticVarName"), externStaticVarName);
  }

  @Test
  void testAccessExternalMemberOverInstance() {
    VariableDeclaration externalInstance =
        Util.getSubnodeOfTypeWithName(outerFunction4, VariableDeclaration.class, "externalClass");
    // Todo Points to the member of inner class instead of the external class
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func4_external_instance_varName"),
        externalInstance,
        externVarName); //  external_instance_field
  }

  @Test
  void testAccessExternalStaticMemberAfterInstanceCreation() {
    // Refers to unknown field of staticVarName
    // Todo Points to the definition instead of the declaration
    VRUtil.assertUsageOf(
        callParamMap.get("func4_second_external_staticVarName"), externStaticVarName);
  }

  @Test
  void testAccessStaticMemberThroughInstanceFirst() {
    // Refers to unknown field of staticVarName
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(outerFunction5, VariableDeclaration.class, "first");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func5_staticVarName_throughInstance_first"),
        declaration,
        outerStaticVarName); // external_static_field
  }

  @Test
  void testAccessStaticMemberThroughInstanceSecond() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(outerFunction5, VariableDeclaration.class, "second");

    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func5_staticVarName_throughInstance_second"),
        declaration,
        outerStaticVarName); // external_static_field
  }

  @Test
  void testImplicitThisAccessOfInnerClassMember() {
    VRUtil.assertUsageOf(callParamMap.get("func1_inner_imp_this_varName"), innerVarName);
  }

  @Test
  void testAccessOfInnerClassMemberOverInstance() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(innerFunction1, VariableDeclaration.class, "inner");
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func1_inner_instance_varName"),
        declaration,
        innerVarName); // inner_instance_field
  }

  @Test
  void testAccessOfOuterMemberOverInstance() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(innerFunction1, VariableDeclaration.class, "scopeVariables");
    // Todo points to inner varName instead of outerVarname

    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func1_outer_instance_varName"), declaration, outerVarName);
  }

  @Test
  void testAccessOfOuterStaticMember() {
    // Todo Points to the definition/declaration at the end of the file instead of the static
    // variable in the class
    // Todo we have to handle this type of redeclarations
    VRUtil.assertUsageOf(callParamMap.get("func1_outer_static_staticVarName"), outerStaticVarName);
  }

  @Test
  void testAccessOfInnerStaticMember() {
    // Todo Points to the definition/declaration at the end of the file instead of the static
    // variable in the class
    // Todo we have to handle this type of redeclarations
    VRUtil.assertUsageOf(callParamMap.get("func1_inner_static_staticVarName"), innerStaticVarName);
  }

  @Test
  void testAccessOfInnerClassMemberOverInstanceWithSameNamedVariable() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(innerFunction2, VariableDeclaration.class, "inner");
    // Todo points to the shadowing parameter of same name
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_inner_instance_varName_with_shadows"), declaration, innerVarName);
  }

  @Test
  void testAccessOfOuterMemberOverInstanceWithSameNamedVariable() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(innerFunction2, VariableDeclaration.class, "scopeVariables");
    // Todo points to the shadowing parameter
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_outer_instance_varName_with_shadows"), declaration, outerVarName);
  }

  @Test
  void testAccessOfOuterStaticMembertWithSameNamedVariable() {
    ; // static_field
    // Todo Points to the definition/declaration at the end of the file instead of the static
    // variable in the class
    // Todo we have to handle this type of redeclarations
    VRUtil.assertUsageOf(
        callParamMap.get("func2_outer_static_staticVarName_with_shadows"), outerStaticVarName);
  }

  @Test
  void testAccessOfInnerStaticMemberWithSameNamedVariable() {
    ; // inner_static_field
    // Todo Points to the definition/declaration at the end of the file instead of the static
    // variable in the class
    // Todo we have to handle this type of redeclarations
    VRUtil.assertUsageOf(
        callParamMap.get("func2_inner_static_staticVarName_with_shadows"), innerStaticVarName);
  }

  @Test
  void testLocalVariableUsedAsParameter() {
    VariableDeclaration declaration =
        Util.getSubnodeOfTypeWithName(main, VariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(callParamMap.get("main_local_varName"), declaration);
  }
}
