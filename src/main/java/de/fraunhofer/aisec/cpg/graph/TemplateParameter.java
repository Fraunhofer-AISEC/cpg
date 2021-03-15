package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;

import java.util.List;

public interface TemplateParameter<T extends Node> {
  public List<T> getPossibleInitializations();

  public List<PropertyEdge<T>> getPossibleInitializationsPropertyEdge();

  public void addPossibleInitialization(T t);

}
