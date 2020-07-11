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
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
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

      IASTDeclSpecifier declSpecifier = ctx.getDeclSpecifier();

      String typeString;
      if (declaration instanceof FunctionDeclaration) {
        // if it is a function definition, we are only interested in the return type
        typeString = ctx.getRawSignature().split("[ ]")[0];
      } else if (declaration instanceof VariableDeclaration) {
        // if it is a variable declaration, we are only interested in the declaration, not the
        // initializer (if any)
        typeString = ctx.getRawSignature().split("[=]")[0];
      } else {
        // otherwise, use the complete raw code and let the type parser handle it
        typeString = ctx.getRawSignature();
      }

      Type result = TypeParser.createFrom(typeString, true);
      declaration.setType(result);

      // cache binding
      this.lang.cacheDeclaration(declarator.getName().resolveBinding(), declaration);

      list.add(declaration);
    }

    return list;
  }
}
