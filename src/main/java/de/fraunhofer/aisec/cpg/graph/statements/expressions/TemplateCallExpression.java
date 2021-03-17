package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.neo4j.ogm.annotation.Relationship;

public class TemplateCallExpression extends CallExpression implements HasType.SecondaryTypeEdge {
  @Relationship(value = "TEMPLATE_PARAMETERS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<Node>> templateParameters = new ArrayList<>();

  public List<PropertyEdge<Node>> getTemplateParametersPropertyEdge() {
    return templateParameters;
  }

  public List<Node> getTemplateParameters() {
    return PropertyEdge.unwrap(this.templateParameters);
  }

  public List<Type> getTypeTemplateParameters() {
    List<Type> types = new ArrayList<>();
    for (Node n : getTemplateParameters()) {
      if (n instanceof Type) {
        types.add((Type) n);
      }
    }
    return types;
  }

  public void addTemplateParameter(Type typeTemplateParam) {
    PropertyEdge<Node> propertyEdge = new PropertyEdge<>(this, typeTemplateParam);
    propertyEdge.addProperty(Properties.INDEX, this.templateParameters.size());
    this.templateParameters.add(propertyEdge);
  }

  public void replateTypeTemplateParameter(Type oldType, Type newType) {
    for (int i = 0; i < this.templateParameters.size(); i++) {
      PropertyEdge<Node> propertyEdge = this.templateParameters.get(i);
      if (propertyEdge.getEnd().equals(oldType)) {
        PropertyEdge<Node> replacement = new PropertyEdge<>(this, newType);
        propertyEdge.addProperty(Properties.INDEX, i);
        this.templateParameters.set(i, replacement);
      }
    }
  }

  public void addTemplateParameter(Expression expressionTemplateParam) {
    PropertyEdge<Node> propertyEdge = new PropertyEdge<>(this, expressionTemplateParam);
    propertyEdge.addProperty(Properties.INDEX, this.templateParameters.size());
    this.templateParameters.add(propertyEdge);
  }

  public void removeRealization(Node templateParam) {
    this.templateParameters.removeIf(propertyEdge -> propertyEdge.getEnd().equals(templateParam));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    TemplateCallExpression that = (TemplateCallExpression) o;
    return templateParameters.equals(that.templateParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }

  @Override
  public void updateType(Collection<Type> typeState) {

    for (Type t : this.getTypeTemplateParameters()) {
      for (Type t2 : typeState) {
        if (t2.equals(t)) {
          this.replateTypeTemplateParameter(t, t2);
        }
      }
    }
  }
}
