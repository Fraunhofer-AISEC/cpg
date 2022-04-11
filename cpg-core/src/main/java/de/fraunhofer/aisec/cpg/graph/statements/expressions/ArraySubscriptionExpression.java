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
package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.HasBase;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the Subscription or access of an array of the form <code>array[index]</code>, where
 * both <code>array</code> and <code>index</code> are of type {@link Expression}. CPP can overload
 * operators thus changing semantics of array access.
 */
public class ArraySubscriptionExpression extends Expression implements TypeListener, HasBase {

  @SubGraph("AST")
  private Expression arrayExpression;

  @SubGraph("AST")
  private Expression subscriptExpression;

  public Expression getArrayExpression() {
    return arrayExpression;
  }

  public void setArrayExpression(Expression arrayExpression) {
    this.arrayExpression = arrayExpression;
    this.addPrevDFG(arrayExpression);
    setType(getSubscriptType(arrayExpression.getType()));
    arrayExpression.registerTypeListener(this);
  }

  private Type getSubscriptType(Type arrayType) {
    return arrayType.dereference();
  }

  public Expression getSubscriptExpression() {
    return subscriptExpression;
  }

  public void setSubscriptExpression(Expression subscriptExpression) {
    this.subscriptExpression = subscriptExpression;
  }

  @Override
  public void typeChanged(HasType src, Collection<HasType> root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Type previous = this.type;
    setType(getSubscriptType(src.getPropagationType()), root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(
      HasType src, Collection<HasType> root, Set<Type> oldSubTypes) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(
        src.getPossibleSubTypes().stream()
            .map(this::getSubscriptType)
            .collect(Collectors.toList()));
    setPossibleSubTypes(subTypes, root);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArraySubscriptionExpression)) {
      return false;
    }
    ArraySubscriptionExpression that = (ArraySubscriptionExpression) o;
    return super.equals(that)
        && Objects.equals(arrayExpression, that.arrayExpression)
        && Objects.equals(subscriptExpression, that.subscriptExpression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.arrayExpression, this.subscriptExpression);
  }

  @NotNull
  @Override
  public Expression getBase() {
    return this.arrayExpression;
  }
}
