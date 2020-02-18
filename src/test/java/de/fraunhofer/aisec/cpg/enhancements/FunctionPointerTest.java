package de.fraunhofer.aisec.cpg.enhancements;

import static org.junit.jupiter.api.Assertions.*;

import de.fraunhofer.aisec.cpg.TestUtils;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class FunctionPointerTest {

  private List<TranslationUnitDeclaration> analyze(String language) throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "functionPointers");
    File[] files =
        Files.walk(topLevel, Integer.MAX_VALUE)
            .map(Path::toFile)
            .filter(File::isFile)
            .filter(f -> f.getName().endsWith("." + language.toLowerCase()))
            .sorted()
            .toArray(File[]::new);

    TranslationConfiguration config =
        TranslationConfiguration.builder()
            .sourceLocations(files)
            .topLevel(topLevel.toFile())
            .defaultPasses()
            .debugParser(true)
            .failOnError(true)
            .build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();

    return analyzer.analyze().get().getTranslationUnits();
  }

  public void test(String language) throws Exception {
    List<TranslationUnitDeclaration> result = analyze(language);
    List<FunctionDeclaration> functions = Util.subnodesOfType(result, FunctionDeclaration.class);
    FunctionDeclaration main = TestUtils.findByName(functions, "main");
    FunctionDeclaration noParam =
        functions.stream()
            .filter(f -> f.getName().equals("target") && f.getParameters().isEmpty())
            .findFirst()
            .orElseThrow();
    FunctionDeclaration singleParam =
        functions.stream()
            .filter(f -> f.getName().equals("target") && f.getParameters().size() == 1)
            .findFirst()
            .orElseThrow();
    List<CallExpression> calls = Util.subnodesOfType(main, CallExpression.class);
    Pattern pattern = Pattern.compile("\\((?<member>.+)?\\*(?<func>.+)\\)");
    for (CallExpression call : calls) {
      Matcher matcher = pattern.matcher(call.getName());
      assertTrue(matcher.matches(), "Unexpected call " + call.getName());

      switch (matcher.group("func")) {
        case "no_param":
          assertEquals(List.of(noParam), call.getInvokes());
          break;
        case "single_param":
          assertEquals(List.of(singleParam), call.getInvokes());
          break;
        case "unused_no_param":
        case "unused_single_param":
          // TODO once we have dedicated function pointer types, we need to distinguish here!
          assertEquals(List.of(noParam, singleParam), call.getInvokes());
          break;
        default:
          fail("Unexpected call " + call.getName());
      }
    }
  }

  @Test
  public void testC() throws Exception {
    test("C");
  }

  @Test
  public void testCPP() throws Exception {
    test("CPP");
  }
}
