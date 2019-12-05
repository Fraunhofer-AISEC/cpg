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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Expressions of the form new Type[] that creates an Array. */
public class ArrayCreationExpression extends Expression implements TypeListener {

  @SubGraph("AST")
  private InitializerListExpression initializer;

  @SubGraph("AST")
  private List<Expression> dimensions = new ArrayList<>();

  public InitializerListExpression getInitializer() {
    return initializer;
  }

  public void setInitializer(InitializerListExpression initializer) {
    if (this.initializer != null) {
      this.initializer.unregisterTypeListener(this);
      this.removePrevDFG(initializer);
    }
    this.initializer = initializer;
    if (initializer != null) {
      initializer.registerTypeListener(this);
      this.addPrevDFG(initializer);
    }
  }

  public List<Expression> getDimensions() {
    return dimensions;
  }

  public void setDimensions(List<Expression> dimensions) {
    this.dimensions = dimensions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArrayCreationExpression)) {
      return false;
    }
    ArrayCreationExpression that = (ArrayCreationExpression) o;
    return super.equals(that)
        && Objects.equals(initializer, that.initializer)
        && Objects.equals(dimensions, that.dimensions);
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
  public int hashCode() {
    return super.hashCode();
  }
}
