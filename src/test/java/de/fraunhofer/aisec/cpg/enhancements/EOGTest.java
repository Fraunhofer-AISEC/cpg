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
package de.fraunhofer.aisec.cpg.enhancements;

import static de.fraunhofer.aisec.cpg.helpers.Util.Connect.NODE;
import static de.fraunhofer.aisec.cpg.helpers.Util.Connect.SUBTREE;
import static de.fraunhofer.aisec.cpg.helpers.Util.Edge.ENTRIES;
import static de.fraunhofer.aisec.cpg.helpers.Util.Edge.EXITS;
import static de.fraunhofer.aisec.cpg.helpers.Util.Quantifier.ALL;
import static de.fraunhofer.aisec.cpg.helpers.Util.Quantifier.ANY;
import static de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.locationLink;
import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass;
import de.fraunhofer.aisec.cpg.processing.IVisitor;
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.exception.TransactionException;

/**
 * Tests correct path building for EOG focusing on loops, conditions, breaks ect.
 *
 * @author konrad.weiss@aisec.fraunhofer.de
 */
class EOGTest extends BaseTest {

  public static String REFNODESTRINGJAVA = "System.out.println();";
  public static String REFNODESTRINGCXX = "printf(\"\\n\");";

  @Test
  void testJavaIf() throws Exception {
    testIf("src/test/resources/cfg/If.java", REFNODESTRINGJAVA);
  }

  @Test
  void testCppIf() throws Exception {
    testIf("src/test/resources/cfg/if.cpp", REFNODESTRINGCXX);
  }

  /**
   * Tests EOG building in the presence of if/else statements.
   *
   * @param relPath
   * @param refNodeString - Exact string of reference nodes, do not change/insert nodes in the test
   *     file.
   * @throws TranslationException
   */
  void testIf(String relPath, String refNodeString) throws Exception {
    List<Node> nodes = translateToNodes(relPath);

    // All BinaryOperators (including If conditions) have only one successor
    List<BinaryOperator> binops = Util.filterCast(nodes, BinaryOperator.class);
    for (BinaryOperator binop : binops) {
      SubgraphWalker.Border binopEOG = SubgraphWalker.getEOGPathEdges(binop);
      assertEquals(1, binopEOG.getExits().size());
    }

    List<IfStatement> ifs = Util.filterCast(nodes, IfStatement.class);

    assertEquals(2, ifs.size());
    ifs.forEach(ifnode -> Assertions.assertNotNull(ifnode.getThenStatement()));

    assertTrue(
        ifs.stream().anyMatch(node -> node.getElseStatement() == null)
            && ifs.stream().anyMatch(node -> node.getElseStatement() != null));

    IfStatement ifSimple = ifs.get(0);
    IfStatement ifBranched = ifs.get(1);

    List<Node> prints =
        nodes.stream()
            .filter(node -> node.getCode().equals(refNodeString))
            .collect(Collectors.toList());

    SubgraphWalker.Border ifEOG = SubgraphWalker.getEOGPathEdges(ifSimple);
    SubgraphWalker.Border conditionEOG = SubgraphWalker.getEOGPathEdges(ifSimple.getCondition());
    SubgraphWalker.Border thenEOG = SubgraphWalker.getEOGPathEdges(ifSimple.getThenStatement());

    // IfStmt has 2 outgoing EOG edges (for true and false branch)
    assertEquals(2, ifEOG.getExits().size());

    // Assert: Only single entry and exit NODE per block
    assertTrue(conditionEOG.getEntries().size() == 1 && conditionEOG.getExits().size() == 1);
    assertTrue(thenEOG.getEntries().size() == 1 && thenEOG.getExits().size() == 1);

    // Assert: Condition of simple if is preceded by print
    assertTrue(Util.eogConnect(ENTRIES, ifSimple.getCondition(), prints.get(0)));

    // Assert: All EOGs going into the then branch (=the 2nd print stmt) come from the IfStatement
    assertTrue(Util.eogConnect(ENTRIES, ifSimple.getThenStatement(), NODE, ifSimple));
    // Assert: The EOGs going into the second print come either from the then branch or the
    // IfStatement
    assertTrue(Util.eogConnect(NODE, EXITS, ifSimple, prints.get(1)));
    assertTrue(Util.eogConnect(NODE, EXITS, ifSimple, ifSimple.getThenStatement()));
    assertTrue(Util.eogConnect(NODE, EXITS, ifSimple.getThenStatement(), prints.get(1)));

    conditionEOG = SubgraphWalker.getEOGPathEdges(ifBranched.getCondition());
    thenEOG = SubgraphWalker.getEOGPathEdges(ifBranched.getThenStatement());
    SubgraphWalker.Border elseEOG = SubgraphWalker.getEOGPathEdges(ifBranched.getElseStatement());

    // Assert: Only single entry and exit NODE per block
    assertTrue(conditionEOG.getEntries().size() == 1 && conditionEOG.getExits().size() == 1);
    assertTrue(thenEOG.getEntries().size() == 1 && thenEOG.getExits().size() == 1);
    assertTrue(elseEOG.getEntries().size() == 1 && elseEOG.getExits().size() == 1);

    // Assert: Branched if is preceded by the second print
    assertTrue(Util.eogConnect(ENTRIES, ifBranched, prints.get(1)));

    // IfStatement has exactly 2 outgoing EOGS: true (then) and false (else) branch
    assertTrue(Util.eogConnect(NODE, EXITS, ifBranched, ifBranched.getThenStatement()));
    assertTrue(Util.eogConnect(NODE, EXITS, ifBranched, ifBranched.getElseStatement()));
    SubgraphWalker.Border ifBranchedEOG = SubgraphWalker.getEOGPathEdges(ifBranched);
    assertEquals(2, ifBranchedEOG.getExits().size());

    // Assert: EOG going into then branch comes from the condition branch
    assertTrue(Util.eogConnect(ENTRIES, ifBranched.getThenStatement(), NODE, ifBranched));

    // Assert: EOG going into else branch comes from the condition branch
    assertTrue(Util.eogConnect(ENTRIES, ifBranched.getElseStatement(), NODE, ifBranched));

    // Assert: EOG edges going into the third print either come from the then or else branch
    assertTrue(Util.eogConnect(SUBTREE, EXITS, ifBranched, prints.get(2)));
    // Assert: EOG edges going into the branch root node either come from the then or else branch
    assertTrue(Util.eogConnect(NODE, ENTRIES, ifBranched, ifBranched.getCondition()));
  }

