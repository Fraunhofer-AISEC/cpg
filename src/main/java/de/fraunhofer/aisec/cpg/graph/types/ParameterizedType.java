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
public class ParameterizedType extends Type implements TemplateParameter {

  public ParameterizedType(Type type) {
    super(type);
  }

  public ParameterizedType(String typeName) {
    super(typeName);
  }

  @Relationship(value = "POSSIBLE_INITIALIZATIONS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<Expression>> possibleInitializations = new ArrayList<>();

  public List<Expression> getPossibleInitializations() {
    return unwrap(this.possibleInitializations);
  }

  public List<PropertyEdge<Expression>> getPossibleInitializationsPropertyEdge() {
    return this.possibleInitializations;
  }

  public void addPossibleInitialization(Expression expression) {
    PropertyEdge<Expression> propertyEdge = new PropertyEdge<>(this, expression);
    propertyEdge.addProperty(Properties.INDEX, this.possibleInitializations.size());
    this.possibleInitializations.add(propertyEdge);
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
