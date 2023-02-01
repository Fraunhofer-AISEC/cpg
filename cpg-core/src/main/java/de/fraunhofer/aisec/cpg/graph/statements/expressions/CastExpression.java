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
import de.fraunhofer.aisec.cpg.graph.Name;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
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
    this.expression = expression;
  }

  public Type getCastType() {
    return castType;
  }

  public void setCastType(Type castType) {
    this.castType = castType;
    this.setType(castType);
  }

  @Override
  public void updateType(@NotNull Type type) {
    super.updateType(type);
    this.castType = type;
  }

  @Override
  public void typeChanged(
      @NotNull HasType src, @NotNull List<HasType> root, @NotNull Type oldType) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    Type previous = this.getType();

    if (TypeManager.getInstance().isSupertypeOf(this.castType, src.getPropagationType(), this)) {
      setType(src.getPropagationType(), root);
    } else {
      resetTypes(this.getCastType());
    }

    if (!previous.equals(this.getType())) {
      this.getType().setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(@NotNull HasType src, @NotNull List<HasType> root) {
    if (!TypeManager.isTypeSystemActive()) {
      return;
    }
    setPossibleSubTypes(new ArrayList<>(src.getPossibleSubTypes()), root);
  }

  public void setCastOperator(int operatorCode) {
    String localName = null;
    switch (operatorCode) {
      case 0 -> localName = "cast";
      case 1 -> localName = "dynamic_cast";
      case 2 -> localName = "static_cast";
      case 3 -> localName = "reinterpret_cast";
      case 4 -> localName = "const_cast";
      default -> log.error("unknown operator {}", operatorCode);
    }

    if (localName != null) {
      setName(new Name(localName, null, this.getLanguage()));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CastExpression that)) {
      return false;
    }
    return Objects.equals(expression, that.expression) && Objects.equals(castType, that.castType);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