  @Test
  void testConditionShortCircuit() throws Exception {
    List<Node> nodes = translateToNodes("src/test/resources/cfg/ShortCircuit.java");

    List<BinaryOperator> binaryOperators =
        Util.filterCast(nodes, BinaryOperator.class).stream()
            .filter(bo -> bo.getOperatorCode().equals("&&") || bo.getOperatorCode().equals("||"))
            .collect(Collectors.toList());

    for (BinaryOperator bo : binaryOperators) {
      assertTrue(Util.eogConnect(ALL, SUBTREE, EXITS, bo.getLhs(), SUBTREE, bo.getRhs()));
      assertTrue(Util.eogConnect(ALL, SUBTREE, EXITS, bo.getLhs(), NODE, bo));

      assertTrue(bo.getLhs().getNextEOG().size() == 2);
    }
  }

  @Test
  void testJavaFor() throws Exception {
    List<Node> nodes = translateToNodes("src/test/resources/cfg/ForLoop.java");
    List<Node> prints =
        nodes.stream()
            .filter(node -> node.getCode().equals(REFNODESTRINGJAVA))
            .collect(Collectors.toList());
    List<ForStatement> fstat = Util.filterCast(nodes, ForStatement.class);

    ForStatement fs = fstat.get(0);
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(0), SUBTREE, fs));
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(0), SUBTREE, fs.getInitializerStatement()));
    assertTrue(Util.eogConnect(EXITS, fs.getInitializerStatement(), SUBTREE, fs.getCondition()));

    assertTrue(Util.eogConnect(EXITS, fs.getCondition(), NODE, fs));
    assertTrue(Util.eogConnect(NODE, EXITS, fs, SUBTREE, fs.getStatement(), prints.get(1)));

    assertTrue(Util.eogConnect(EXITS, fs.getStatement(), SUBTREE, fs.getIterationExpression()));
    assertTrue(Util.eogConnect(EXITS, fs.getIterationExpression(), SUBTREE, fs.getCondition()));

    fs = fstat.get(1);
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(1), SUBTREE, fs));
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(1), SUBTREE, fs.getInitializerStatement()));
    assertTrue(Util.eogConnect(EXITS, fs.getInitializerStatement(), SUBTREE, fs.getCondition()));
    assertTrue(Util.eogConnect(EXITS, fs.getCondition(), NODE, fs));
    assertTrue(Util.eogConnect(EXITS, fs.getStatement(), SUBTREE, fs.getIterationExpression()));
    assertTrue(Util.eogConnect(EXITS, fs.getIterationExpression(), SUBTREE, fs.getCondition()));
    assertTrue(Util.eogConnect(SUBTREE, EXITS, fs, SUBTREE, prints.get(2)));
  }

  @Test
  void testCPPFor() throws Exception {
    List<Node> nodes = translateToNodes("src/test/resources/cfg/forloop.cpp");
    List<Node> prints =
        nodes.stream()
            .filter(node -> node.getCode().equals(REFNODESTRINGCXX))
            .collect(Collectors.toList());
    List<ForStatement> fstat = Util.filterCast(nodes, ForStatement.class);

    ForStatement fs = fstat.get(0);
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(0), SUBTREE, fs));
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(0), SUBTREE, fs.getInitializerStatement()));
    assertTrue(
        Util.eogConnect(
            EXITS, fs.getInitializerStatement(), SUBTREE, fs.getConditionDeclaration()));
    assertTrue(Util.eogConnect(EXITS, fs.getConditionDeclaration(), NODE, fs));
    assertTrue(Util.eogConnect(EXITS, fs.getStatement(), SUBTREE, fs.getIterationExpression()));
    assertTrue(
        Util.eogConnect(EXITS, fs.getIterationExpression(), SUBTREE, fs.getConditionDeclaration()));
    assertTrue(Util.eogConnect(SUBTREE, EXITS, fs, SUBTREE, prints.get(1)));

    fs = fstat.get(1);
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(1), SUBTREE, fs));
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(1), SUBTREE, fs.getInitializerStatement()));
    assertTrue(Util.eogConnect(EXITS, fs.getInitializerStatement(), SUBTREE, fs.getCondition()));
    assertTrue(Util.eogConnect(EXITS, fs.getCondition(), NODE, fs));
    assertTrue(Util.eogConnect(EXITS, fs.getStatement(), SUBTREE, fs.getIterationExpression()));
    assertTrue(Util.eogConnect(EXITS, fs.getIterationExpression(), SUBTREE, fs.getCondition()));
    assertTrue(Util.eogConnect(SUBTREE, EXITS, fs, SUBTREE, prints.get(2)));

    fs = fstat.get(2);
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(3), SUBTREE, fs));
    assertTrue(Util.eogConnect(NODE, EXITS, prints.get(3), SUBTREE, fs.getInitializerStatement()));
    assertTrue(Util.eogConnect(EXITS, fs.getInitializerStatement(), SUBTREE, fs.getCondition()));
    assertTrue(Util.eogConnect(EXITS, fs.getCondition(), NODE, fs));
    assertTrue(Util.eogConnect(EXITS, fs.getStatement(), SUBTREE, fs.getIterationExpression()));
    assertTrue(Util.eogConnect(EXITS, fs.getIterationExpression(), SUBTREE, fs.getCondition()));
    assertTrue(Util.eogConnect(SUBTREE, EXITS, fs, SUBTREE, prints.get(4)));
  }

  /**
   * Test function (not method) calls.
   *
   * @throws TransactionException
   */
  @Test
  void testCPPCallGraph() throws Exception {
    List<Node> nodes = translateToNodes("src/test/resources/cg.cpp");
    List<CallExpression> calls = TestUtils.subnodesOfType(nodes, CallExpression.class);
    List<FunctionDeclaration> functions =
        TestUtils.subnodesOfType(nodes, FunctionDeclaration.class);
    FunctionDeclaration target;

    CallExpression first = TestUtils.findByUniqueName(calls, "first");
    target = TestUtils.findByUniqueName(functions, "first");
    assertEquals(List.of(target), first.getInvokes());

    CallExpression second = TestUtils.findByUniqueName(calls, "second");
    target = TestUtils.findByUniqueName(functions, "second");
    assertEquals(List.of(target), second.getInvokes());

    CallExpression third = TestUtils.findByUniqueName(calls, "third");
    target =
        TestUtils.findByUniquePredicate(
            functions, f -> f.getName().equals("third") && f.getParameters().size() == 2);
    assertEquals(List.of(target), third.getInvokes());

    CallExpression fourth = TestUtils.findByUniqueName(calls, "fourth");
    target = TestUtils.findByUniqueName(functions, "fourth");
    assertEquals(List.of(target), fourth.getInvokes());
  }

  @Test
  void testJavaLoops() throws Exception {
    testLoops("src/test/resources/cfg/Loops.java", "System.out.println();");
  }

  @Test
  void testCppLoops() throws Exception {
    testLoops("src/test/resources/cfg/loops.cpp", "printf(\"\\n\");");
  }

  /**
   * Tests EOG building in the presence of while-, do-while-loop statements.
   *
   * @param relPath
   * @param refNodeString - Exact string of reference nodes, do not change/insert nodes in the test
   *     file.
   */
  void testLoops(String relPath, String refNodeString) throws Exception {
    List<Node> nodes = translateToNodes(relPath);

    List<Node> prints =
        nodes.stream()
            .filter(node -> Objects.equals(node.getCode(), refNodeString))
            .collect(Collectors.toList());

    assertEquals(1, nodes.stream().filter(node -> node instanceof WhileStatement).count());
    WhileStatement wstat = Util.filterCast(nodes, WhileStatement.class).get(0);
    SubgraphWalker.Border conditionEOG = SubgraphWalker.getEOGPathEdges(wstat.getCondition());
    SubgraphWalker.Border blockEOG = SubgraphWalker.getEOGPathEdges(wstat.getStatement());

    // Print EOG edges for debugging
    nodes
        .get(0)
        .accept(
            Strategy::EOG_BACKWARD,
            new IVisitor<Node>() {
              @Override
              public void visit(Node n) {
                System.out.println(
                    locationLink(n.getLocation()) + " -> " + locationLink(n.getLocation()));
              }
            });

    // Assert: Only single entry and exit NODE per block
    assertTrue(conditionEOG.getEntries().size() == 1 && conditionEOG.getExits().size() == 1);
    assertTrue(blockEOG.getEntries().size() == 1 && blockEOG.getExits().size() == 1);

    // Assert: While is preceded by a specific printf("\n")
    assertTrue(Util.eogConnect(NODE, ENTRIES, wstat, wstat.getCondition()));
    // Assert: Condition is preceded by print or block of the loop itself
    assertTrue(Util.eogConnect(ENTRIES, wstat.getCondition(), prints.get(0), wstat.getStatement()));

    // Assert: All EOGs going into the loop branch come from the condition
    assertTrue(Util.eogConnect(ENTRIES, wstat.getStatement(), NODE, wstat));

    // Assert: The EOGs going into the second print come either from the then branch or the
    // condition
    assertTrue(Util.eogConnect(SUBTREE, EXITS, wstat, prints.get(1)));

    DoStatement dostat = Util.filterCast(nodes, DoStatement.class).get(0);

    conditionEOG = SubgraphWalker.getEOGPathEdges(dostat.getCondition());
    blockEOG = SubgraphWalker.getEOGPathEdges(dostat.getStatement());

    // Assert: Only single entry and exit NODE per block
    assertTrue(conditionEOG.getEntries().size() == 1 && conditionEOG.getExits().size() == 1);
    assertTrue(blockEOG.getEntries().size() == 1 && blockEOG.getExits().size() == 1);

    // Assert: do is preceded by print
    assertTrue(Util.eogConnect(NODE, ENTRIES, dostat, dostat.getCondition()));
    // Assert: All EOGs going into the loop branch come from the condition
    assertTrue(Util.eogConnect(EXITS, prints.get(1), dostat.getStatement()));
    assertTrue(Util.eogConnect(ANY, NODE, EXITS, dostat, dostat.getStatement()));

    // Assert: Condition is preceded by the loop branch
    assertTrue(Util.eogConnect(ENTRIES, dostat.getCondition(), dostat.getStatement()));

    // Assert: The EOGs going into the second print come either from the then branch or the
    // condition
    assertTrue(Util.eogConnect(SUBTREE, EXITS, dostat, prints.get(2)));
  }

  void testSwitch(String relPath, String refNodeString) throws Exception {
    List<Node> nodes = translateToNodes(relPath);

    List<FunctionDeclaration> functions =
        Util.filterCast(nodes, FunctionDeclaration.class).stream()
            .filter(f -> !(f instanceof ConstructorDeclaration))
            .collect(Collectors.toList());

    // main()
    SwitchStatement swch = TestUtils.subnodesOfType(functions.get(0), SwitchStatement.class).get(0);
    List<Node> prints = Util.subnodesOfCode(functions.get(0), refNodeString);
    List<CaseStatement> cases = TestUtils.subnodesOfType(swch, CaseStatement.class);
    List<DefaultStatement> defaults = TestUtils.subnodesOfType(swch, DefaultStatement.class);

    assertTrue(Util.eogConnect(EXITS, prints.get(0), SUBTREE, swch.getSelector()));
    assertTrue(Util.eogConnect(SUBTREE, EXITS, swch, prints.get(1)));

    // Assert: Selector exits connect to the switch root node
    assertTrue(
        Util.eogConnect(
            NODE,
            EXITS,
            swch,
            Stream.of(cases, defaults).flatMap(l -> l.stream()).toArray(size -> new Node[size])));

    assertTrue(Util.eogConnect(EXITS, swch.getSelector(), NODE, swch));

    // Assert: Entries of case statements have one edge to switch root
    for (Statement s :
        Stream.of(cases, defaults).flatMap(n -> n.stream()).collect(Collectors.toList())) {
      assertTrue(Util.eogConnect(ANY, ENTRIES, s, NODE, swch));
    }

    List<Node> printEntries = SubgraphWalker.getEOGPathEdges(prints.get(1)).getEntries();
    // Assert: All breaks inside of switch connect to the switch root node
    for (BreakStatement b : TestUtils.subnodesOfType(swch, BreakStatement.class))
      assertTrue(Util.eogConnect(ALL, SUBTREE, EXITS, b, SUBTREE, prints.get(1)));

    // whileswitch
    swch = TestUtils.subnodesOfType(functions.get(1), SwitchStatement.class).get(0);
    prints = Util.subnodesOfCode(functions.get(1), refNodeString);
    cases = TestUtils.subnodesOfType(swch, CaseStatement.class);
    defaults = TestUtils.subnodesOfType(swch, DefaultStatement.class);
    WhileStatement wstat = TestUtils.subnodesOfType(functions.get(1), WhileStatement.class).get(0);

    assertTrue(Util.eogConnect(EXITS, prints.get(0), wstat));
    assertTrue(Util.eogConnect(NODE, EXITS, wstat, prints.get(2)));
    // Assert: switch root node exits connect to either case or default statements entries
    assertTrue(
        Util.eogConnect(
            NODE,
            EXITS,
            swch,
            Stream.of(cases, defaults).flatMap(l -> l.stream()).toArray(size -> new Node[size])));
    // Assert: Selector exits connect to the switch root node
    assertTrue(Util.eogConnect(SUBTREE, EXITS, swch.getSelector(), NODE, swch));

    // switchwhile
    swch = TestUtils.subnodesOfType(functions.get(2), SwitchStatement.class).get(0);
    prints = Util.subnodesOfCode(functions.get(2), refNodeString);
    wstat = TestUtils.subnodesOfType(functions.get(2), WhileStatement.class).get(0);
    cases = TestUtils.subnodesOfType(swch, CaseStatement.class);
    defaults = TestUtils.subnodesOfType(swch, DefaultStatement.class);

    assertTrue(Util.eogConnect(EXITS, prints.get(0), swch));
    assertTrue(Util.eogConnect(EXITS, swch, prints.get(2)));
    // Assert: Selector exits connect to either case or default statements entries
    assertTrue(Util.eogConnect(EXITS, swch.getSelector(), NODE, swch));

    swch = TestUtils.subnodesOfType(functions.get(1), SwitchStatement.class).get(0);
    prints = Util.subnodesOfCode(functions.get(1), refNodeString);
    List<BreakStatement> breaks = TestUtils.subnodesOfType(swch, BreakStatement.class);
    WhileStatement whiles = TestUtils.subnodesOfType(functions.get(1), WhileStatement.class).get(0);

    // Assert: whileswitch, all breaks inside the switch connect to the containing switch unless it
    // has a label which connects the break to the  while
    for (BreakStatement b : breaks) {
      if (b.getLabel() != null && b.getLabel().length() > 0) {
        assertTrue(Util.eogConnect(EXITS, b, SUBTREE, prints.get(2)));
      } else {
        assertTrue(Util.eogConnect(EXITS, b, SUBTREE, prints.get(1)));
      }
    }
    swch = TestUtils.subnodesOfType(functions.get(2), SwitchStatement.class).get(0);
    prints = Util.subnodesOfCode(functions.get(2), refNodeString);
    whiles = TestUtils.subnodesOfType(functions.get(2), WhileStatement.class).get(0);
    breaks = TestUtils.subnodesOfType(whiles, BreakStatement.class);

    // Assert: switchwhile, all breaks inside the while connect to the containing while unless it
    // has a label which connects the break to the switch
    for (BreakStatement b : breaks)
      if (b.getLabel() != null && b.getLabel().length() > 0)
        assertTrue(Util.eogConnect(EXITS, b, SUBTREE, prints.get(2)));
      else assertTrue(Util.eogConnect(EXITS, b, SUBTREE, prints.get(1)));
  }

  @Test
  void testCppSwitch() throws Exception {
    testSwitch("src/test/resources/cfg/switch.cpp", REFNODESTRINGCXX);
  }

  @Test
  void testJavaSwitch() throws Exception {
    testSwitch("src/test/resources/cfg/Switch.java", REFNODESTRINGJAVA);
  }

  @Test
  void testJavaBreakContinue() throws Exception {
    testBreakContinue("src/test/resources/cfg/BreakContinue.java", "System.out.println();");
  }

  @Test
  void testCppBreakContinue() throws Exception {
    testBreakContinue("src/test/resources/cfg/break_continue.cpp", "printf(\"\\n\");");
  }

  /**
   * Tests EOG branch edge property in if/else if/else construct
   *
   * @throws Exception
   */
  @Test
  void testBranchProperty() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "eog");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(List.of(topLevel.resolve("EOG.java").toFile()), topLevel, true);

    // Test If-Block
    IfStatement firstIf =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, IfStatement.class),
                l -> l.getLocation().getRegion().getStartLine() == 6)
            .get(0);

    DeclaredReferenceExpression a =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                l -> l.getLocation().getRegion().getStartLine() == 8 && l.getName().equals("a"))
            .get(0);

    DeclaredReferenceExpression b =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                l -> l.getLocation().getRegion().getStartLine() == 7 && l.getName().equals("b"))
            .get(0);

    List<PropertyEdge<Node>> nextEOG = firstIf.getNextEOGProperties();
    assertEquals(2, nextEOG.size());
    for (PropertyEdge<Node> edge : nextEOG) {
      assertEquals(firstIf, edge.getStart());
      if (edge.getEnd().equals(b)) {
        assertEquals(true, edge.getProperty(Properties.BRANCH));
        assertEquals(0, edge.getProperty(Properties.INDEX));
      } else {
        assertEquals(a, edge.getEnd());
        assertEquals(false, edge.getProperty(Properties.BRANCH));
        assertEquals(1, edge.getProperty(Properties.INDEX));
      }
    }

    IfStatement elseIf =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, IfStatement.class),
                l -> l.getLocation().getRegion().getStartLine() == 8)
            .get(0);

    assertEquals(elseIf, firstIf.getElseStatement());

    DeclaredReferenceExpression b2 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                l -> l.getLocation().getRegion().getStartLine() == 9 && l.getName().equals("b"))
            .get(0);

    DeclaredReferenceExpression x =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                l -> l.getLocation().getRegion().getStartLine() == 11 && l.getName().equals("x"))
            .get(0);

    nextEOG = elseIf.getNextEOGProperties();
    assertEquals(2, nextEOG.size());

    for (PropertyEdge<Node> edge : nextEOG) {
      assertEquals(elseIf, edge.getStart());
      if (edge.getEnd().equals(b2)) {
        assertEquals(true, edge.getProperty(Properties.BRANCH));
        assertEquals(0, edge.getProperty(Properties.INDEX));
      } else {
        assertEquals(x, edge.getEnd());
        assertEquals(false, edge.getProperty(Properties.BRANCH));
        assertEquals(1, edge.getProperty(Properties.INDEX));
      }
    }
  }

  /**
   * Tests EOG building in the presence of break-,continue- statements in loops.
   *
   * @param relPath
   * @param refNodeString - Exact string of reference nodes, do not change/insert nodes in the test
   *     file.
   * @throws TranslationException
   */
  void testBreakContinue(String relPath, String refNodeString) throws Exception {
    List<Node> nodes = translateToNodes(relPath);

    List<Node> prints =
        nodes.stream()
            .filter(node -> node.getCode().equals(refNodeString))
            .collect(Collectors.toList());

    assertEquals(1, nodes.stream().filter(node -> node instanceof WhileStatement).count());
    List<BreakStatement> breaks = Util.filterCast(nodes, BreakStatement.class);
    List<ContinueStatement> continues = Util.filterCast(nodes, ContinueStatement.class);

    WhileStatement wstat = Util.filterCast(nodes, WhileStatement.class).get(0);

    SubgraphWalker.Border conditionEOG = SubgraphWalker.getEOGPathEdges(wstat.getCondition());
    SubgraphWalker.Border blockEOG = SubgraphWalker.getEOGPathEdges(wstat.getStatement());

    // Assert: Only single entry and two exit NODEs per block
    assertTrue(conditionEOG.getEntries().size() == 1 && conditionEOG.getExits().size() == 1);
    assertTrue(blockEOG.getEntries().size() == 1 && blockEOG.getExits().size() == 3);

    // Assert: Print is only followed by first nodes in condition
    assertTrue(Util.eogConnect(EXITS, prints.get(0), wstat.getCondition()));

    // Assert: condition nodes are preceded by either continue, last nodes in block or last nodes in
    // print

    assertTrue(
        Util.eogConnect(ENTRIES, wstat.getCondition(), prints.get(0), wstat.getStatement())
            || Util.eogConnect(NODE, EXITS, continues.get(0), wstat.getCondition()));

    // Assert: All EOGs going into the loop branch come from the Loop root node
    assertTrue(Util.eogConnect(ENTRIES, wstat.getStatement(), wstat));

    // Assert: The EOGs going into the second print come either from the while root or break
    assertTrue(
        Util.eogConnect(NODE, EXITS, wstat, prints.get(1))
            || Util.eogConnect(NODE, EXITS, breaks.get(0), prints.get(1)));

    DoStatement dostat = Util.filterCast(nodes, DoStatement.class).get(0);

    conditionEOG = SubgraphWalker.getEOGPathEdges(dostat.getCondition());

    blockEOG = (SubgraphWalker.getEOGPathEdges(dostat.getStatement()));

    // Assert: Only single entry and two exit NODEs per block
    assertTrue(conditionEOG.getEntries().size() == 1 && conditionEOG.getExits().size() == 1);
    assertTrue(blockEOG.getEntries().size() == 1 && blockEOG.getExits().size() == 3);

    // Assert: All EOGs going into the loop branch come from the condition
    assertTrue(Util.eogConnect(EXITS, prints.get(1), dostat.getStatement()));
    assertTrue(Util.eogConnect(ANY, NODE, EXITS, dostat, SUBTREE, dostat.getStatement()));

    assertTrue(Util.eogConnect(EXITS, prints.get(1), dostat.getStatement()));
    assertTrue(Util.eogConnect(ANY, NODE, EXITS, dostat, dostat.getStatement()));

    // Assert: Condition is preceded by the loop branch
    assertTrue(
        Util.eogConnect(ENTRIES, dostat.getCondition(), dostat.getStatement())
            || Util.eogConnect(NODE, EXITS, continues.get(1), dostat.getCondition()));

    // Assert: The EOGs going into the third print come  from the loop root
    assertTrue(Util.eogConnect(NODE, EXITS, dostat, prints.get(2)));
  }

  @Test
  void testEOGInvariant() throws Exception {
    File file = new File("src/main/java/de/fraunhofer/aisec/cpg/passes/scopes/ScopeManager.java");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    assertTrue(EvaluationOrderGraphPass.checkEOGInvariant(tu));
  }

  /**
   * Translates the given file into CPG and returns the graph. Extracted to reduce code duplicates
   *
   * @param path - path for the file to test.
   */
  private List<Node> translateToNodes(String path) throws Exception {
    File toTranslate = new File(path);
    Path topLevel = toTranslate.getParentFile().toPath();
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(toTranslate), topLevel, true);
    List<Node> nodes = SubgraphWalker.flattenAST(tu);
    // Todo until explicitly added Return Statements are either removed again or code and region set
    // properly

    nodes = nodes.stream().filter(node -> node.getCode() != null).collect(Collectors.toList());
    nodes.sort(new NodeComparator());
    return nodes;
  }
}
