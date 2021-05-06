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

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TemplateParameter;
import de.fraunhofer.aisec.cpg.graph.types.ObjectType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.*;
import org.neo4j.ogm.annotation.Relationship;

public class TypeParamDeclaration extends ValueDeclaration
    implements TemplateParameter<Type>, HasType.SecondaryTypeEdge {

  @Relationship(value = "POSSIBLE_INITIALIZATIONS", direction = "OUTGOING")
  @SubGraph("AST")
  protected Set<Type> possibleInitializations = new HashSet<>();

  @Relationship(value = "DEFAULT", direction = "OUTGOING")
  @SubGraph("AST")
  private Type defaultType;

  public Set<Type> getPossibleInitializations() {
    return this.possibleInitializations;
  }

  public void addPossibleInitialization(Type parameterizedType) {
    this.possibleInitializations.add(parameterizedType);
  }

  public boolean canBeInstantiated(Type type) {
    return type instanceof ObjectType;
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
    TypeParamDeclaration that = (TypeParamDeclaration) o;
    return possibleInitializations.equals(that.possibleInitializations)
        && Objects.equals(defaultType, that.defaultType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), possibleInitializations, defaultType);
  }

  @Override
  public void updateType(Collection<Type> typeState) {
    Type oldType = this.getDefault();
    if (oldType != null) {
      for (Type t : typeState) {
        if (t.equals(oldType)) {
          this.setDefault(t);
          this.addPossibleInitialization(t);
        }
      }
    }
  }
}
