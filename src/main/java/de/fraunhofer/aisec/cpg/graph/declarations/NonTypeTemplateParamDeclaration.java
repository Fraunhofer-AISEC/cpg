package de.fraunhofer.aisec.cpg.graph.declarations;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TemplateParameter;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.ogm.annotation.Relationship;

public class NonTypeTemplateParamDeclaration extends ParamVariableDeclaration
    implements TemplateParameter<Expression> {
  @Relationship(value = "POSSIBLE_INITIALIZATIONS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<Expression>> possibleInitializations = new ArrayList<>();

  public List<Expression> getPossibleInitializations() {
    return unwrap(this.possibleInitializations);
  }

  public List<PropertyEdge<Expression>> getPossibleInitializationsPropertyEdge() {
    return this.possibleInitializations;
  }

  public void addPossibleInitialization(Expression expression) {
    PropertyEdge<Expression> propertyEdge = new PropertyEdge<>(this, expression);
    propertyEdge.addProperty(Properties.INDEX, this.possibleInitializations.size());
    this.possibleInitializations.add(propertyEdge);
  }
}
