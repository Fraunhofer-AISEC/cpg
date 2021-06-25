/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements.calls;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConstructorsTest extends BaseTest {

  private final Path topLevel = Path.of("src", "test", "resources", "constructors");

  @Test
  void testJava() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("java", topLevel, true);
    List<ConstructorDeclaration> constructors =
        TestUtils.subnodesOfType(result, ConstructorDeclaration.class);
    ConstructorDeclaration noArg =
        TestUtils.findByUniquePredicate(constructors, c -> c.getParameters().size() == 0);
    ConstructorDeclaration singleArg =
        TestUtils.findByUniquePredicate(constructors, c -> c.getParameters().size() == 1);
    ConstructorDeclaration twoArgs =
        TestUtils.findByUniquePredicate(constructors, c -> c.getParameters().size() == 2);

    List<VariableDeclaration> variables =
        TestUtils.subnodesOfType(result, VariableDeclaration.class);

    VariableDeclaration a1 = TestUtils.findByUniqueName(variables, "a1");
    assertTrue(a1.getInitializer() instanceof NewExpression);
    assertTrue(
        ((NewExpression) a1.getInitializer()).getInitializer() instanceof ConstructExpression);
    ConstructExpression a1Initializer =
        (ConstructExpression) ((NewExpression) a1.getInitializer()).getInitializer();
    assertEquals(noArg, a1Initializer.getConstructor());

    VariableDeclaration a2 = TestUtils.findByUniqueName(variables, "a2");
    assertTrue(a2.getInitializer() instanceof NewExpression);
    assertTrue(
        ((NewExpression) a2.getInitializer()).getInitializer() instanceof ConstructExpression);
    ConstructExpression a2Initializer =
        (ConstructExpression) ((NewExpression) a2.getInitializer()).getInitializer();
    assertEquals(singleArg, a2Initializer.getConstructor());

    VariableDeclaration a3 = TestUtils.findByUniqueName(variables, "a3");
    assertTrue(a3.getInitializer() instanceof NewExpression);
    assertTrue(
        ((NewExpression) a3.getInitializer()).getInitializer() instanceof ConstructExpression);
    ConstructExpression a3Initializer =
        (ConstructExpression) ((NewExpression) a3.getInitializer()).getInitializer();
    assertEquals(twoArgs, a3Initializer.getConstructor());

    VariableDeclaration a4 = TestUtils.findByUniqueName(variables, "a4");
    assertTrue(a4.getInitializer() instanceof UninitializedValue);
  }

  @Test
  void testCPP() throws Exception {
    List<TranslationUnitDeclaration> result = TestUtils.analyze("cpp", topLevel, true);
    List<ConstructorDeclaration> constructors =
        TestUtils.subnodesOfType(result, ConstructorDeclaration.class);
    ConstructorDeclaration noArg =
        TestUtils.findByUniquePredicate(
            constructors, c -> c.getParameters().size() == 0 && c.getName().equals("A"));
    ConstructorDeclaration singleArg =
        TestUtils.findByUniquePredicate(
            constructors, c -> c.getParameters().size() == 1 && c.getName().equals("A"));
    ConstructorDeclaration twoArgs =
        TestUtils.findByUniquePredicate(
            constructors, c -> c.getParameters().size() == 2 && c.getName().equals("A"));

    List<VariableDeclaration> variables =
        TestUtils.subnodesOfType(result, VariableDeclaration.class);

    VariableDeclaration a1 = TestUtils.findByUniqueName(variables, "a1");
    assertTrue(a1.getInitializer() instanceof ConstructExpression);
    ConstructExpression a1Initializer = (ConstructExpression) a1.getInitializer();
    assertEquals(noArg, a1Initializer.getConstructor());

    VariableDeclaration a2 = TestUtils.findByUniqueName(variables, "a2");
    assertTrue(a2.getInitializer() instanceof ConstructExpression);
    ConstructExpression a2Initializer = (ConstructExpression) a2.getInitializer();
    assertEquals(singleArg, a2Initializer.getConstructor());

    VariableDeclaration a3 = TestUtils.findByUniqueName(variables, "a3");
    assertTrue(a3.getInitializer() instanceof ConstructExpression);
    ConstructExpression a3Initializer = (ConstructExpression) a3.getInitializer();
    assertEquals(twoArgs, a3Initializer.getConstructor());

    VariableDeclaration a4 = TestUtils.findByUniqueName(variables, "a4");
    assertTrue(a4.getInitializer() instanceof ConstructExpression);
    ConstructExpression a4Initializer = (ConstructExpression) a4.getInitializer();
    assertEquals(noArg, a4Initializer.getConstructor());

    VariableDeclaration a5 = TestUtils.findByUniqueName(variables, "a5");
    assertTrue(a5.getInitializer() instanceof ConstructExpression);
    ConstructExpression a5Initializer = (ConstructExpression) a5.getInitializer();
    assertEquals(singleArg, a5Initializer.getConstructor());

    VariableDeclaration a6 = TestUtils.findByUniqueName(variables, "a6");
    assertTrue(a6.getInitializer() instanceof ConstructExpression);
    ConstructExpression a6Initializer = (ConstructExpression) a6.getInitializer();
    assertEquals(twoArgs, a6Initializer.getConstructor());

    VariableDeclaration a7 = TestUtils.findByUniqueName(variables, "a7");
    assertTrue(a7.getInitializer() instanceof NewExpression);
    assertTrue(
        ((NewExpression) a7.getInitializer()).getInitializer() instanceof ConstructExpression);
    ConstructExpression a7Initializer =
        (ConstructExpression) ((NewExpression) a7.getInitializer()).getInitializer();
    assertEquals(noArg, a7Initializer.getConstructor());

    VariableDeclaration a8 = TestUtils.findByUniqueName(variables, "a8");
    assertTrue(a8.getInitializer() instanceof NewExpression);
    assertTrue(
        ((NewExpression) a8.getInitializer()).getInitializer() instanceof ConstructExpression);
    ConstructExpression a8Initializer =
        (ConstructExpression) ((NewExpression) a8.getInitializer()).getInitializer();
    assertEquals(noArg, a8Initializer.getConstructor());

    VariableDeclaration a9 = TestUtils.findByUniqueName(variables, "a9");
    assertTrue(a9.getInitializer() instanceof NewExpression);
    assertTrue(
        ((NewExpression) a9.getInitializer()).getInitializer() instanceof ConstructExpression);
    ConstructExpression a9Initializer =
        (ConstructExpression) ((NewExpression) a9.getInitializer()).getInitializer();
    assertEquals(singleArg, a9Initializer.getConstructor());

    VariableDeclaration a10 = TestUtils.findByUniqueName(variables, "a10");
    assertTrue(a10.getInitializer() instanceof NewExpression);
    assertTrue(
        ((NewExpression) a10.getInitializer()).getInitializer() instanceof ConstructExpression);
    ConstructExpression a10Initializer =
        (ConstructExpression) ((NewExpression) a10.getInitializer()).getInitializer();
    assertEquals(twoArgs, a10Initializer.getConstructor());
  }

  @Test
  void testCPPFullDefault() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "defaultarg", "constructorDefault.cpp").toFile()),
            topLevel,
            true);
    List<ConstructorDeclaration> constructors =
        TestUtils.subnodesOfType(result, ConstructorDeclaration.class);

    List<VariableDeclaration> variables =
        TestUtils.subnodesOfType(result, VariableDeclaration.class);

    ConstructorDeclaration twoDefaultArg =
        TestUtils.findByUniquePredicate(
            constructors, c -> c.getDefaultParameters().size() == 2 && c.getName().equals("D"));

    Literal<?> literal0 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(0));
    Literal<?> literal1 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(1));

    VariableDeclaration d1 = TestUtils.findByUniqueName(variables, "d1");
    assertTrue(d1.getInitializer() instanceof ConstructExpression);
    ConstructExpression d1Initializer = (ConstructExpression) d1.getInitializer();
    assertEquals(twoDefaultArg, d1Initializer.getConstructor());
    assertEquals(0, d1Initializer.getArguments().size());
    assertTrue(twoDefaultArg.getNextEOG().contains(literal0));
    assertTrue(twoDefaultArg.getNextEOG().contains(literal1));
    assertTrue(literal0.getNextEOG().contains(literal1));
    for (Node node : twoDefaultArg.getNextEOG()) {
      if (!(node.equals(literal0) || node.equals(literal1))) {
        assertTrue(literal1.getNextEOG().contains(node));
      }
    }

    VariableDeclaration d2 = TestUtils.findByUniqueName(variables, "d2");
    assertTrue(d2.getInitializer() instanceof ConstructExpression);
    ConstructExpression d2Initializer = (ConstructExpression) d2.getInitializer();
    assertEquals(twoDefaultArg, d2Initializer.getConstructor());
    assertEquals(1, d2Initializer.getArguments().size());
    assertEquals(2, ((Literal) d2Initializer.getArguments().get(0)).getValue());

    VariableDeclaration d3 = TestUtils.findByUniqueName(variables, "d3");
    assertTrue(d3.getInitializer() instanceof ConstructExpression);
    ConstructExpression d3Initializer = (ConstructExpression) d3.getInitializer();
    assertEquals(twoDefaultArg, d3Initializer.getConstructor());
    assertEquals(2, d3Initializer.getArguments().size());
    assertEquals(3, ((Literal) d3Initializer.getArguments().get(0)).getValue());
    assertEquals(4, ((Literal) d3Initializer.getArguments().get(1)).getValue());
  }

  @Test
  void testCPPPartialDefault() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(Path.of(topLevel.toString(), "defaultarg", "constructorDefault.cpp").toFile()),
            topLevel,
            true);
    List<ConstructorDeclaration> constructors =
        TestUtils.subnodesOfType(result, ConstructorDeclaration.class);

    List<VariableDeclaration> variables =
        TestUtils.subnodesOfType(result, VariableDeclaration.class);

    ConstructorDeclaration singleDefaultArg =
        TestUtils.findByUniquePredicate(
            constructors, c -> c.getParameters().size() == 2 && c.getName().equals("E"));

    Literal<?> literal10 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(10));

    VariableDeclaration e1 = TestUtils.findByUniqueName(variables, "e1");
    assertTrue(e1.getInitializer() instanceof ConstructExpression);
    ConstructExpression e1Initializer = (ConstructExpression) e1.getInitializer();
    assertTrue(e1Initializer.getConstructor().isImplicit());
    assertEquals(0, e1Initializer.getArguments().size());

    VariableDeclaration e2 = TestUtils.findByUniqueName(variables, "e2");
    assertTrue(e2.getInitializer() instanceof ConstructExpression);
    ConstructExpression e2Initializer = (ConstructExpression) e2.getInitializer();
    assertEquals(singleDefaultArg, e2Initializer.getConstructor());
    assertEquals(1, e2Initializer.getArguments().size());
    assertEquals(5, ((Literal) e2Initializer.getArguments().get(0)).getValue());
    assertTrue(singleDefaultArg.getNextEOG().contains(literal10));
    for (Node node : singleDefaultArg.getNextEOG()) {
      if (!node.equals(literal10)) {
        assertTrue(literal10.getNextEOG().contains(node));
      }
    }

    VariableDeclaration e3 = TestUtils.findByUniqueName(variables, "e3");
    assertTrue(e3.getInitializer() instanceof ConstructExpression);
    ConstructExpression e3Initializer = (ConstructExpression) e3.getInitializer();
    assertEquals(singleDefaultArg, e3Initializer.getConstructor());
    assertEquals(2, e3Initializer.getArguments().size());
    assertEquals(6, ((Literal) e3Initializer.getArguments().get(0)).getValue());
    assertEquals(7, ((Literal) e3Initializer.getArguments().get(1)).getValue());
  }

  @Test
  void testCPPImplicitCast() throws Exception {
    List<TranslationUnitDeclaration> result =
        TestUtils.analyze(
            List.of(
                Path.of(topLevel.toString(), "implicitcastarg", "constructorImplicit.cpp")
                    .toFile()),
            topLevel,
            true);
    List<ConstructorDeclaration> constructors =
        TestUtils.subnodesOfType(result, ConstructorDeclaration.class);

    List<VariableDeclaration> variables =
        TestUtils.subnodesOfType(result, VariableDeclaration.class);

    ConstructorDeclaration implicitConstructor =
        TestUtils.findByUniquePredicate(constructors, c -> c.getName().equals("I"));

    Literal<?> literal10 =
        TestUtils.findByUniquePredicate(
            TestUtils.subnodesOfType(result, Literal.class), l -> l.getValue().equals(10));

    VariableDeclaration i1 = TestUtils.findByUniqueName(variables, "i1");
    assertTrue(i1.getInitializer() instanceof ConstructExpression);
    ConstructExpression i1Constructor = (ConstructExpression) i1.getInitializer();
    assertFalse(i1Constructor.isImplicit());

    assertEquals(implicitConstructor, i1Constructor.getConstructor());
    assertEquals(1, i1Constructor.getArguments().size());
    assertTrue(i1Constructor.getArguments().get(0) instanceof CastExpression);
    CastExpression i1ConstructorArgument = (CastExpression) i1Constructor.getArguments().get(0);
    assertEquals("int", i1ConstructorArgument.getCastType().getName());
    assertEquals("1.0", i1ConstructorArgument.getExpression().getCode());
    assertEquals("double", i1ConstructorArgument.getExpression().getType().getName());

    ConstructorDeclaration implicitConstructorWithDefault =
        TestUtils.findByUniquePredicate(constructors, c -> c.getName().equals("H"));

    VariableDeclaration h1 = TestUtils.findByUniqueName(variables, "h1");
    assertTrue(h1.getInitializer() instanceof ConstructExpression);
    ConstructExpression h1Constructor = (ConstructExpression) h1.getInitializer();
    assertFalse(h1Constructor.isImplicit());

    assertEquals(implicitConstructorWithDefault, h1Constructor.getConstructor());
    assertEquals(1, h1Constructor.getArguments().size());

    assertTrue(h1Constructor.getArguments().get(0) instanceof CastExpression);
    CastExpression h1ConstructorArgument1 = (CastExpression) h1Constructor.getArguments().get(0);
    assertEquals("int", h1ConstructorArgument1.getCastType().getName());
    assertEquals("2.0", h1ConstructorArgument1.getExpression().getCode());
    assertEquals("double", h1ConstructorArgument1.getExpression().getType().getName());

    assertTrue(implicitConstructorWithDefault.getNextEOG().contains(literal10));
    for (Node node : implicitConstructorWithDefault.getNextEOG()) {
      if (!node.equals(literal10)) {
        assertTrue(literal10.getNextEOG().contains(node));
      }
    }
  }
}
