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

import static de.fraunhofer.aisec.cpg.TestUtils.getByLineNr;
import static de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.locationLink;
import static org.junit.jupiter.api.Assertions.assertSame;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.passes.ControlFlowGraphPass;
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Tests correctness of CFG for C/CPP files.
 *
 * @author julian
 */
class CXXCfgTest extends BaseTest {

  @Test
  void testCfg() throws TranslationException, InterruptedException, ExecutionException {

    TranslationManager analyzer =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .sourceLocations(new File("src/test/resources/cfg.cpp"))
                    .registerPass(new ControlFlowGraphPass()) // creates CFG
                    .registerPass(new EvaluationOrderGraphPass()) // creates EOG
                    .debugParser(true)
                    .build())
            .build();
    TranslationResult res = analyzer.analyze().get();

    TranslationUnitDeclaration tu = res.getTranslationUnits().get(0);
    FunctionDeclaration f = (FunctionDeclaration) tu.getDeclarations().get(0);
    CompoundStatement body = (CompoundStatement) f.getBody();

    // Just for debugging
    SubgraphWalker.visit(
        body,
        (stmt) -> {
          for (Node target : stmt.getNextCFG()) {
            System.out.println(
                "CFG: "
                    + locationLink(stmt.getLocation())
                    + " -> "
                    + locationLink(target.getLocation()));
          }
        });

