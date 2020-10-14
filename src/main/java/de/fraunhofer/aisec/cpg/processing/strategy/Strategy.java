package de.fraunhofer.aisec.cpg.processing.strategy;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.lang.reflect.Field;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

/** Strategies (iterators) for traversing graphs to be used by visitors. */
public class Strategy {

  private Strategy() {
    // Do not call.
  }

  /**
   * Do not traverse any nodes.
   *
   * @param x
   * @return
   */
  @NonNull
  public static Iterator<Node> NO_STRATEGY(@NonNull Node x) {
    return Collections.emptyIterator();
  }

  /**
   * Traverse Evaluation Order Graph in forward direction.
   *
   * @param x Current node in EOG.
   * @return Iterator over successors.
   */
  @NonNull
  public static Iterator<Node> EOG_FORWARD(@NonNull Node x) {
    return x.getNextEOG().iterator();
  }

  /**
   * Traverse Evaluation Order Graph in backward direction.
   *
   * @param x Current node in EOG.
   * @return Iterator over successors.
   */
  @NonNull
  public static Iterator<Node> EOG_BACKWARD(@NonNull Node x) {
    return x.getPrevEOG().iterator();
  }

  /**
   * Traverse Data Flow Graph in forward direction.
   *
   * @param x Current node in DFG.
   * @return Iterator over successors.
   */
  @NonNull
  public static Iterator<Node> DFG_FORWARD(@NonNull Node x) {
    return x.getNextDFG().iterator();
  }

  /**
   * Traverse Data Flow Graph in backward direction.
   *
   * @param x Current node in DFG.
   * @return Iterator over successors.
   */
  @NonNull
  public static Iterator<Node> DFG_BACKWARD(@NonNull Node x) {
    return x.getPrevDFG().iterator();
  }

  /**
   * Traverse AST in forward direction.
   *
   * @param x
   * @return
   */
  @NonNull
  public static Iterator<Node> AST_FORWARD(@NonNull Node x) {
    HashSet<Node> children = new HashSet<>(); // Set for duplicate elimination
    Class<?> classType = x.getClass();
    for (Field field : getAllFields(classType)) {
      SubGraph subGraph = field.getAnnotation(SubGraph.class);
      if (subGraph != null && Arrays.asList(subGraph.value()).contains("AST")) {
        try {
          // disable access mechanisms
          field.setAccessible(true);

          Object obj = field.get(x);

          // restore old state
          field.setAccessible(false);

          // skip, if null
          if (obj == null) {
            continue;
          }

          if (field.getAnnotation(Relationship.class) != null) {
            boolean outgoing =
                field.getAnnotation(Relationship.class).direction().equals("OUTGOING");
            obj = PropertyEdge.unwrapPropertyEdge(obj, outgoing);
          }

          if (obj instanceof Node) {
            children.add((Node) obj);
          } else if (obj instanceof Collection) {
            Collection<? extends Node> astChildren = (Collection<? extends Node>) obj;
            astChildren.removeIf(Objects::isNull);
            children.addAll(astChildren);
          }
        } catch (IllegalAccessException ex) {
          // Nothing to do here
        }
      }
    }
    return children.iterator();
  }

  /**
   * Helper method to return all Fields of a Class.
   *
   * @param classType
   * @return
   */
  private static Collection<Field> getAllFields(Class<?> classType) {
    if (classType.getSuperclass() != null) {
      Collection<Field> fields = getAllFields(classType.getSuperclass());
      fields.addAll(Arrays.asList(classType.getDeclaredFields()));

      return fields;
    }

    return new ArrayList<>();
  }
}
