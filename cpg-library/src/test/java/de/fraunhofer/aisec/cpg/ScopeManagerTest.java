/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg;

import static de.fraunhofer.aisec.cpg.TestUtils.subnodesOfType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration;
import de.fraunhofer.aisec.cpg.helpers.FileBenchmark;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopeManagerTest extends BaseTest {

  private TranslationConfiguration config;

  @BeforeEach
  void setUp() {
    config = TranslationConfiguration.builder().defaultPasses().build();
  }

  @Test
  void testSetScope() throws TranslationException {
    LanguageFrontend frontend = new JavaLanguageFrontend(config, new ScopeManager());

    assert (frontend == frontend.getScopeManager().getLang());

    frontend.setScopeManager(new ScopeManager());
    assert (frontend == frontend.getScopeManager().getLang());
  }

  @Test
  void testReplaceNode() throws TranslationException {
    var scopeManager = new ScopeManager();
    var frontend = new CXXLanguageFrontend(config, scopeManager);
    var tu =
        frontend.parse(
            new File("src/test/resources/recordstmt.cpp"),
            new FileBenchmark(ScopeManagerTest.class, "Test Benchmark"));

    var methods =
        subnodesOfType(tu.getDeclarations(), MethodDeclaration.class).stream()
            .filter(m -> !(m instanceof ConstructorDeclaration))
            .collect(Collectors.toList());
    assertFalse(methods.isEmpty());

    methods.forEach(
        m -> {
          var scope = scopeManager.getScopeOfStatment(m);
          assertSame(m, scope.getAstNode());
        });

    var constructors = subnodesOfType(tu.getDeclarations(), ConstructorDeclaration.class);
    assertFalse(constructors.isEmpty());

    /*
    make sure that the scope of the constructor actually has the constructor as an ast node.
    this is necessary, since the constructor was probably created as a function declaration which
    later gets 'upgraded' to a constructor declaration.
    */
    constructors.forEach(
        c -> {
          var scope = scopeManager.getScopeOfStatment(c);
          assertSame(c, scope.getAstNode());
        });
  }
}