    /*
    CFG: 4 -> 5
    CFG: 5 -> 6
    CFG: 6 -> 7
    CFG: 6 -> 11
    CFG: 7 -> 8
    CFG: 11 -> 12
    */
    assertSame(
        5, getByLineNr(body, 4).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    Node lineFive = getByLineNr(body, 5);
    List<Node> lineSix = lineFive.getNextCFG();
    System.out.println(lineSix.size());

    assertSame(
        6, getByLineNr(body, 5).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        7, getByLineNr(body, 6).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        11, getByLineNr(body, 6).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 7).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        12, getByLineNr(body, 11).getNextCFG().get(0).getLocation().getRegion().getStartLine());
  }

  /**
   * Tests do-while and while-do loops.
   *
   * @throws TranslationException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Test
  void testLoops() throws TranslationException, InterruptedException, ExecutionException {

    TranslationManager analyzer =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .sourceLocations(new File("src/test/resources/cfg/loopscfg.cpp"))
                    .registerPass(new ControlFlowGraphPass()) // creates CFG
                    .debugParser(true)
                    .build())
            .build();
    TranslationResult res = analyzer.analyze().get();

    TranslationUnitDeclaration tu = res.getTranslationUnits().get(0);
    FunctionDeclaration f = (FunctionDeclaration) tu.getDeclarations().get(0);
    CompoundStatement body = (CompoundStatement) f.getBody();

    // Just for debugging
    SubgraphWalker.visit(
        body,
        (stmt) -> {
          for (Node target : stmt.getNextCFG()) {
            System.out.println(
                "CFG: "
                    + locationLink(stmt.getLocation())
                    + " -> "
                    + locationLink(target.getLocation()));
          }
        });

    /*
    CFG: 4 -> 5
    CFG: 5 -> 7
    CFG: 7 -> 8
    CFG: 8 -> 10
    CFG: 10 -> 12
    CFG: 12 -> 10
    CFG: 12 -> 14
    CFG: 14 -> 15
    CFG: 15 -> 17
    CFG: 17 -> 19
    CFG: 19 -> 17
    CFG: 19 -> 20
       */
    assertSame(
        5, getByLineNr(body, 4).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        7, getByLineNr(body, 5).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 7).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        10, getByLineNr(body, 8).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        15, getByLineNr(body, 14).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        17, getByLineNr(body, 15).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        19, getByLineNr(body, 17).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        17, getByLineNr(body, 19).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        20, getByLineNr(body, 19).getNextCFG().get(1).getLocation().getRegion().getStartLine());
  }

  /**
   * Tests If-statements. s
   *
   * @throws TranslationException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Test
  void testIf() throws TranslationException, InterruptedException, ExecutionException {

    TranslationManager analyzer =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .sourceLocations(new File("src/test/resources/cfg/if.cpp"))
                    .registerPass(new ControlFlowGraphPass()) // creates CFG
                    .debugParser(true)
                    .build())
            .build();
    TranslationResult res = analyzer.analyze().get();

    TranslationUnitDeclaration tu = res.getTranslationUnits().get(0);
    FunctionDeclaration f = (FunctionDeclaration) tu.getDeclarations().get(0);
    CompoundStatement body = (CompoundStatement) f.getBody();

    // Just for debugging
    SubgraphWalker.visit(
        body,
        (stmt) -> {
          for (Node target : stmt.getNextCFG()) {
            System.out.println(
                "CFG: "
                    + locationLink(stmt.getLocation())
                    + " -> "
                    + locationLink(target.getLocation()));
          }
        });

    /*
    CFG: 4 -> 5
    CFG: 5 -> 6
    CFG: 6 -> 7
    CFG: 6 -> 8
    CFG: 7 -> 8
    CFG: 8 -> 9
    CFG: 9 -> 10
    CFG: 9 -> 12
    CFG: 10 -> 13
    CFG: 12 -> 13
    */
    assertSame(
        5, getByLineNr(body, 4).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        6, getByLineNr(body, 5).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        7, getByLineNr(body, 6).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 6).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 7).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        9, getByLineNr(body, 8).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        10, getByLineNr(body, 9).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        12, getByLineNr(body, 9).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 10).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 12).getNextCFG().get(0).getLocation().getRegion().getStartLine());
  }

  /**
   * Tests break and continue statements.
   *
   * @throws TranslationException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Test
  void testBreak() throws TranslationException, InterruptedException, ExecutionException {

    TranslationManager analyzer =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .sourceLocations(new File("src/test/resources/cfg/break_continue.cpp"))
                    .registerPass(new ControlFlowGraphPass()) // creates CFG
                    .debugParser(true)
                    .build())
            .build();
    TranslationResult res = analyzer.analyze().get();

    TranslationUnitDeclaration tu = res.getTranslationUnits().get(0);
    FunctionDeclaration f = (FunctionDeclaration) tu.getDeclarations().get(0);
    CompoundStatement body = (CompoundStatement) f.getBody();

    // Just for debugging
    SubgraphWalker.visit(
        body,
        (stmt) -> {
          for (Node target : stmt.getNextCFG()) {
            System.out.println(
                "CFG: "
                    + locationLink(stmt.getLocation())
                    + " -> "
                    + locationLink(target.getLocation()));
          }
        });

    /*
    CFG: 4 -> 5
    CFG: 5 -> 6
    CFG: 6 -> 7
    CFG: 7 -> 7
    CFG: 7 -> 8
    CFG: 7 -> 7
    CFG: 8 -> 8
    CFG: 8 -> 9
    CFG: 8 -> 11
    CFG: 9 -> 6
    CFG: 9 -> 11
    CFG: 11 -> 12
    CFG: 12 -> 13
    CFG: 13 -> 13
    CFG: 13 -> 14
    CFG: 13 -> 20
    CFG: 14 -> 15
    CFG: 14 -> 18
    CFG: 16 -> 13
    CFG: 15 -> 16
    CFG: 18 -> 19
    CFG: 19 -> 13
    CFG: 19 -> 20
        */
    assertSame(
        5, getByLineNr(body, 4).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        6, getByLineNr(body, 5).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        7, getByLineNr(body, 6).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        7, getByLineNr(body, 7).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        8, getByLineNr(body, 7).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    // Returns only first stmt in line
    assertSame(
        8, getByLineNr(body, 8).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        9, getByLineNr(body, 8).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        6, getByLineNr(body, 9).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        12, getByLineNr(body, 11).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 12).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 13).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        14, getByLineNr(body, 13).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    // Returns only first stmt in line
    assertSame(
        15, getByLineNr(body, 14).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        18, getByLineNr(body, 14).getNextCFG().get(1).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 16).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        16, getByLineNr(body, 15).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        19, getByLineNr(body, 18).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        13, getByLineNr(body, 19).getNextCFG().get(0).getLocation().getRegion().getStartLine());
    assertSame(
        20, getByLineNr(body, 19).getNextCFG().get(1).getLocation().getRegion().getStartLine());
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
