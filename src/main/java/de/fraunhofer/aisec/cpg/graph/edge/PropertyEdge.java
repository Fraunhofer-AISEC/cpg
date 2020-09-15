package de.fraunhofer.aisec.cpg.graph.edge;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.Persistable;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@RelationshipEntity(type = "GENERICS")
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

  /**
   * Map containing all properties of an edge
   */
  @Convert(PropertyEdgeConverter.class)
  private Map<String, Object> properties;

  public Object getProperty(String property) {
    return properties.getOrDefault(property, null);
  }

  /**
   * Adds a property to a {@link PropertyEdge}
   * If the object is not a built-in type you must provide a serializer and deserializer in the {@link PropertyEdgeConverter}
   * @param property String containing the name of the property
   * @param value Object containing the value of the property
   */
  public void addProperty(String property, Object value) {
    properties.put(property, value);
  }

  public Node getEnd() {
    return end;
  }

  public Node getStart() {
    return start;
  }
}
