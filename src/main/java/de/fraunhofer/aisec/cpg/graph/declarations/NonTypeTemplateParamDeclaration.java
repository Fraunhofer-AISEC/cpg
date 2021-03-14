package de.fraunhofer.aisec.cpg.graph.declarations;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.ogm.annotation.Relationship;

public class NonTypeTemplateParamDeclaration extends ParamVariableDeclaration {
  // TODO vfsrfs: Use node as target of possibleInitializations?
  @Relationship(value = "POSSIBLE_INITIALIZATIONS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<Node>> possibleInitializations = new ArrayList<>();

  public List<Node> getPossibleInitializations() {
    return unwrap(this.possibleInitializations);
  }

  public List<PropertyEdge<Node>> getPossibleInitializationsPropertyEdge() {
    return this.possibleInitializations;
  }

  public void addParameter(Node node) {
    PropertyEdge<Node> propertyEdge = new PropertyEdge<>(this, node);
    propertyEdge.addProperty(Properties.INDEX, this.possibleInitializations.size());
    this.possibleInitializations.add(propertyEdge);
  }
}
