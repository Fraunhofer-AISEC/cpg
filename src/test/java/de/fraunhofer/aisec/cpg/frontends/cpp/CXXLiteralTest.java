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

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.declaration.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declaration.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.declaration.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statement.expression.Literal;
import de.fraunhofer.aisec.cpg.graph.statement.expression.UnaryOperator;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CXXLiteralTest extends BaseTest {

  @Test
  void testZeroIntegerLiterals() throws Exception {
    File file = new File("src/test/resources/integer_literals.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> zero = tu.getDeclarationsByName("zero", FunctionDeclaration.class);
    assertFalse(zero.isEmpty());
    FunctionDeclaration funcDecl = zero.iterator().next();
    assertEquals("zero", funcDecl.getName());

    assertLiteral(0, TypeParser.createFrom("int", true), funcDecl, "i");
    assertLiteral(0L, TypeParser.createFrom("long", true), funcDecl, "l_with_suffix");
    assertLiteral(
        0L, TypeParser.createFrom("long long", true), funcDecl, "l_long_long_with_suffix");
    assertLiteral(
        BigInteger.valueOf(0),
        TypeParser.createFrom("unsigned long long", true),
        funcDecl,
        "l_unsigned_long_long_with_suffix");
  }

  @Test
  void testDecimalIntegerLiterals() throws Exception {
    File file = new File("src/test/resources/integer_literals.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> decimal =
        tu.getDeclarationsByName("decimal", FunctionDeclaration.class);
    assertFalse(decimal.isEmpty());
    FunctionDeclaration funcDecl = decimal.iterator().next();
    assertEquals("decimal", funcDecl.getName());

    assertLiteral(42, TypeParser.createFrom("int", true), funcDecl, "i");
    assertLiteral(9223372036854775807L, TypeParser.createFrom("long", true), funcDecl, "l");
    assertLiteral(
        9223372036854775807L, TypeParser.createFrom("long", true), funcDecl, "l_with_suffix");
    assertLiteral(
        9223372036854775807L,
        TypeParser.createFrom("long long", true),
        funcDecl,
        "l_long_long_with_suffix");

    assertLiteral(
        new BigInteger("9223372036854775809"),
        TypeParser.createFrom("unsigned long", true),
        funcDecl,
        "l_unsigned_long_with_suffix");
    assertLiteral(
        new BigInteger("9223372036854775808"),
        TypeParser.createFrom("unsigned long long", true),
        funcDecl,
        "l_long_long_implicit");
    assertLiteral(
        new BigInteger("9223372036854775809"),
        TypeParser.createFrom("unsigned long long", true),
        funcDecl,
        "l_unsigned_long_long_with_suffix");
  }

  @Test
  void testOctalIntegerLiterals() throws Exception {
    File file = new File("src/test/resources/integer_literals.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> octal = tu.getDeclarationsByName("octal", FunctionDeclaration.class);
    assertFalse(octal.isEmpty());
    FunctionDeclaration funcDecl = octal.iterator().next();
    assertEquals("octal", funcDecl.getName());

    assertLiteral(42, TypeParser.createFrom("int", true), funcDecl, "i");
    assertLiteral(42L, TypeParser.createFrom("long", true), funcDecl, "l_with_suffix");
    assertLiteral(
        BigInteger.valueOf(42),
        TypeParser.createFrom("unsigned long long", true),
        funcDecl,
        "l_unsigned_long_long_with_suffix");
  }

  @ParameterizedTest
  @ValueSource(strings = {"octal", "hex", "binary"})
  void testNonDecimalIntegerLiterals() throws Exception {
    File file = new File("src/test/resources/integer_literals.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> hex = tu.getDeclarationsByName("hex", FunctionDeclaration.class);
    assertFalse(hex.isEmpty());
    FunctionDeclaration funcDecl = hex.iterator().next();
    assertEquals("hex", funcDecl.getName());

    assertLiteral(42, TypeParser.createFrom("int", true), funcDecl, "i");
    assertLiteral(42L, TypeParser.createFrom("long", true), funcDecl, "l_with_suffix");
    assertLiteral(
        BigInteger.valueOf(42),
        TypeParser.createFrom("unsigned long long", true),
        funcDecl,
        "l_unsigned_long_long_with_suffix");
  }

  @Test
  void testLargeNegativeNumber() throws Exception {
    File file = new File("src/test/resources/largenegativenumber.cpp");
    TranslationUnitDeclaration tu =
        TestUtils.analyzeAndGetFirstTU(List.of(file), file.getParentFile().toPath(), true);

    Set<FunctionDeclaration> main = tu.getDeclarationsByName("main", FunctionDeclaration.class);
    assertFalse(main.isEmpty());
    FunctionDeclaration funcDecl = main.iterator().next();

    VariableDeclaration a = funcDecl.getVariableDeclarationByName("a").orElse(null);
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
    VariableDeclaration b = funcDecl.getVariableDeclarationByName("b").orElse(null);
    assertNotNull(b);
    assertEquals(
        2147483648L,
        ((Literal) Objects.requireNonNull(b.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration c = funcDecl.getVariableDeclarationByName("c").orElse(null);
    assertNotNull(c);
    assertEquals(
        2147483649L,
        ((Literal) Objects.requireNonNull(c.getInitializerAs(UnaryOperator.class)).getInput())
            .getValue());

    VariableDeclaration d = funcDecl.getVariableDeclarationByName("d").orElse(null);
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
