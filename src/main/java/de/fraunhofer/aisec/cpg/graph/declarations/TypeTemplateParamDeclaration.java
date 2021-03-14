package de.fraunhofer.aisec.cpg.graph.declarations;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TemplateParameter;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.neo4j.ogm.annotation.Relationship;

public class TypeTemplateParamDeclaration extends ValueDeclaration
    implements TemplateParameter<ParameterizedType> {

  @Relationship(value = "POSSIBLE_INITIALIZATIONS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<ParameterizedType>> possibleInitializations = new ArrayList<>();

  @Relationship(value = "DEFAULT", direction = "OUTGOING")
  @SubGraph("AST")
  private ParameterizedType defaultType;

  public List<ParameterizedType> getPossibleInitializations() {
    return unwrap(this.possibleInitializations);
  }

  public List<PropertyEdge<ParameterizedType>> getPossibleInitializationsPropertyEdge() {
    return this.possibleInitializations;
  }

  public void addPossibleInitialization(ParameterizedType parameterizedType) {
    PropertyEdge<ParameterizedType> propertyEdge = new PropertyEdge<>(this, parameterizedType);
    propertyEdge.addProperty(Properties.INDEX, this.possibleInitializations.size());
    this.possibleInitializations.add(propertyEdge);
  }

  public ParameterizedType getDefault() {
    return defaultType;
  }

  public void setDefault(ParameterizedType defaultType) {
    this.defaultType = defaultType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    TypeTemplateParamDeclaration that = (TypeTemplateParamDeclaration) o;
    return possibleInitializations.equals(that.possibleInitializations) && Objects.equals(defaultType, that.defaultType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), possibleInitializations, defaultType);
  }
}
