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

import de.fraunhofer.aisec.cpg.graph.Type.Origin;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the Subscription or access of an array of the form <code>array[index]</code>, where
 * both <code>array</code> and <code>index</code> are of type {@link Expression}. CPP can overload
 * operators thus changing semantics of array access.
 */
public class ArraySubscriptionExpression extends Expression implements HasType.TypeListener {

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
    Type t = new Type(arrayType);
    // check whether this is a pointer or normal array
    if (t.getTypeAdjustment().contains("*")) {
      t.setTypeAdjustment(t.getTypeAdjustment().replaceFirst("\\*", ""));
    } else if (t.getTypeAdjustment().contains("[]")) {
      t.setTypeAdjustment(t.getTypeAdjustment().replaceFirst("\\[]", ""));
    }
    // if neither * nor [] are present, we should rather do nothing, as we have no idea how the
    // correct type would look like
    return t;
  }

  public Expression getSubscriptExpression() {
    return subscriptExpression;
  }

  public void setSubscriptExpression(Expression subscriptExpression) {
    this.subscriptExpression = subscriptExpression;
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {
    Type previous = this.type;
    setType(getSubscriptType(src.getType()), root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes) {
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
    return super.hashCode();
  }
}
