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
import java.util.Objects;

public class ForEachStatement extends Statement {

  /**
   * This field contains the iteration variable of the loop. It can be either a new variable
   * declaration or a reference to an existing variable.
   */
  @SubGraph("AST")
  private Statement variable;

  /** This field contains the iteration subject of the loop. */
  @SubGraph("AST")
  private Statement iterable;

  /** This field contains the body of the loop. */
  @SubGraph("AST")
  private Statement statement;

  public Statement getStatement() {
    return statement;
  }

  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  public Statement getVariable() {
    return variable;
  }

  public void setVariable(Statement variable) {
    this.variable = variable;
  }

  public Statement getIterable() {
    return iterable;
  }

  public void setIterable(Statement iterable) {
    this.iterable = iterable;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ForEachStatement)) {
      return false;
    }
    ForEachStatement that = (ForEachStatement) o;
    return super.equals(that)
        && Objects.equals(variable, that.variable)
        && Objects.equals(iterable, that.iterable)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
