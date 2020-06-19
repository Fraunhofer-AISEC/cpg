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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CXXIncludeTest {

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
  void testDefinitionsAndDeclaration() throws TranslationException {
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(
                TranslationConfiguration.builder().loadIncludes(true).defaultPasses().build(),
                new ScopeManager())
            .parse(new File("src/test/resources/include.cpp"));
    assertEquals(4, tu.getDeclarations().size());

    RecordDeclaration someClass =
        tu.getDeclarationByName("SomeClass", RecordDeclaration.class).orElse(null);
    assertNotNull(someClass);

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    ConstructorDeclaration someClassConstructor =
        tu.getDeclarationByName("SomeClass", ConstructorDeclaration.class).orElse(null);
    assertNotNull(someClassConstructor);
    assertEquals(someClass, someClassConstructor.getRecordDeclaration());

    MethodDeclaration doSomething =
        tu.getDeclarationByName("DoSomething", MethodDeclaration.class).orElse(null);
    assertNotNull(doSomething);
    assertEquals(someClass, doSomething.getRecordDeclaration());
  }

  @Test
  void testCodeAndRegionInInclude() throws TranslationException {
    // checks, whether code and region for nodes in includes are properly set
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(
                TranslationConfiguration.builder().loadIncludes(true).defaultPasses().build(),
                new ScopeManager())
            .parse(new File("src/test/resources/include.cpp"));

    RecordDeclaration someClass =
        tu.getDeclarationByName("SomeClass", RecordDeclaration.class).orElse(null);
    assertNotNull(someClass);

    ConstructorDeclaration decl = someClass.getConstructors().get(0);
    assertEquals("SomeClass();", decl.getCode());

    PhysicalLocation location = decl.getLocation();
    assertNotNull(location);

    assertEquals(new Region(16, 3, 16, 15), location.getRegion());
  }
}
