/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface DeclarationHolder {

  /**
   * Adds the specified declaration to this declaration holder. Ideally, the declaration holder
   * should use the {@link #addIfNotContains(Collection, Declaration)} method to consistently add
   * declarations.
   *
   * @param declaration the declaration
   */
  void addDeclaration(@NonNull Declaration declaration);

  default <N extends Declaration> void addIfNotContains(Collection<N> collection, N declaration) {
    if (!collection.contains(declaration)) {
      collection.add(declaration);
    }
  }

  default void addIfNotContains(Collection<PropertyEdge> collection, PropertyEdge propertyEdge) {
    boolean contains = false;
    for (PropertyEdge pEdge : collection) {
      if (pEdge.getEnd().equals(propertyEdge.getEnd())) {
        contains = true;
        break;
      }
    }
    if (!contains) {
      collection.add(propertyEdge);
    }
  }
}
