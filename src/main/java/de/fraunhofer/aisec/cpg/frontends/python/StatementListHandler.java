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
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import io.github.oxisto.reticulated.ast.statement.Statements;
import java.util.ArrayList;
import java.util.List;

public class StatementListHandler
    extends Handler<List<Statement>, Statements, PythonLanguageFrontend> {

  public StatementListHandler(PythonLanguageFrontend lang) {
    super(ArrayList::new, lang);

    this.map.put(Statements.class, this::handleStatementList);
  }

  private List<Statement> handleStatementList(Statements statementList) {
    List<Statement> list = new ArrayList<>();

    for (io.github.oxisto.reticulated.ast.statement.Statement node : statementList) {
      list.add(this.lang.getStatementHandler().handle(node));
    }

    return list;
  }
}
