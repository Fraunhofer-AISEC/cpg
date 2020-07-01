package de.fraunhofer.aisec.cpg.processing;

/**
 * A visitor visits visitable objects.
 *
 * @param <V> V will typically implement {@code IVisitable}.
 */
public interface IVisitor<V> {
  void visit(V node);
}
