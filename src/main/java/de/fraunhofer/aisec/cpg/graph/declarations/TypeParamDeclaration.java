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

import de.fraunhofer.aisec.cpg.graph.HasDefault;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.Collection;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.neo4j.ogm.annotation.Relationship;

/** A declaration of a type template parameter */
public class TypeParamDeclaration extends ValueDeclaration
    implements HasType.SecondaryTypeEdge, HasDefault<Type> {

  /**
   * TemplateParameters can define a default for the type parameter Since the primary type edge
   * points to the ParameterizedType, the default edge is a secondary type edge. Therefore the
   * TypeResolver requires to implement the {@link HasType.SecondaryTypeEdge} to be aware of the
   * edge to be able to merge the type nodes.
   */
  @Relationship(value = "DEFAULT", direction = "OUTGOING")
  @SubGraph("AST")
  @Nullable
  private Type defaultType;

  @Nullable
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
    return Objects.equals(defaultType, that.defaultType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), defaultType);
  }

  @Override
  public void updateType(Collection<Type> typeState) {
    Type oldType = this.getDefault();
    if (oldType != null) {
      for (Type t : typeState) {
        if (t.equals(oldType)) {
          this.setDefault(t);
        }
      }
    }
  }
}
