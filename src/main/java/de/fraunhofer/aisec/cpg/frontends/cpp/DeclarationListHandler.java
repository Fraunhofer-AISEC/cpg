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
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;

class DeclarationListHandler
    extends Handler<List<Declaration>, IASTDeclaration, CXXLanguageFrontend> {

  DeclarationListHandler(CXXLanguageFrontend lang) {
    super(ArrayList::new, lang);

    map.put(
        CPPASTSimpleDeclaration.class,
        ctx -> handleSimpleDeclaration((CPPASTSimpleDeclaration) ctx));
  }

  private List<Declaration> handleSimpleDeclaration(CPPASTSimpleDeclaration ctx) {
    ArrayList<Declaration> list = new ArrayList<>();

    for (IASTDeclarator declarator : ctx.getDeclarators()) {
      ValueDeclaration declaration =
          (ValueDeclaration) this.lang.getDeclaratorHandler().handle(declarator);

      boolean isAutoType = false;
      if (ctx.getDeclSpecifier() instanceof CPPASTSimpleDeclSpecifier
          && ((CPPASTSimpleDeclSpecifier) ctx.getDeclSpecifier()).getType()
              == IASTSimpleDeclSpecifier.t_auto) {
        isAutoType = true;
      }

      String typeAdjustment = declaration.getType().getTypeAdjustment();
      declaration.setType(Type.createFrom(ctx.getDeclSpecifier().toString()));
      declaration.getType().setTypeAdjustment(typeAdjustment);

      if (isAutoType) {
        if (declaration instanceof VariableDeclaration
            && ((VariableDeclaration) declaration).getInitializer() != null) {
          // TODO @SH: investigate whether this is necessary, should be covered by type listeners
          declaration.getType().setTypeModifier(ctx.getDeclSpecifier().toString());
          // set to the type of the initializer
          declaration
              .getType()
              .setTypeName(
                  ((VariableDeclaration) declaration).getInitializer().getType().getTypeName());
          declaration
              .getType()
              .setTypeAdjustment(
                  ((VariableDeclaration) declaration)
                      .getInitializer()
                      .getType()
                      .getTypeAdjustment());
        } else {
          log.warn("cannot determine auto type for {}. we keep it at auto", declaration.getClass());
          declaration.setType(Type.createFrom("auto"));
        }
      } else {
        // this is not an auto type and therefore already set
      }

      // cache binding
      this.lang.cacheDeclaration(declarator.getName().resolveBinding(), declaration);

      list.add(declaration);
    }

    return list;
  }
}
