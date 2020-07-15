package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DFGTest {
  // Test DFG

  // Test ControlFlowSensitiveDFGPass
  @Test
  void testControlSensitiveDFGPassIf() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("ControlFlowSensitiveDFGIfMerge.java").toFile()),
            topLevel,
            true);
  }

  // Test DFG when ReadWrite access occurs, such as compoundoperators or unaryoperators

  @Test
  void testCompoundOperatorDFG() throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "dfg");
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(topLevel.resolve("compoundoperator.cpp").toFile()), topLevel, true);

    BinaryOperator rwCompoundOperator =
        TestUtils.findByUniqueName(Util.subnodesOfType(result, BinaryOperator.class), "+=");
    DeclaredReferenceExpression expression =
        TestUtils.findByUniqueName(
            Util.subnodesOfType(result, DeclaredReferenceExpression.class), "i");
    VariableDeclaration variableDeclaration =
        TestUtils.findByUniqueName(Util.subnodesOfType(result, VariableDeclaration.class), "i");

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
        TestUtils.findByUniqueName(Util.subnodesOfType(result, UnaryOperator.class), "++");
    DeclaredReferenceExpression expression =
        TestUtils.findByUniqueName(
            Util.subnodesOfType(result, DeclaredReferenceExpression.class), "i");
    VariableDeclaration variableDeclaration =
        TestUtils.findByUniqueName(Util.subnodesOfType(result, VariableDeclaration.class), "i");

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
