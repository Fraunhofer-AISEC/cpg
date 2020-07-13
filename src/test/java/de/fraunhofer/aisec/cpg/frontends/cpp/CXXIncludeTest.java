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

import static de.fraunhofer.aisec.cpg.TestUtils.analyzeWithBuilder;
import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.io.File;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CXXIncludeTest extends BaseTest {

  @Test
  void testDefinitionsAndDeclaration() throws Exception {
    File file = new File("src/test/resources/include.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);
    for (Declaration d : tu.getDeclarations()) {
      System.out.println(d.getName() + " " + d.getLocation());
    }
    assertEquals(5, tu.getDeclarations().size());

    Set<RecordDeclaration> someClass =
        tu.getDeclarationsByName("SomeClass", RecordDeclaration.class);
    assertFalse(someClass.isEmpty());

    Set<FunctionDeclaration> main = tu.getDeclarationsByName("main", FunctionDeclaration.class);
    assertFalse(main.isEmpty());

    Set<ConstructorDeclaration> someClassConstructor =
        tu.getDeclarationsByName("SomeClass", ConstructorDeclaration.class);
    assertFalse(someClassConstructor.isEmpty());
    assertEquals(
        someClass.iterator().next(), someClassConstructor.iterator().next().getRecordDeclaration());

    Set<MethodDeclaration> doSomething =
        tu.getDeclarationsByName("DoSomething", MethodDeclaration.class);
    assertFalse(doSomething.isEmpty());
    assertEquals(someClass.iterator().next(), doSomething.iterator().next().getRecordDeclaration());
  }

  @Test
  void testCodeAndRegionInInclude() throws Exception {
    // checks, whether code and region for nodes in includes are properly set
    File file = new File("src/test/resources/include.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<RecordDeclaration> someClass =
        tu.getDeclarationsByName("SomeClass", RecordDeclaration.class);
    assertFalse(someClass.isEmpty());

    ConstructorDeclaration decl = someClass.iterator().next().getConstructors().get(0);
    assertEquals("SomeClass();", decl.getCode());

    PhysicalLocation location = decl.getLocation();
    assertNotNull(location);

    assertEquals(new Region(16, 3, 16, 15), location.getRegion());
  }

  @Test
  void testIncludeBlacklist() throws Exception {
    File file = new File("src/test/resources/include.cpp");
    List<TranslationUnitDeclaration> translationUnitDeclarations =
        analyzeWithBuilder(
            TranslationConfiguration.builder()
                .sourceLocations(List.of(file))
                .topLevel(file.getParentFile())
                .loadIncludes(true)
                .debugParser(true)
                .includeBlacklist(new File("src/test/resources/include.h").getAbsolutePath())
                .failOnError(true));

    TranslationUnitDeclaration next = translationUnitDeclarations.iterator().next();
    assertNotNull(next);

    // the only include should have been blacklisted
    assertTrue(next.getIncludes().isEmpty());
  }

  @Test
  void testIncludeWhitelist() throws Exception {
    File file = new File("src/test/resources/include.cpp");
    List<TranslationUnitDeclaration> translationUnitDeclarations =
        analyzeWithBuilder(
            TranslationConfiguration.builder()
                .sourceLocations(List.of(file))
                .topLevel(file.getParentFile())
                .loadIncludes(true)
                .debugParser(true)
                .includeWhitelist(
                    new File("src/test/resources/another-include.h").getAbsolutePath())
                .failOnError(true));

    TranslationUnitDeclaration next = translationUnitDeclarations.iterator().next();
    assertNotNull(next);

    // include.h was not in the whitelist, so it should be empty
    assertTrue(next.getIncludes().isEmpty());
  }
}
