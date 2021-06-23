/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * This represents a sequence of one or more declaration(s). The purpose of this node is primarily
 * to bridge between a single declaration and a list of declarations in the front-end handlers. It
 * will be converted into a list-structure and all its children will be added to the parent, i.e.
 * the translation unit. It should not end up in the final graph.
 */
public class DeclarationSequence extends Declaration implements DeclarationHolder {

  @Relationship(value = "CHILDREN", direction = "OUTGOING")
  private final List<PropertyEdge<Declaration>> children = new ArrayList<>();

  public List<PropertyEdge<Declaration>> getChildrenPropertyEdge() {
    return this.children;
  }

  public List<Declaration> getChildren() {
    List<Declaration> target = new ArrayList<>();
    for (PropertyEdge<Declaration> propertyEdge : this.children) {
      target.add(propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(target);
  }

  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof DeclarationSequence) {
      for (Declaration declarationChild : ((DeclarationSequence) declaration).getChildren()) {
        addIfNotContains(this.children, declarationChild);
      }
    }

    addIfNotContains(this.children, declaration);
  }

  public List<Declaration> asList() {
    return getChildren();
  }

  public boolean isSingle() {
    return children.size() == 1;
  }

  public Declaration first() {
    return children.get(0).getEnd();
  }

  @NotNull
  public List<Declaration> getDeclarations() {
    return getChildren();
  }
}
