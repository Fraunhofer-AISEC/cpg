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
import io.github.oxisto.reticulated.ast.simple.SimpleStatement;
import io.github.oxisto.reticulated.ast.statement.StatementList;
import java.util.ArrayList;
import java.util.List;

public class StatementListHandler
    extends Handler<List<Statement>, StatementList, PythonLanguageFrontend> {

  public StatementListHandler(PythonLanguageFrontend lang) {
    super(ArrayList::new, lang);

    this.map.put(StatementList.class, this::handleStatementList);
  }

  private List<Statement> handleStatementList(StatementList statementList) {
    List<Statement> list = new ArrayList<>();

    for (SimpleStatement node : statementList) {
      list.add(this.lang.getSimpleStatementHandler().handle(node));
    }

    return list;
  }
}
