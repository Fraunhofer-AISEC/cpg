package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.helpers.ControlFlowSensitiveDFG;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;

/**
 * This pass tracks VariableDeclarations and values that are included in the graph by the DFG edge.
 * For this pass we traverse the EOG in order to detect mutually exclusive code blocks (e.g. If-Else
 * Statement) in order to remove the DFG edges that are not feasible.
 *
 * <p>Control Flow Sensitivity in the DFG is only performed on VariableDeclarations and not on
 * FieldDeclarations. The reason for this being the fact, that the value of a field might be
 * modified to a value that is not present in the method, thus it is not detected by our variable
 * tracking
 */
public class ControlFlowSensitiveDFGPass extends Pass {

  @Override
  public void cleanup() {
    // Nothing to cleanup
  }

  @Override
  public void accept(TranslationResult translationResult) {
    SubgraphWalker.IterativeGraphWalker walker = new SubgraphWalker.IterativeGraphWalker();
    walker.registerOnNodeVisit(this::handle);
    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }
  }

  /**
   * Removes unrefined DFG edges
   *
   * @param dfg ControlFlowSensitiveDFG of entire Method
   */
  private void removeValues(ControlFlowSensitiveDFG dfg) {
    for (Node currNode : dfg.getRemoves().keySet()) {
      for (Node prev : dfg.getRemoves().get(currNode)) {
        currNode.removePrevDFG(prev);
      }
    }
  }

  /**
   * ControlFlowSensitiveDFG Pass is perfomed on every Method
   *
   * @param node every node in the TranslationResult
   */
  public void handle(Node node) {
    if (node instanceof FunctionDeclaration) {
      ControlFlowSensitiveDFG controlFlowSensitiveDFG = new ControlFlowSensitiveDFG(node);
      controlFlowSensitiveDFG.handle();
      removeValues(controlFlowSensitiveDFG);
    }
  }
}
