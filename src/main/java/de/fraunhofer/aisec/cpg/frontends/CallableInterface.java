package de.fraunhofer.aisec.cpg.frontends;

import de.fraunhofer.aisec.cpg.graph.Node;

@FunctionalInterface
public interface CallableInterface<T extends Node> {
  void dispatch(T expr);
}
