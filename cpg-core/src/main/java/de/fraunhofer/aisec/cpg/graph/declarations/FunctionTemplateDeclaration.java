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

import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

/** Node representing a declaration of a FunctionTemplate */
public class FunctionTemplateDeclaration extends TemplateDeclaration {

  /**
   * Edges pointing to all FunctionDeclarations that are realized by the FunctionTemplate. Before
   * the expansion pass there is only a single FunctionDeclaration which is instantiated After the
   * expansion pass for each instantiation of the FunctionTemplate there will be a realization
   */
  @Relationship(value = "REALIZATION", direction = "OUTGOING")
  private final List<PropertyEdge<FunctionDeclaration>> realization = new ArrayList<>();

  public List<FunctionDeclaration> getRealization() {
    return unwrap(this.realization);
  }

  public List<Declaration> getRealizationDeclarations() {
    return new ArrayList<>(getRealization());
  }

  public List<PropertyEdge<FunctionDeclaration>> getRealizationPropertyEdge() {
    return this.realization;
  }

  public void addRealization(FunctionDeclaration realizedFunction) {
    PropertyEdge<FunctionDeclaration> propertyEdge = new PropertyEdge<>(this, realizedFunction);
    propertyEdge.addProperty(Properties.INDEX, this.realization.size());
    this.realization.add(propertyEdge);
  }

  public void removeRealization(FunctionDeclaration realizedFunction) {
    this.realization.removeIf(propertyEdge -> propertyEdge.getEnd().equals(realizedFunction));
  }

  @Override
  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof TypeParamDeclaration
        || declaration instanceof ParamVariableDeclaration) {
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
    return Objects.equals(getRealization(), that.getRealization())
        && PropertyEdge.Companion.propertyEqualsList(realization, that.realization)
        && Objects.equals(getParameters(), that.getParameters())
        && PropertyEdge.Companion.propertyEqualsList(parameters, that.parameters);
  }

  // Do NOT add parameters to hashcode, as they are added incrementally to the list. If the
  // parameters field is added, the ScopeManager is not able to find it anymore and we cannot leave
  // the TemplateScope. Analogous for realization
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }
}
