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

package de.fraunhofer.aisec.cpg.graph.statements;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Represents a conditional loop statement of the form: <code>while(...){...}</code>. */
public class WhileStatement extends Statement {

  /** C++ allows defining a declaration instead of a pure logical expression as condition */
  @SubGraph("AST")
  private Declaration conditionDeclaration;

  /** The condition that decides if the block is executed. */
  @SubGraph("AST")
  private Expression condition;

  /**
   * The statement that is going to be executed, until the condition evaluates to false for the
   * first time. Usually a {@link CompoundStatement}.
   */
  @SubGraph("AST")
  private Statement statement;

  public Declaration getConditionDeclaration() {
    return conditionDeclaration;
  }

  public void setConditionDeclaration(Declaration conditionDeclaration) {
    this.conditionDeclaration = conditionDeclaration;
  }

  public Expression getCondition() {
    return condition;
  }

  public void setCondition(Expression condition) {
    this.condition = condition;
  }

  public Statement getStatement() {
    return statement;
  }

  public void setStatement(Statement thenStatement) {
    this.statement = thenStatement;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("condition", condition)
        .append("statement", statement)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WhileStatement)) {
      return false;
    }
    WhileStatement that = (WhileStatement) o;
    return super.equals(that)
        && Objects.equals(conditionDeclaration, that.conditionDeclaration)
        && Objects.equals(condition, that.condition)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
