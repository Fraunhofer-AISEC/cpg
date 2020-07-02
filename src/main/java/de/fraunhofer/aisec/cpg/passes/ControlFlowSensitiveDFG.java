package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.type.ObjectType;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;

public class ControlFlowSensitiveDFG extends Pass{


    @Override
    public void cleanup() {

    }

    @Override
    public void accept(TranslationResult translationResult) {
        SubgraphWalker.IterativeGraphWalker walker = new SubgraphWalker.IterativeGraphWalker();
        walker.registerOnNodeVisit(this::handle);
        for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
            walker.iterate(tu);
        }
    }

    public void handle(Node node) {
        if (node instanceof MethodDeclaration) {
            handleMethod((MethodDeclaration) node);
        }
    }

    private void handleMethod(MethodDeclaration methodDeclaration){

    }
}
