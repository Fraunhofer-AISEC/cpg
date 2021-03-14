package de.fraunhofer.aisec.cpg.graph.types;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TemplateParameter;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.ogm.annotation.Relationship;

/**
 * ParameterizedTypes describe types, that are passed as Paramters to Classes E.g. uninitialized
 * generics in the graph are represented as ParameterizedTypes
 */
public class ParameterizedType extends Type {

  public ParameterizedType(Type type) {
    super(type);
  }

  public ParameterizedType(String typeName) {
    super(typeName);
  }

  @Override
  public Type reference(PointerType.PointerOrigin pointer) {
    return this;
  }

  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type duplicate() {
    return new ParameterizedType(this);
  }
}
