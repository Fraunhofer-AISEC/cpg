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
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Represents a condition control flow statement, usually indicating by <code>If</code>. */
public class IfStatement extends Statement {

  /** C++ initializer statement */
  @SubGraph("AST")
  private Statement initializerStatement;

  /** C++ alternative to the condition */
  @SubGraph("AST")
  private Declaration conditionDeclaration;

  /** The condition to be evaluated. */
  @SubGraph("AST")
  private Expression condition;

  /** C++ constexpr construct */
  private boolean isConstExpression = false;

  /**
   * The statement that is going to executed, if the condition is evaluated as true. Usually a
   * {@link CompoundStatement}.
   */
  @SubGraph("AST")
  private Statement thenStatement;

  /**
   * The statement that is going to executed, if the condition is evaluated as false. Usually a
   * {@link CompoundStatement}.
   */
  @SubGraph("AST")
  private Statement elseStatement;

  public Expression getCondition() {
    return condition;
  }

  public void setCondition(Expression condition) {
    this.condition = condition;
  }

  public Statement getInitializerStatement() {
    return initializerStatement;
  }

  public void setInitializerStatement(Statement initializerStatement) {
    this.initializerStatement = initializerStatement;
  }

  public Declaration getConditionDeclaration() {
    return conditionDeclaration;
  }

  public void setConditionDeclaration(Declaration conditionDeclaration) {
    this.conditionDeclaration = conditionDeclaration;
  }

  public boolean isConstExpression() {
    return isConstExpression;
  }

  public void setConstExpression(boolean constExpression) {
    isConstExpression = constExpression;
  }

  public Statement getThenStatement() {
    return thenStatement;
  }

  public void setThenStatement(Statement thenStatement) {
    this.thenStatement = thenStatement;
  }

  public Statement getElseStatement() {
    return elseStatement;
  }

  public void setElseStatement(Statement elseStatement) {
    this.elseStatement = elseStatement;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("condition", condition)
        .append("thenStatement", thenStatement)
        .append("elseStatement", elseStatement)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof IfStatement)) {
      return false;
    }
    IfStatement that = (IfStatement) o;
    return super.equals(that)
        && isConstExpression == that.isConstExpression
        && Objects.equals(initializerStatement, that.initializerStatement)
        && Objects.equals(conditionDeclaration, that.conditionDeclaration)
        && Objects.equals(condition, that.condition)
        && Objects.equals(thenStatement, that.thenStatement)
        && Objects.equals(elseStatement, that.elseStatement);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
