package de.fraunhofer.aisec.cpg.processing;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Iterator;

/**
 * The strategy determines the order in which nodes in the structure are traversed.
 *
 * For each node, the strategy returns a non-null but possibly empty iterator over the successors.
 * @param <V>
 */
public interface IStrategy<V> {
  @NonNull
  Iterator<V> getIterator(V v);
}
