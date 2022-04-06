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
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CastExpression extends Expression implements TypeListener {

  private static final Logger log = LoggerFactory.getLogger(CastExpression.class);

  @SubGraph("AST")
  private Expression expression;

  private Type castType;

  public Expression getExpression() {
    return expression;
  }

  public void setExpression(Expression expression) {
    if (this.expression != null) {
      this.removePrevDFG(this.expression);
    }

    this.expression = expression;

    if (expression != null) {
      this.addPrevDFG(expression);
    }
  }

  public Type getCastType() {
    return castType;
  }

  public void setCastType(Type castType) {
    this.castType = castType;
    this.type = castType;
  }

  @Override
  public void updateType(Type type) {
    super.updateType(type);
    this.castType = type;
  }

  @Override
  public void typeChanged(HasType src, Collection<HasType> root, Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Type previous = this.type;

    if (TypeManager.getInstance().isSupertypeOf(this.castType, src.getPropagationType())) {
      setType(src.getPropagationType(), root);
    } else {
      resetTypes(this.getCastType());
    }

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
    setPossibleSubTypes(src.getPossibleSubTypes(), root);
  }

  public void setCastOperator(int operatorCode) {
    switch (operatorCode) {
      case 0:
        setName("cast");
        break;
      case 1:
        setName("dynamic_cast");
        break;
      case 2:
        setName("static_cast");
        break;
      case 3:
        setName("reinterpret_cast");
        break;
      case 4:
        setName("const_cast");
        break;
      default:
        log.error("unknown operator {}", operatorCode);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CastExpression)) {
      return false;
    }
    CastExpression that = (CastExpression) o;
    return Objects.equals(expression, that.expression)
        && Objects.equals(castType, that.castType)
        && Objects.equals(this.getName(), that.getName());
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
