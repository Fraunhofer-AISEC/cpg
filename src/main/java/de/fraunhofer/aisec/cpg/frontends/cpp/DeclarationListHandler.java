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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
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

      String typeAdjustment = declaration.getType().getTypeAdjustment();
      String typeString = ctx.getDeclSpecifier().toString();
      declaration.setType(Type.createFrom(typeString));
      declaration.getType().setTypeAdjustment(typeAdjustment);

      // cache binding
      this.lang.cacheDeclaration(declarator.getName().resolveBinding(), declaration);

      list.add(declaration);
    }

    return list;
  }
}
