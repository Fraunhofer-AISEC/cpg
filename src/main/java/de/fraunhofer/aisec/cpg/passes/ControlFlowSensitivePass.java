package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.helpers.ControlFlowSensitiveDFG;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;

public class ControlFlowSensitivePass extends Pass {

  @Override
  public void cleanup() {}

  @Override
  public void accept(TranslationResult translationResult) {
    SubgraphWalker.IterativeGraphWalker walker = new SubgraphWalker.IterativeGraphWalker();
    walker.registerOnNodeVisit(this::handle);
    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }
  }

  /**
   * ControlFlowSensitiveDFG Pass is perfomed on every Method
   *
   * @param node every node in the TranslationResult
   */
  public void handle(Node node) {
    if (node instanceof MethodDeclaration) {
      ControlFlowSensitiveDFG controlFlowSensitiveDFG = new ControlFlowSensitiveDFG(node);
      controlFlowSensitiveDFG.handle();
    }
  }
}
