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

package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubgraphWalkerTest {

  /**
   * {@link TypeParser} and {@link TypeManager} hold static state. This needs to be cleared before
   * all tests in order to avoid strange errors
   */
  @BeforeEach
  void resetPersistentState() {
    TypeParser.reset();
    TypeManager.reset();
  }

  @Test
  void testASTChildrenGetter() throws TranslationException {
    File file = new File("src/test/resources/compiling/RecordDeclaration.java");
    TranslationConfiguration config = TranslationConfiguration.builder().build();
    TranslationUnitDeclaration declaration =
        new JavaLanguageFrontend(config, new ScopeManager()).parse(file);
    NamespaceDeclaration namespace = declaration.getDeclarationAs(0, NamespaceDeclaration.class);

    assertNotNull(declaration);

    RecordDeclaration recordDeclaration = namespace.getDeclarationAs(0, RecordDeclaration.class);

    Set<Node> ast = SubgraphWalker.getAstChildren(recordDeclaration);

    assertFalse(ast.isEmpty());

    // should contain 4 AST nodes, 1 field (+1 this field), 1 method, 1 constructor
    assertEquals(4, ast.size());
  }
}
