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

import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Type.Origin;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents the declaration of a variable. */
public class VariableDeclaration extends ValueDeclaration implements TypeListener {

  /** The (optional) initializer of the declaration. */
  @SubGraph("AST")
  @Nullable
  protected Expression initializer;

  @Nullable
  public Expression getInitializer() {
    return initializer;
  }

  public void setInitializer(@Nullable Expression initializer) {
    if (this.initializer != null) {
      this.removePrevDFG(this.initializer);
      this.initializer.unregisterTypeListener(this);
    }

    this.initializer = initializer;

    if (initializer != null) {
      this.addPrevDFG(initializer);
      initializer.registerTypeListener(this);

      if (initializer instanceof ConstructExpression) {
        this.registerTypeListener((ConstructExpression) initializer);
      }
    }
  }

  @Override
  public void typeChanged(HasType src, Type oldType) {
    Type previous = this.type;
    setType(src.getType());
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, Set<Type> oldSubTypes) {
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .append("initializer", initializer)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VariableDeclaration)) {
      return false;
    }
    VariableDeclaration that = (VariableDeclaration) o;
    return super.equals(that) && Objects.equals(initializer, that.initializer);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
