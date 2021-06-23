package de.fraunhofer.aisec.cpg.meta;

import de.fraunhofer.aisec.cpg.graph.Node;
import java.util.Map;
import java.util.Objects;

public class Edge {

  private final String label;
  private final Map<String, Object> properties;
  private final Node from, to;

  public Edge(Node from, String label, Map<String, Object> properties, Node to) {
    this.label = label;
    this.properties = properties;
    this.from = from;
    this.to = to;
  }

  public String getLabel() {
    return label;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Node getFrom() {
    return from;
  }

  public Node getTo() {
    return to;
  }

  @Override
  public String toString() {
    return "{" + from + "} --" + label + "-> {" + to + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Edge edge = (Edge) o;
    return Objects.equals(label, edge.label)
        && Objects.equals(properties, edge.properties)
        && from == edge.from
        && to == edge.to;
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, properties, from, to);
  }
}
