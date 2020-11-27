package de.fraunhofer.aisec.cpg.graph.edge;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.Persistable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RelationshipEntity()
public class PropertyEdge implements Persistable {

  /** Required field for object graph mapping. It contains the node id. */
  @Id @GeneratedValue private Long id;

  protected static final Logger log = LoggerFactory.getLogger(PropertyEdge.class);

  // Node where the edge is outgoing
  @StartNode private Node start;

  // Node where the edge is ingoing
  @EndNode private Node end;

  public PropertyEdge(Node start, Node end) {
    this.start = start;
    this.end = end;
    this.properties = new EnumMap<>(Properties.class);
  }

  public PropertyEdge(PropertyEdge propertyEdge) {
    this.start = propertyEdge.start;
    this.end = propertyEdge.end;
    this.properties = new EnumMap<>(Properties.class);
    this.properties.putAll(propertyEdge.properties);
  }

  public PropertyEdge(Node start, Node end, Map<Properties, Object> properties) {
    this.start = start;
    this.end = end;
    this.properties = properties;
  }

  /** Map containing all properties of an edge */
  @Convert(PropertyEdgeConverter.class)
  private Map<Properties, Object> properties;

  public static <S extends PropertyEdge> List<S> findPropertyEdgesByPredicate(
      Collection<S> edges, Predicate<S> predicate) {
    return edges.stream().filter(predicate).collect(Collectors.toList());
  }

  public Object getProperty(Properties property) {
    return properties.getOrDefault(property, null);
  }

  /**
   * Adds a property to a {@link PropertyEdge} If the object is not a built-in type you must provide
   * a serializer and deserializer in the {@link PropertyEdgeConverter}
   *
   * @param property String containing the name of the property
   * @param value Object containing the value of the property
   */
  public void addProperty(Properties property, Object value) {
    properties.put(property, value);
  }

  public void addProperties(Map<Properties, Object> propertyMap) {
    this.properties.putAll(propertyMap);
  }

  public Node getEnd() {
    return end;
  }

  public Node getStart() {
    return start;
  }

  /**
   * Add/Update index element of list of PropertyEdges
   *
   * @param propertyEdges propertyEdge list
   * @return new PropertyEdge list with updated index property
   */
  public static List<PropertyEdge> applyIndexProperty(List<PropertyEdge> propertyEdges) {
    int counter = 0;
    for (PropertyEdge propertyEdge : propertyEdges) {
      propertyEdge.addProperty(Properties.INDEX, counter);
      counter++;
    }
    return propertyEdges;
  }

  /**
   * Transforms a List of Nodes into targets of PropertyEdges depending on the direction of the
   * edge. Include Index Property as Lists are indexed
   *
   * @param nodes List of nodes that should be transformed into PropertyEdges
   * @param commonRelationshipNode node where all the Edges should start or end (depending on the
   *     direction)
   * @param outgoing direction of the edge
   * @return List of PropertyEdges with the targets of the nodes and index property.
   */
  public static List<PropertyEdge> transformIntoPropertyEdgeList(
      List<? extends Node> nodes, Node commonRelationshipNode, boolean outgoing) {
    List<PropertyEdge> propertyEdges = new ArrayList<>();
    for (Node n : nodes) {
      if (outgoing) {
        propertyEdges.add(new PropertyEdge(commonRelationshipNode, n));
      } else {
        propertyEdges.add(new PropertyEdge(n, commonRelationshipNode));
      }
    }
    return propertyEdges;
  }

  /**
   * @param obj is a collection of propertyedges
   * @param outgoing direction of the edges
   * @return collection of nodes containing the targets of the edges
   */
  private static Object unwrapPropertyEdgeCollection(Object obj, boolean outgoing) {
    Object element = null;
    Optional<?> value = ((Collection<?>) obj).stream().findAny();
    if (value.isPresent()) {
      element = value.get();
    }

    if (element instanceof PropertyEdge) {
      try {
        Collection<Node> outputCollection =
            (Collection<Node>) obj.getClass().getDeclaredConstructor().newInstance();
        for (PropertyEdge propertyEdge : (Collection<PropertyEdge>) obj) {
          if (outgoing) {
            outputCollection.add(propertyEdge.getEnd());
          } else {
            outputCollection.add(propertyEdge.getStart());
          }
        }
        return outputCollection;
      } catch (InstantiationException
          | IllegalAccessException
          | InvocationTargetException
          | NoSuchMethodException e) {
        log.warn("PropertyEdges could not be unwrapped");
      }
    }
    return obj;
  }

  /**
   * @param obj PropertyEdge that must be unwrapped
   * @param outgoing direction of the edge
   * @return node representing target of edge
   */
  public static Object unwrapPropertyEdge(Object obj, boolean outgoing) {
    if (obj instanceof PropertyEdge) {
      if (outgoing) {
        return ((PropertyEdge) obj).getEnd();
      } else {
        return ((PropertyEdge) obj).getStart();
      }
    } else if (obj instanceof Collection && !((Collection<?>) obj).isEmpty()) {
      return unwrapPropertyEdgeCollection(obj, outgoing);
    }
    return obj;
  }

  /**
   * Checks if an Object is a PropertyEdge or a collection of PropertyEdges
   *
   * @param f Field containing the object
   * @param obj object that is checked if it is a PropertyEdge
   * @return true if obj is/contains a PropertyEdge
   */
  public static boolean checkForPropertyEdge(Field f, Object obj) {
    if (obj instanceof PropertyEdge) {
      return true;
    } else if (obj instanceof Collection<?>) {
      return List.of(((ParameterizedType) f.getGenericType()).getActualTypeArguments())
          .contains(PropertyEdge.class);
    }
    return false;
  }

  /**
   * @param propertyEdges List of PropertyEdges
   * @param outgoing direction of the edge
   * @return List of nodes corresponding to the targets of the edges depending on their direction
   */
  public static List<Node> getTarget(List<PropertyEdge> propertyEdges, boolean outgoing) {
    List<Node> targets = new ArrayList<>();
    for (PropertyEdge propertyEdge : propertyEdges) {
      if (outgoing) {
        targets.add(propertyEdge.getEnd());
      } else {
        targets.add(propertyEdge.getStart());
      }
    }
    return targets;
  }

  public static List<PropertyEdge> removeElementFromList(
      List<PropertyEdge> propertyEdges, Node element, boolean end) {
    List<PropertyEdge> newPropertyEdges = new ArrayList<>();
    for (PropertyEdge propertyEdge : propertyEdges) {
      if (end && !propertyEdge.getEnd().equals(element)) {
        newPropertyEdges.add(propertyEdge);
      }
      if (!end && !propertyEdge.getStart().equals(element)) {
        newPropertyEdges.add(propertyEdge);
      }
    }
    return applyIndexProperty(newPropertyEdges);
  }

  /**
   * Note that the start and end node cannot be checked for equality here, as it would create an
   * endless loop. Check of start and end node must be done separately.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PropertyEdge)) return false;
    PropertyEdge propertyEdge = (PropertyEdge) obj;
    return Objects.equals(this.properties, propertyEdge.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(end, properties);
  }
}
