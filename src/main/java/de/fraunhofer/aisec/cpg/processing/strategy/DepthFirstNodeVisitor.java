package de.fraunhofer.aisec.cpg.processing.strategy;

import de.fraunhofer.aisec.cpg.processing.AbstractVisitor;
import de.fraunhofer.aisec.cpg.processing.IAction;
import de.fraunhofer.aisec.cpg.processing.IStrategy;
import de.fraunhofer.aisec.cpg.processing.IVisitable;
import java.util.HashSet;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Depth-first visitor that avoid cycles by skipping already visited nodes.
 *
 * @param <V>
 */
public class DepthFirstNodeVisitor<V extends IVisitable> extends AbstractVisitor<V> {
  // Set of visited nodes to avoid endless cycles
  private HashSet<V> visited = new HashSet<>();

  public DepthFirstNodeVisitor(@NonNull IAction<V> action, @NonNull IStrategy<V> strategy) {
    super(action, strategy);
  }

  @Override
  public void visit(V node) {
    // Skip node if already visited.
    if (!visited.contains(node)) {
      visited.add(node);

      // First, act on node
      action.doSth(node);

      // Then, act on successors
      Iterator<V> it = iterator.getIterator(node);
      while (it.hasNext()) {
        it.next().accept(this);
      }
    }
  }
}
