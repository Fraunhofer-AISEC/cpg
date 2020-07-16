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

import static de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU;
import static de.fraunhofer.aisec.cpg.TestUtils.analyzeWithBuilder;
import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.graph.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CXXSymbolConfigurationTest extends BaseTest {
  @Test
  void testWithoutSymbols() throws Exception {
    File file = new File("src/test/resources/symbols.cpp");
    // parse without symbols
    TranslationUnitDeclaration tu =
        analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> main = tu.getDeclarationsByName("main", FunctionDeclaration.class);
    assertFalse(main.isEmpty());
    FunctionDeclaration funcDecl = main.iterator().next();

    BinaryOperator binaryOperator = funcDecl.getBodyStatementAs(0, BinaryOperator.class);
    assertNotNull(binaryOperator);

    // without additional symbols, the first line will look like a reference (to something we do not
    // know)
    DeclaredReferenceExpression dre = binaryOperator.getRhsAs(DeclaredReferenceExpression.class);
    assertNotNull(dre);
    assertEquals("HELLO_WORLD", dre.getName());

    binaryOperator = funcDecl.getBodyStatementAs(1, BinaryOperator.class);
    assertNotNull(binaryOperator);

    // without additional symbols, the second line will look like a function call (to something we
    // do not know)
    CallExpression call = binaryOperator.getRhsAs(CallExpression.class);
    assertNotNull(call);
    assertEquals("INCREASE", call.getName());
  }

  @Test
  void testWithSymbols() throws Exception {
    File file = new File("src/test/resources/symbols.cpp");

    TranslationUnitDeclaration tu =
        analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(List.of(file))
                    .topLevel(file.getParentFile())
                    .symbols(
                        Map.of(
                            "HELLO_WORLD", "\"Hello World\"",
                            "INCREASE(X)", "X+1"))
                    .defaultPasses())
            .iterator()
            .next();

    Set<FunctionDeclaration> main = tu.getDeclarationsByName("main", FunctionDeclaration.class);
    assertFalse(main.isEmpty());
    FunctionDeclaration funcDecl = main.iterator().next();

    BinaryOperator binaryOperator = funcDecl.getBodyStatementAs(0, BinaryOperator.class);
    assertNotNull(binaryOperator);

    // should be a literal now
    Literal<?> literal = binaryOperator.getRhsAs(Literal.class);
    assertEquals("Hello World", literal.getValue());

    binaryOperator = funcDecl.getBodyStatementAs(1, BinaryOperator.class);
    assertNotNull(binaryOperator);

    // should be expanded to another binary operation 1+1
    BinaryOperator add = binaryOperator.getRhsAs(BinaryOperator.class);
    assertNotNull(add);
    assertEquals("+", add.getOperatorCode());

    Literal<?> literal2 = add.getLhsAs(Literal.class);
    assertEquals(2, literal2.getValue());

    Literal<?> literal1 = add.getRhsAs(Literal.class);
    assertEquals(1, literal1.getValue());
  }
}
