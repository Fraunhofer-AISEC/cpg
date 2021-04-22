/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.junit.jupiter.api.Test;

class CXXBindingsTest extends BaseTest {

  void checkBindings(CXXLanguageFrontend cxxLanguageFrontend) {
    for (IBinding binding : cxxLanguageFrontend.getCachedDeclarations().keySet()) {
      Declaration declaration = cxxLanguageFrontend.getCachedDeclaration(binding);
      for (Expression expression : cxxLanguageFrontend.getCachedExpression(binding)) {
        assertEquals(declaration, ((DeclaredReferenceExpression) expression).getRefersTo());
      }
    }
  }

  @Test
  void testUseThenDeclaration() throws Exception {
    CXXLanguageFrontend cxxLanguageFrontend =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build(), new ScopeManager());
    cxxLanguageFrontend.parse(new File("src/test/resources/bindings/use_then_declare.cpp"));

    checkBindings(cxxLanguageFrontend);
  }

  @Test
  void testDeclarationReplacement() throws Exception {
    CXXLanguageFrontend cxxLanguageFrontend =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build(), new ScopeManager());
    cxxLanguageFrontend.parse(new File("src/test/resources/bindings/replace_declaration.cpp"));

    checkBindings(cxxLanguageFrontend);
  }
}
