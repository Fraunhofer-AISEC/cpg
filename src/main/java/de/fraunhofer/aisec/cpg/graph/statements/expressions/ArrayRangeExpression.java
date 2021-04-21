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

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import java.util.Objects;

/** Expressions of the form floor ... ceiling */
public class ArrayRangeExpression extends Expression {

  @SubGraph("AST")
  private Expression floor;

  @SubGraph("AST")
  private Expression ceiling;

  public Expression getCeiling() {
    return ceiling;
  }

  public void setCeiling(Expression ceiling) {
    this.ceiling = ceiling;
  }

  public Expression getFloor() {
    return floor;
  }

  public void setFloor(Expression floor) {
    this.floor = floor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArrayRangeExpression)) {
      return false;
    }
    ArrayRangeExpression that = (ArrayRangeExpression) o;
    return super.equals(that)
        && Objects.equals(floor, that.floor)
        && Objects.equals(ceiling, that.ceiling);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
