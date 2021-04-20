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

package de.fraunhofer.aisec.cpg.frontends.java;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.statements.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.processing.IVisitor;
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JavaLanguageFrontendTest extends BaseTest {

  @Test
  void testLargeNegativeNumber() throws Exception {
    File file = new File("src/test/resources/LargeNegativeNumber.java");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);
    RecordDeclaration declaration = (RecordDeclaration) tu.getDeclarations().get(0);

    MethodDeclaration main = declaration.getMethods().get(0);

    VariableDeclaration a = main.getVariableDeclarationByName("a").orElse(null);
    assertNotNull(a);
    assertEquals(
        1,
        ((Literal) Objects.requireNonNull(a.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration b = main.getVariableDeclarationByName("b").orElse(null);
    assertNotNull(b);
    assertEquals(
        2147483648L,
        ((Literal) Objects.requireNonNull(b.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration c = main.getVariableDeclarationByName("c").orElse(null);
    assertNotNull(c);
    assertEquals(
        new BigInteger("9223372036854775808"),
        ((Literal) Objects.requireNonNull(c.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration d = main.getVariableDeclarationByName("d").orElse(null);
    assertNotNull(d);
    assertEquals(
        9223372036854775807L,
        ((Literal) Objects.requireNonNull(d.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());
  }

  @Test
  void testFor() throws Exception {
    File file = new File("src/test/resources/components/ForStmt.java");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    RecordDeclaration declaration = (RecordDeclaration) tu.getDeclarations().get(0);

    MethodDeclaration main = declaration.getMethods().get(0);

    VariableDeclaration ls = main.getVariableDeclarationByName("ls").orElse(null);
    assertNotNull(ls);

    ForStatement forStatement = main.getBodyStatementAs(2, ForStatement.class);
    assertNotNull(forStatement);

    // initializer is an expression list
    ExpressionList list = (ExpressionList) forStatement.getInitializerStatement();
    assertNotNull(list);

    // check calculated location of sub-expression
    PhysicalLocation location = list.getLocation();
    assertNotNull(location);

    assertEquals(new Region(9, 10, 9, 22), location.getRegion());
  }

  @Test
  void testForeach() throws Exception {
    File file = new File("src/test/resources/components/ForEachStmt.java");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    RecordDeclaration declaration = (RecordDeclaration) tu.getDeclarations().get(0);

    MethodDeclaration main = declaration.getMethods().get(0);

    VariableDeclaration ls = main.getVariableDeclarationByName("ls").orElse(null);
    assertNotNull(ls);

    ForEachStatement forEachStatement = main.getBodyStatementAs(1, ForEachStatement.class);
    assertNotNull(forEachStatement);

    // should loop over ls
    assertEquals(ls, ((DeclaredReferenceExpression) forEachStatement.getIterable()).getRefersTo());

    // should declare String s
    VariableDeclaration s = (VariableDeclaration) forEachStatement.getVariable();
    assertNotNull(s);
    assertEquals("s", s.getName());
    assertEquals(TypeParser.createFrom("java.lang.String", true), s.getType());

    // should contain a single statement
    var sce = (MemberCallExpression) forEachStatement.getStatement();
    assertNotNull(sce);
    assertEquals("println", sce.getName());
    assertEquals("java.io.PrintStream.println", sce.getFqn());
  }

  @Test
  void testTryCatch() throws Exception {
    File file = new File("src/test/resources/components/TryStmt.java");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    RecordDeclaration declaration = (RecordDeclaration) tu.getDeclarations().get(0);

    MethodDeclaration main = declaration.getMethods().get(0);

    // lets get our try statement
    TryStatement tryStatement = main.getBodyStatementAs(0, TryStatement.class);
    assertNotNull(tryStatement);

    // should have 3 catch clauses
    List<CatchClause> catchClauses = tryStatement.getCatchClauses();
    assertEquals(3, catchClauses.size());
    // first exception type was resolved, so we can expect a FQN
    assertEquals(
        TypeParser.createFrom("java.lang.NumberFormatException", true),
        Objects.requireNonNull(catchClauses.get(0).getParameter()).getType());
    // second one could not be resolved so we do not have an FQN
    assertEquals(
        TypeParser.createFrom("NotResolvableTypeException", true),
        Objects.requireNonNull(catchClauses.get(1).getParameter()).getType());
    // third type should have been resolved through the import
    assertEquals(
        TypeParser.createFrom("some.ImportedException", true),
        Objects.requireNonNull(catchClauses.get(2).getParameter()).getType());

    // and 1 finally
    CompoundStatement finallyBlock = tryStatement.getFinallyBlock();
    assertNotNull(finallyBlock);
  }

  @Test
  void testLiteral() throws Exception {
    File file = new File("src/test/resources/components/LiteralExpr.java");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    RecordDeclaration declaration = (RecordDeclaration) tu.getDeclarations().get(0);

    MethodDeclaration main = declaration.getMethods().get(0);

    // int i = 1;
    VariableDeclaration i =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(0, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(i);

    Literal literal = i.getInitializerAs(Literal.class);
    assertNotNull(literal);
    assertEquals(1, literal.getValue());

    // String s = "string";
    VariableDeclaration s =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(1, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(s);

    literal = s.getInitializerAs(Literal.class);
    assertNotNull(literal);
    assertEquals("string", literal.getValue());

    // boolean b = true;
    VariableDeclaration b =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(2, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(b);

    literal = b.getInitializerAs(Literal.class);
    assertNotNull(literal);
    assertEquals(true, literal.getValue());

    // char c = '0';
    VariableDeclaration c =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(3, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(c);

    literal = c.getInitializerAs(Literal.class);
    assertNotNull(literal);
    assertEquals('0', literal.getValue());

    // double d = 1.0;
    VariableDeclaration d =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(4, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(d);

    literal = d.getInitializerAs(Literal.class);
    assertNotNull(literal);
    assertEquals(1.0, literal.getValue());

    // long l = 1L;
    VariableDeclaration l =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(5, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(l);

    literal = l.getInitializerAs(Literal.class);
    assertNotNull(literal);
    assertEquals(1L, literal.getValue());

    // Object o = null;
    VariableDeclaration o =
        (VariableDeclaration)
            Objects.requireNonNull(main.getBodyStatementAs(6, DeclarationStatement.class))
                .getSingleDeclaration();
    assertNotNull(o);

    literal = o.getInitializerAs(Literal.class);
    assertNotNull(literal);
    assertNull(literal.getValue());
  }

  @Test
  void testRecordDeclaration() throws Exception {
    File file = new File("src/test/resources/compiling/RecordDeclaration.java");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(declaration);
    NamespaceDeclaration namespaceDeclaration =
        declaration.getDeclarationAs(0, NamespaceDeclaration.class);
    RecordDeclaration recordDeclaration =
        namespaceDeclaration.getDeclarationAs(0, RecordDeclaration.class);

    List<String> fields =
        recordDeclaration.getFields().stream()
            .map(FieldDeclaration::getName)
            .collect(Collectors.toList());

    assertTrue(fields.contains("this"));
    assertTrue(fields.contains("field"));

    MethodDeclaration method = recordDeclaration.getMethods().get(0);
    assertEquals(recordDeclaration, method.getRecordDeclaration());
    assertEquals("method", method.getName());
    assertEquals(TypeParser.createFrom("java.lang.Integer", true), method.getType());

    ConstructorDeclaration constructor = recordDeclaration.getConstructors().get(0);
    assertEquals(recordDeclaration, constructor.getRecordDeclaration());
    assertEquals("SimpleClass", constructor.getName());
  }

  @Test
  void testNameExpressions() throws Exception {
    File file = new File("src/test/resources/compiling/NameExpression.java");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(declaration);
  }

  @Test
  void testSwitch() throws Exception {
    File file = new File("src/test/resources/cfg/Switch.java");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    List<Node> graphNodes = SubgraphWalker.flattenAST(declaration);
    graphNodes.sort(new NodeComparator());
    assertTrue(graphNodes.size() != 0);

    List<SwitchStatement> switchStatements = Util.filterCast(graphNodes, SwitchStatement.class);
    assertEquals(3, switchStatements.size());

    SwitchStatement switchStatement = switchStatements.get(0);

    assertEquals(11, ((CompoundStatement) switchStatement.getStatement()).getStatements().size());

    List<CaseStatement> caseStatements =
        Util.filterCast(SubgraphWalker.flattenAST(switchStatement), CaseStatement.class);
    assertEquals(4, caseStatements.size());

    List<DefaultStatement> defaultStatements =
        Util.filterCast(SubgraphWalker.flattenAST(switchStatement), DefaultStatement.class);
    assertEquals(1, defaultStatements.size());
  }

  @Test
  void testCast() throws Exception {
    File file = new File("src/test/resources/components/CastExpr.java");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(declaration);

    NamespaceDeclaration namespaceDeclaration =
        declaration.getDeclarationAs(0, NamespaceDeclaration.class);
    RecordDeclaration record = namespaceDeclaration.getDeclarationAs(0, RecordDeclaration.class);

    assertNotNull(record);

    MethodDeclaration main = record.getMethods().get(0);

    assertNotNull(main);

    // e = new ExtendedClass()
    DeclarationStatement stmt = main.getBodyStatementAs(0, DeclarationStatement.class);
    assertNotNull(stmt);

    VariableDeclaration e = stmt.getSingleDeclarationAs(VariableDeclaration.class);
    // This test can be simplified once we solved the issue with inconsistently used simple names
    // vs. fully qualified names.
    assertTrue(
        e.getType().getName().equals("ExtendedClass")
            || e.getType().getName().equals("cast.ExtendedClass"));

    // b = (BaseClass) e
    stmt = main.getBodyStatementAs(1, DeclarationStatement.class);
    assertNotNull(stmt);

    VariableDeclaration b = stmt.getSingleDeclarationAs(VariableDeclaration.class);
    assertTrue(
        b.getType().getName().equals("BaseClass")
            || b.getType().getName().equals("cast.BaseClass"));

    // initializer
    CastExpression cast = (CastExpression) b.getInitializer();
    assertNotNull(cast);
    assertTrue(
        cast.getType().getName().equals("BaseClass")
            || cast.getType().getName().equals("cast.BaseClass"));

    // expression itself should be a reference
    DeclaredReferenceExpression ref = (DeclaredReferenceExpression) cast.getExpression();
    assertNotNull(ref);
    assertEquals(e, ref.getRefersTo());
  }

  @Test
  void testArrays() throws Exception {
    File file = new File("src/test/resources/compiling/Arrays.java");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(declaration);

    NamespaceDeclaration namespaceDeclaration =
        declaration.getDeclarationAs(0, NamespaceDeclaration.class);
    RecordDeclaration record = namespaceDeclaration.getDeclarationAs(0, RecordDeclaration.class);

    assertNotNull(record);

    MethodDeclaration main = record.getMethods().get(0);

    assertNotNull(main);

    List<Statement> statements = ((CompoundStatement) main.getBody()).getStatements();

    VariableDeclaration a =
        (VariableDeclaration) ((DeclarationStatement) statements.get(0)).getSingleDeclaration();

    // type should be Integer[]
    assertEquals(TypeParser.createFrom("int[]", true), a.getType());

    // it has an array creation initializer
    ArrayCreationExpression ace = (ArrayCreationExpression) a.getInitializer();
    assertNotNull(ace);

    // which has a initializer list (1 entry)
    InitializerListExpression ile = ace.getInitializer();
    assertNotNull(ile);
    assertEquals(1, ile.getInitializers().size());

    // first one is an int literal
    Literal<Integer> literal = (Literal<Integer>) ile.getInitializers().get(0);
    assertEquals(1, literal.getValue().intValue());

    // next one is a declaration with array subscription
    VariableDeclaration b =
        (VariableDeclaration) ((DeclarationStatement) statements.get(1)).getSingleDeclaration();

    // initializer is array subscription
    ArraySubscriptionExpression ase = (ArraySubscriptionExpression) b.getInitializer();
    assertNotNull(ase);
    assertEquals(a, ((DeclaredReferenceExpression) ase.getArrayExpression()).getRefersTo());
    assertEquals(0, ((Literal<Integer>) ase.getSubscriptExpression()).getValue().intValue());
  }

  @Test
  void testFieldAccessExpressions() throws Exception {
    File file = new File("src/test/resources/compiling/FieldAccess.java");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(declaration);

    NamespaceDeclaration namespaceDeclaration =
        declaration.getDeclarationAs(0, NamespaceDeclaration.class);
    RecordDeclaration record = namespaceDeclaration.getDeclarationAs(0, RecordDeclaration.class);

    assertNotNull(record);

    MethodDeclaration main = record.getMethods().get(0);

    assertNotNull(main);

    List<Statement> statements = ((CompoundStatement) main.getBody()).getStatements();

    VariableDeclaration l =
        (VariableDeclaration) ((DeclarationStatement) statements.get(1)).getSingleDeclaration();

    assertEquals("l", l.getName());

    MemberExpression length = (MemberExpression) l.getInitializer();

    assertNotNull(length);
    assertEquals("length", length.getName());
    assertEquals("int", length.getType().getTypeName());
  }

  @Test
  void testMemberCallExpressions() throws Exception {
    File file = new File("src/test/resources/compiling/MemberCallExpression.java");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(tu);

    // Simply count MemberCallExpressions
    final int[] count = {0};
    tu.accept(
        Strategy::AST_FORWARD,
        new IVisitor<Node>() {
          public void visit(MemberCallExpression ex) {
            count[0]++;
          }
        });
    assertEquals(6, count[0]);
  }

  @Test
  void testLocation() throws Exception {
    File file = new File("src/test/resources/compiling/FieldAccess.java");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertNotNull(declaration);

    NamespaceDeclaration namespaceDeclaration =
        declaration.getDeclarationAs(0, NamespaceDeclaration.class);
    RecordDeclaration record = namespaceDeclaration.getDeclarationAs(0, RecordDeclaration.class);

    assertNotNull(record);

    MethodDeclaration main = record.getMethods().get(0);

    assertNotNull(main);

    PhysicalLocation location = main.getLocation();

    assertNotNull(location);

    Path path = Path.of(location.getArtifactLocation().getUri());
    assertEquals("FieldAccess.java", path.getFileName().toString());
    assertEquals(new Region(7, 3, 10, 4), location.getRegion());
  }

  @Test
  void testAnnotations() throws Exception {
    File file = new File("src/test/resources/Annotation.java");
    List<TranslationUnitDeclaration> declarations =
        TestUtils.analyzeWithBuilder(
            TranslationConfiguration.builder()
                .sourceLocations(List.of(file))
                .topLevel(file.getParentFile())
                .defaultPasses()
                .defaultLanguages()
                .processAnnotations(true));
    assertFalse(declarations.isEmpty());

    TranslationUnitDeclaration tu = declarations.get(0);
    assertNotNull(tu);

    var record = tu.getDeclarationAs(0, RecordDeclaration.class);
    assertNotNull(record);

    var annotations = record.getAnnotations();
    assertEquals(1, annotations.size());

    var forClass = annotations.get(0);
    assertEquals("AnnotationForClass", forClass.getName());

    var value = forClass.getMembers().get(0);
    assertEquals("value", value.getName());
    assertEquals(2, ((Literal<Integer>) value.getValue()).getValue());

    var field = record.getField("field");
    assertNotNull(field);

    annotations = field.getAnnotations();
    assertEquals(1, annotations.size());

    var forField = annotations.get(0);
    assertEquals("AnnotatedField", forField.getName());

    field = record.getField("anotherField");
    assertNotNull(field);

    annotations = field.getAnnotations();
    assertEquals(1, annotations.size());

    forField = annotations.get(0);
    assertEquals("AnnotatedField", forField.getName());

    value = forField.getMembers().get(0);
    assertEquals(JavaLanguageFrontend.ANNOTATION_MEMBER_VALUE, value.getName());
    assertEquals("myString", ((Literal<String>) value.getValue()).getValue());
  }

  @Test
  void testChainedCalls() throws Exception {
    var file = new File("src/test/resources/Issue285.java");
    var tu = TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);
    var record = (RecordDeclaration) tu.getDeclarationAs(0, RecordDeclaration.class);
    assertNotNull(record);

    var func = record.getMethods().stream().findFirst().orElse(null);
    assertNotNull(func);
    assertNotNull(func.getReceiver());

    // make sure, that the type system correctly cleans up these duplicate types
    assertSame(record.getThis().getType(), func.getReceiver().getType());

    var nodes = SubgraphWalker.flattenAST(record);

    var request =
        nodes.stream()
            .filter(
                node ->
                    node instanceof VariableDeclaration
                        && Objects.equals("request", node.getName()))
            .map(node -> (VariableDeclaration) node)
            .findFirst()
            .orElse(null);
    assertNotNull(request);

    var initializer = request.getInitializer();
    assertNotNull(initializer);

    assertTrue(initializer instanceof MemberCallExpression);
    var call = (MemberCallExpression) initializer;
    assertEquals("get", call.getName());

    var staticCall =
        nodes.stream()
            .filter(node -> node instanceof StaticCallExpression)
            .map(node -> (StaticCallExpression) node)
            .findFirst()
            .orElse(null);
    assertNotNull(staticCall);

    assertEquals("doSomethingStatic", staticCall.getName());
  }

  @Test
  void testSuperFieldUsage() throws Exception {
    var file1 = new File("src/test/resources/fix-328/Cat.java");
    var file2 = new File("src/test/resources/fix-328/Animal.java");
    var tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file1, file2), file1.getParentFile().toPath(), true);
    var namespace = tu.getDeclarationAs(0, NamespaceDeclaration.class);

    assertNotNull(namespace);

    var record = namespace.getDeclarationAs(0, RecordDeclaration.class);

    assertNotNull(record);

    var constructor = record.getConstructors().get(0);
    var op = constructor.getBodyStatementAs(0, BinaryOperator.class);

    assertNotNull(op);

    var lhs = (MemberExpression) op.getLhs();

    var superThisField =
        (FieldDeclaration) ((DeclaredReferenceExpression) lhs.getBase()).getRefersTo();

    assertNotNull(superThisField);
    assertEquals("this", superThisField.getName());
    assertEquals(TypeParser.createFrom("my.Animal", false), superThisField.getType());
  }
}
