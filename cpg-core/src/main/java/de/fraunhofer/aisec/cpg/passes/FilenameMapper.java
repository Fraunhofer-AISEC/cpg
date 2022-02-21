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
package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;

public class FilenameMapper extends Pass {

  @Override
  public void accept(TranslationResult translationResult) {
    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      String name = tu.getName();
      tu.setFile(name);
      tu.getDeclarations().forEach(d -> handle(d, name));
      tu.getIncludes().forEach(d -> handle(d, name));
      tu.getNamespaces().forEach(d -> handle(d, name));
    }
  }

  private void handle(Node node, String file) {
    if (node != null) {
      node.setFile(file);
      for (Node child : SubgraphWalker.getAstChildren(node)) {
        handle(child, file);
      }
    }
  }

  @Override
  public void cleanup() {}
}
