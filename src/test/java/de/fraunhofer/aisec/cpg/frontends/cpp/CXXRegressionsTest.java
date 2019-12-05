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
import static org.junit.jupiter.api.Assertions.fail;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CXXRegressionsTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(CXXRegressionsTest.class);

  /** Issue 84, NPE at de.fraunhofer.aisec.cpg.passes.CallResolver.resolve(CallResolver.java:116) */
  @Test
  void test84NPE() {
    try {
      TranslationUnitDeclaration declaration =
          new CXXLanguageFrontend(TranslationConfiguration.builder().build())
              .parse(new File("src/test/resources/regressions/AntiCheat.cpp"));
    } catch (Exception e) {
      fail(e);
    }
  }

  /**
   * Issue 117, OOM Exception when parsing large source file.
   *
   * <p>CDT parser requires "-Xmx4048m" heap size because it will create > 2.5 mio literals for this
   * specific source file.
   */
  @Test
  @Disabled
  void test117OOMException() {
    try {
      TranslationUnitDeclaration declaration =
          new CXXLanguageFrontend(TranslationConfiguration.builder().build())
              .parse(new File("src/test/resources/regressions/qrc_bitcoin.cpp"));
    } catch (Exception e) {
      fail(e);
    }
  }

  /** Issue 107, incorrect types when referencing/dereferencing pointers */
  @Test
  void testPointerDereference() throws Exception {
    TranslationUnitDeclaration declaration =
        new CXXLanguageFrontend(TranslationConfiguration.builder().build())
            .parse(new File("src/test/resources/regressions/pointers.cpp"));
    FunctionDeclaration main = (FunctionDeclaration) declaration.getDeclarations().get(0);
    for (Statement s : ((CompoundStatement) main.getBody()).getStatements()) {
      switch (s.getRegion().getStartLine()) {
        case 4:
          // int ***x;
          VariableDeclaration x =
              (VariableDeclaration) ((DeclarationStatement) s).getSingleDeclaration();
          assertEquals("x", x.getName());
          assertEquals("int", x.getType().getTypeName());
          assertEquals("***", x.getType().getTypeAdjustment());
          break;
        case 5:
          // int *y = **x;
          VariableDeclaration y =
              (VariableDeclaration) ((DeclarationStatement) s).getSingleDeclaration();
          assertEquals("y", y.getName());
          assertEquals("int", y.getType().getTypeName());
          assertEquals("*", y.getType().getTypeAdjustment());
          assertEquals("int", y.getInitializer().getType().getTypeName());
          assertEquals("*", y.getInitializer().getType().getTypeAdjustment());
          break;
        case 6:
          // int **z = &y;
          VariableDeclaration z =
              (VariableDeclaration) ((DeclarationStatement) s).getSingleDeclaration();
          assertEquals("z", z.getName());
          assertEquals("int", z.getType().getTypeName());
          assertEquals("**", z.getType().getTypeAdjustment());
          assertEquals("int", z.getInitializer().getType().getTypeName());
          assertEquals("**", z.getInitializer().getType().getTypeAdjustment());
      }
    }
  }

  @Test
  void run_concurrent_queue() throws Exception {
    // Caused by: java.lang.NullPointerException
    analyze("src/test/resources/regressions/concurrent_queue.cpp");
  }

  private void analyze(String pathname) throws InterruptedException {
    try {
      TranslationResult analyzer =
          TranslationManager.builder()
              .config(
                  TranslationConfiguration.builder()
                      .sourceFiles(new File(pathname))
                      .defaultPasses()
                      .debugParser(false)
                      .loadIncludes(false)
                      .build())
              .build()
              .analyze()
              .get(12000, TimeUnit.SECONDS);

    } catch (Throwable e) {
      e.printStackTrace();
      fail();
    }
  }
}
