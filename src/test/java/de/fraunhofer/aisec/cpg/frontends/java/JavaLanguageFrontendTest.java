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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.ArrayCreationExpression;
import de.fraunhofer.aisec.cpg.graph.ArraySubscriptionExpression;
import de.fraunhofer.aisec.cpg.graph.CaseStatement;
import de.fraunhofer.aisec.cpg.graph.CastExpression;
import de.fraunhofer.aisec.cpg.graph.CatchClause;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.DefaultStatement;
import de.fraunhofer.aisec.cpg.graph.ExpressionList;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.ForEachStatement;
import de.fraunhofer.aisec.cpg.graph.ForStatement;
import de.fraunhofer.aisec.cpg.graph.InitializerListExpression;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.StaticCallExpression;
import de.fraunhofer.aisec.cpg.graph.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.TryStatement;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.UnaryOperator;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JavaLanguageFrontendTest {

  private TranslationConfiguration config;

  @BeforeEach
  void setUp() {
    config = TranslationConfiguration.builder().defaultPasses().build();
  }

  @Test
  void testLargeNegativeNumber() throws TranslationException {
    TranslationUnitDeclaration tu =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/LargeNegativeNumber.java"));
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
  void testFor() throws TranslationException {
    TranslationUnitDeclaration tu =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/components/ForStmt.java"));

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
  void testForeach() throws TranslationException {
    TranslationUnitDeclaration tu =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/components/ForEachStmt.java"));

    RecordDeclaration declaration = (RecordDeclaration) tu.getDeclarations().get(0);

    MethodDeclaration main = declaration.getMethods().get(0);

    VariableDeclaration ls = main.getVariableDeclarationByName("ls").orElse(null);
    assertNotNull(ls);

    ForEachStatement forEachStatement = main.getBodyStatementAs(1, ForEachStatement.class);
    assertNotNull(forEachStatement);

    // should loop over ls
    assertEquals(
        Set.of(ls), ((DeclaredReferenceExpression) forEachStatement.getIterable()).getRefersTo());

    // should declare String s
    VariableDeclaration s = (VariableDeclaration) forEachStatement.getVariable();
    assertNotNull(s);
    assertEquals("s", s.getName());
    assertEquals(Type.createFrom("java.lang.String"), s.getType());

    // should contain a single statement
    StaticCallExpression sce = (StaticCallExpression) forEachStatement.getStatement();
    assertNotNull(sce);
    assertEquals("println", sce.getName());
    // TODO: this FQN looks weird but it seems that we resolve it like this all over the place
    // this will fail once we chance the FQN to something real
    assertEquals("java.io.PrintStream.println", sce.getFqn());
  }

  @Test
  void testTryCatch() throws TranslationException {
    TranslationUnitDeclaration tu =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/components/TryStmt.java"));

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
        Type.createFrom("java.lang.NumberFormatException"),
        Objects.requireNonNull(catchClauses.get(0).getParameter()).getType());
    // second one could not be resolved so we do not have an FQN
    assertEquals(
        Type.createFrom("NotResolvableTypeException"),
        Objects.requireNonNull(catchClauses.get(1).getParameter()).getType());
    // third type should have been resolved through the import
    assertEquals(
        Type.createFrom("some.ImportedException"),
        Objects.requireNonNull(catchClauses.get(2).getParameter()).getType());

    // and 1 finally
    CompoundStatement finallyBlock = tryStatement.getFinallyBlock();
    assertNotNull(finallyBlock);
  }

  @Test
  void testLiteral() throws TranslationException {
    TranslationUnitDeclaration tu =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/components/LiteralExpr.java"));

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
  void testRecordDeclaration() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/compiling/RecordDeclaration.java"));

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
    assertEquals(Type.createFrom("java.lang.Integer"), method.getType());

    ConstructorDeclaration constructor = recordDeclaration.getConstructors().get(0);
    assertEquals(recordDeclaration, constructor.getRecordDeclaration());
    assertEquals("SimpleClass", constructor.getName());
  }

  @Test
  void testVariables() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/compiling/Variables.java"));

    assertNotNull(declaration);
  }

  @Test
  void testNameExpressions() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/compiling/NameExpression.java"));

    assertNotNull(declaration);
  }

  @Test
  void testSwitch() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/cfg/Switch.java"));

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
  void testCast() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/components/CastExpr.java"));

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
    assertEquals(Type.createFrom("ExtendedClass"), e.getType());

    // b = (BaseClass) e
    stmt = main.getBodyStatementAs(1, DeclarationStatement.class);
    assertNotNull(stmt);

    VariableDeclaration b = stmt.getSingleDeclarationAs(VariableDeclaration.class);
    assertEquals(Type.createFrom("BaseClass"), b.getType());

    // initializer
    CastExpression cast = (CastExpression) b.getInitializer();
    assertNotNull(cast);
    assertEquals(Type.createFrom("BaseClass"), cast.getCastType());

    // expression itself should be a reference
    DeclaredReferenceExpression ref = (DeclaredReferenceExpression) cast.getExpression();
    assertNotNull(ref);
    assertEquals(Set.of(e), ref.getRefersTo());
  }

  @Test
  void testArrays() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/compiling/Arrays.java"));

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
    assertEquals(Type.createFrom("int[]"), a.getType());

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
    assertEquals(Set.of(a), ((DeclaredReferenceExpression) ase.getArrayExpression()).getRefersTo());
    assertEquals(0, ((Literal<Integer>) ase.getSubscriptExpression()).getValue().intValue());
  }

  @Test
  void testFieldAccessExpressions() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/compiling/FieldAccess.java"));

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
    assertEquals("length", length.getMember().getName());
    assertEquals("int", length.getType().toString());
  }

  @Test
  public void testLocation() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/compiling/FieldAccess.java"));

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
}
