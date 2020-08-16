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
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.UnknownType;
import io.github.oxisto.reticulated.ast.Suite;
import io.github.oxisto.reticulated.ast.expression.primary.atom.Identifier;
import io.github.oxisto.reticulated.ast.statement.Definition;
import io.github.oxisto.reticulated.ast.statement.FunctionDefinition;
import io.github.oxisto.reticulated.ast.statement.parameter.BaseParameter;
import io.github.oxisto.reticulated.ast.statement.parameter.ParameterList;
import java.util.ArrayList;
import java.util.List;

/** Transforms python definitions into declarations. */
public class DefinitionHandler extends Handler<Declaration, Definition, PythonLanguageFrontend> {

  public DefinitionHandler(PythonLanguageFrontend lang) {
    super(Declaration::new, lang);

    this.map.put(FunctionDefinition.class, this::handleFunctionDefinition);
  }

  private Declaration handleFunctionDefinition(Definition def) {
    FunctionDefinition func = def.asFunctionDefinition();

    FunctionDeclaration declaration =
        NodeBuilder.newFunctionDeclaration(func.getFuncName().getName(), null);

    this.lang.getScopeManager().enterScope(declaration);

    // build parameters
    List<ParamVariableDeclaration> parameters = new ArrayList<>();
    ParameterList list = (ParameterList) func.getParameterList();
    for (BaseParameter parameter : list) {
      String name = ((Identifier) parameter).getName();
      Type type = UnknownType.getUnknownType();

      // TODO: resolve parameter/type

      ParamVariableDeclaration param = NodeBuilder.newMethodParameterIn(name, type, false, null);
      parameters.add(param);
    }
    declaration.setParameters(parameters);

    // build function body
    CompoundStatement body = this.lang.getSuiteHandler().handle((Suite) func.getSuite());

    declaration.setBody(body);

    this.lang.getScopeManager().leaveScope(declaration);

    return declaration;
  }
}
