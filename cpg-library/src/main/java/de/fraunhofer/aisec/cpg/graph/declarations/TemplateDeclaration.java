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
package de.fraunhofer.aisec.cpg.graph.declarations;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.neo4j.ogm.annotation.Relationship;

/** Abstract class representing the template concept */
public abstract class TemplateDeclaration extends Declaration implements DeclarationHolder {
  public enum TemplateInitialization {
    AUTO_DEDUCTION,
    DEFAULT,
    EXPLICIT,
    UNKNOWN
  }

  /** Parameters the Template requires for instantiation */
  @Relationship(value = "PARAMETERS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<Declaration>> parameters = new ArrayList<>();

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

  public void addParameter(TypeParamDeclaration parameterizedType) {
    PropertyEdge<Declaration> propertyEdge = new PropertyEdge<>(this, parameterizedType);
    propertyEdge.addProperty(Properties.INDEX, this.parameters.size());
    this.parameters.add(propertyEdge);
  }

  public void addParameter(ParamVariableDeclaration nonTypeTemplateParamDeclaration) {
    PropertyEdge<Declaration> propertyEdge =
        new PropertyEdge<>(this, nonTypeTemplateParamDeclaration);
    propertyEdge.addProperty(Properties.INDEX, this.parameters.size());
    this.parameters.add(propertyEdge);
  }

  public void removeParameter(TypeParamDeclaration parameterizedType) {
    this.parameters.removeIf(propertyEdge -> propertyEdge.getEnd().equals(parameterizedType));
  }

  public void removeParameter(ParamVariableDeclaration nonTypeTemplateParamDeclaration) {
    this.parameters.removeIf(
        propertyEdge -> propertyEdge.getEnd().equals(nonTypeTemplateParamDeclaration));
  }

  public abstract List<Declaration> getRealizationDeclarations();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    TemplateDeclaration that = (TemplateDeclaration) o;
    return Objects.equals(parameters, that.parameters)
        && PropertyEdge.propertyEqualsList(parameters, that.parameters);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
