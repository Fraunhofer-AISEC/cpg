/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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

package de.fraunhofer.aisec.cpg.graph.declarations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This represents a sequence of one or more declaration(s). The purpose of this node is primarily
 * to bridge between a single declaration and a list of declarations in the front-end handlers. It
 * will be converted into a list-structure and all its children will be added to the parent, i.e.
 * the translation unit. It should not end up in the final graph.
 */
public class DeclarationSequence extends Declaration {

  private final List<Declaration> children = new ArrayList<>();

  public void add(@NonNull Declaration declaration) {
    if (declaration instanceof DeclarationSequence) {
      children.addAll(((DeclarationSequence) declaration).asList());
    }

    children.add(declaration);
  }

  public List<Declaration> asList() {
    return children;
  }

  public boolean isSingle() {
    return children.size() == 1;
  }

  public Declaration first() {
    return children.get(0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    DeclarationSequence that = (DeclarationSequence) o;
    return Objects.equals(children, that.children);
  }
}
