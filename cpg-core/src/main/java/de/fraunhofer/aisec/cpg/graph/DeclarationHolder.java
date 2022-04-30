/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface DeclarationHolder {

  /**
   * Adds the specified declaration to this declaration holder. Ideally, the declaration holder
   * should use the {@link #addIfNotContains(Collection, Declaration)} method to consistently add
   * declarations.
   *
   * @param declaration the declaration
   */
  void addDeclaration(@NotNull Declaration declaration);

  default <N extends Declaration> void addIfNotContains(Collection<N> collection, N declaration) {
    if (!collection.contains(declaration)) {
      collection.add(declaration);
    }
  }

  default <T extends Node> void addIfNotContains(
      Collection<PropertyEdge<T>> collection, T declaration) {
    addIfNotContains(collection, declaration, true);
  }

  /**
   * Adds a declaration to a collection of property edges, which contain the declarations
   *
   * @param collection the collection
   * @param declaration the declaration
   * @param <T> the type of the declaration
   * @param outgoing whether the property is outgoing
   */
  default <T extends Node> void addIfNotContains(
      Collection<PropertyEdge<T>> collection, T declaration, boolean outgoing) {
    // create a new property edge
    var propertyEdge =
        outgoing
            ? new PropertyEdge<>((Node) this, declaration)
            : new PropertyEdge<>(declaration, (T) this);

    // set the index property
    propertyEdge.addProperty(Properties.INDEX, collection.size());

    boolean contains = false;
    for (PropertyEdge<T> element : collection) {
      if (element.getEnd().equals(propertyEdge.getEnd())) {
        contains = true;
        break;
      }
    }
    if (!contains) {
      collection.add(propertyEdge);
    }
  }

  @NotNull
  List<Declaration> getDeclarations();

  default void plusAssign(@NotNull Declaration declaration) {
    this.addDeclaration(declaration);
  }
}
