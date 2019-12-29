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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.ArrayCreationExpression;
import de.fraunhofer.aisec.cpg.graph.ArraySubscriptionExpression;
import de.fraunhofer.aisec.cpg.graph.CaseStatement;
import de.fraunhofer.aisec.cpg.graph.CastExpression;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.DefaultStatement;
import de.fraunhofer.aisec.cpg.graph.FieldDeclaration;
import de.fraunhofer.aisec.cpg.graph.InitializerListExpression;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JavaLanguageFrontendTest {

  private TranslationConfiguration config;

  @BeforeEach
  void setUp() {
    config = TranslationConfiguration.builder().build();
  }

  @Test
  void testRecordDeclaration() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config)
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

    assertEquals("method", method.getName());
    assertEquals("java.lang.Integer", method.getType().toString());

    ConstructorDeclaration constructor = recordDeclaration.getConstructors().get(0);

    assertEquals("SimpleClass", constructor.getName());
  }

  @Test
  void testVariables() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config)
            .parse(new File("src/test/resources/compiling/Variables.java"));

    assertNotNull(declaration);
  }

  @Test
  void testNameExpressions() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config)
            .parse(new File("src/test/resources/compiling/NameExpression.java"));

    assertNotNull(declaration);
  }

  @Test
  void testSwitch() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(TranslationConfiguration.builder().build())
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
        new JavaLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/cast/Cast.java"));

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
    assertEquals(e, ref.getRefersTo());
  }

  @Test
  void testArrays() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(TranslationConfiguration.builder().build())
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
    assertEquals(a, ((DeclaredReferenceExpression) ase.getArrayExpression()).getRefersTo());
    assertEquals(0, ((Literal<Integer>) ase.getSubscriptExpression()).getValue().intValue());
  }

  @Test
  void testFieldAccessExpressions() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(TranslationConfiguration.builder().build())
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
}
