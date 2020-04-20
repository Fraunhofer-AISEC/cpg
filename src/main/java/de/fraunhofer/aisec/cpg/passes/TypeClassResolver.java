package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.type.ObjectType;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeClassResolver extends Pass {
  private Map<String, ObjectType> nameObjectType = new HashMap<>();


  /**
   * Pass on the TypeSystem: Sets RecordDeclaration Relationship from ObjectType to RecordDeclaration
   *
   * @param translationResult
   */
  @Override
  public void accept(TranslationResult translationResult) {
    Set<Type> types = TypeManager.getInstance().getTypeState().keySet();
    for (Type t : types) {
      if (t instanceof ObjectType) {
        nameObjectType.put(t.getName(), (ObjectType) t);
      }
    }

    SubgraphWalker.IterativeGraphWalker walker = new SubgraphWalker.IterativeGraphWalker();
    walker.registerOnNodeVisit(this::handle);
    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      walker.iterate(tu);
    }
  }

  /**
   * Creates the recordDeclaration relationship between ObjectTypes and RecordDeclaration (from the
   * Type to the Class)
   *
   * @param node
   */
  public void handle(Node node) {
    if (node instanceof RecordDeclaration) {
      if (this.nameObjectType.containsKey(node.getName())) {
        this.nameObjectType.get(node.getName()).setRecordDeclaration((RecordDeclaration) node);
      }
    }
  }

  @Override
  public void cleanup() {
    this.nameObjectType.clear();
  }
}
