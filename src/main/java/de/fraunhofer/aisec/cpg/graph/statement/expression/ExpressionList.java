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

package de.fraunhofer.aisec.cpg.graph.statement.expression;

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.statement.Statement;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import java.util.*;

public class ExpressionList extends Expression implements TypeListener {

  @org.neo4j.ogm.annotation.Relationship(value = "SUBEXPR")
  @SubGraph("AST")
  private List<Statement> expressions = new ArrayList<>();

  public List<Statement> getExpressions() {
    return expressions;
  }

  public void setExpressions(List<Statement> expressions) {
    if (!this.expressions.isEmpty()) {
      Statement lastExpression = this.expressions.get(this.expressions.size() - 1);
      if (lastExpression instanceof HasType)
        ((HasType) lastExpression).unregisterTypeListener(this);
      this.removePrevDFG(lastExpression);
    }
    this.expressions = expressions;
    if (!this.expressions.isEmpty()) {
      Statement lastExpression = this.expressions.get(this.expressions.size() - 1);
      this.addPrevDFG(lastExpression);
      if (lastExpression instanceof HasType) ((HasType) lastExpression).registerTypeListener(this);
    }
  }

  public void addExpression(Statement expression) {
    if (!this.expressions.isEmpty()) {
      Statement lastExpression = this.expressions.get(this.expressions.size() - 1);
      if (lastExpression instanceof HasType)
        ((HasType) lastExpression).unregisterTypeListener(this);
      this.removePrevDFG(lastExpression);
    }
    this.expressions.add(expression);
    this.addPrevDFG(expression);
    if (expression instanceof HasType) {
      ((HasType) expression).registerTypeListener(this);
    }
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {
    Type previous = this.type;
    setType(src.getPropagationType(), root);
    setPossibleSubTypes(new HashSet<>(src.getPossibleSubTypes()), root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes) {
    setPossibleSubTypes(new HashSet<>(src.getPossibleSubTypes()), root);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ExpressionList)) {
      return false;
    }
    ExpressionList that = (ExpressionList) o;
    return super.equals(that) && Objects.equals(expressions, that.expressions);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
