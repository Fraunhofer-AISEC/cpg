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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.*;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class BotanExampleTest extends BaseTest {

  @Test
  void testExample() throws Exception {
    File file = new File("src/test/resources/botan/symm_block_cipher.cpp");
    TranslationUnitDeclaration declaration =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), false, true);

    assertNotNull(declaration);

    List<Declaration> declarations = declaration.getDeclarations();

    assertEquals(5, declarations.size());

    Declaration doCrypt =
        declarations.stream().filter(decl -> decl.getName().equals("do_crypt")).findFirst().get();

    assertTrue(doCrypt instanceof FunctionDeclaration);
    assertEquals("do_crypt", doCrypt.getName());

    Declaration encrypt =
        declarations.stream().filter(decl -> decl.getName().equals("encrypt")).findFirst().get();

    assertTrue(encrypt instanceof FunctionDeclaration);
    assertEquals("encrypt", encrypt.getName());

    Declaration decrypt =
        declarations.stream().filter(decl -> decl.getName().equals("decrypt")).findFirst().get();

    assertTrue(decrypt instanceof FunctionDeclaration);
    assertEquals("decrypt", decrypt.getName());

    Declaration main =
        declarations.stream().filter(decl -> decl.getName().equals("main")).findFirst().get();

    assertTrue(main instanceof FunctionDeclaration);
    assertEquals("main", main.getName());
  }
}
