package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.List;

public interface TemplateParameter {
  public List<Expression> getPossibleInitializations();

  public List<PropertyEdge<Expression>> getPossibleInitializationsPropertyEdge();

  public void addPossibleInitialization(Expression expression);
}
