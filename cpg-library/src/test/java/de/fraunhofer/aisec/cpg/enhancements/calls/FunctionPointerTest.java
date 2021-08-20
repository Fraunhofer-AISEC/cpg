/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class FunctionPointerTest extends BaseTest {

  private List<TranslationUnitDeclaration> analyze(String language) throws Exception {
    Path topLevel = Path.of("src", "test", "resources", "functionPointers");
    List<File> files =
        Files.walk(topLevel, Integer.MAX_VALUE)
            .map(Path::toFile)
            .filter(File::isFile)
            .filter(f -> f.getName().endsWith("." + language.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());

    TranslationConfiguration config =
        TranslationConfiguration.builder()
            .sourceLocations(files)
            .topLevel(topLevel.toFile())
            .defaultPasses()
            .defaultLanguages()
            .debugParser(true)
            .failOnError(true)
            .build();

    TranslationManager analyzer = TranslationManager.builder().config(config).build();

    return analyzer.analyze().get().getTranslationUnits();
  }

  void test(String language) throws Exception {
    List<TranslationUnitDeclaration> result = analyze(language);
    List<FunctionDeclaration> functions =
        TestUtils.subnodesOfType(result, FunctionDeclaration.class);
    FunctionDeclaration main = TestUtils.findByUniqueName(functions, "main");
    List<CallExpression> calls = TestUtils.subnodesOfType(main, CallExpression.class);
    FunctionDeclaration noParam =
        TestUtils.findByUniquePredicate(
            functions, f -> f.getName().equals("target") && f.getParameters().isEmpty());
    FunctionDeclaration singleParam =
        TestUtils.findByUniquePredicate(
            functions, f -> f.getName().equals("target") && f.getParameters().size() == 1);
    FunctionDeclaration noParamUnknown =
        TestUtils.findByUniquePredicate(
            functions, f -> f.getName().equals("fun") && f.getParameters().isEmpty());
    FunctionDeclaration singleParamUnknown =
        TestUtils.findByUniquePredicate(
            functions, f -> f.getName().equals("fun") && f.getParameters().size() == 1);
    Pattern pattern = Pattern.compile("\\((?<member>.+)?\\*(?<obj>.+\\.)?(?<func>.+)\\)");
    for (CallExpression call : calls) {
      if (call instanceof ConstructExpression) {
        continue;
      }

      String func;
      if (!call.getName().contains("(")) {
        func = call.getName();
        assertNotEquals("", func, "Unexpected call " + func);
      } else {
        Matcher matcher = pattern.matcher(call.getName());
        assertTrue(matcher.matches(), "Unexpected call " + call.getName());
        func = matcher.group("func");
      }

      switch (func) {
        case "no_param":
        case "no_param_uninitialized":
        case "no_param_field":
        case "no_param_field_uninitialized":
          assertEquals(List.of(noParam), call.getInvokes());
          break;
        case "single_param":
        case "single_param_uninitialized":
        case "single_param_field":
        case "single_param_field_uninitialized":
          assertEquals(List.of(singleParam), call.getInvokes());
          break;
        case "no_param_unknown":
        case "no_param_unknown_uninitialized":
        case "no_param_unknown_field":
        case "no_param_unknown_field_uninitialized":
          assertEquals(List.of(noParamUnknown), call.getInvokes());
          assertTrue(noParamUnknown.isInferred());
          break;
        case "single_param_unknown":
        case "single_param_unknown_uninitialized":
        case "single_param_unknown_field":
        case "single_param_unknown_field_uninitialized":
          assertEquals(List.of(singleParamUnknown), call.getInvokes());
          assertTrue(singleParamUnknown.isInferred());
          break;
        default:
          fail("Unexpected call " + call.getName());
      }

      List<VariableDeclaration> variables =
          TestUtils.subnodesOfType(result, VariableDeclaration.class);
      for (VariableDeclaration variable : variables) {
        switch (variable.getName()) {
          case "no_param_unused":
          case "no_param_unused_field":
          case "no_param_unused_uninitialized":
            assertEquals(noParam, getSourceFunction(variable));
            break;
          case "single_param_unused":
          case "single_param_unused_field":
          case "single_param_unused_field_uninitialized":
            assertEquals(singleParam, getSourceFunction(variable));
            break;
        }
      }
    }
  }

  private FunctionDeclaration getSourceFunction(VariableDeclaration variable) {
    List<FunctionDeclaration> functions = new ArrayList<>();
    Deque<Node> worklist = new ArrayDeque<>();
    Set<Node> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    worklist.push(variable);

    while (!worklist.isEmpty()) {
      Node curr = worklist.pop();
      if (!seen.add(curr)) {
        continue;
      }

      if (curr instanceof FunctionDeclaration) {
        functions.add((FunctionDeclaration) curr);
      } else {
        curr.getPrevDFG().forEach(worklist::push);
      }
    }

    assertEquals(1, functions.size());
    return functions.get(0);
  }

  @Test
  void testC() throws Exception {
    test("C");
  }

  @Test
  void testCPP() throws Exception {
    test("CPP");
  }
}
