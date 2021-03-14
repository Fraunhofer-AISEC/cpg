package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;

import java.util.List;

public interface TemplateParameter<T extends Node> {
  public List<T> getPossibleInitializations();

  public List<PropertyEdge<T>> getPossibleInitializationsPropertyEdge();

  public void addPossibleInitialization(T t);

  public T getDefault();

  public void setDefault(T defaultT);
}
