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
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class CXXSymbolConfigurationTest {
  @Test
  void testWithoutSymbols() throws TranslationException {
    // parse without symbols
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(
                TranslationConfiguration.builder().defaultPasses().build(), new ScopeManager())
            .parse(new File("src/test/resources/symbols.cpp"));

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    BinaryOperator binaryOperator = main.getBodyStatementAs(0, BinaryOperator.class);
    assertNotNull(binaryOperator);

    // without additional symbols, the first line will look like a reference (to something we do not
    // know)
    DeclaredReferenceExpression dre = binaryOperator.getRhsAs(DeclaredReferenceExpression.class);
    assertNotNull(dre);
    assertEquals("HELLO_WORLD", dre.getName());

    binaryOperator = main.getBodyStatementAs(1, BinaryOperator.class);
    assertNotNull(binaryOperator);

    // without additional symbols, the second line will look like a function call (to something we
    // do not know)
    CallExpression call = binaryOperator.getRhsAs(CallExpression.class);
    assertNotNull(call);
    assertEquals("INCREASE", call.getName());
  }

  @Test
  void testWithSymbols() throws TranslationException {
    // let's try with symbol definitions
    TranslationUnitDeclaration tu =
        new CXXLanguageFrontend(
                TranslationConfiguration.builder()
                    .symbols(
                        Map.of(
                            "HELLO_WORLD", "\"Hello World\"",
                            "INCREASE(X)", "X+1"))
                    .defaultPasses()
                    .build(),
                new ScopeManager())
            .parse(new File("src/test/resources/symbols.cpp"));

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    BinaryOperator binaryOperator = main.getBodyStatementAs(0, BinaryOperator.class);
    assertNotNull(binaryOperator);

    // should be a literal now
    Literal literal = binaryOperator.getRhsAs(Literal.class);
    assertEquals("Hello World", literal.getValue());

    binaryOperator = main.getBodyStatementAs(1, BinaryOperator.class);
    assertNotNull(binaryOperator);

    // should be expanded to another binary operation 1+1
    BinaryOperator add = binaryOperator.getRhsAs(BinaryOperator.class);
    assertNotNull(add);
    assertEquals("+", add.getOperatorCode());

    Literal literal2 = add.getLhsAs(Literal.class);
    assertEquals(2, literal2.getValue());

    Literal literal1 = add.getRhsAs(Literal.class);
    assertEquals(1, literal1.getValue());
  }
}
