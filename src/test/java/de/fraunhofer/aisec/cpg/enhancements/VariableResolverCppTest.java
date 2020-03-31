package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
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

public class VariableResolverCppTest {

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

  private MethodDeclaration outer_function1;
  private List<ForStatement> forStatements;
  private MethodDeclaration outer_function2;
  private MethodDeclaration outer_function3;
  private MethodDeclaration outer_function4;

  private MethodDeclaration inner_function1;
  private MethodDeclaration inner_function2;

  private Map<String, Expression> callParamMap = new HashMap<>();

  @BeforeAll
  public void initTests() throws ExecutionException, InterruptedException {
    final String topLevelPath = "src/test/resources/variables/";
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
        Util.getOfTypeWithName(nodes, RecordDeclaration.class, "variables.ExternalClass");
    externVarName = Util.getSubnodeOfTypeWithName(externalClass, FieldDeclaration.class, "varName");
    externStaticVarName =
        Util.getSubnodeOfTypeWithName(externalClass, FieldDeclaration.class, "staticVarName");
    outerClass = Util.getOfTypeWithName(nodes, RecordDeclaration.class, "variables.ScopeVariables");
    outerVarName = Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "varName");
    outerStaticVarName =
        Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "staticVarName");
    outerImpThis = Util.getSubnodeOfTypeWithName(outerClass, FieldDeclaration.class, "this");

    // Inner class and its fields
    innerClass =
        Util.getOfTypeWithName(
            nodes, RecordDeclaration.class, "variables.ScopeVariables.InnerClass");
    innerVarName = Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "varName");
    innerStaticVarName =
        Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "staticVarName");
    innerImpThis = Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "this");
    innerImpOuter =
        Util.getSubnodeOfTypeWithName(innerClass, FieldDeclaration.class, "ScopeVariables.this");

    main = Util.getSubnodeOfTypeWithName(outerClass, MethodDeclaration.class, "main");

    outer_function1 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function1"))
            .collect(Collectors.toList())
            .get(0);
    forStatements = Util.filterCast(SubgraphWalker.flattenAST(outer_function1), ForStatement.class);

    // Functions i nthe outer and inner object
    outer_function2 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function2"))
            .collect(Collectors.toList())
            .get(0);
    outer_function3 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function3"))
            .collect(Collectors.toList())
            .get(0);
    outer_function4 =
        outerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function4"))
            .collect(Collectors.toList())
            .get(0);
    inner_function1 =
        innerClass.getMethods().stream()
            .filter(method -> method.getName().equals("function1"))
            .collect(Collectors.toList())
            .get(0);
    inner_function2 =
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
  public void testOuterVarNameAccessedImplicitThis() {
    MemberExpression asMemberExpression =
        getCallWithMemberExpression("func1_impl_this_varName"); // instance_field
    assertNotNull(asMemberExpression);
  }

  @Test
  public void testStaticFieldAccessedImplicitly() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_static_staticVarName"); // static_field");
    assertNotNull(asReference);
  }

  @Test
  public void testVarNameOfFirstLoopAccessed() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_first_loop_varName"); // first_loop_local
    assertNotNull(asReference);
  }

  @Test
  public void testAccessLocalVarNameInNestedBlock() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_nested_block_shadowed_local_varName"); // local_in_inner_block
    assertNotNull(asReference);
  }

  @Test
  public void testVarNameOfSecondLoopAccessed() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_second_loop_varName"); // second_loop_local
    assertNotNull(asReference);
  }

  @Test
  public void testParamVarNameAccessed() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func2_param_varName"); // parameter
    assertNotNull(asReference);
  }

  @Test
  public void testMemberVarNameOverExplicitThis() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func2_this_varName"); // instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testVarNameDeclaredInIfClause() {
    DeclaredReferenceExpression asReference = getCallWithReference("func2_if_varName"); // if_local
    assertNotNull(asReference);
  }

  @Test
  public void testCarNameCoughtAsException() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func2_catch_varName"); // exception_string
    assertNotNull(asReference);
  }

  @Test
  public void testMemberAccessedOverInstance() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func2_instance_varName"); // instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testMemberAccessedOverInstanceAfterParamDeclaration() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func3_instance_varName"); // instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessExternalClassMemberVarnameOverInstance() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func3_external_instance_varName"); // external_instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testExplicitlyReferenceStaticMemberInInternalClass() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func4_static_staticVarName"); // static_field
    assertNotNull(asReference);
  }

  @Test
  public void testExplicitlyReferenceStaticMemberInExternalClass() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func4_external_staticVarName"); // external_static_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessExternalMemberOverInstance() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func4_external_instance_varName"); //  external_instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessExternalStaticMemberAfterInstanceCreation() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func4_second_external_staticVarName"); // external_static_field
    assertNotNull(asReference);
  }

  @Test
  public void testImplicitThisAccessOfInnerClassMember() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_inner_imp_this_varName"); // inner_instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessOfInnerClassMemberOverInstance() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_inner_instance_varName"); // inner_instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessOfOuterMemberOverInstance() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_outer_instance_varName"); // instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessOfOuterStaticMember() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_outer_static_staticVarName"); // static_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessOfInnerStaticMember() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func1_inner_static_staticVarName"); // inner_static_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessOfInnerClassMemberOverInstanceWithSameNamedVariable() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func2_inner_instance_varName_with_shadows"); // inner_instance_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessOfOuterMemberOverInstanceWithSameNamedVariable() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func2_outer_instance_varName_with_shadows"); // instance_field
    assertNotNull(asReference);
  }

  @Test
  public void tesAccessOfOuterStaticMembertWithSameNamedVariable() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func2_outer_static_staticVarName_with_shadows"); // static_field
    assertNotNull(asReference);
  }

  @Test
  public void testAccessOfInnerStaticMemberWithSameNamedVariable() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("func2_inner_static_staticVarName_with_shadows"); // inner_static_field
    assertNotNull(asReference);
  }

  @Test
  public void testLocalVariableUsedAsParameter() {
    DeclaredReferenceExpression asReference =
        getCallWithReference("main_local_varName"); // parameter
    assertNotNull(asReference);
  }
}
