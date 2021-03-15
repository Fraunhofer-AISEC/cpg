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
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LanguageFrontendFactory {

  private static final List<String> JAVA_EXTENSIONS = List.of(".java");
  public static final List<String> CXX_EXTENSIONS = List.of(".c", ".cpp", ".cc");
  private static final List<String> CXX_HEADER_EXTENSIONS = List.of(".h", ".hpp");
  private static final List<String> PYTHON_EXTENSIONS = List.of(".py");

  // hide ctor
  private LanguageFrontendFactory() {}

  @Nullable
  public static LanguageFrontend getFrontend(
      String fileType, TranslationConfiguration config, ScopeManager scopeManager) {

    if (JAVA_EXTENSIONS.contains(fileType)) {
      return new JavaLanguageFrontend(config, scopeManager);
    } else if (CXX_EXTENSIONS.contains(fileType) || CXX_HEADER_EXTENSIONS.contains(fileType)) {
      return new CXXLanguageFrontend(config, scopeManager);
    } else if (PYTHON_EXTENSIONS.contains(fileType)) {
      return new PythonLanguageFrontend(config, scopeManager);
    } else {
      return null;
    }
  }
}
