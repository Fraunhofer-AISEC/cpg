package de.fraunhofer.aisec.cpg.processing;

import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An object that can be visited by a visitor.
 *
 * @param <V>
 */
public abstract class IVisitable<V extends IVisitable> {

  /**
   * @param strategy Traversal strategy.
   * @param visitor Instance of the visitor to call.
   */
  public void accept(IStrategy<V> strategy, IVisitor<V> visitor) {
    if (!visitor.getVisited().contains(this)) {
      visitor.getVisited().add((V) this);
      visitor.visit((V) this);
      @NonNull Iterator<V> it = strategy.getIterator((V) this);
      while (it.hasNext()) {
        it.next().accept(strategy, visitor);
      }
    }
  }
}
