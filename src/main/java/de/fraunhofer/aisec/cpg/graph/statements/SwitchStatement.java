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

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import java.util.Objects;

/**
 * Represents a Java or C++ switch statement of the <code>switch (selector) {...}</code> that can
 * include case and default statements. Break statements break out of the switch and labeled breaks
 * in JAva are handled properly.
 */
public class SwitchStatement extends Statement {

  /** Selector that determines the case/default statement of the subsequent execution */
  @SubGraph("AST")
  public Expression selector;
  /** C++ can have an initializer statement in a switch */
  @SubGraph("AST")
  private Statement initializerStatement;
  /** C++ allows to use a declaration instead of a expression as selector */
  @SubGraph("AST")
  private Declaration selectorDeclaration;
  /**
   * The compound statement that contains break/default statements with regular statements on the
   * same hierarchy
   */
  @SubGraph("AST")
  private Statement statement;

  public Statement getInitializerStatement() {
    return initializerStatement;
  }

  public void setInitializerStatement(Statement initializerStatement) {
    this.initializerStatement = initializerStatement;
  }

  public Declaration getSelectorDeclaration() {
    return selectorDeclaration;
  }

  public void setSelectorDeclaration(Declaration selectorDeclaration) {
    this.selectorDeclaration = selectorDeclaration;
  }

  public Expression getSelector() {
    return selector;
  }

  public void setSelector(Expression selector) {
    this.selector = selector;
  }

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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    SwitchStatement that = (SwitchStatement) o;
    return Objects.equals(selector, that.selector)
        && Objects.equals(initializerStatement, that.initializerStatement)
        && Objects.equals(selectorDeclaration, that.selectorDeclaration)
        && Objects.equals(statement, that.statement);
  }
}
