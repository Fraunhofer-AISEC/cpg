/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DFGTest {
  // Test DFG

  // Test ControlFlowSensitiveDFGPass
  @Test
  void testControlSensitiveDFGPassIfMerge() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("ControlFlowSensitiveDFGIfMerge.java").toFile()),
            topLevel,
            true);

    // Test If-Block
    Literal<?> literal2 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(2))
            .get(0);

    DeclaredReferenceExpression a2 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                e -> e.getAccess().equals(AccessValues.WRITE))
            .get(0);

    assertTrue(literal2.getNextDFG().contains(a2));
    assertEquals(1, a2.getNextDFG().size()); // Outgoing DFG Edges only to VariableDeclaration

    var refersTo = a2.getRefersToAs(VariableDeclaration.class);
    assertNotNull(refersTo);

    assertEquals(0, refersTo.getNextDFG().size());
    assertEquals(a2.getNextDFG().iterator().next(), refersTo);

    // Test Else-Block with System.out.println()
    Literal<?> literal1 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(1))
            .get(0);

    CallExpression println =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, CallExpression.class),
                c -> c.getName().equals("println"))
            .get(0);

    DeclaredReferenceExpression a1 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                e -> e.getNextEOG().contains(println))
            .get(0);

    assertEquals(1, a1.getPrevDFG().size());
    assertEquals(literal1, a1.getPrevDFG().iterator().next());

    assertEquals(1, a1.getNextEOG().size());
    assertEquals(println, a1.getNextEOG().get(0));

    // Test Merging
    VariableDeclaration b =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, VariableDeclaration.class),
                v -> v.getName().equals("b"))
            .get(0);

    DeclaredReferenceExpression ab = (DeclaredReferenceExpression) b.getPrevEOG().get(0);

    assertTrue(literal1.getNextDFG().contains(ab));
    assertTrue(literal2.getNextDFG().contains(ab));
  }

  /**
   * Tests the ControlFlowSensitiveDFGPass and checks if an assignment located within one block
   * clears the values from the map and includes only the new (assigned) value.
   *
   * @throws Exception Any exception that happens during the analysis process
   */
  @Test
  void testControlSensitiveDFGPassIfNoMerge() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("ControlFlowSensitiveDFGIfNoMerge.java").toFile()),
            topLevel,
            true);

    VariableDeclaration b =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, VariableDeclaration.class),
                v -> v.getName().equals("b"))
            .get(0);

    DeclaredReferenceExpression ab = (DeclaredReferenceExpression) b.getPrevEOG().get(0);

    Literal<?> literal4 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(4))
            .get(0);

    assertTrue(literal4.getNextDFG().contains(ab));
    assertEquals(1, ab.getPrevDFG().size());
  }

  /**
   * To test assignments of different value in an expression that then has a joinPoint. a = a == b ?
   * b = 2: b = 3;
   *
   * @throws Exception
   */
  @Test
  void testConditionalExpression() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("conditional_expression.cpp").toFile()), topLevel, true);

    DeclaredReferenceExpression b =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                v -> v.getName().equals("b") && v.getLocation().getRegion().getStartLine() == 6)
            .get(0);

    Literal val2 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), v -> v.getValue().equals(2))
            .get(0);

    Literal val3 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), v -> v.getValue().equals(3))
            .get(0);

    assertEquals(2, b.getPrevDFG().size());
    assertTrue(b.getPrevDFG().contains(val2));
    assertTrue(b.getPrevDFG().contains(val3));
  }

  @Test
  void testControlSensitiveDFGPassSwitch() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("ControlFlowSensitiveDFGSwitch.java").toFile()),
            topLevel,
            true);

    VariableDeclaration a =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, VariableDeclaration.class),
                v -> v.getName().equals("a"))
            .get(0);

    VariableDeclaration b =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, VariableDeclaration.class),
                v -> v.getName().equals("b"))
            .get(0);

    DeclaredReferenceExpression ab = (DeclaredReferenceExpression) b.getPrevEOG().get(0);

    DeclaredReferenceExpression a10 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                dre -> TestUtils.compareLineFromLocationIfExists(dre, true, 8))
            .get(0);
    DeclaredReferenceExpression a11 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                dre -> TestUtils.compareLineFromLocationIfExists(dre, true, 11))
            .get(0);
    DeclaredReferenceExpression a12 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                dre -> TestUtils.compareLineFromLocationIfExists(dre, true, 14))
            .get(0);

    Literal<?> literal0 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(0))
            .get(0);
    Literal<?> literal10 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(10))
            .get(0);
    Literal<?> literal11 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(11))
            .get(0);
    Literal<?> literal12 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(12))
            .get(0);

    assertEquals(3, literal10.getNextDFG().size());
    assertTrue(literal10.getNextDFG().contains(a10));

    assertEquals(3, literal11.getNextDFG().size());
    assertTrue(literal11.getNextDFG().contains(a11));

    assertEquals(4, literal12.getNextDFG().size());
    assertTrue(literal12.getNextDFG().contains(a12));

    assertEquals(4, a.getPrevDFG().size());
    assertTrue(a.getPrevDFG().contains(literal0));
    assertTrue(a.getPrevDFG().contains(a10));
    assertTrue(a.getPrevDFG().contains(a11));
    assertTrue(a.getPrevDFG().contains(a12));

    assertTrue(ab.getPrevDFG().contains(literal0));
    assertTrue(ab.getPrevDFG().contains(literal10));
    assertTrue(ab.getPrevDFG().contains(literal11));
    assertTrue(ab.getPrevDFG().contains(literal12));

    assertEquals(1, ab.getNextDFG().size());
    assertTrue(ab.getNextDFG().contains(b));

    // Fallthrough test
    CallExpression println =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, CallExpression.class),
                c -> c.getName().equals("println"))
            .get(0);

    DeclaredReferenceExpression aPrintln =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
                e -> e.getNextEOG().contains(println))
            .get(0);

    assertEquals(2, aPrintln.getPrevDFG().size());
    assertTrue(aPrintln.getPrevDFG().contains(literal0));
    assertTrue(aPrintln.getPrevDFG().contains(literal12));
  }

  // Test DFG when ReadWrite access occurs, such as compoundoperators or unaryoperators

  @Test
  void testCompoundOperatorDFG() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("compoundoperator.cpp").toFile()), topLevel, true);

    BinaryOperator rwCompoundOperator =
        TestUtils.findByUniqueName(TestUtils.subnodesOfType(result, BinaryOperator.class), "+=");
    DeclaredReferenceExpression expression =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class), "i");

    Set<Node> prevDFGOperator = rwCompoundOperator.getPrevDFG();
    Set<Node> nextDFGOperator = rwCompoundOperator.getNextDFG();

    assertTrue(prevDFGOperator.contains(expression));
    assertTrue(nextDFGOperator.contains(expression));
  }

  @Test
  void testUnaryOperatorDFG() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(List.of(topLevel.resolve("unaryoperator.cpp").toFile()), topLevel, true);

    UnaryOperator rwUnaryOperator =
        TestUtils.findByUniqueName(TestUtils.subnodesOfType(result, UnaryOperator.class), "++");
    DeclaredReferenceExpression expression =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class), "i");

    Set<Node> prevDFGOperator = rwUnaryOperator.getPrevDFG();
    Set<Node> nextDFGOperator = rwUnaryOperator.getNextDFG();

    assertTrue(prevDFGOperator.contains(expression));
    assertTrue(nextDFGOperator.contains(expression));
  }

  /**
   * Ensures that if there is an assignment like a = a + b the replacement of the current value of
   * the VariableDeclaration is delayed until the entire assignment has been traversed. This is
   * necessary, since if the replacement was not delayed the rhs a would have an incoming dfg edge
   * from a + b
   *
   * @throws Exception
   */
  @Test
  void testDelayedAssignment() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("DelayedAssignmentAfterRHS.java").toFile()), topLevel, true);

    BinaryOperator binaryOperatorAssignment =
        TestUtils.findByUniqueName(TestUtils.subnodesOfType(result, BinaryOperator.class), "=");

    BinaryOperator binaryOperatorAddition =
        TestUtils.findByUniqueName(TestUtils.subnodesOfType(result, BinaryOperator.class), "+");

    VariableDeclaration varA =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "a");
    VariableDeclaration varB =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "b");

    DeclaredReferenceExpression lhsA =
        (DeclaredReferenceExpression) binaryOperatorAssignment.getLhs();
    DeclaredReferenceExpression rhsA =
        (DeclaredReferenceExpression) binaryOperatorAddition.getLhs();

    DeclaredReferenceExpression b =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class), "b");

    Literal<?> literal0 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(0))
            .get(0);

    Literal<?> literal1 =
        TestUtils.findByPredicate(
                TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(1))
            .get(0);

    assertEquals(0, varA.getNextDFG().size()); // No outgoing DFG edges from VariableDeclaration a
    assertEquals(0, varB.getNextDFG().size()); // No outgoing DFG edges from VariableDeclaration b

    // Check that the replacement of the current value for VariableDeclaration a is delayed until
    // the assignment is completed. This means that the DeclaredReferenceExpression on the rhs must
    // contain a prev dfg edge to the previous valid value for VariableDeclaration a (literal 0)
    assertEquals(1, rhsA.getPrevDFG().size());
    assertTrue(rhsA.getPrevDFG().contains(literal0));

    // Check outgoing dfg edges of literal 0 (VariableDeclaration a initializer and rhs expression
    // of a = a + b
    assertEquals(2, literal0.getNextDFG().size());
    assertEquals(0, literal0.getPrevDFG().size());
    assertTrue(literal0.getNextDFG().contains(varA));

    // Check incoming dfg edges of VariableDeclaration a (lhs of a = a + b, 0 and expr a + b
    assertEquals(2, varA.getPrevDFG().size());
    assertTrue(varA.getPrevDFG().contains(lhsA));
    assertTrue(varA.getPrevDFG().contains(literal0));

    // Check incoming dfg edges in binaryOperator + (DeclaredReferenceExpression a and b)
    assertEquals(2, binaryOperatorAddition.getPrevDFG().size());
    assertTrue(binaryOperatorAddition.getPrevDFG().contains(b));
    assertTrue(binaryOperatorAddition.getPrevDFG().contains(rhsA));

    // Check outgoing dfg edges from a of a = a + b and into
    // VariableDeclaration a)
    assertEquals(2, binaryOperatorAddition.getNextDFG().size());
    assertTrue(binaryOperatorAddition.getNextDFG().contains(lhsA));

    // Check outgoing dfg edges of literal1 (VariableDeclaration b and DeclaredReferenceExpression
    // b)
    assertEquals(2, literal1.getNextDFG().size());
    assertTrue(literal1.getNextDFG().contains(varB));
    assertTrue(literal1.getNextDFG().contains(b));
  }

  /**
   * Tests that there are no outgoing DFG edges from a VariableDeclaration
   *
   * @throws Exception
   */
  @Test
  void testNoOutgoingDFGFromVariableDeclaration() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(List.of(topLevel.resolve("BasicSlice.java").toFile()), topLevel, true);

    VariableDeclaration varA =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "a");

    assertEquals(0, varA.getNextDFG().size());
    assertEquals(7, varA.getPrevDFG().size());
  }

  @Test
  void testSensitivityThroughLoop() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    TranslationUnitDeclaration result =
        TestUtils.analyze(List.of(topLevel.resolve("LoopDFGs.java").toFile()), topLevel, true)
            .get(0);

    MethodDeclaration looping =
        TestUtils.getSubnodeOfTypeWithName(result, MethodDeclaration.class, "looping");
    List<Node> methodNodes = SubgraphWalker.flattenAST(looping);
    Literal l0 = getLiteral(methodNodes, 0);
    Literal l1 = getLiteral(methodNodes, 1);
    Literal l2 = getLiteral(methodNodes, 2);
    Literal l3 = getLiteral(methodNodes, 3);
    List<Node> calls =
        TestUtils.findByPredicate(
            SubgraphWalker.flattenAST(looping),
            n -> n instanceof CallExpression && n.getName().equals("println"));
    Set<Node> dfgNodes =
        flattenDFGGraph(
            TestUtils.getSubnodeOfTypeWithName(
                calls.get(0), DeclaredReferenceExpression.class, "a"),
            false);
    assertTrue(dfgNodes.contains(l0));
    assertTrue(dfgNodes.contains(l1));
    assertTrue(dfgNodes.contains(l2));

    assertFalse(dfgNodes.contains(l3));
  }

  /**
   * Gets Integer Literal from the List of nodes to simplify testsyntax. The Literal is expected to
   * be contained in the list and the function will throw an {@link IndexOutOfBoundsException}
   * otherwise.
   *
   * @param nodes - The list of nodes to filter for the Literal.
   * @param v - The integer value expected from the Literal.
   * @return The Literal with the specified value.
   */
  private Literal getLiteral(List<Node> nodes, int v) {
    return (Literal)
        TestUtils.findByPredicate(
                nodes,
                n -> n instanceof Literal && ((Literal) n).getValue().equals(Integer.valueOf(v)))
            .get(0);
  }

  @Test
  void testSensitivityWithLabels() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    TranslationUnitDeclaration result =
        TestUtils.analyze(List.of(topLevel.resolve("LoopDFGs.java").toFile()), topLevel, true)
            .get(0);
    MethodDeclaration looping =
        TestUtils.getSubnodeOfTypeWithName(result, MethodDeclaration.class, "labeledBreakContinue");

    List<Node> methodNodes = SubgraphWalker.flattenAST(looping);
    Literal l0 = getLiteral(methodNodes, 0);
    Literal l1 = getLiteral(methodNodes, 1);
    Literal l2 = getLiteral(methodNodes, 2);
    Literal l3 = getLiteral(methodNodes, 3);
    Literal l4 = getLiteral(methodNodes, 4);
    List<Node> calls =
        TestUtils.findByPredicate(
            SubgraphWalker.flattenAST(looping),
            n -> n instanceof CallExpression && n.getName().equals("println"));
    calls.sort(new NodeComparator());
    Set<Node> dfgNodesA0 =
        flattenDFGGraph(
            TestUtils.getSubnodeOfTypeWithName(
                calls.get(0), DeclaredReferenceExpression.class, "a"),
            false);

    Set<Node> dfgNodesA1 =
        flattenDFGGraph(
            TestUtils.getSubnodeOfTypeWithName(
                calls.get(1), DeclaredReferenceExpression.class, "a"),
            false);
    Set<Node> dfgNodesA2 =
        flattenDFGGraph(
            TestUtils.getSubnodeOfTypeWithName(
                calls.get(2), DeclaredReferenceExpression.class, "a"),
            false);

    assertTrue(dfgNodesA0.contains(l0));
    assertTrue(dfgNodesA0.contains(l1));
    assertTrue(dfgNodesA0.contains(l3));
    assertFalse(dfgNodesA0.contains(l4));

    assertTrue(dfgNodesA1.contains(l0));
    assertTrue(dfgNodesA1.contains(l1));
    assertTrue(dfgNodesA1.contains(l3));
    assertFalse(dfgNodesA1.contains(l4));

    assertTrue(dfgNodesA2.contains(l0));
    assertTrue(dfgNodesA2.contains(l1));
    assertTrue(dfgNodesA2.contains(l2));
    assertTrue(dfgNodesA2.contains(l3));
    assertFalse(dfgNodesA2.contains(l4));
  }

  /**
   * Traverses the DFG Graph induced by the provided node in the specified direction and retrieves
   * all nodes that are passed by and are therefor part of the incoming or outgoing data-flow.
   *
   * @param node - The node that induces the DFG-subgraph for which nodes are retrieved
   * @param outgoing - true if the Data-Flow from this node should be considered, false if the
   *     data-flow is to this node.
   * @return A set of nodes that are part of the data-flow
   */
  public Set<Node> flattenDFGGraph(Node node, boolean outgoing) {
    Set<Node> dfgNodes = new LinkedHashSet<>();
    dfgNodes.add(node);
    LinkedHashSet<Node> worklist = new LinkedHashSet<Node>();
    worklist.add(node);
    while (!worklist.isEmpty()) {
      Node toProcess = worklist.iterator().next();
      worklist.remove(toProcess);
      Set<Node> nextDFGNodes;
      // DataFlow direction
      if (outgoing) {
        nextDFGNodes = toProcess.getNextDFG();
      } else {
        nextDFGNodes = toProcess.getPrevDFG();
      }
      // Adding all NEWLY discovered df-nodes to the worklist.
      for (Node dfgNode : nextDFGNodes) {
        if (!dfgNodes.contains(dfgNode)) {
          worklist.add(dfgNode);
          dfgNodes.add(dfgNode);
        }
      }
    }
    return dfgNodes;
  }
}
