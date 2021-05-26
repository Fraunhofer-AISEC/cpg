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

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

/** Node representing a declaration of a ClassTemplate */
public class ClassTemplateDeclaration extends TemplateDeclaration {
  /**
   * Edges pointing to all RecordDeclarations that are realized by the ClassTempalte. Before the
   * expansion pass there is only a single RecordDeclaration which is instantiated after the
   * expansion pass for each instantiation of the ClassTemplate there will be a realization
   */
  @Relationship(value = "REALIZATION", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<RecordDeclaration>> realization = new ArrayList<>();

  public List<RecordDeclaration> getRealization() {
    return unwrap(this.realization);
  }

  public List<Declaration> getRealizationDeclarations() {
    return new ArrayList<>(getRealization());
  }

  public List<PropertyEdge<RecordDeclaration>> getRealizationPropertyEdge() {
    return this.realization;
  }

  public void addRealization(RecordDeclaration realizedRecord) {
    PropertyEdge<RecordDeclaration> propertyEdge = new PropertyEdge<>(this, realizedRecord);
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
    } else if (declaration instanceof RecordDeclaration) {
      addIfNotContains(this.realization, (RecordDeclaration) declaration);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ClassTemplateDeclaration that = (ClassTemplateDeclaration) o;
    return realization.equals(that.realization) && parameters.equals(that.parameters);
  }

  // Do NOT add parameters to hashcode, as they are added incrementally to the list. If the
  // parameters field is added, the ScopeManager is not able to find it anymore and we cannot leave
  // the TemplateScope. Analogous for realization
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode());
  }
}
