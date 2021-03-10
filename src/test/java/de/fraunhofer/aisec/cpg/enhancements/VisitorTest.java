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

package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.processing.IVisitor;
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VisitorTest extends BaseTest {

  private static NamespaceDeclaration namespace;

  @BeforeAll
  public static void setup()
      throws TranslationException, InterruptedException, ExecutionException, TimeoutException {
    File file = new File("src/test/resources/compiling/RecordDeclaration.java");
    TranslationConfiguration config =
        TranslationConfiguration.builder().sourceLocations(file).defaultPasses().build();
    TranslationResult result =
        TranslationManager.builder().config(config).build().analyze().get(20, TimeUnit.SECONDS);
    TranslationUnitDeclaration tu = result.getTranslationUnits().get(0);
    namespace = (NamespaceDeclaration) tu.getDeclarations().get(0);
  }

  /** Visits all nodes along EOG. */
  @Test
  void testAllEogNodeVisitor() {
    List<Node> nodeList = new ArrayList<>();
    RecordDeclaration recordDeclaration = namespace.getDeclarationAs(0, RecordDeclaration.class);
    MethodDeclaration method =
        recordDeclaration.getMethods().stream()
            .filter(m -> m.getName().equals("method"))
            .collect(Collectors.toList())
            .get(0);

    /* TODO A better way to get the "first" statement in a method body is needed.
    This is currently the only (fragile, ugly and unsafe) way to get to the first "real" statement in a method body.
    getNextEOG() and getNextCFG() return empty lists.
    */
    Statement firstStmt = ((CompoundStatement) method.getBody()).getStatements().get(0);

    firstStmt.accept(
        Strategy::EOG_FORWARD,
        new IVisitor<Node>() {
          public void visit(Node n) {
            System.out.println(n);
            nodeList.add(n);
          }
        });

    assertEquals(22, nodeList.size());
  }

  /** Visits all nodes along AST. */
  @Test
  void testAllAstNodeVisitor() {
    RecordDeclaration recordDeclaration = namespace.getDeclarationAs(0, RecordDeclaration.class);

    List<Node> nodeList = new ArrayList<>();
    recordDeclaration.accept(
        Strategy::AST_FORWARD,
        new IVisitor<Node>() {
          public void visit(Node r) {
            System.out.println(r);
            nodeList.add(r);
          }
        });

    assertEquals(34, nodeList.size());
  }

  /** Visits only ReturnStatement nodes. */
  @Test
  void testReturnStmtVisitor() {
    List<ReturnStatement> returnStmts = new ArrayList<>();

    RecordDeclaration recordDeclaration = namespace.getDeclarationAs(0, RecordDeclaration.class);
    recordDeclaration.accept(
        Strategy::AST_FORWARD,
        new IVisitor<Node>() {
          public void visit(ReturnStatement r) {
            returnStmts.add(r);
          }
        });

    assertEquals(2, returnStmts.size());
  }
}
