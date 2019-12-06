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
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import io.github.oxisto.reticulated.ast.expression.Call;
import io.github.oxisto.reticulated.ast.expression.Expression;
import io.github.oxisto.reticulated.ast.expression.Identifier;

public class ExpressionHandler
    extends Handler<de.fraunhofer.aisec.cpg.graph.Expression, Expression, PythonLanguageFrontend> {

  public ExpressionHandler(PythonLanguageFrontend lang) {
    super(de.fraunhofer.aisec.cpg.graph.Expression::new, lang);

    this.map.put(Call.class, this::handleCall);
  }

  private CallExpression handleCall(Expression expression) {
    Call call = expression.asCall();

    Identifier id = call.getPrimary().asIdentifier();

    String name = id.getName();
    String fqn = name;

    CallExpression callExpression = NodeBuilder.newCallExpression(name, fqn, null);

    return callExpression;
  }
}
