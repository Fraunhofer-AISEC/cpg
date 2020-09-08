package de.fraunhofer.aisec.cpg.graph.edge;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.Persistable;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.StartNode;

public class PropertyEdge implements Persistable {

  /** Required field for object graph mapping. It contains the node id. */
  @Id @GeneratedValue private Long id;

  @StartNode private Node start;

  @EndNode private Node end;

  public PropertyEdge(Node start, Node end) {
    this.start = start;
    this.end = end;
  }

  public PropertyEdge() {}

  public Node getEnd() {
    return end;
  }

  public Node getStart() {
    return start;
  }
}
