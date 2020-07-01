package de.fraunhofer.aisec.cpg.processing;

/**
 * Functional interface for actions that can be applied to visited nodes.
 *
 * @param <V>
 */
public interface IAction<V> {
  void doSth(V node);
}
