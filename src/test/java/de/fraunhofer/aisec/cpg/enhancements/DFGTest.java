package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import java.nio.file.Path;
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
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(2));

    DeclaredReferenceExpression a2 =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
            e -> e.getAccess().equals(AccessValues.WRITE));

    assertTrue(literal2.getNextDFG().contains(a2));
    assertEquals(0, a2.getNextDFG().size()); // Outgoing DFG Edges are not allowed

    assertEquals(1, a2.getRefersTo().size());
    Node refersTo = a2.getRefersTo().iterator().next();
    assertTrue(refersTo instanceof VariableDeclaration);
    VariableDeclaration a = (VariableDeclaration) refersTo;
    assertEquals(0, a.getNextDFG().size());

    // Test Else-Block with System.out.println()
    Literal<?> literal1 =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(1));

    CallExpression println =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, CallExpression.class),
            c -> c.getName().equals("println"));

    DeclaredReferenceExpression a1 =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, DeclaredReferenceExpression.class),
            e -> e.getNextEOG().contains(println));

    assertEquals(1, a1.getPrevDFG().size());
    assertEquals(literal1, a1.getPrevDFG().iterator().next());

    assertEquals(1, a1.getNextEOG().size());
    assertEquals(println, a1.getNextEOG().get(0));

    // Test Merging
    VariableDeclaration b =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, VariableDeclaration.class),
            v -> v.getName().equals("b"));

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
            v -> v.getName().equals("b"));

    DeclaredReferenceExpression ab = (DeclaredReferenceExpression) b.getPrevEOG().get(0);

    Literal<?> literal4 =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(4));

    assertTrue(literal4.getNextDFG().contains(ab));
    assertEquals(1, ab.getPrevDFG().size());
  }

  @Test
  void testControlSensitiveDFGPassSwitch() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("ControlFlowSensitiveDFGSwitch.java").toFile()),
            topLevel,
            true);

    VariableDeclaration b =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, VariableDeclaration.class),
            v -> v.getName().equals("b"));

    DeclaredReferenceExpression ab = (DeclaredReferenceExpression) b.getPrevEOG().get(0);

    Literal<?> literal0 =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(0));
    Literal<?> literal10 =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(10));
    Literal<?> literal11 =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(11));
    Literal<?> literal12 =
        TestUtils.findByPredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(12));

    assertTrue(ab.getPrevDFG().contains(literal0));
    assertTrue(ab.getPrevDFG().contains(literal10));
    assertTrue(ab.getPrevDFG().contains(literal11));
    assertTrue(ab.getPrevDFG().contains(literal12));

    // Fallthrough test
    // TODO: Right now we are not able to have an order in SwitchStatements. This is required in
    // order to determine to which case the execution performs the fallthrough
    /*CallExpression println =
        TestUtils.findByPredicate(
            Util.subnodesOfType(result, CallExpression.class), c -> c.getName().equals("println"));

    DeclaredReferenceExpression a =
        TestUtils.findByPredicate(
            Util.subnodesOfType(result, DeclaredReferenceExpression.class),
            e -> e.getNextEOG().contains(println));

    assertEquals(2, a.getPrevDFG().size());
    assertTrue(a.getPrevDFG().contains(literal0));
    assertTrue(a.getPrevDFG().contains(literal12));*/
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
    VariableDeclaration variableDeclaration =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "i");

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
    VariableDeclaration variableDeclaration =
        TestUtils.findByUniqueName(
            TestUtils.subnodesOfType(result, VariableDeclaration.class), "i");

    Set<Node> prevDFGOperator = rwUnaryOperator.getPrevDFG();
    Set<Node> nextDFGOperator = rwUnaryOperator.getNextDFG();

    assertTrue(prevDFGOperator.contains(expression));
    assertTrue(nextDFGOperator.contains(expression));

    Set<Node> prevDFGDeclaredReferenceExpression = expression.getPrevDFG();
    Set<Node> nextDFGDeclaredReferenceExpression = expression.getNextDFG();

    assertTrue(prevDFGDeclaredReferenceExpression.contains(variableDeclaration));
    assertTrue(nextDFGDeclaredReferenceExpression.contains(variableDeclaration));
  }
}
