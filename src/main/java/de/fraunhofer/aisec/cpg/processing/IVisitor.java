package de.fraunhofer.aisec.cpg.processing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

/**
 * Reflective visitor that visits the most specific implementation of visit() methods.
 *
 * @param <V> V must implement {@code IVisitable}.
 */
public abstract class IVisitor<V extends IVisitable> {
  private final Collection<V> visited = new HashSet<>();

  public Collection<V> getVisited() {
    return visited;
  }

  public void visit(V t) {
    try {
      Method mostSpecificVisit = this.getClass().getMethod("visit", new Class[] {t.getClass()});

      if (mostSpecificVisit != null) {
        mostSpecificVisit.setAccessible(true);
        mostSpecificVisit.invoke(this, new Object[] {t});
      }
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      // Nothing to do here
    }
  }
}
