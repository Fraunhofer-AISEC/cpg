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
import java.util.List;

public class LanguageFrontendFactory {

  private static final List<String> JAVA_EXTENSIONS = List.of(".java");
  private static final List<String> CXX_EXTENSIONS = List.of(".h", ".c", ".cpp", ".cc");

  // hide ctor
  private LanguageFrontendFactory() {}

  public static LanguageFrontend getFrontend(String fileType, TranslationConfiguration config) {

    if (JAVA_EXTENSIONS.contains(fileType)) {
      return new JavaLanguageFrontend(config);
    } else if (CXX_EXTENSIONS.contains(fileType)) {
      return new CXXLanguageFrontend(config);
    } else {
      return null;
    }
  }
}
