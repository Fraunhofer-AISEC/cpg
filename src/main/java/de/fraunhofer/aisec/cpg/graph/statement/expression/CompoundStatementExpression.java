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

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declaration.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.statement.Statement;
import java.util.Objects;

/**
 * An expression, which calls another function. It has a list of arguments (list of {@link
 * Expression}s) and is connected via the INVOKES edge to its {@link FunctionDeclaration}.
 */
public class CompoundStatementExpression extends Expression {

  /** The list of arguments. */
  @SubGraph("AST")
  private Statement statement;

  public Statement getStatement() {
    return statement;
  }

  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CompoundStatementExpression)) {
      return false;
    }
    CompoundStatementExpression that = (CompoundStatementExpression) o;
    return super.equals(that) && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
