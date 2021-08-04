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
package de.fraunhofer.aisec.cpg.frontends;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fraunhofer.aisec.cpg.*;
import java.io.File;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class LanguageFrontendTest extends BaseTest {

  @Test
  void testParseDirectory() throws ExecutionException, InterruptedException {
    TranslationManager analyzer =
        TranslationManager.builder()
            .config(
                TranslationConfiguration.builder()
                    .sourceLocations(new File("src/test/resources/botan"))
                    .debugParser(true)
                    .defaultLanguages()
                    .build())
            .build();
    TranslationResult res = analyzer.analyze().get();
    assertEquals(3, res.getTranslationUnits().size());
  }
}
