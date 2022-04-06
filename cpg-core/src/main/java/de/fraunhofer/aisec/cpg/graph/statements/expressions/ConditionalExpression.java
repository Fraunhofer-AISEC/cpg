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

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents an expression containing a ternary operator: var x = condition ? valueIfTrue :
 * valueIfFalse;
 */
public class ConditionalExpression extends Expression implements TypeListener {

  @SubGraph("AST")
  private Expression condition;

  @SubGraph("AST")
  private Expression thenExpr;

  @SubGraph("AST")
  private Expression elseExpr;

  public Expression getCondition() {
    return condition;
  }

  public void setCondition(Expression condition) {
    this.condition = condition;
  }

  public Expression getThenExpr() {
    return thenExpr;
  }

  public void setThenExpr(Expression thenExpr) {
    if (this.thenExpr != null) {
      this.thenExpr.unregisterTypeListener(this);
      this.removePrevDFG(this.thenExpr);
    }
    this.thenExpr = thenExpr;
    if (thenExpr != null) {
      thenExpr.registerTypeListener(this);
      this.addPrevDFG(thenExpr);
    }
  }

  public Expression getElseExpr() {
    return elseExpr;
  }

  public void setElseExpr(Expression elseExpr) {
    if (this.elseExpr != null) {
      this.elseExpr.unregisterTypeListener(this);
      this.removePrevDFG(this.elseExpr);
    }
    this.elseExpr = elseExpr;
    if (elseExpr != null) {
      elseExpr.registerTypeListener(this);
      this.addPrevDFG(elseExpr);
    }
  }

  @Override
  public void typeChanged(HasType src, Collection<HasType> root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Type previous = this.type;

    List<Type> types = new ArrayList<>();
    if (thenExpr != null && thenExpr.getPropagationType() != null) {
      types.add(thenExpr.getPropagationType());
    }
    if (elseExpr != null && elseExpr.getPropagationType() != null) {
      types.add(elseExpr.getPropagationType());
    }
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.remove(oldType);
    subTypes.addAll(types);

    Type alternative = !types.isEmpty() ? types.get(0) : UnknownType.getUnknownType();
    setType(TypeManager.getInstance().getCommonType(types).orElse(alternative), root);
    setPossibleSubTypes(subTypes, root);

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
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("condition", condition)
        .append("thenExpr", thenExpr)
        .append("elseExpr", elseExpr)
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConditionalExpression)) {
      return false;
    }
    ConditionalExpression that = (ConditionalExpression) o;
    return super.equals(that)
        && Objects.equals(condition, that.condition)
        && Objects.equals(thenExpr, that.thenExpr)
        && Objects.equals(elseExpr, that.elseExpr);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
