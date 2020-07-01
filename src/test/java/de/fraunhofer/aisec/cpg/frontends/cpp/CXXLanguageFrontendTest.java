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

package de.fraunhofer.aisec.cpg.frontends.cpp;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.graph.type.UnknownType;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CXXLanguageFrontendTest extends BaseTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CXXLanguageFrontendTest.class);

  @Test
  void testForEach() throws Exception {
    File file = new File("src/test/resources/components/foreachstmt.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    VariableDeclaration ls = main.getVariableDeclarationByName("ls").orElse(null);
    assertNotNull(ls);
    assertEquals(TypeParser.createFrom("std::vector<int>", true), ls.getType());
    assertEquals("ls", ls.getName());

    ForEachStatement forEachStatement = main.getBodyStatementAs(1, ForEachStatement.class);
    assertNotNull(forEachStatement);

    // should loop over ls
    assertEquals(
        Set.of(ls), ((DeclaredReferenceExpression) forEachStatement.getIterable()).getRefersTo());

    // should declare auto i (so far no concrete type inferrable)
    VariableDeclaration i = (VariableDeclaration) forEachStatement.getVariable();
    assertNotNull(i);
    assertEquals("i", i.getName());
    assertEquals(UnknownType.getUnknownType(), i.getType());
  }

  @Test
  void testTryCatch() throws Exception {
    File file = new File("src/test/resources/components/trystmt.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    TryStatement tryStatement = main.getBodyStatementAs(0, TryStatement.class);
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

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    VariableDeclaration i = main.getVariableDeclarationByName("i").orElse(null);
    assertNotNull(i);

    TypeIdExpression sizeof = (TypeIdExpression) i.getInitializer();
    assertNotNull(sizeof);
    assertEquals("sizeof", sizeof.getName());
    assertEquals(TypeParser.createFrom("std::size_t", true), sizeof.getType());

    VariableDeclaration typeInfo = main.getVariableDeclarationByName("typeInfo").orElse(null);
    assertNotNull(typeInfo);

    TypeIdExpression typeid = (TypeIdExpression) typeInfo.getInitializer();
    assertNotNull(typeid);
    assertEquals("typeid", typeid.getName());
    assertEquals(TypeParser.createFrom("const std::type_info&", true), typeid.getType());

    VariableDeclaration j = main.getVariableDeclarationByName("j").orElse(null);
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

    assertEquals(Set.of(x), ((DeclaredReferenceExpression) ase.getArrayExpression()).getRefersTo());
    assertEquals(0, ((Literal<Integer>) ase.getSubscriptExpression()).getValue().intValue());
  }

  @Test
  void testFunctionDeclaration() throws Exception {
    File file = new File("src/test/resources/functiondecl.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    // should be four method nodes
    assertEquals(4, declaration.getDeclarations().size());

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

    List<Declaration> twoDeclarations =
        ((DeclarationStatement) statements.get(2)).getDeclarations();

    assertEquals(2, twoDeclarations.size());

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
    assertEquals("ptr", pointerWithAssign.getName());
    assertEquals("NULL", pointerWithAssign.getInitializer().getName());
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
      assertEquals(Set.of(b), ((DeclaredReferenceExpression) expression).getRefersTo());
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
    Expression input = (Expression) postfix.getInput();

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

    assertEquals("SomeClass", recordDeclaration.getName());
    assertEquals("class", recordDeclaration.getKind());
    assertEquals(2, recordDeclaration.getFields().size());

    FieldDeclaration field =
        recordDeclaration.getFields().stream()
            .filter(f -> f.getName().equals("field"))
            .findFirst()
            .get();

    assertEquals(TypeParser.createFrom("void*", true), field.getType());

    assertEquals(2, recordDeclaration.getMethods().size());

    MethodDeclaration method = recordDeclaration.getMethods().get(0);

    assertEquals("method", method.getName());
    assertEquals(TypeParser.createFrom("void*", true), method.getType());
    assertFalse(method.hasBody());

    MethodDeclaration inlineMethod = recordDeclaration.getMethods().get(1);

    assertEquals("inlineMethod", inlineMethod.getName());
    assertEquals(TypeParser.createFrom("void*", true), inlineMethod.getType());
    assertTrue(inlineMethod.hasBody());

    ConstructorDeclaration constructor = recordDeclaration.getConstructors().get(0);

    assertEquals(recordDeclaration.getName(), constructor.getName());
    assertEquals(TypeParser.createFrom("SomeClass", true), constructor.getType());
    assertTrue(constructor.hasBody());
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

    // type should be Integer
    assertEquals(TypeParser.createFrom("Integer", true), m.getType());

    // initializer should be a new expression
    NewExpression newExpression = (NewExpression) m.getInitializer();

    // type of the new expression should also be Integer
    assertEquals(TypeParser.createFrom("Integer", true), newExpression.getType());

    // initializer should be a construct expression
    constructExpression = (ConstructExpression) newExpression.getInitializer();

    // type of the construct expression should also be Integer
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
    assertEquals("y", ((DeclaredReferenceExpression) die.getLhs().get(0)).getName());
    assertEquals(0, ((Literal) die.getRhs()).getValue());

    die =
        (DesignatedInitializerExpression)
            ((InitializerListExpression) initializer).getInitializers().get(1);
    assertTrue(die.getLhs().get(0) instanceof DeclaredReferenceExpression);
    assertTrue(die.getRhs() instanceof Literal);
    assertEquals("z", ((DeclaredReferenceExpression) die.getLhs().get(0)).getName());
    assertEquals(1, ((Literal) die.getRhs()).getValue());

    die =
        (DesignatedInitializerExpression)
            ((InitializerListExpression) initializer).getInitializers().get(2);
    assertTrue(die.getLhs().get(0) instanceof DeclaredReferenceExpression);
    assertTrue(die.getRhs() instanceof Literal);
    assertEquals("x", ((DeclaredReferenceExpression) die.getLhs().get(0)).getName());
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
    assertEquals("x", ((DeclaredReferenceExpression) die.getLhs().get(0)).getName());
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

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    PhysicalLocation location = main.getLocation();

    assertNotNull(location);

    Path path = Path.of(location.getArtifactLocation().getUri());
    assertEquals("foreachstmt.cpp", path.getFileName().toString());
    assertEquals(new Region(4, 1, 8, 2), location.getRegion());
  }
}
