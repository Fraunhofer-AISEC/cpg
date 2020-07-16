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

package de.fraunhofer.aisec.cpg.frontends;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend;
import de.fraunhofer.aisec.cpg.helpers.Util;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LanguageFrontendFactory {

  private static final List<String> JAVA_EXTENSIONS = List.of(".java");
  private static final List<String> CXX_EXTENSIONS = List.of(".h", ".c", ".cpp", ".cc");

  // hide ctor
  private LanguageFrontendFactory() {}

  @Nullable
  public static LanguageFrontend getFrontend(
      File file, TranslationConfiguration config, ScopeManager scopeManager) {

    String fileType = Util.getExtension(file);

    if (JAVA_EXTENSIONS.contains(fileType)) {
      return new JavaLanguageFrontend(file, config);
    } else if (CXX_EXTENSIONS.contains(fileType)) {
      return new CXXLanguageFrontend(file, config);
    } else {
      return null;
    }
  }
}
