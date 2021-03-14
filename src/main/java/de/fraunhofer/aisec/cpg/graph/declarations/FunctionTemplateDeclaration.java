package de.fraunhofer.aisec.cpg.graph.declarations;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import java.util.*;
import org.neo4j.ogm.annotation.Relationship;

public class FunctionTemplateDeclaration extends TemplateDeclaration {

  @Relationship(value = "REALIZATION", direction = "OUTGOING")
  @SubGraph("AST")
  private FunctionDeclaration realization;

  @Relationship(value = "PARAMETERS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<Node>> parameters = new ArrayList<>();

  public FunctionDeclaration getRealization() {
    return realization;
  }

  public void setRealization(FunctionDeclaration realization) {
    this.realization = realization;
  }

  public List<Node> getParameters() {
    return unwrap(this.parameters);
  }

  public List<Node> getParametersOfClazz(Class<? extends Node> clazz) {
    List<Node> reducedParametersByType = new ArrayList<>();
    for (Node n : this.getParameters()) {
      if (clazz.isInstance(n)) {
        reducedParametersByType.add(n);
      }
    }
    return reducedParametersByType;
  }

  public List<PropertyEdge<Node>> getParametersPropertyEdge() {
    return this.parameters;
  }

  public void addParameter(ParameterizedType parameterizedType) {
    PropertyEdge<Node> propertyEdge = new PropertyEdge<>(this, parameterizedType);
    propertyEdge.addProperty(Properties.INDEX, this.parameters.size());
    this.parameters.add(propertyEdge);
  }

  public void addParameter(NonTypeTemplateParamDeclaration nonTypeTemplateParamDeclaration) {
    PropertyEdge<Node> propertyEdge = new PropertyEdge<>(this, nonTypeTemplateParamDeclaration);
    propertyEdge.addProperty(Properties.INDEX, this.parameters.size());
    this.parameters.add(propertyEdge);
  }

  public void removeParameter(ParameterizedType parameterizedType) {
    this.parameters.removeIf(propertyEdge -> propertyEdge.getEnd().equals(parameterizedType));
  }

  public void removeParameter(NonTypeTemplateParamDeclaration nonTypeTemplateParamDeclaration) {
    this.parameters.removeIf(
        propertyEdge -> propertyEdge.getEnd().equals(nonTypeTemplateParamDeclaration));
  }

  public Map<String, Type> applyParametersToFuncSignature(String templateParameters) {
    List<String> singleTemplateParameters = singleTemplateParameters(templateParameters);
    Map<String, Type> map = new HashMap<>();

    List<Node> typeParameters = this.getParameters();
    // TODO vfsrfs: Problem with auto typing (no explicit information of type in < >)
    if (singleTemplateParameters.size() != typeParameters.size()) {
      return map;
    }

    for (int i = 0; i < typeParameters.size(); i++) {
      if (typeParameters.get(i) instanceof ParameterizedType) {
        map.put(
            typeParameters.get(i).getName(),
            TypeParser.createFrom(singleTemplateParameters.get(i), false));
      }
    }

    return map;
  }

  private List<String> singleTemplateParameters(String templateParameters) {
    return Arrays.asList(templateParameters.split(","));
  }
}
