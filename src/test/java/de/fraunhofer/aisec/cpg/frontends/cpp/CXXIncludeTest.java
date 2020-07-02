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

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class CXXIncludeTest extends BaseTest {

  @Test
  void testDefinitionsAndDeclaration() throws Exception {
    File file = new File("src/test/resources/include.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);
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
  void testCodeAndRegionInInclude() throws Exception {
    // checks, whether code and region for nodes in includes are properly set
    File file = new File("src/test/resources/include.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

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
