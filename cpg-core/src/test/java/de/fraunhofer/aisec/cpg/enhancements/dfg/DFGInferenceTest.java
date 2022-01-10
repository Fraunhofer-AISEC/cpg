/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements.dfg;

import de.fraunhofer.aisec.cpg.BaseTest;
import de.fraunhofer.aisec.cpg.InferenceConfiguration;
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DFGInferenceTest extends BaseTest {

  private static List<CallExpression> calls = new ArrayList<>();

  @BeforeAll
  public static void setup()
      throws TranslationException, InterruptedException, ExecutionException, TimeoutException {
    File file = new File("src/test/resources/dfg/examples/Calls.java");

    InferenceConfiguration iConf =
        InferenceConfiguration.builder().overapproximateDataFlows(true).build();

    TranslationConfiguration config =
        TranslationConfiguration.builder()
            .sourceLocations(file)
            .defaultPasses()
            .defaultLanguages()
            .inferenceConfiguration(iConf)
            .build();
    TranslationResult result =
        TranslationManager.builder().config(config).build().analyze().get(20, TimeUnit.SECONDS);
    TranslationUnitDeclaration tu = result.getTranslationUnits().get(0);
    calls = Util.filterCast(SubgraphWalker.flattenAST(tu), CallExpression.class);
  }

  @Test
  void testInferParameters() throws Exception {
    for (CallExpression call : calls) {
      List<FunctionDeclaration> funcs = call.getInvokes();
      for (FunctionDeclaration func :
          funcs.stream().filter(f -> f.isInferred()).collect(Collectors.toList())) {
        for (int i = 0; i < call.getArguments().size(); i++) {
          Assertions.assertTrue(
              func.getParameters().get(i).getPrevDFG().contains(call.getArguments().get(i)));
        }
        Assertions.assertTrue(func.getPrevDFG().containsAll(func.getParameters()));
      }
    }
  }

  @Test
  void testInferDFGFromBase() throws Exception {
    for (CallExpression call : Util.filterCast(calls, MemberCallExpression.class)) {
      List<FunctionDeclaration> funcs = call.getInvokes();
      for (FunctionDeclaration func :
          funcs.stream().filter(f -> f.isInferred()).collect(Collectors.toList())) {

        Assertions.assertTrue(func.getPrevDFG().contains(call.getBase()));
      }
    }
  }
}
