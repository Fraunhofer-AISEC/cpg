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
package de.fraunhofer.aisec.cpg.graph.statements;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.Objects;

public class SynchronizedStatement extends Statement {

  @SubGraph("AST")
  public Expression expression;

  @SubGraph("AST")
  public CompoundStatement blockStatement;

  public Expression getExpression() {
    return expression;
  }

  public void setExpression(Expression expression) {
    this.expression = expression;
  }

  public CompoundStatement getBlockStatement() {
    return blockStatement;
  }

  public void setBlockStatement(CompoundStatement blockStatement) {
    this.blockStatement = blockStatement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SynchronizedStatement)) {
      return false;
    }
    SynchronizedStatement that = (SynchronizedStatement) o;
    return super.equals(that)
        && Objects.equals(expression, that.expression)
        && Objects.equals(blockStatement, that.blockStatement);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
