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
import io.github.oxisto.reticulated.ast.simple.ExpressionStatement;
import io.github.oxisto.reticulated.ast.statement.Statement;

public class StatementHandler
    extends Handler<
        de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement, PythonLanguageFrontend> {

  public StatementHandler(PythonLanguageFrontend lang) {
    super(de.fraunhofer.aisec.cpg.graph.statements.Statement::new, lang);

    this.map.put(
        ExpressionStatement.class, x -> this.handleExpressionStatement((ExpressionStatement) x));
  }

  private de.fraunhofer.aisec.cpg.graph.statements.Statement handleExpressionStatement(
      ExpressionStatement statement) {
    return this.lang.getExpressionHandler().handle(statement.getExpression());
  }
}
