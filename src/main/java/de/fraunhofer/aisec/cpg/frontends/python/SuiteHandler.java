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
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import io.github.oxisto.reticulated.ast.Suite;
import io.github.oxisto.reticulated.ast.statement.Statement;
import io.github.oxisto.reticulated.ast.statement.StatementList;
import java.util.ArrayList;
import java.util.List;

/**
 * This converts a suite (which is part of Python's compound statement) into a CompoundStatement
 * node in our graph.
 */
public class SuiteHandler extends Handler<CompoundStatement, Suite, PythonLanguageFrontend> {

  public SuiteHandler(PythonLanguageFrontend lang) {
    super(CompoundStatement::new, lang);

    this.map.put(Suite.class, this::handleSuite);
  }

  private de.fraunhofer.aisec.cpg.graph.CompoundStatement handleSuite(Suite suite) {
    CompoundStatement statement = new CompoundStatement();

    List<de.fraunhofer.aisec.cpg.graph.Statement> list = new ArrayList<>();
    // loop through child statements
    for (Statement node : suite) {
      // if it is a statement list, we need to flatten the list
      if (node instanceof StatementList) {
        list.addAll(this.lang.getStatementListHandler().handle((StatementList) node));
      }
    }
    statement.setStatements(list);

    return statement;
  }
}
