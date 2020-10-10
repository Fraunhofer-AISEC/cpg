package de.fraunhofer.aisec.cpg.graph.edge;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.Persistable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal;
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

  public PropertyEdge(Node start, Node end, Map<Properties, Object> properties) {
    this.start = start;
    this.end = end;
    this.properties = properties;
  }

  /**
   * Map containing all properties of an edge
   */
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
   * @param value    Object containing the value of the property
   */
  public void addProperty(Properties property, Object value) {
    properties.put(property, value);
  }

  public Node getEnd() {
    return end;
  }

  public Node getStart() {
    return start;
  }

  /**
   * Note that the start and end node cannot be checked for equality here, as it would create an endless loop. Check of start and end node must be done separately.
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
