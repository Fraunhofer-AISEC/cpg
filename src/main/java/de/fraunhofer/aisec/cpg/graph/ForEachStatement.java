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

import java.util.Objects;

public class ForEachStatement extends Statement {

  @SubGraph("AST")
  private Declaration variable;

  @SubGraph("AST")
  private Statement iterable;

  @SubGraph("AST")
  private Statement statement;

  public Statement getStatement() {
    return statement;
  }

  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  public Declaration getVariable() {
    return variable;
  }

  public void setVariable(Declaration variable) {
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
