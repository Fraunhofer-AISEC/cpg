package de.fraunhofer.aisec.cpg.enhancements.calls;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UninitializedValue;

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
        TestUtils.findByUniquePredicate(constructors, c -> c.getParameters().size() == 0);
    ConstructorDeclaration singleArg =
        TestUtils.findByUniquePredicate(constructors, c -> c.getParameters().size() == 1);
    ConstructorDeclaration twoArgs =
        TestUtils.findByUniquePredicate(constructors, c -> c.getParameters().size() == 2);

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
}
