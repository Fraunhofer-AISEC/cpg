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
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CXXLiteralTest extends BaseTest {

  @Test
  void testZeroIntegerLiterals() throws Exception {
    File file = new File("src/test/resources/integer_literals.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true, true);

    FunctionDeclaration zero =
        tu.getDeclarationByName("zero", FunctionDeclaration.class).orElse(null);
    assertNotNull(zero);
    assertEquals("zero", zero.getName());

    assertLiteral(0, TypeParser.createFrom("int", true), zero, "i");
    assertLiteral(0L, TypeParser.createFrom("long", true), zero, "l_with_suffix");
    assertLiteral(0L, TypeParser.createFrom("long long", true), zero, "l_long_long_with_suffix");
    assertLiteral(
        BigInteger.valueOf(0),
        TypeParser.createFrom("unsigned long long", true),
        zero,
        "l_unsigned_long_long_with_suffix");
  }

  @Test
  void testDecimalIntegerLiterals() throws Exception {
    File file = new File("src/test/resources/integer_literals.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true, true);

    FunctionDeclaration decimal =
        tu.getDeclarationByName("decimal", FunctionDeclaration.class).orElse(null);
    assertNotNull(decimal);
    assertEquals("decimal", decimal.getName());

    assertLiteral(42, TypeParser.createFrom("int", true), decimal, "i");
    assertLiteral(9223372036854775807L, TypeParser.createFrom("long", true), decimal, "l");
    assertLiteral(
        9223372036854775807L, TypeParser.createFrom("long", true), decimal, "l_with_suffix");
    assertLiteral(
        9223372036854775807L,
        TypeParser.createFrom("long long", true),
        decimal,
        "l_long_long_with_suffix");

    assertLiteral(
        new BigInteger("9223372036854775809"),
        TypeParser.createFrom("unsigned long", true),
        decimal,
        "l_unsigned_long_with_suffix");
    assertLiteral(
        new BigInteger("9223372036854775808"),
        TypeParser.createFrom("unsigned long long", true),
        decimal,
        "l_long_long_implicit");
    assertLiteral(
        new BigInteger("9223372036854775809"),
        TypeParser.createFrom("unsigned long long", true),
        decimal,
        "l_unsigned_long_long_with_suffix");
  }

  @Test
  void testOctalIntegerLiterals() throws Exception {
    File file = new File("src/test/resources/integer_literals.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true, true);

    FunctionDeclaration octal =
        tu.getDeclarationByName("octal", FunctionDeclaration.class).orElse(null);
    assertNotNull(octal);
    assertEquals("octal", octal.getName());

    assertLiteral(42, TypeParser.createFrom("int", true), octal, "i");
    assertLiteral(42L, TypeParser.createFrom("long", true), octal, "l_with_suffix");
    assertLiteral(
        BigInteger.valueOf(42),
        TypeParser.createFrom("unsigned long long", true),
        octal,
        "l_unsigned_long_long_with_suffix");
  }

  @ParameterizedTest
  @ValueSource(strings = {"octal", "hex", "binary"})
  void testNonDecimalIntegerLiterals() throws Exception {
    File file = new File("src/test/resources/integer_literals.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true, true);

    FunctionDeclaration functionDeclaration =
        tu.getDeclarationByName("hex", FunctionDeclaration.class).orElse(null);
    assertNotNull(functionDeclaration);
    assertEquals("hex", functionDeclaration.getName());

    assertLiteral(42, TypeParser.createFrom("int", true), functionDeclaration, "i");
    assertLiteral(42L, TypeParser.createFrom("long", true), functionDeclaration, "l_with_suffix");
    assertLiteral(
        BigInteger.valueOf(42),
        TypeParser.createFrom("unsigned long long", true),
        functionDeclaration,
        "l_unsigned_long_long_with_suffix");
  }

  @Test
  void testLargeNegativeNumber() throws Exception {
    File file = new File("src/test/resources/largenegativenumber.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true, true);

    FunctionDeclaration main =
        tu.getDeclarationByName("main", FunctionDeclaration.class).orElse(null);
    assertNotNull(main);

    VariableDeclaration a = main.getVariableDeclarationByName("a").orElse(null);
    assertNotNull(a);
    assertEquals(
        1,
        ((Literal) Objects.requireNonNull(a.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    // there are no negative literals, so the construct "-2147483648" is
    // a unary expression and the literal "2147483648". Since "2147483648" is too large to fit
    // in an integer, it should be automatically converted to a long. The resulting value
    // -2147483648 however is small enough to fit into an int, so it is ok for the variable a to
    // have an int type
    VariableDeclaration b = main.getVariableDeclarationByName("b").orElse(null);
    assertNotNull(b);
    assertEquals(
        2147483648L,
        ((Literal) Objects.requireNonNull(b.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration c = main.getVariableDeclarationByName("c").orElse(null);
    assertNotNull(c);
    assertEquals(
        2147483649L,
        ((Literal) Objects.requireNonNull(c.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration d = main.getVariableDeclarationByName("d").orElse(null);
    assertNotNull(d);
    assertEquals(
        new BigInteger("9223372036854775808"),
        ((Literal) Objects.requireNonNull(d.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());
  }

  private void assertLiteral(
      Number expectedValue,
      Type expectedType,
      FunctionDeclaration functionDeclaration,
      String name) {
    VariableDeclaration variableDeclaration =
        functionDeclaration.getVariableDeclarationByName(name).orElse(null);
    assertNotNull(variableDeclaration);

    Literal<?> literal = variableDeclaration.getInitializerAs(Literal.class);
    assertNotNull(literal);

    assertEquals(expectedType, literal.getType());
    assertEquals(expectedValue, literal.getValue());
  }
}
