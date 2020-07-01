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

import static de.fraunhofer.aisec.cpg.TestUtils.getByLineNr;
import static de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.locationLink;
import static org.junit.jupiter.api.Assertions.assertSame;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.passes.ControlFlowGraphPass;
import de.fraunhofer.aisec.cpg.processing.strategy.DepthFirstNodeVisitor;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Tests correctness of CFG for Java files.
 *
 * @author julian
 */
class JavaCfgTest extends BaseTest {

  /**
   * Tests do-while and while-do loops.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Test
  void testLoops() throws InterruptedException, ExecutionException {

    TranslationManager analyzer =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .sourceLocations(new File("src/test/resources/cfg/Loops.java"))
                    .registerPass(new ControlFlowGraphPass()) // creates CFG
                    .debugParser(true)
                    .build())
            .build();
    TranslationResult res = analyzer.analyze().get();

    TranslationUnitDeclaration tu = res.getTranslationUnits().get(0);
    NamespaceDeclaration namespace = tu.getDeclarationAs(0, NamespaceDeclaration.class);
    RecordDeclaration rec = (RecordDeclaration) namespace.getDeclarations().get(0);
    CompoundStatement body = (CompoundStatement) rec.getMethods().get(0).getBody();

    // Just for debugging
    body.accept(
        new DepthFirstNodeVisitor<>(
            n -> {
              for (Node x : n.getNextCFG()) {
                System.out.println(
                    "Das ist er: "
                        + locationLink(n.getLocation())
                        + " -> "
                        + locationLink(x.getLocation()));
              }
            },
            n -> n.getNextCFG().iterator()));

    /*
    CFG: 6 -> 7
    CFG: 7 -> 8
    CFG: 8 -> 9
    CFG: 8 -> 11
    CFG: 9 -> 8
    CFG: 11 -> 12
    CFG: 12 -> 13
    CFG: 13 -> 14
    CFG: 14 -> 13
    CFG: 14 -> 15
    */
    assertSame(
        7, getByLineNr(body, 6).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 7).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        9, getByLineNr(body, 8).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        11, getByLineNr(body, 8).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 9).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        12, getByLineNr(body, 11).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 12).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        14, getByLineNr(body, 13).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 14).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        15, getByLineNr(body, 14).getNextCFG().get(1).getLocation().getRegion().getStartLine());
  }

  /**
   * Tests If-statements. s
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Test
  void testIf() throws InterruptedException, ExecutionException {

    TranslationManager analyzer =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .sourceLocations(new File("src/test/resources/cfg/If.java"))
                    .registerPass(new ControlFlowGraphPass()) // creates CFG
                    .debugParser(true)
                    .build())
            .build();
    TranslationResult res = analyzer.analyze().get();

    TranslationUnitDeclaration tu = res.getTranslationUnits().get(0);
    NamespaceDeclaration namespace = tu.getDeclarationAs(0, NamespaceDeclaration.class);
    RecordDeclaration f = (RecordDeclaration) namespace.getDeclarations().get(0);
    CompoundStatement body = (CompoundStatement) f.getMethods().get(0).getBody();

    // Just for debugging
    body.accept(
        new DepthFirstNodeVisitor<>(
            n -> {
              for (Node x : n.getNextCFG()) {
                System.out.println(
                    "CFG: "
                        + locationLink(n.getLocation())
                        + " -> "
                        + locationLink(x.getLocation()));
              }
            },
            n -> n.getNextCFG().iterator()));

    /*
    CFG: 6 -> 7
    CFG: 7 -> 8
    CFG: 8 -> 9
    CFG: 8 -> 10
    CFG: 9 -> 10
    CFG: 10 -> 11
    CFG: 11 -> 12
    CFG: 11 -> 14
    CFG: 14 -> 15
    CFG: 12 -> 15
        */
    assertSame(
        7, getByLineNr(body, 6).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 7).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        9, getByLineNr(body, 8).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        10, getByLineNr(body, 8).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        10, getByLineNr(body, 9).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        11, getByLineNr(body, 10).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        12, getByLineNr(body, 11).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        14, getByLineNr(body, 11).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        15, getByLineNr(body, 14).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        15, getByLineNr(body, 12).getNextCFG().get(0).getLocation().getRegion().getStartLine());
  }

  /**
   * Tests break and continue statements.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Test
  void testBreak() throws InterruptedException, ExecutionException {

    TranslationManager analyzer =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .sourceLocations(new File("src/test/resources/cfg/BreakContinue.java"))
                    .registerPass(new ControlFlowGraphPass()) // creates CFG
                    .debugParser(true)
                    .build())
            .build();
    TranslationResult res = analyzer.analyze().get();

    TranslationUnitDeclaration tu = res.getTranslationUnits().get(0);
    NamespaceDeclaration namespace = tu.getDeclarationAs(0, NamespaceDeclaration.class);
    RecordDeclaration f = (RecordDeclaration) namespace.getDeclarations().get(0);
    CompoundStatement body = (CompoundStatement) f.getMethods().get(0).getBody();

    // Just for debugging
    body.accept(
        new DepthFirstNodeVisitor<>(
            n -> {
              for (Node x : n.getNextCFG()) {
                System.out.println(
                    "CFG: "
                        + locationLink(n.getLocation())
                        + " -> "
                        + locationLink(x.getLocation()));
              }
            },
            n -> n.getNextCFG().iterator()));

    /*
    CFG: 6 -> 7
    CFG: 7 -> 8
    CFG: 8 -> 9
    CFG: 8 -> 13
    CFG: 9 -> 9
    CFG: 9 -> 9
    CFG: 9 -> 10
    CFG: 10 -> 10
    CFG: 10 -> 11
    CFG: 10 -> 13
    CFG: 11 -> 8
    CFG: 13 -> 14
    CFG: 14 -> 15
    CFG: 16 -> 17
    CFG: 16 -> 20
    CFG: 16 -> 20
    CFG: 17 -> 18
    CFG: 18 -> 15
    CFG: 15 -> 15
    CFG: 15 -> 16
    CFG: 15 -> 22
    CFG: 20 -> 21
    CFG: 21 -> 15
    CFG: 21 -> 22
           */
    assertSame(
        7, getByLineNr(body, 6).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 7).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        9, getByLineNr(body, 8).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 8).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        9, getByLineNr(body, 9).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        10, getByLineNr(body, 9).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        10, getByLineNr(body, 10).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        11, getByLineNr(body, 10).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 11).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        14, getByLineNr(body, 13).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        15, getByLineNr(body, 14).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        17, getByLineNr(body, 16).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        20, getByLineNr(body, 16).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        18, getByLineNr(body, 17).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        15, getByLineNr(body, 18).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        15, getByLineNr(body, 15).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        16, getByLineNr(body, 15).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        21, getByLineNr(body, 20).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        15, getByLineNr(body, 21).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        22, getByLineNr(body, 21).getNextCFG().get(1).getLocation().getRegion().getStartLine());
  }

  /**
   * Dump statement and all possibly contained statements.
   *
   * @param stmt
   */
  void visitStatements(Statement stmt, Consumer<Statement> visitor) {
    if (stmt instanceof CompoundStatement) {
      for (Statement s : ((CompoundStatement) stmt).getStatements()) {
        visitStatements(s, visitor);
      }
    } else if (stmt instanceof IfStatement) {
      visitor.accept(stmt);

      visitStatements(((IfStatement) stmt).getThenStatement(), visitor);
      visitStatements(((IfStatement) stmt).getElseStatement(), visitor);
    } else {
      visitor.accept(stmt);
    }
  }
}
