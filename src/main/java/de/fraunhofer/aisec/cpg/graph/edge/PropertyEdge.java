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

@RelationshipEntity()
public class PropertyEdge implements Persistable {

  /** Required field for object graph mapping. It contains the node id. */
  @Id @GeneratedValue private Long id;

  // Node where the edge is outgoing
  @StartNode private Node start;

  // Node where the edge is ingoing
  @EndNode private Node end;

  public PropertyEdge(Node start, Node end) {
    this.start = start;
    this.end = end;
    this.properties = new HashMap<>();
  }

  public PropertyEdge(PropertyEdge propertyEdge) {
    this.start = propertyEdge.start;
    this.end = propertyEdge.end;
    this.properties = new HashMap<>();
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

  public static List<PropertyEdge> applyIndexProperty(List<PropertyEdge> propertyEdges) {
    int counter = 0;
    for (PropertyEdge propertyEdge : propertyEdges) {
      propertyEdge.addProperty(Properties.Index, counter);
      counter++;
    }
    return propertyEdges;
  }

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

  public static Object unwrapPropertyEdge(Object obj, Boolean outgoing) {
    if (obj instanceof PropertyEdge) {
      if (outgoing) {
        return ((PropertyEdge) obj).getEnd();
      } else {
        return ((PropertyEdge) obj).getStart();
      }
    } else if (obj instanceof Collection && ((Collection<?>) obj).size() > 0) {
      Object element = ((Collection<?>) obj).stream().findAny().get();
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
          e.printStackTrace();
        }
      }
    }
    return obj;
  }

  public static boolean checkForPropertyEdge(Field f, Object obj) {
    if (obj instanceof PropertyEdge) {
      return true;
    } else if (obj instanceof Collection<?>) {
      return List.of(((ParameterizedType) f.getGenericType()).getActualTypeArguments())
          .contains(PropertyEdge.class);
    }
    return false;
  }

  public static List<? extends Node> getTarget(List<PropertyEdge> propertyEdges, boolean outgoing) {
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
