package de.fraunhofer.aisec.cpg.graph.declarations;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TemplateParameter;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.ogm.annotation.Relationship;

public class TypeTemplateParamDeclaration extends ValueDeclaration
    implements TemplateParameter<Type> {

  @Relationship(value = "POSSIBLE_INITIALIZATIONS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<Type>> possibleInitializations = new ArrayList<>();

  @Relationship(value = "DEFAULT", direction = "OUTGOING")
  @SubGraph("AST")
  private Type defaultType;

  public List<Type> getPossibleInitializations() {
    return unwrap(this.possibleInitializations);
  }

  public List<PropertyEdge<Type>> getPossibleInitializationsPropertyEdge() {
    return this.possibleInitializations;
  }

  public void addPossibleInitialization(Type parameterizedType) {
    PropertyEdge<Type> propertyEdge = new PropertyEdge<>(this, parameterizedType);
    propertyEdge.addProperty(Properties.INDEX, this.possibleInitializations.size());
    this.possibleInitializations.add(propertyEdge);
  }

  public Type getDefault() {
    return defaultType;
  }

  public void setDefault(Type defaultType) {
    this.defaultType = defaultType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    TypeTemplateParamDeclaration that = (TypeTemplateParamDeclaration) o;
    return possibleInitializations.equals(that.possibleInitializations)
        && Objects.equals(defaultType, that.defaultType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), possibleInitializations, defaultType);
  }
}
