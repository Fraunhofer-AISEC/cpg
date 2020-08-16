/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  $$$$$$\  $$$$$$$\   $$$$$$\
 * $$  __$$\ $$  __$$\ $$  __$$\
 * $$ /  \__|$$ |  $$ |$$ /  \__|
 * $$ |      $$$$$$$  |$$ |$$$$\
 * $$ |      $$  ____/ $$ |\_$$ |
 * $$ |  $$\ $$ |      $$ |  $$ |
 * \$$$$$   |$$ |      \$$$$$   |
 *  \______/ \__|       \______/
 *
 */

package de.fraunhofer.aisec.cpg.frontends.python;

import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.Statement;
import io.github.oxisto.reticulated.ast.simple.ExpressionStatement;
import io.github.oxisto.reticulated.ast.simple.SimpleStatement;

public class SimpleStatementHandler
    extends Handler<Statement, SimpleStatement, PythonLanguageFrontend> {

  public SimpleStatementHandler(PythonLanguageFrontend lang) {
    super(Statement::new, lang);

    this.map.put(ExpressionStatement.class, this::handleExpressionStatement);
  }

  private Statement handleExpressionStatement(SimpleStatement simpleStatement) {
    ExpressionStatement expressionStatement = (ExpressionStatement) simpleStatement;

    // un-wrap it
    // return this.lang.getExpressionHandler().handle(expressionStatement.getExpression());
    // does not work any more with latest rp version
    return null;
  }
}
