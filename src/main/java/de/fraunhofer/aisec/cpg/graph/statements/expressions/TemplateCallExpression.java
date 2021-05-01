/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionTemplateDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;

import java.util.*;

import org.neo4j.ogm.annotation.Relationship;

public class TemplateCallExpression extends CallExpression implements HasType.SecondaryTypeEdge {
  @Relationship(value = "TEMPLATE_PARAMETERS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<Node>> templateParameters = new ArrayList<>();

  @Relationship(value = "INITIALIZATION", direction = "OUTGOING")
  @SubGraph("AST")
  private FunctionTemplateDeclaration initialization;

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

  public void replaceTypeTemplateParameter(Type oldType, Type newType) {
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

  public void setTemplateParameters(List<PropertyEdge<Node>> templateParameters) {
    this.templateParameters = templateParameters;
  }

  public FunctionTemplateDeclaration getInitialization() {
    return initialization;
  }

  public void setInitialization(FunctionTemplateDeclaration initialization) {
    this.initialization = initialization;
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
          this.replaceTypeTemplateParameter(t, t2);
        }
      }
    }
  }
}
