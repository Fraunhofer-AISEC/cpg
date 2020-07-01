package de.fraunhofer.aisec.cpg.processing;

/**
 * An object that can be visited by a visitor.
 *
 * @param <V>
 */
public interface IVisitable<V> {
  void accept(IVisitor<V> visitor);
}
