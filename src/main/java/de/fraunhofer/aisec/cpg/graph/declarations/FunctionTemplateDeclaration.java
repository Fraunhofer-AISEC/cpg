package de.fraunhofer.aisec.cpg.graph.declarations;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

public class FunctionTemplateDeclaration extends TemplateDeclaration {

  @Relationship(value = "REALIZATION", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<FunctionDeclaration>> realization = new ArrayList<>();

  @Relationship(value = "PARAMETERS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<Declaration>> parameters = new ArrayList<>();

  public List<FunctionDeclaration> getRealization() {
    return unwrap(this.realization);
  }

  public List<PropertyEdge<FunctionDeclaration>> getRealizationPropertyEdge() {
    return this.realization;
  }

  public void addRealization(FunctionDeclaration realizedFunction) {
    PropertyEdge<FunctionDeclaration> propertyEdge = new PropertyEdge<>(this, realizedFunction);
    propertyEdge.addProperty(Properties.INDEX, this.parameters.size());
    this.realization.add(propertyEdge);
  }

  public void removeRealization(FunctionDeclaration realizedFunction) {
    this.realization.removeIf(propertyEdge -> propertyEdge.getEnd().equals(realizedFunction));
  }

  public List<Declaration> getParameters() {
    return unwrap(this.parameters);
  }

  public List<Declaration> getParametersOfClazz(Class<? extends Declaration> clazz) {
    List<Declaration> reducedParametersByType = new ArrayList<>();
    for (Declaration n : this.getParameters()) {
      if (clazz.isInstance(n)) {
        reducedParametersByType.add(n);
      }
    }
    return reducedParametersByType;
  }

  public List<PropertyEdge<Declaration>> getParametersPropertyEdge() {
    return this.parameters;
  }

  public void addParameter(TypeTemplateParamDeclaration parameterizedType) {
    PropertyEdge<Declaration> propertyEdge = new PropertyEdge<>(this, parameterizedType);
    propertyEdge.addProperty(Properties.INDEX, this.parameters.size());
    this.parameters.add(propertyEdge);
  }

  public void addParameter(NonTypeTemplateParamDeclaration nonTypeTemplateParamDeclaration) {
    PropertyEdge<Declaration> propertyEdge =
        new PropertyEdge<>(this, nonTypeTemplateParamDeclaration);
    propertyEdge.addProperty(Properties.INDEX, this.parameters.size());
    this.parameters.add(propertyEdge);
  }

  public void removeParameter(Declaration parameterizedType) {
    this.parameters.removeIf(propertyEdge -> propertyEdge.getEnd().equals(parameterizedType));
  }

  public void removeParameter(NonTypeTemplateParamDeclaration nonTypeTemplateParamDeclaration) {
    this.parameters.removeIf(
        propertyEdge -> propertyEdge.getEnd().equals(nonTypeTemplateParamDeclaration));
  }


  private List<String> singleTemplateParameters(String templateParameters) {
    return Arrays.asList(templateParameters.split(","));
  }

  @Override
  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof TypeTemplateParamDeclaration) {
      addIfNotContains(this.parameters, declaration);
    } else if (declaration instanceof NonTypeTemplateParamDeclaration) {
      addIfNotContains(this.parameters, declaration);
    } else if (declaration instanceof FunctionDeclaration) {
      addIfNotContains(this.realization, (FunctionDeclaration) declaration);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    FunctionTemplateDeclaration that = (FunctionTemplateDeclaration) o;
    return realization.equals(that.realization) && parameters.equals(that.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), realization, parameters);
  }
}
