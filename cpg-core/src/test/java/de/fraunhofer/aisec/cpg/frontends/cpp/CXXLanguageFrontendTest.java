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
package de.fraunhofer.aisec.cpg.frontends.cpp;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.InferenceConfiguration;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.Annotation;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.CaseStatement;
import de.fraunhofer.aisec.cpg.graph.statements.CatchClause;
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.statements.DefaultStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement;
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.statements.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.statements.TryStatement;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.processing.IVisitor;
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CXXLanguageFrontendTest extends BaseTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CXXLanguageFrontendTest.class);

  /*@Test
  void testFF() throws Exception {
    File file = new File("src/test/resources/hqxvlc.c");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(tu);

    var nodes = SubgraphWalker.flattenAST(tu);
    var types = new HashMap<String, Integer>();
    var codes = new HashMap<String, Set<Integer>>();
    for (var n : nodes) {
      var count = types.computeIfAbsent(n.getClass().getSimpleName(), key -> 0);
      types.put(n.getClass().getSimpleName(), count + 1);

      var code = codes.computeIfAbsent(n.getClass().getSimpleName(), key -> new HashSet<>());
      code.add(n.hashCode());
    }

    for (var t : types.keySet()) {
      System.out.println(t + ": " + types.get(t) + " | unique hash codes: " + codes.get(t).size());
    }
  }*/

  @Test
  void testForEach() throws Exception {
    File file = new File("src/test/resources/components/foreachstmt.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    @NonNull
    Set<FunctionDeclaration> main = tu.getDeclarationsByName("main", FunctionDeclaration.class);
    assertFalse(main.isEmpty());

    FunctionDeclaration decl = main.iterator().next();
    VariableDeclaration ls = decl.getVariableDeclarationByName("ls").orElse(null);
    assertNotNull(ls);
    assertEquals(TypeParser.createFrom("std::vector<int>", true), ls.getType());
    assertEquals("ls", ls.getName());

    ForEachStatement forEachStatement = decl.getBodyStatementAs(1, ForEachStatement.class);
    assertNotNull(forEachStatement);

    // should loop over ls
    assertEquals(ls, ((DeclaredReferenceExpression) forEachStatement.getIterable()).getRefersTo());

    // should declare auto i (so far no concrete type inferrable)
    Statement stmt = forEachStatement.getVariable();
    assertNotNull(stmt);
    assertTrue(stmt instanceof DeclarationStatement);
    assertTrue(((DeclarationStatement) stmt).isSingleDeclaration());
    VariableDeclaration i =
        (VariableDeclaration) ((DeclarationStatement) stmt).getSingleDeclaration();
    assertNotNull(i);
    assertEquals("i", i.getName());
    assertEquals(UnknownType.getUnknownType(), i.getType());
  }

  @Test
  void testTryCatch() throws Exception {
    File file = new File("src/test/resources/components/trystmt.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> main = tu.getDeclarationsByName("main", FunctionDeclaration.class);
    assertFalse(main.isEmpty());

    TryStatement tryStatement = main.iterator().next().getBodyStatementAs(0, TryStatement.class);
    assertNotNull(tryStatement);

    List<CatchClause> catchClauses = tryStatement.getCatchClauses();
    // should have 3 catch clauses
    assertEquals(3, catchClauses.size());

    // declared exception variable
    VariableDeclaration parameter = catchClauses.get(0).getParameter();
    assertNotNull(parameter);
    assertEquals("e", parameter.getName());
    assertEquals(TypeParser.createFrom("const std::exception&", true), parameter.getType());

    // anonymous variable (this is not 100% handled correctly but will do for now)
    parameter = catchClauses.get(1).getParameter();
    assertNotNull(parameter);
    // this is currently our 'unnamed' parameter
    assertEquals("", parameter.getName());
    assertEquals(TypeParser.createFrom("const std::exception&", true), parameter.getType());

    // catch all
    parameter = catchClauses.get(2).getParameter();
    assertNull(parameter);
  }

  @Test
  void testTypeId() throws Exception {
    File file = new File("src/test/resources/typeidexpr.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> main = tu.getDeclarationsByName("main", FunctionDeclaration.class);
    assertNotNull(main);

    FunctionDeclaration funcDecl = main.iterator().next();
    VariableDeclaration i = funcDecl.getVariableDeclarationByName("i").orElse(null);
    assertNotNull(i);

    TypeIdExpression sizeof = (TypeIdExpression) i.getInitializer();
    assertNotNull(sizeof);
    assertEquals("sizeof", sizeof.getName());
    assertEquals(TypeParser.createFrom("std::size_t", true), sizeof.getType());

    VariableDeclaration typeInfo = funcDecl.getVariableDeclarationByName("typeInfo").orElse(null);
    assertNotNull(typeInfo);

    TypeIdExpression typeid = (TypeIdExpression) typeInfo.getInitializer();
    assertNotNull(typeid);
    assertEquals("typeid", typeid.getName());
    assertEquals(TypeParser.createFrom("const std::type_info&", true), typeid.getType());

    VariableDeclaration j = funcDecl.getVariableDeclarationByName("j").orElse(null);
    assertNotNull(j);

    TypeIdExpression alignof = (TypeIdExpression) j.getInitializer();
    assertNotNull(sizeof);
    assertEquals("alignof", alignof.getName());
    assertEquals(TypeParser.createFrom("std::size_t", true), alignof.getType());
  }

  @Test
  void testCast() throws Exception {
    File file = new File("src/test/resources/components/castexpr.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration main = tu.getDeclarationAs(0, FunctionDeclaration.class);

    VariableDeclaration e =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(0, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(e);
    assertEquals(TypeParser.createFrom("ExtendedClass*", true), e.getType());

    VariableDeclaration b =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(1, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(b);
    assertEquals(TypeParser.createFrom("BaseClass*", true), b.getType());

    // initializer
    CastExpression cast = (CastExpression) b.getInitializer();
    assertNotNull(cast);
    assertEquals(TypeParser.createFrom("BaseClass*", true), cast.getCastType());

    BinaryOperator staticCast = main.getBodyStatementAs(2, BinaryOperator.class);
    assertNotNull(staticCast);

    cast = (CastExpression) staticCast.getRhs();
    assertNotNull(cast);
    assertEquals("static_cast", cast.getName());

    BinaryOperator reinterpretCast = main.getBodyStatementAs(3, BinaryOperator.class);
    assertNotNull(reinterpretCast);

    cast = (CastExpression) reinterpretCast.getRhs();
    assertNotNull(cast);
    assertEquals("reinterpret_cast", cast.getName());

    VariableDeclaration d =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(4, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(d);

    cast = (CastExpression) d.getInitializer();
    assertNotNull(cast);
    assertEquals(TypeParser.createFrom("int", true), cast.getCastType());
  }

  @Test
  void testArrays() throws Exception {
    File file = new File("src/test/resources/arrays.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration main = tu.getDeclarationAs(0, FunctionDeclaration.class);

    CompoundStatement statement = (CompoundStatement) main.getBody();

    // first statement is the variable declaration
    VariableDeclaration x =
        (VariableDeclaration)
            ((DeclarationStatement) statement.getStatements().get(0)).getSingleDeclaration();
    assertNotNull(x);
    assertEquals(TypeParser.createFrom("int[]", true), x.getType());

    // initializer is a initializer list expression
    InitializerListExpression ile = (InitializerListExpression) x.getInitializer();
    List<Expression> initializers = ile.getInitializers();
    assertNotNull(initializers);

    assertEquals(3, initializers.size());

    // second statement is an expression directly
    ArraySubscriptionExpression ase =
        (ArraySubscriptionExpression) statement.getStatements().get(1);
    assertNotNull(ase);

    assertEquals(x, ((DeclaredReferenceExpression) ase.getArrayExpression()).getRefersTo());
    assertEquals(0, ((Literal<Integer>) ase.getSubscriptExpression()).getValue().intValue());
  }

  @Test
  void testFunctionDeclaration() throws Exception {
    File file = new File("src/test/resources/functiondecl.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    // should be seven function nodes
    assertEquals(7, declaration.getDeclarations().size());

    FunctionDeclaration method = declaration.getDeclarationAs(0, FunctionDeclaration.class);
    assertEquals("function0(int)void", method.getSignature());

    method = declaration.getDeclarationAs(1, FunctionDeclaration.class);
    assertEquals("function1(int, std.string, SomeType*, AnotherType&)int", method.getSignature());

    List<String> args =
        method.getParameters().stream().map(Node::getName).collect(Collectors.toList());
    assertEquals(List.of("arg0", "arg1", "arg2", "arg3"), args);

    method = declaration.getDeclarationAs(2, FunctionDeclaration.class);
    assertEquals("function0(int)void", method.getSignature());

    List<Statement> statements = ((CompoundStatement) method.getBody()).getStatements();
    assertFalse(statements.isEmpty());
    assertEquals(2, statements.size());

    // last statement should be an implicit return
    ReturnStatement statement =
        method.getBodyStatementAs(statements.size() - 1, ReturnStatement.class);
    assertNotNull(statement);
    assertTrue(statement.isImplicit());

    method = declaration.getDeclarationAs(3, FunctionDeclaration.class);
    assertEquals("function2()void*", method.getSignature());

    statements = ((CompoundStatement) method.getBody()).getStatements();
    assertFalse(statements.isEmpty());
    assertEquals(1, statements.size());

    // should only contain 1 explicit return statement
    statement = method.getBodyStatementAs(0, ReturnStatement.class);
    assertNotNull(statement);
    assertFalse(statement.isImplicit());

    method = declaration.getDeclarationAs(4, FunctionDeclaration.class);
    assertNotNull(method);
    assertEquals("function3()UnknownType*", method.getSignature());

    method = declaration.getDeclarationAs(5, FunctionDeclaration.class);
    assertNotNull(method);
    assertEquals("function4(int)void", method.getSignature());

    method = declaration.getDeclarationAs(6, FunctionDeclaration.class);
    assertNotNull(method);
    assertEquals(0, method.getParameters().size());
    assertEquals("function5()void", method.getSignature());
  }

  @Test
  void testCompoundStatement() throws Exception {
    File file = new File("src/test/resources/compoundstmt.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration function = declaration.getDeclarationAs(0, FunctionDeclaration.class);

    assertNotNull(function);

    Statement functionBody = function.getBody();

    assertNotNull(functionBody);

    List<Statement> statements = ((CompoundStatement) functionBody).getStatements();
    assertEquals(1, statements.size());

    ReturnStatement returnStatement = (ReturnStatement) statements.get(0);

    assertNotNull(returnStatement);

    Expression returnValue = returnStatement.getReturnValue();

    assertTrue(returnValue instanceof Literal);

    assertEquals(1, ((Literal) returnValue).getValue());
  }

  @Test
  void testPostfixExpression() throws Exception {
    File file = new File("src/test/resources/postfixexpression.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    List<Statement> statements =
        getStatementsOfFunction(declaration.getDeclarationAs(0, FunctionDeclaration.class));

    assertEquals(6, statements.size());

    CallExpression callExpression = (CallExpression) statements.get(0);

    assertEquals("printf", callExpression.getName());
    Expression arg = callExpression.getArguments().get(0);

    assertTrue(arg instanceof Literal);
    assertEquals("text", ((Literal) arg).getValue());

    UnaryOperator unaryOperatorPlus = (UnaryOperator) statements.get(1);

    assertEquals(UnaryOperator.OPERATOR_POSTFIX_INCREMENT, unaryOperatorPlus.getOperatorCode());
    assertTrue(unaryOperatorPlus.isPostfix());

    UnaryOperator unaryOperatorMinus = (UnaryOperator) statements.get(2);

    assertEquals(UnaryOperator.OPERATOR_POSTFIX_DECREMENT, unaryOperatorMinus.getOperatorCode());
    assertTrue(unaryOperatorMinus.isPostfix());

    // 4th statement is not yet parsed correctly

    MemberCallExpression memberCallExpression = (MemberCallExpression) statements.get(4);

    assertEquals("test", memberCallExpression.getBase().getName());
    assertEquals("c_str", memberCallExpression.getName());
  }

  @Test
  void testIf() throws Exception {
    File file = new File("src/test/resources/if.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    List<Statement> statements =
        getStatementsOfFunction(declaration.getDeclarationAs(0, FunctionDeclaration.class));

    IfStatement ifStatement = (IfStatement) statements.get(0);

    assertNotNull(ifStatement);
    assertNotNull(ifStatement.getCondition());

    assertEquals("bool", ifStatement.getCondition().getType().getTypeName());
    assertEquals(true, ((Literal) ifStatement.getCondition()).getValue());

    assertTrue(
        ((CompoundStatement) ifStatement.getThenStatement()).getStatements().get(0)
            instanceof ReturnStatement);
    assertTrue(
        ((CompoundStatement) ifStatement.getElseStatement()).getStatements().get(0)
            instanceof ReturnStatement);
  }

  @Test
  void testSwitch() throws Exception {
    File file = new File("src/test/resources/cfg/switch.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    List<Node> graphNodes = SubgraphWalker.flattenAST(declaration);
    graphNodes.sort(new NodeComparator());
    assertTrue(graphNodes.size() != 0);

    List<SwitchStatement> switchStatements = Util.filterCast(graphNodes, SwitchStatement.class);
    assertTrue(switchStatements.size() == 3);

    SwitchStatement switchStatement = switchStatements.get(0);

    assertTrue(((CompoundStatement) switchStatement.getStatement()).getStatements().size() == 11);

    List<CaseStatement> caseStatements =
        Util.filterCast(SubgraphWalker.flattenAST(switchStatement), CaseStatement.class);
    assertTrue(caseStatements.size() == 4);

    List<DefaultStatement> defaultStatements =
        Util.filterCast(SubgraphWalker.flattenAST(switchStatement), DefaultStatement.class);
    assertTrue(defaultStatements.size() == 1);
  }

  @Test
  void testDeclarationStatement() throws Exception {
    File file = new File("src/test/resources/declstmt.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration function = declaration.getDeclarationAs(0, FunctionDeclaration.class);

    List<Statement> statements = getStatementsOfFunction(function);

    statements.forEach(
        node -> {
          LOGGER.debug("{}", node);

          assertTrue(
              node instanceof DeclarationStatement
                  || statements.indexOf(node) == statements.size() - 1
                      && node instanceof ReturnStatement);
        });

    VariableDeclaration declFromMultiplicateExpression =
        ((DeclarationStatement) statements.get(0))
            .getSingleDeclarationAs(VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("SSL_CTX*", true), declFromMultiplicateExpression.getType());
    assertEquals("ptr", declFromMultiplicateExpression.getName());

    VariableDeclaration withInitializer =
        ((DeclarationStatement) statements.get(1))
            .getSingleDeclarationAs(VariableDeclaration.class);
    Expression initializer = withInitializer.getInitializer();

    assertNotNull(initializer);
    assertTrue(initializer instanceof Literal);
    assertEquals(1, ((Literal) initializer).getValue());

    List<Declaration> twoDeclarations = statements.get(2).getDeclarations();

    assertEquals(2, twoDeclarations.size());
    VariableDeclaration b = (VariableDeclaration) twoDeclarations.get(0);
    assertNotNull(b);
    assertEquals("b", b.getName());
    assertEquals(TypeParser.createFrom("int*", false), b.getType());

    VariableDeclaration c = (VariableDeclaration) twoDeclarations.get(1);
    assertNotNull(c);
    assertEquals("c", c.getName());
    assertEquals(TypeParser.createFrom("int", false), c.getType());

    VariableDeclaration withoutInitializer =
        ((DeclarationStatement) statements.get(3))
            .getSingleDeclarationAs(VariableDeclaration.class);
    initializer = withoutInitializer.getInitializer();

    assertEquals(TypeParser.createFrom("int*", true), withoutInitializer.getType());
    assertEquals("d", withoutInitializer.getName());

    assertNull(initializer);

    VariableDeclaration qualifiedType =
        ((DeclarationStatement) statements.get(4))
            .getSingleDeclarationAs(VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("std.string", true), qualifiedType.getType());
    assertEquals("text", qualifiedType.getName());
    assertTrue(qualifiedType.getInitializer() instanceof Literal);
    assertEquals("some text", ((Literal) qualifiedType.getInitializer()).getValue());

    VariableDeclaration pointerWithAssign =
        ((DeclarationStatement) statements.get(5))
            .getSingleDeclarationAs(VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("void*", true), pointerWithAssign.getType());
    assertEquals("ptr2", pointerWithAssign.getName());
    assertEquals("NULL", pointerWithAssign.getInitializer().getName());

    List<Declaration> classWithVariable = statements.get(6).getDeclarations();
    assertEquals(2, classWithVariable.size());

    RecordDeclaration classA = (RecordDeclaration) classWithVariable.get(0);
    assertNotNull(classA);
    assertEquals("A", classA.getName());

    VariableDeclaration myA = (VariableDeclaration) classWithVariable.get(1);
    assertNotNull(myA);
    assertEquals("myA", myA.getName());
    assertEquals(classA, ((ObjectType) myA.getType()).getRecordDeclaration());
  }

  @Test
  void testAssignmentExpression() throws Exception {
    File file = new File("src/test/resources/assignmentexpression.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    // just take a look at the second function
    FunctionDeclaration functionDeclaration =
        declaration.getDeclarationAs(1, FunctionDeclaration.class);

    List<Statement> statements = getStatementsOfFunction(functionDeclaration);

    Statement declareA = statements.get(0);
    Declaration a = ((DeclarationStatement) declareA).getSingleDeclaration();

    Statement assignA = statements.get(1);
    assertTrue(assignA instanceof BinaryOperator);

    Expression lhs = ((BinaryOperator) assignA).getLhs();
    Expression rhs = ((BinaryOperator) assignA).getRhs();

    assertEquals("a", ((BinaryOperator) assignA).getLhs().getName());
    assertEquals(2, ((Literal) ((BinaryOperator) assignA).getRhs()).getValue());
    assertRefersTo(((BinaryOperator) assignA).getLhs(), a);

    Statement declareB = statements.get(2);

    assertTrue(declareB instanceof DeclarationStatement);
    Declaration b = ((DeclarationStatement) declareB).getSingleDeclaration();

    // a = b
    Statement assignB = statements.get(3);
    assertTrue(assignB instanceof BinaryOperator);

    lhs = ((BinaryOperator) assignB).getLhs();
    rhs = ((BinaryOperator) assignB).getRhs();

    assertEquals("a", lhs.getName());
    assertTrue(rhs instanceof DeclaredReferenceExpression);
    assertEquals("b", rhs.getName());
    assertRefersTo(rhs, b);

    Statement assignBWithFunction = statements.get(4);

    assertTrue(assignBWithFunction instanceof BinaryOperator);
    assertEquals("a", ((BinaryOperator) assignBWithFunction).getLhs().getName());
    assertTrue(((BinaryOperator) assignBWithFunction).getRhs() instanceof CallExpression);

    CallExpression call = (CallExpression) ((BinaryOperator) assignBWithFunction).getRhs();

    assertEquals("someFunction", call.getName());
    assertRefersTo(call.getArguments().get(0), b);
  }

  private void assertRefersTo(Expression expression, Declaration b) {
    if (expression instanceof DeclaredReferenceExpression) {
      assertEquals(b, ((DeclaredReferenceExpression) expression).getRefersTo());
    } else {
      fail();
    }
  }

  @Test
  void testShiftExpression() throws Exception {
    File file = new File("src/test/resources/shiftexpression.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration functionDeclaration =
        declaration.getDeclarationAs(0, FunctionDeclaration.class);

    List<Statement> statements = getStatementsOfFunction(functionDeclaration);

    assertTrue(statements.get(1) instanceof BinaryOperator);
  }

  @Test
  void testUnaryOperator() throws Exception {
    File file = new File("src/test/resources/unaryoperator.cpp");
    TranslationUnitDeclaration unit =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    List<Statement> statements =
        getStatementsOfFunction(unit.getDeclarationAs(0, FunctionDeclaration.class));

    int line = -1;

    // int a
    DeclarationStatement statement = (DeclarationStatement) statements.get(++line);

    assertNotNull(statement);

    // a++
    UnaryOperator postfix = (UnaryOperator) statements.get(++line);
    Expression input = postfix.getInput();

    assertEquals("a", input.getName());
    assertEquals("++", postfix.getOperatorCode());
    assertTrue(postfix.isPostfix());

    // --a
    UnaryOperator prefix = (UnaryOperator) statements.get(++line);
    input = prefix.getInput();

    assertEquals("a", input.getName());
    assertEquals("--", prefix.getOperatorCode());
    assertTrue(prefix.isPrefix());

    // int len = sizeof(a);
    statement = (DeclarationStatement) statements.get(++line);
    VariableDeclaration declaration = (VariableDeclaration) statement.getSingleDeclaration();
    UnaryOperator sizeof = (UnaryOperator) declaration.getInitializer();

    input = sizeof.getInput();

    assertEquals("a", input.getName());
    assertEquals("sizeof", sizeof.getOperatorCode());
    assertTrue(sizeof.isPrefix());

    // bool b = !false;
    statement = (DeclarationStatement) statements.get(++line);
    declaration = (VariableDeclaration) statement.getSingleDeclaration();
    UnaryOperator negation = (UnaryOperator) declaration.getInitializer();

    input = negation.getInput();

    assertTrue(input instanceof Literal);

    assertEquals(false, ((Literal) input).getValue());
    assertEquals("!", negation.getOperatorCode());
    assertTrue(negation.isPrefix());

    // int* ptr = 0;
    statement = (DeclarationStatement) statements.get(++line);

    assertNotNull(statement);

    // b = *ptr;
    BinaryOperator assign = (BinaryOperator) statements.get(++line);
    UnaryOperator dereference = (UnaryOperator) assign.getRhs();
    input = dereference.getInput();

    assertEquals("ptr", input.getName());
    assertEquals("*", dereference.getOperatorCode());
    assertTrue(dereference.isPrefix());
  }

  @Test
  void testBinaryOperator() throws Exception {
    File file = new File("src/test/resources/binaryoperator.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    List<Statement> statements =
        getStatementsOfFunction(declaration.getDeclarationAs(0, FunctionDeclaration.class));

    // first two statements are just declarations

    // a = b * 2
    BinaryOperator operator = (BinaryOperator) statements.get(2);

    assertEquals("a", operator.getLhs().getName());
    assertTrue(operator.getRhs() instanceof BinaryOperator);

    BinaryOperator rhs = (BinaryOperator) operator.getRhs();

    assertTrue(rhs.getLhs() instanceof DeclaredReferenceExpression);
    assertEquals("b", rhs.getLhs().getName());
    assertTrue(rhs.getRhs() instanceof Literal);
    assertEquals(2, ((Literal) rhs.getRhs()).getValue());

    // a = 1 * 1
    operator = (BinaryOperator) statements.get(3);

    assertEquals("a", operator.getLhs().getName());
    assertTrue(operator.getRhs() instanceof BinaryOperator);

    rhs = (BinaryOperator) operator.getRhs();

    assertTrue(rhs.getLhs() instanceof Literal);
    assertEquals(1, ((Literal) rhs.getLhs()).getValue());
    assertTrue(rhs.getRhs() instanceof Literal);
    assertEquals(1, ((Literal) rhs.getRhs()).getValue());

    // std::string* notMultiplication
    // this is not a multiplication, but a variable declaration with a pointer type, but
    // syntactically no different than the previous ones
    DeclarationStatement stmt = (DeclarationStatement) statements.get(4);
    VariableDeclaration decl = (VariableDeclaration) stmt.getSingleDeclaration();

    assertEquals(TypeParser.createFrom("std.string*", true), decl.getType());
    assertEquals("notMultiplication", decl.getName());
    assertTrue(decl.getInitializer() instanceof BinaryOperator);

    operator = (BinaryOperator) decl.getInitializer();

    assertTrue(operator.getLhs() instanceof Literal);
    assertEquals(0, ((Literal) operator.getLhs()).getValue());

    assertTrue(operator.getRhs() instanceof Literal);
    assertEquals(0, ((Literal) operator.getRhs()).getValue());
  }

  @Test
  void testRecordDeclaration() throws Exception {
    File file = new File("src/test/resources/recordstmt.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    RecordDeclaration recordDeclaration = declaration.getDeclarationAs(0, RecordDeclaration.class);
    assertNotNull(recordDeclaration);

    assertEquals("SomeClass", recordDeclaration.getName());
    assertEquals("class", recordDeclaration.getKind());
    assertEquals(3, recordDeclaration.getFields().size());

    var field = recordDeclaration.getField("field");
    assertNotNull(field);

    var constant = recordDeclaration.getField("CONSTANT");
    assertNotNull(constant);

    assertEquals(TypeParser.createFrom("void*", true), field.getType());

    assertEquals(3, recordDeclaration.getMethods().size());

    MethodDeclaration method = recordDeclaration.getMethods().get(0);

    assertEquals("method", method.getName());
    assertEquals(0, method.getParameters().size());
    assertEquals(TypeParser.createFrom("void*", true), method.getType());
    assertFalse(method.hasBody());

    MethodDeclaration definition = (MethodDeclaration) method.getDefinition();
    assertNotNull(definition);
    assertEquals("method", definition.getName());
    assertEquals(0, definition.getParameters().size());
    assertTrue(definition.isDefinition());

    MethodDeclaration methodWithParam = recordDeclaration.getMethods().get(1);

    assertEquals("method", methodWithParam.getName());
    assertEquals(1, methodWithParam.getParameters().size());
    assertEquals(
        TypeParser.createFrom("int", true), methodWithParam.getParameters().get(0).getType());
    assertEquals(TypeParser.createFrom("void*", true), methodWithParam.getType());
    assertFalse(methodWithParam.hasBody());

    definition = (MethodDeclaration) methodWithParam.getDefinition();
    assertNotNull(definition);
    assertEquals("method", definition.getName());
    assertEquals(1, definition.getParameters().size());
    assertTrue(definition.isDefinition());

    MethodDeclaration inlineMethod = recordDeclaration.getMethods().get(2);

    assertEquals("inlineMethod", inlineMethod.getName());
    assertEquals(TypeParser.createFrom("void*", true), inlineMethod.getType());
    assertTrue(inlineMethod.hasBody());

    ConstructorDeclaration inlineConstructor = recordDeclaration.getConstructors().get(0);

    assertEquals(recordDeclaration.getName(), inlineConstructor.getName());
    assertEquals(TypeParser.createFrom("SomeClass", true), inlineConstructor.getType());
    assertTrue(inlineConstructor.hasBody());

    ConstructorDeclaration constructorDefinition =
        declaration.getDeclarationAs(3, ConstructorDeclaration.class);

    assertNotNull(constructorDefinition);
    assertEquals(1, constructorDefinition.getParameters().size());
    assertEquals(
        TypeParser.createFrom("int", true), constructorDefinition.getParameters().get(0).getType());
    assertEquals(TypeParser.createFrom("SomeClass", true), constructorDefinition.getType());
    assertTrue(constructorDefinition.hasBody());

    ConstructorDeclaration constructorDeclaration = recordDeclaration.getConstructors().get(1);

    assertNotNull(constructorDeclaration);
    assertFalse(constructorDeclaration.isDefinition());
    assertEquals(constructorDefinition, constructorDeclaration.getDefinition());

    var main =
        declaration.getDeclarationsByName("main", FunctionDeclaration.class).iterator().next();
    assertNotNull(main);

    var methodCallWithConstant = main.getBodyStatementAs(2, CallExpression.class);
    assertNotNull(methodCallWithConstant);

    var arg = methodCallWithConstant.getArguments().get(0);
    assertSame(constant, ((DeclaredReferenceExpression) arg).getRefersTo());
  }

  @Test
  void testLiterals() throws Exception {
    File file = new File("src/test/resources/literals.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    VariableDeclaration s = declaration.getDeclarationAs(0, VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("char[]", true), s.getType());
    assertEquals("s", s.getName());

    Expression initializer = s.getInitializer();

    assertEquals("string", ((Literal) initializer).getValue());

    VariableDeclaration i = declaration.getDeclarationAs(1, VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("int", true), i.getType());
    assertEquals("i", i.getName());

    initializer = i.getInitializer();

    assertEquals(1, ((Literal) initializer).getValue());

    VariableDeclaration f = declaration.getDeclarationAs(2, VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("float", true), f.getType());
    assertEquals("f", f.getName());

    initializer = f.getInitializer();

    assertEquals(0.2f, ((Literal) initializer).getValue());

    VariableDeclaration d = declaration.getDeclarationAs(3, VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("double", true), d.getType());
    assertEquals("d", d.getName());

    initializer = d.getInitializer();

    assertEquals(0.2d, ((Literal) initializer).getValue());

    VariableDeclaration b = declaration.getDeclarationAs(4, VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("bool", true), b.getType());
    assertEquals("b", b.getName());

    initializer = b.getInitializer();

    assertEquals(false, ((Literal) initializer).getValue());

    VariableDeclaration c = declaration.getDeclarationAs(5, VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("char", true), c.getType());
    assertEquals("c", c.getName());

    initializer = c.getInitializer();

    assertEquals('c', ((Literal) initializer).getValue());
  }

  @Test
  void testInitListExpression() throws Exception {
    File file = new File("src/test/resources/initlistexpression.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    // x y = { 1, 2 };
    VariableDeclaration y = declaration.getDeclarationAs(1, VariableDeclaration.class);

    assertEquals("y", y.getName());

    Expression initializer = y.getInitializer();

    assertNotNull(initializer);
    assertTrue(initializer instanceof InitializerListExpression);

    InitializerListExpression listExpression = (InitializerListExpression) initializer;

    assertEquals(2, listExpression.getInitializers().size());

    Literal a = (Literal) listExpression.getInitializers().get(0);
    Literal b = (Literal) listExpression.getInitializers().get(1);

    assertEquals(1, a.getValue());
    assertEquals(2, b.getValue());

    // int z[] = { 2, 3, 4 };
    VariableDeclaration z = declaration.getDeclarationAs(2, VariableDeclaration.class);

    assertEquals(TypeParser.createFrom("int[]", true), z.getType());

    initializer = z.getInitializer();

    assertNotNull(initializer);
    assertTrue(initializer instanceof InitializerListExpression);

    listExpression = (InitializerListExpression) initializer;

    assertEquals(3, listExpression.getInitializers().size());
  }

  @Test
  void testObjectCreation() throws Exception {
    File file = new File("src/test/resources/objcreation.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(declaration);

    // get the main method
    FunctionDeclaration main = declaration.getDeclarationAs(3, FunctionDeclaration.class);
    CompoundStatement statement = (CompoundStatement) main.getBody();

    // Integer i
    VariableDeclaration i =
        (VariableDeclaration)
            ((DeclarationStatement) statement.getStatements().get(0)).getSingleDeclaration();

    // type should be Integer
    assertEquals(TypeParser.createFrom("Integer", true), i.getType());

    // initializer should be a construct expression
    ConstructExpression constructExpression = (ConstructExpression) i.getInitializer();

    // type of the construct expression should also be Integer
    assertEquals(TypeParser.createFrom("Integer", true), constructExpression.getType());

    // auto (Integer) m
    VariableDeclaration m =
        (VariableDeclaration)
            ((DeclarationStatement) statement.getStatements().get(6)).getSingleDeclaration();

    // type should be Integer*
    assertEquals(TypeParser.createFrom("Integer*", true), m.getType());

    // initializer should be a new expression
    NewExpression newExpression = (NewExpression) m.getInitializer();

    // type of the new expression should also be Integer*
    assertEquals(TypeParser.createFrom("Integer*", true), newExpression.getType());

    // initializer should be a construct expression
    constructExpression = (ConstructExpression) newExpression.getInitializer();

    // type of the construct expression should be Integer
    assertEquals(TypeParser.createFrom("Integer", true), constructExpression.getType());

    // argument should be named k and of type m
    DeclaredReferenceExpression k =
        (DeclaredReferenceExpression) constructExpression.getArguments().get(0);
    assertEquals("k", k.getName());

    // type of the construct expression should also be Integer
    assertEquals(TypeParser.createFrom("int", true), k.getType());
  }

  List<Statement> getStatementsOfFunction(FunctionDeclaration declaration) {
    return ((CompoundStatement) declaration.getBody()).getStatements();
  }

  @Test
  void testRegionsCfg() throws Exception {
    File file = new File("src/test/resources/cfg.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration fdecl = declaration.getDeclarationAs(0, FunctionDeclaration.class);
    CompoundStatement body = (CompoundStatement) fdecl.getBody();

    Map<String, Region> expected = new HashMap<>();
    expected.put("cout << \"bla\";", new Region(4, 3, 4, 17));
    expected.put("cout << \"blubb\";", new Region(5, 3, 5, 19));
    expected.put("return 0;", new Region(15, 3, 15, 12));
    for (Node d : body.getStatements()) {
      if (expected.containsKey(d.getCode())) {
        assertEquals(expected.get(d.getCode()), d.getLocation().getRegion(), d.getCode());
        expected.remove(d.getCode());
      }
    }
    assertTrue(expected.isEmpty(), String.join(", ", expected.keySet()));
  }

  @Test
  void testDesignatedInitializer() throws Exception {
    File file = new File("src/test/resources/components/designatedInitializer.c");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    // should be four method nodes
    assertEquals(2, declaration.getDeclarations().size());

    FunctionDeclaration method = declaration.getDeclarationAs(1, FunctionDeclaration.class);

    assertEquals("main()int", method.getSignature());

    assertTrue(method.getBody() instanceof CompoundStatement);

    List<Statement> statements = ((CompoundStatement) method.getBody()).getStatements();

    assertEquals(4, statements.size());
    assertTrue(statements.get(0) instanceof DeclarationStatement);
    assertTrue(statements.get(1) instanceof DeclarationStatement);
    assertTrue(statements.get(2) instanceof DeclarationStatement);
    assertTrue(statements.get(3) instanceof ReturnStatement);

    Expression initializer =
        ((VariableDeclaration) ((DeclarationStatement) statements.get(0)).getSingleDeclaration())
            .getInitializer();
    assertTrue(initializer instanceof InitializerListExpression);

    assertEquals(3, ((InitializerListExpression) initializer).getInitializers().size());
    assertTrue(
        ((InitializerListExpression) initializer).getInitializers().get(0)
            instanceof DesignatedInitializerExpression);
    assertTrue(
        ((InitializerListExpression) initializer).getInitializers().get(1)
            instanceof DesignatedInitializerExpression);
    assertTrue(
        ((InitializerListExpression) initializer).getInitializers().get(2)
            instanceof DesignatedInitializerExpression);

    DesignatedInitializerExpression die =
        (DesignatedInitializerExpression)
            ((InitializerListExpression) initializer).getInitializers().get(0);
    assertTrue(die.getLhs().get(0) instanceof DeclaredReferenceExpression);
    assertTrue(die.getRhs() instanceof Literal);
    assertEquals("y", die.getLhs().get(0).getName());
    assertEquals(0, ((Literal) die.getRhs()).getValue());

    die =
        (DesignatedInitializerExpression)
            ((InitializerListExpression) initializer).getInitializers().get(1);
    assertTrue(die.getLhs().get(0) instanceof DeclaredReferenceExpression);
    assertTrue(die.getRhs() instanceof Literal);
    assertEquals("z", die.getLhs().get(0).getName());
    assertEquals(1, ((Literal) die.getRhs()).getValue());

    die =
        (DesignatedInitializerExpression)
            ((InitializerListExpression) initializer).getInitializers().get(2);
    assertTrue(die.getLhs().get(0) instanceof DeclaredReferenceExpression);
    assertTrue(die.getRhs() instanceof Literal);
    assertEquals("x", die.getLhs().get(0).getName());
    assertEquals(2, ((Literal) die.getRhs()).getValue());

    initializer =
        ((VariableDeclaration) ((DeclarationStatement) statements.get(1)).getSingleDeclaration())
            .getInitializer();
    assertTrue(initializer instanceof InitializerListExpression);

    assertEquals(1, ((InitializerListExpression) initializer).getInitializers().size());
    assertTrue(
        ((InitializerListExpression) initializer).getInitializers().get(0)
            instanceof DesignatedInitializerExpression);

    die =
        (DesignatedInitializerExpression)
            ((InitializerListExpression) initializer).getInitializers().get(0);
    assertTrue(die.getLhs().get(0) instanceof DeclaredReferenceExpression);
    assertTrue(die.getRhs() instanceof Literal);
    assertEquals("x", die.getLhs().get(0).getName());
    assertEquals(20, ((Literal) die.getRhs()).getValue());

    initializer =
        ((VariableDeclaration) ((DeclarationStatement) statements.get(2)).getSingleDeclaration())
            .getInitializer();
    assertTrue(initializer instanceof InitializerListExpression);

    assertEquals(2, ((InitializerListExpression) initializer).getInitializers().size());
    assertTrue(
        ((InitializerListExpression) initializer).getInitializers().get(0)
            instanceof DesignatedInitializerExpression);
    assertTrue(
        ((InitializerListExpression) initializer).getInitializers().get(1)
            instanceof DesignatedInitializerExpression);

    die =
        (DesignatedInitializerExpression)
            ((InitializerListExpression) initializer).getInitializers().get(0);
    assertTrue(die.getLhs().get(0) instanceof Literal);
    assertTrue(die.getRhs() instanceof Literal);
    assertEquals(3, ((Literal) die.getLhs().get(0)).getValue());
    assertEquals(1, ((Literal) die.getRhs()).getValue());

    die =
        (DesignatedInitializerExpression)
            ((InitializerListExpression) initializer).getInitializers().get(1);
    assertTrue(die.getLhs().get(0) instanceof Literal);
    assertTrue(die.getRhs() instanceof Literal);
    assertEquals(5, ((Literal) die.getLhs().get(0)).getValue());
    assertEquals(2, ((Literal) die.getRhs()).getValue());
  }

  @Test
  void testLocalVariables() throws Exception {
    File file = new File("src/test/resources/variables/local_variables.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration function = declaration.getDeclarationAs(2, FunctionDeclaration.class);

    assertEquals("testExpressionInExpressionList()int", function.getSignature());

    List<VariableDeclaration> locals = function.getBody().getLocals();
    // Expecting x, foo, t
    Set<String> localNames = locals.stream().map(Node::getName).collect(Collectors.toSet());
    assertTrue(localNames.contains("x"));
    assertTrue(localNames.contains("foo"));
    assertTrue(localNames.contains("t"));

    // ... and nothing else
    assertEquals(3, localNames.size());

    RecordDeclaration clazz = declaration.getDeclarationAs(0, RecordDeclaration.class);
    assertEquals("this", clazz.getFields().get(0).getName());
    assertEquals(1, clazz.getFields().size());
  }

  @Test
  void testLocation() throws Exception {
    File file = new File("src/test/resources/components/foreachstmt.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> main = tu.getDeclarationsByName("main", FunctionDeclaration.class);
    assertFalse(main.isEmpty());

    PhysicalLocation location = main.iterator().next().getLocation();

    assertNotNull(location);

    Path path = Path.of(location.getArtifactLocation().getUri());
    assertEquals("foreachstmt.cpp", path.getFileName().toString());
    assertEquals(new Region(4, 1, 8, 2), location.getRegion());
  }

  @Test
  void testNamespaces() throws Exception {
    File file = new File("src/test/resources/namespaces.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);
    assertNotNull(tu);

    NamespaceDeclaration firstNamespace =
        tu.getDeclarationsByName("FirstNamespace", NamespaceDeclaration.class).iterator().next();
    assertNotNull(firstNamespace);

    RecordDeclaration someClass =
        firstNamespace
            .getDeclarationsByName("FirstNamespace::SomeClass", RecordDeclaration.class)
            .iterator()
            .next();
    assertNotNull(someClass);

    RecordDeclaration anotherClass =
        tu.getDeclarationsByName("AnotherClass", RecordDeclaration.class).iterator().next();
    assertNotNull(anotherClass);
  }

  @Test
  void testAttributes() throws Exception {
    File file = new File("src/test/resources/attributes.cpp");
    List<TranslationUnitDeclaration> declarations =
        TestUtils.analyzeWithBuilder(
            TranslationConfiguration.builder()
                .sourceLocations(List.of(file))
                .topLevel(file.getParentFile())
                .defaultPasses()
                .defaultLanguages()
                .processAnnotations(true)
                .symbols(
                    Map.of("PROPERTY_ATTRIBUTE(...)", "[[property_attribute(#__VA_ARGS__)]]")));
    assertFalse(declarations.isEmpty());

    TranslationUnitDeclaration tu = declarations.get(0);
    assertNotNull(tu);

    FunctionDeclaration main =
        tu.getDeclarationsByName("main", FunctionDeclaration.class).iterator().next();
    assertNotNull(main);

    assertEquals("function_attribute", main.getAnnotations().get(0).getName());

    RecordDeclaration someClass =
        tu.getDeclarationsByName("SomeClass", RecordDeclaration.class).iterator().next();
    assertNotNull(someClass);

    assertEquals("record_attribute", someClass.getAnnotations().get(0).getName());

    FieldDeclaration a =
        someClass.getFields().stream().filter(f -> f.getName().equals("a")).findAny().orElse(null);
    assertNotNull(a);

    Annotation annotation = a.getAnnotations().get(0);
    assertNotNull(annotation);

    assertEquals("property_attribute", annotation.getName());
    assertEquals(3, annotation.getMembers().size());
    assertEquals("a", ((Literal<String>) annotation.getMembers().get(0).getValue()).getValue());

    FieldDeclaration b =
        someClass.getFields().stream().filter(f -> f.getName().equals("b")).findAny().orElse(null);
    assertNotNull(a);

    annotation = b.getAnnotations().get(0);
    assertNotNull(annotation);

    assertEquals("property_attribute", annotation.getName());
    assertEquals(1, annotation.getMembers().size());
    assertEquals(
        "SomeCategory, SomeOtherThing",
        ((Literal<String>) annotation.getMembers().get(0).getValue()).getValue());
  }

  @Test
  void testUnityBuild() throws Exception {
    File file = new File("src/test/resources/unity");
    List<TranslationUnitDeclaration> declarations =
        TestUtils.analyzeWithBuilder(
            TranslationConfiguration.builder()
                .sourceLocations(List.of(file))
                .topLevel(file.getParentFile())
                .useUnityBuild(true)
                .loadIncludes(true)
                .defaultPasses()
                .defaultLanguages());

    assertEquals(1, declarations.size());

    // should contain 3 declarations (2 include and 1 function decl from the include)
    assertEquals(3, declarations.get(0).getDeclarations().size());
  }

  @Test
  void testEOGCompleteness() throws Exception {
    var file = new File("src/test/resources/fix-455/main.cpp");
    var tu = TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    var main = tu.getDeclarationsByName("main", FunctionDeclaration.class).iterator().next();
    assertNotNull(main);

    var body = (CompoundStatement) main.getBody();
    assertNotNull(body);

    var returnStatement = body.getStatements().get(body.getStatements().size() - 1);
    assertNotNull(returnStatement);

    // we need to assert, that we have a consistent chain of EOG edges from the first statement to
    // the return statement. otherwise, the EOG chain is somehow broken
    var eogEdges = new ArrayList<Node>();

    main.accept(
        Strategy::EOG_FORWARD,
        new IVisitor<>() {
          public void visit(Node n) {
            System.out.println(n);
            eogEdges.add(n);
          }
        });

    assertTrue(eogEdges.contains(returnStatement));
  }

  @Test
  void testParenthesis() throws Exception {
    var file = new File("src/test/resources/parenthesis.cpp");
    var tu =
        TestUtils.analyzeAndGetFirstTU(
            List.of(file),
            file.getParentFile().toPath(),
            true,
            config -> {
              config.inferenceConfiguration(
                  InferenceConfiguration.builder().guessCastExpressions(true).build());
            });

    var main = tu.getDeclarationsByName("main", FunctionDeclaration.class).iterator().next();
    assertNotNull(main);

    var declStatement = main.getBodyStatementAs(0, DeclarationStatement.class);
    assertNotNull(declStatement);

    var decl = (VariableDeclaration) declStatement.getSingleDeclaration();
    assertNotNull(decl);

    var initializer = decl.getInitializer();
    assertNotNull(initializer);
    assertTrue(initializer instanceof CastExpression);
    assertEquals("size_t", ((CastExpression) initializer).getCastType().getName());
  }

  @Test
  void testCppThisField() throws Exception {
    var file = new File("src/test/resources/cpp-this-field.cpp");
    var tu = TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    var main = tu.getDeclarationsByName("main", FunctionDeclaration.class).iterator().next();
    assertNotNull(main);

    var classT = tu.getDeclarationsByName("T", RecordDeclaration.class).iterator().next();
    assertNotNull(classT);
    var classTFoo = classT.getMethods().iterator().next();
    assertNotNull(classTFoo);
    var classTThis = classT.getThis();
    assertNotNull(classTThis);
    var classTReturn = classTFoo.getBodyStatementAs(0, ReturnStatement.class);
    assertNotNull(classTReturn);
    var classTReturnMember = (MemberExpression) classTReturn.getReturnValue();
    assertNotNull(classTReturnMember);
    var classTThisExpression = (DeclaredReferenceExpression) classTReturnMember.getBase();
    assertEquals(classTThisExpression.getRefersTo(), classTThis);

    var classS = tu.getDeclarationsByName("S", RecordDeclaration.class).iterator().next();
    assertNotNull(classS);
    var classSFoo = classS.getMethods().iterator().next();
    assertNotNull(classSFoo);
    var classSThis = classS.getThis();
    assertNotNull(classSThis);
    var classSReturn = classSFoo.getBodyStatementAs(0, ReturnStatement.class);
    assertNotNull(classSReturn);
    var classSReturnMember = (MemberExpression) classSReturn.getReturnValue();
    assertNotNull(classSReturnMember);
    var classSThisExpression = (DeclaredReferenceExpression) classSReturnMember.getBase();
    assertEquals(classSThisExpression.getRefersTo(), classSThis);

    assertNotEquals(classTFoo, classSFoo);
    assertNotEquals(classTThis, classSThis);
  }
}
