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

package de.fraunhofer.aisec.cpg.frontends.java;

import static org.junit.jupiter.api.Assertions.fail;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JavaRegressionsTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaRegressionsTest.class);

  @Test
  void testCompiledTypesExtractorTask() throws Exception {
    // Caused java.lang.NullPointerException
    analyze("src/test/resources/regressions/CompiledTypesExtractorTask.java");
  }

  @Test
  void testStringDecoder() throws Exception {
    // Caused java.lang.RuntimeException: Method 'readUtf8Char' cannot be resolved in context
    // readUtf8Char() (line: 53) MethodCallExprContext{wrapped=readUtf8Char()}. Parameter types: []
    analyze("src/test/resources/regressions/StringDecoder.java");
  }

  @Test
  void testPrefspecsLexerprs() throws Exception {
    // Caused java.lang.StackOverflowError in VariableUsageResolver.resolveFieldUsages
    analyze("src/test/resources/regressions/PrefspecsLexerprs.java");
  }

  private void analyze(String pathname) throws InterruptedException {
    try {
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
