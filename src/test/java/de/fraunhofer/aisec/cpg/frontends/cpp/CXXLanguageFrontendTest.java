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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.ArraySubscriptionExpression;
import de.fraunhofer.aisec.cpg.graph.BinaryOperator;
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.CaseStatement;
import de.fraunhofer.aisec.cpg.graph.CastExpression;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.DefaultStatement;
import de.fraunhofer.aisec.cpg.graph.DesignatedInitializerExpression;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.InitializerListExpression;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.MemberCallExpression;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.NewExpression;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.Region;
import de.fraunhofer.aisec.cpg.graph.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.TypeIdExpression;
import de.fraunhofer.aisec.cpg.graph.UnaryOperator;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CXXLanguageFrontendTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CXXLanguageFrontendTest.class);

  @Test
  void testTypeId() throws TranslationException {
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/typeidexpr.cpp"));

    FunctionDeclaration main =
        tu.getDeclarations().stream()
            .filter(
                function ->
                    function instanceof FunctionDeclaration
                        && Objects.equals("main", function.getName()))
            .map(FunctionDeclaration.class::cast)
            .findAny()
            .orElse(null);
    assertNotNull(main);

    VariableDeclaration i = main.getVariableDeclarationByName("i").orElse(null);
    assertNotNull(i);

    TypeIdExpression sizeof = (TypeIdExpression) i.getInitializer();
    assertNotNull(sizeof);
    assertEquals("sizeof", sizeof.getName());
    assertEquals(Type.createFrom("std::size_t"), sizeof.getType());

    VariableDeclaration typeInfo = main.getVariableDeclarationByName("typeInfo").orElse(null);
    assertNotNull(typeInfo);

    TypeIdExpression typeid = (TypeIdExpression) typeInfo.getInitializer();
    assertNotNull(typeid);
    assertEquals("typeid", typeid.getName());
    assertEquals(Type.createFrom("const std::type_info&"), typeid.getType());

    VariableDeclaration j = main.getVariableDeclarationByName("j").orElse(null);
    assertNotNull(j);

    TypeIdExpression alignof = (TypeIdExpression) j.getInitializer();
    assertNotNull(sizeof);
    assertEquals("alignof", alignof.getName());
    assertEquals(Type.createFrom("std::size_t"), alignof.getType());
  }

  @Test
  void testCast() throws TranslationException {
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/components/castexpr.cpp"));

    FunctionDeclaration main = tu.getDeclarationAs(0, FunctionDeclaration.class);

    VariableDeclaration e =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(0, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(e);
    assertEquals(Type.createFrom("ExtendedClass*"), e.getType());

    VariableDeclaration b =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(1, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(b);
    assertEquals(Type.createFrom("BaseClass*"), b.getType());

    // initializer
    CastExpression cast = (CastExpression) b.getInitializer();
    assertNotNull(cast);
    assertEquals(Type.createFrom("BaseClass*"), cast.getCastType());

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
    assertEquals(Type.createFrom("int"), cast.getCastType());
  }

  @Test
  void testArrays() throws TranslationException {
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/arrays.cpp"));

    FunctionDeclaration main = tu.getDeclarationAs(0, FunctionDeclaration.class);

    CompoundStatement statement = (CompoundStatement) main.getBody();

    // first statement is the variable declaration
    VariableDeclaration x =
        (VariableDeclaration)
            ((DeclarationStatement) statement.getStatements().get(0)).getSingleDeclaration();
    assertNotNull(x);
    assertEquals(Type.createFrom("int[]"), x.getType());

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
  void testFunctionDeclaration() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/functiondecl.cpp"));

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

    method = declaration.getDeclarationAs(3, FunctionDeclaration.class);

    assertEquals("function2()void*", method.getSignature());
  }

  @Test
  void testCompoundStatement() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/compoundstmt.cpp"));

    FunctionDeclaration function = declaration.getDeclarationAs(0, FunctionDeclaration.class);

    assertNotNull(function);

    Statement functionBody = function.getBody();

    assertNotNull(functionBody);

    List<Statement> statements = ((CompoundStatement) functionBody).getStatements();

    assertEquals(2, statements.size());

    ReturnStatement returnStatement = (ReturnStatement) statements.get(0);

    assertNotNull(returnStatement);

    Expression returnValue = returnStatement.getReturnValue();

    assertTrue(returnValue instanceof Literal);

    assertEquals(1, ((Literal) returnValue).getValue());
  }

  @Test
  void testPostfixExpression() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/postfixexpression.cpp"));

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
  void testIf() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/if.cpp"));

    List<Statement> statements =
        getStatementsOfFunction(declaration.getDeclarationAs(0, FunctionDeclaration.class));

    IfStatement ifStatement = (IfStatement) statements.get(0);

    assertNotNull(ifStatement);
    assertNotNull(ifStatement.getCondition());

    assertEquals("bool", ifStatement.getCondition().getType().toString());
    assertEquals(true, ((Literal) ifStatement.getCondition()).getValue());

    assertTrue(
        ((CompoundStatement) ifStatement.getThenStatement()).getStatements().get(0)
            instanceof ReturnStatement);
    assertTrue(
        ((CompoundStatement) ifStatement.getElseStatement()).getStatements().get(0)
            instanceof ReturnStatement);
  }

  @Test
  void testSwitch() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/cfg/switch.cpp"));

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
  void testDeclarationStatement() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/declstmt.cpp"));

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

    assertEquals("SSL_CTX*", declFromMultiplicateExpression.getType().toString());
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

    assertEquals("int*", withoutInitializer.getType().toString());
    assertEquals("d", withoutInitializer.getName());

    assertNull(initializer);

    VariableDeclaration qualifiedType =
        ((DeclarationStatement) statements.get(4))
            .getSingleDeclarationAs(VariableDeclaration.class);

    assertEquals("std.string", qualifiedType.getType().toString());
    assertEquals("text", qualifiedType.getName());
    assertTrue(qualifiedType.getInitializer() instanceof Literal);
    assertEquals("some text", ((Literal) qualifiedType.getInitializer()).getValue());

    VariableDeclaration pointerWithAssign =
        ((DeclarationStatement) statements.get(5))
            .getSingleDeclarationAs(VariableDeclaration.class);

    assertEquals("void*", pointerWithAssign.getType().toString());
    assertEquals("ptr", pointerWithAssign.getName());
    assertEquals("NULL", pointerWithAssign.getInitializer().getName());
  }

  @Test
  void testAssignmentExpression() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/assignmentexpression.cpp"));

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

  private boolean assertRefersTo(Expression expression, Declaration b) {
    if (expression instanceof DeclaredReferenceExpression) {
      return ((DeclaredReferenceExpression) expression).getRefersTo() == b;
    }

    return false;
  }

  @Test
  void testShiftExpression() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/shiftexpression.cpp"));

    FunctionDeclaration functionDeclaration =
        declaration.getDeclarationAs(0, FunctionDeclaration.class);

    List<Statement> statements = getStatementsOfFunction(functionDeclaration);

    assertTrue(statements.get(1) instanceof BinaryOperator);
  }

  @Test
  void testUnaryOperator() throws TranslationException {
    TranslationUnitDeclaration unit =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/unaryoperator.cpp"));

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
  void testBinaryOperator() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/binaryoperator.cpp"));

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

    assertEquals("std.string*", decl.getType().toString());
    assertEquals("notMultiplication", decl.getName());
    assertTrue(decl.getInitializer() instanceof BinaryOperator);

    operator = (BinaryOperator) decl.getInitializer();

    assertTrue(operator.getLhs() instanceof Literal);
    assertEquals(0, ((Literal) operator.getLhs()).getValue());

    assertTrue(operator.getRhs() instanceof Literal);
    assertEquals(0, ((Literal) operator.getRhs()).getValue());
  }

  @Test
  void testRecordDeclaration() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/recordstmt.cpp"));

    RecordDeclaration recordDeclaration = declaration.getDeclarationAs(0, RecordDeclaration.class);

    assertEquals("SomeClass", recordDeclaration.getName());
    assertEquals("class", recordDeclaration.getKind());
    assertEquals(2, recordDeclaration.getFields().size());

    FieldDeclaration field =
        recordDeclaration.getFields().stream()
            .filter(f -> f.getName().equals("field"))
            .findFirst()
            .get();

    assertEquals("void*", field.getType().toString());

    assertEquals(2, recordDeclaration.getMethods().size());

    MethodDeclaration method = recordDeclaration.getMethods().get(0);

    assertEquals("method", method.getName());
    assertEquals("void*", method.getType().toString());
    assertFalse(method.hasBody());

    MethodDeclaration inlineMethod = recordDeclaration.getMethods().get(1);

    assertEquals("inlineMethod", inlineMethod.getName());
    assertEquals("void*", inlineMethod.getType().toString());
    assertTrue(inlineMethod.hasBody());

    ConstructorDeclaration constructor = recordDeclaration.getConstructors().get(0);

    assertEquals(recordDeclaration.getName(), constructor.getName());
    assertEquals("void", constructor.getType().toString());
    assertTrue(constructor.hasBody());
  }

  @Test
  void testLiterals() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/literals.cpp"));

    VariableDeclaration s = declaration.getDeclarationAs(0, VariableDeclaration.class);

    assertEquals("const char[]", s.getType().toString());
    assertEquals("s", s.getName());

    Expression initializer = s.getInitializer();

    assertEquals("string", ((Literal) initializer).getValue());

    VariableDeclaration i = declaration.getDeclarationAs(1, VariableDeclaration.class);

    assertEquals("int", i.getType().toString());
    assertEquals("i", i.getName());

    initializer = i.getInitializer();

    assertEquals(1, ((Literal) initializer).getValue());

    VariableDeclaration f = declaration.getDeclarationAs(2, VariableDeclaration.class);

    assertEquals("float", f.getType().toString());
    assertEquals("f", f.getName());

    initializer = f.getInitializer();

    assertEquals(0.2f, ((Literal) initializer).getValue());

    VariableDeclaration d = declaration.getDeclarationAs(3, VariableDeclaration.class);

    assertEquals("double", d.getType().toString());
    assertEquals("d", d.getName());

    initializer = d.getInitializer();

    assertEquals(0.2d, ((Literal) initializer).getValue());

    VariableDeclaration b = declaration.getDeclarationAs(4, VariableDeclaration.class);

    assertEquals("bool", b.getType().toString());
    assertEquals("b", b.getName());

    initializer = b.getInitializer();

    assertEquals(false, ((Literal) initializer).getValue());

    VariableDeclaration c = declaration.getDeclarationAs(5, VariableDeclaration.class);

    assertEquals("char", c.getType().toString());
    assertEquals("c", c.getName());

    initializer = c.getInitializer();

    assertEquals('c', ((Literal) initializer).getValue());
  }

  @Test
  void testInitListExpression() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/initlistexpression.cpp"));

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

    assertEquals("int[]", z.getType().toString());

    initializer = z.getInitializer();

    assertNotNull(initializer);
    assertTrue(initializer instanceof InitializerListExpression);

    listExpression = (InitializerListExpression) initializer;

    assertEquals(3, listExpression.getInitializers().size());
  }

  @Test
  void testObjectCreation() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/objcreation.cpp"));

    assertNotNull(declaration);

    // get the main method
    FunctionDeclaration main = declaration.getDeclarationAs(3, FunctionDeclaration.class);
    CompoundStatement statement = (CompoundStatement) main.getBody();

    // Integer i
    VariableDeclaration i =
        (VariableDeclaration)
            ((DeclarationStatement) statement.getStatements().get(0)).getSingleDeclaration();

    // type should be Integer
    assertEquals(new Type("Integer"), i.getType());

    // initializer should be a construct expression
    ConstructExpression constructExpression = (ConstructExpression) i.getInitializer();

    // type of the construct expression should also be Integer
    assertEquals(new Type("Integer"), constructExpression.getType());

    // auto (Integer) m
    VariableDeclaration m =
        (VariableDeclaration)
            ((DeclarationStatement) statement.getStatements().get(6)).getSingleDeclaration();

    // type should be Integer
    assertEquals(new Type("Integer"), m.getType());

    // initializer should be a new expression
    NewExpression newExpression = (NewExpression) m.getInitializer();

    // type of the new expression should also be Integer
    assertEquals(new Type("Integer"), newExpression.getType());

    // initializer should be a construct expression
    constructExpression = (ConstructExpression) newExpression.getInitializer();

    // type of the construct expression should also be Integer
    assertEquals(new Type("Integer"), constructExpression.getType());

    // argument should be named k and of type m
    DeclaredReferenceExpression k =
        (DeclaredReferenceExpression) constructExpression.getArguments().get(0);
    assertEquals("k", k.getName());

    // type of the construct expression should also be Integer
    assertEquals(new Type("int"), k.getType());
  }

  List<Statement> getStatementsOfFunction(FunctionDeclaration declaration) {
    return ((CompoundStatement) declaration.getBody()).getStatements();
  }

  @Test
  void testRegionsCfg() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/cfg.cpp"));
    assertNotNull(declaration);

    FunctionDeclaration fdecl = declaration.getDeclarationAs(0, FunctionDeclaration.class);
    CompoundStatement body = (CompoundStatement) fdecl.getBody();

    Map<String, Region> expected = new HashMap<>();
    expected.put("cout << \"bla\";", new Region(4, 3, 4, 17));
    expected.put("cout << \"blubb\";", new Region(5, 3, 5, 19));
    expected.put("return 0;", new Region(15, 3, 15, 12));
    for (Node d : body.getStatements()) {
      if (expected.containsKey(d.getCode())) {
        assertEquals(expected.get(d.getCode()), d.getRegion(), d.getCode());
        expected.remove(d.getCode());
      }
    }
    assertTrue(expected.isEmpty(), String.join(", ", expected.keySet()));
  }

  @Test
  void testDesignatedInitializer() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/components/designatedInitializer.c"));

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
  void testLocalVariables() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/variables/local_variables.cpp"));

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
}
