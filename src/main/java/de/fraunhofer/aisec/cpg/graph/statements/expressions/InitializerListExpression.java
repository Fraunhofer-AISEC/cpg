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

package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** A list of initializer expressions. */
public class InitializerListExpression extends Expression implements TypeListener {

  /** The list of initializers. */
  @SubGraph("AST")
  private List<Expression> initializers = new ArrayList<>();

  public List<Expression> getInitializers() {
    return initializers;
  }

  public void setInitializers(List<Expression> initializers) {
    if (this.initializers != null) {
      this.initializers.forEach(
          i -> {
            i.unregisterTypeListener(this);
            this.removePrevDFG(i);
          });
    }
    this.initializers = initializers;
    if (initializers != null) {
      initializers.forEach(
          i -> {
            i.registerTypeListener(this);
            this.addPrevDFG(i);
          });
    }
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {
    if (!TypeManager.getInstance().isUnknown(this.type)
        && src.getPropagationType().equals(oldType)) {
      return;
    }

    Type previous = this.type;
    Type newType;
    Set<Type> subTypes;

    if (this.initializers.contains(src)) {
      Set<Type> types =
          this.initializers
              .parallelStream()
              .map(Expression::getType)
              .filter(Objects::nonNull)
              .map(t -> TypeManager.getInstance().registerType(t.reference(PointerOrigin.ARRAY)))
              .collect(Collectors.toSet());
      Type alternative = !types.isEmpty() ? types.iterator().next() : UnknownType.getUnknownType();
      newType = TypeManager.getInstance().getCommonType(types).orElse(alternative);
      subTypes = new HashSet<>(getPossibleSubTypes());
      subTypes.remove(oldType);
      subTypes.addAll(types);
    } else {
      newType = src.getType();
      subTypes = new HashSet<>(getPossibleSubTypes());
      subTypes.remove(oldType);
      subTypes.add(newType);
    }

    setType(newType, root);
    setPossibleSubTypes(subTypes, root);

    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes) {
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes, root);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("initializers", initializers)
        .toString();
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
    InitializerListExpression that = (InitializerListExpression) o;
    return Objects.equals(initializers, that.initializers);
  }
}
