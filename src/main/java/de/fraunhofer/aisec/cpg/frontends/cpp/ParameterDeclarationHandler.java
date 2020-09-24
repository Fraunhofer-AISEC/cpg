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

package de.fraunhofer.aisec.cpg.frontends.cpp;

import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTArrayDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTParameterDeclaration;

public class ParameterDeclarationHandler
    extends Handler<ParamVariableDeclaration, IASTParameterDeclaration, CXXLanguageFrontend> {

  public ParameterDeclarationHandler(CXXLanguageFrontend lang) {
    super(ParamVariableDeclaration::new, lang);

    map.put(
        CPPASTParameterDeclaration.class,
        ctx -> handleParameterDeclaration((CPPASTParameterDeclaration) ctx));
  }

  private ParamVariableDeclaration handleParameterDeclaration(CPPASTParameterDeclaration ctx) {
    // The logic of type adjustment computation was copied over from handleDeclarator, it is not
    // clear if it will be necessary but the usage of handleDeclarator had to be avoided because of
    // side effects

    String typeAdjustment =
        List.of(ctx.getDeclarator().getPointerOperators()).stream()
            .map(IASTNode::getRawSignature)
            .collect(Collectors.joining());

    if (ctx.getDeclarator() instanceof CPPASTArrayDeclarator
        && ctx.getDeclarator().getInitializer() instanceof InitializerListExpression) {
      // narrow down array type to size of initializer list expression
      typeAdjustment +=
          "["
              + ((InitializerListExpression) ctx.getDeclarator().getInitializer())
                  .getInitializers()
                  .size()
              + "]";
    } else if (ctx.getDeclarator() instanceof CPPASTArrayDeclarator
        && ctx.getDeclarator().getInitializer() instanceof Literal
        && ((Literal) ctx.getDeclarator().getInitializer()).getValue() instanceof String) {
      // narrow down array type to length of string literal
      typeAdjustment +=
          "["
              + (((String) ((Literal) ctx.getDeclarator().getInitializer()).getValue()).length()
                  + 1)
              + "]";
    } else if (ctx.getDeclarator() instanceof CPPASTArrayDeclarator) {
      typeAdjustment +=
          List.of(((CPPASTArrayDeclarator) ctx.getDeclarator()).getArrayModifiers()).stream()
              .map(IASTNode::getRawSignature)
              .collect(Collectors.joining());
    }

    ParamVariableDeclaration paramVariableDeclaration =
        NodeBuilder.newMethodParameterIn(
            ctx.getDeclarator().getName().toString(),
            UnknownType.getUnknownType(),
            false,
            ctx.getRawSignature());

    if (!typeAdjustment.isEmpty()) {
      paramVariableDeclaration.setType(
          TypeParser.createFrom(ctx.getDeclSpecifier().toString() + typeAdjustment, true));
    } else {
      paramVariableDeclaration.setType(
          TypeParser.createFrom(ctx.getDeclSpecifier().toString(), true));
    }

    return paramVariableDeclaration;
  }
}
