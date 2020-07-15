package de.fraunhofer.aisec.cpg.enhancements.variable_resolution;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.ForStatement;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
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

// Todo VariableResolver 7 Failed, 7 Passed

@Disabled(
    "Until changing variable resolution to ScopeManager. Then in detail disable the tests that need specific fixes")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VariableResolverJavaTest extends BaseTest {

  // Externally defined static global

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

  private MethodDeclaration main;

  private MethodDeclaration outerFunction1;
  private List<ForStatement> forStatements;
  private MethodDeclaration outerFunction2;
  private MethodDeclaration outerFunction3;
  private MethodDeclaration outerFunction4;

  private MethodDeclaration innerFunction1;
  private MethodDeclaration innerFunction2;
  private MethodDeclaration innerFunction3;

  private Map<String, Expression> callParamMap = new HashMap<>();

  @BeforeAll
  public void initTests() throws ExecutionException, InterruptedException {
    final String topLevelPath = "src/test/resources/variables_extended/";
    List<String> fileNames = Arrays.asList("ScopeVariables.java", "ExternalClass.java");
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
    externalClass =
        Util.getOfTypeWithName(nodes, RecordDeclaration.class, "variables_extended.ExternalClass");
    externVarName = Util.getSubnodeOfTypeWithName(externalClass, FieldDeclaration.class, "varName");
    externStaticVarName =
        Util.getSubnodeOfTypeWithName(externalClass, FieldDeclaration.class, "staticVarName");
    outerClass =
        Util.getOfTypeWithName(nodes, RecordDeclaration.class, "variables_extended.ScopeVariables");
    outerVarName = Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "varName");
    outerStaticVarName =
        Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "staticVarName");
    outerImpThis = Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "this");

    // Inner class and its fields
    innerClass =
        Util.getOfTypeWithName(
            nodes, RecordDeclaration.class, "variables_extended.ScopeVariables.InnerClass");
    innerVarName = Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "varName");
    innerStaticVarName =
        Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "staticVarName");
    innerImpThis = Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "this");
    innerImpOuter =
        Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "ScopeVariables.this");

    main = Util.getSubnodeOfTypeWithName(outerClass, MethodDeclaration.class, "main");

    outerFunction1 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function1"))
            .collect(Collectors.toList())
            .get(0);
    forStatements = Util.filterCast(SubgraphWalker.flattenAST(outerFunction1), ForStatement.class);

    // Functions i nthe outer and inner object
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
    innerFunction3 =
        innerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function3"))
            .collect(Collectors.toList())
            .get(0);

    for (CallExpression call : calls) {
      Expression first = call.getArguments().get(0);
      String logId = ((Literal) first).getValue().toString();

      Expression second = call.getArguments().get(1);
      callParamMap.put(logId, second);
    }
  }

  public DeclaredReferenceExpression getCallWithReference(String literal) {
    Expression exp = callParamMap.get(literal);
    if (exp instanceof DeclaredReferenceExpression) return (DeclaredReferenceExpression) exp;
    return null;
  }

  public MemberExpression getCallWithMemberExpression(String literal) {
    Expression exp = callParamMap.get(literal);
    if (exp instanceof MemberExpression) return (MemberExpression) exp;
    return null;
  }

  @Test
  public void testVarNameDeclaredInLoop() {
    VariableDeclaration firstLoopLocal =
        Util.getSubnodeOfTypeWithName(forStatements.get(0), VariableDeclaration.class, "varName");

    VRUtil.assertUsageOf(
        callParamMap.get("func1_first_loop_varName"),
        firstLoopLocal); // Todo refers to the second loop variable, apparently only one is
    // collected and there is no defined scope

    // Todo this is correct now
  }

  @Test
  public void testVarNameInSecondLoop() {
    VariableDeclaration secondLoopLocal =
        Util.getSubnodeOfTypeWithName(forStatements.get(1), VariableDeclaration.class, "varName");

    VRUtil.assertUsageOf(callParamMap.get("func1_second_loop_varName"), secondLoopLocal);
  }

  @Test
  public void testImplicitThisVarNameAfterLoops() {
    // Todo refers to the second loop local
    // Todo This is correct now because we properly pop the loop context
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func1_imp_this_varName"), outerImpThis, outerVarName);
  }

  @Test
  public void testReferenceToParameter() {
    ValueDeclaration param =
        Util.getSubnodeOfTypeWithName(outerFunction2, ParamVariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(callParamMap.get("func2_param_varName"), param);
  }

  @Test
  public void testVarNameInInstanceOfExternalClass() {
    VariableDeclaration externalClassInstance =
        Util.getSubnodeOfTypeWithName(outerFunction3, VariableDeclaration.class, "externalClass");
    // Todo member points to the function parameter with the same name instead of the field in the
    // external class
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func3_external_instance_varName"), externalClassInstance, externVarName);
  }

  @Test
  public void testStaticVarNameInExternalClass() {
    // Todo here a Unknown record declaration is added
    // Todo member refers to local variable with the same name of the
    // static field in external
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func3_external_static_staticVarName"),
        externalClass,
        externStaticVarName);
  }

  @Test
  public void testStaticVarnameWithoutPreviousInstance() {
    // Todo Case is a unknown record declaration
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func4_external_static_staticVarName"),
        externalClass,
        externStaticVarName);
  }

  @Test
  public void testVarNameOverImpThisInnerClass() {
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func1_inner_imp_this_varName"), innerImpThis, innerVarName);
  }

  @Test
  public void testVarNameInOuterFromInnerClass() {
    // Todo Points to varName in inner class instead of outer
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func1_outer_this_varName"), innerImpOuter, outerVarName);
  }

  @Test
  public void testStaticOuterFromInner() {
    // Todo points to innerStaticVar, this is wrong
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func1_outer_static_staticVarName"), outerClass, outerStaticVarName);
  }

  @Test
  public void testParamVarNameInInnerClass() {

    VRUtil.assertUsageOf(
        callParamMap.get("func2_inner_param_varName"),
        Util.getSubnodeOfTypeWithName(innerFunction2, ParamVariableDeclaration.class, "varName"));
  }

  @Test
  public void testInnerVarnameOverExplicitThis() {
    // Todo memeber currently points to an implicitly created field
    VRUtil.assertUsageOfMemberAndBase(
        callParamMap.get("func2_inner_this_varName"), innerImpThis, innerVarName);
  }

  @Test
  public void testStaticVarNameAsCoughtExcpetionInInner() {
    VariableDeclaration staticVarNameException =
        Util.getSubnodeOfTypeWithName(innerFunction3, VariableDeclaration.class, "staticVarName");
    VRUtil.assertUsageOf(
        callParamMap.get("func3_inner_exception_staticVarName"), staticVarNameException);
  }

  @Test
  public void testVarNameAsCoughtExcpetionInInner() {
    VariableDeclaration varNameExcepetion =
        Util.getSubnodeOfTypeWithName(innerFunction3, VariableDeclaration.class, "varName");
    VRUtil.assertUsageOf(callParamMap.get("func3_inner_exception_varName"), varNameExcepetion);
  }
}
