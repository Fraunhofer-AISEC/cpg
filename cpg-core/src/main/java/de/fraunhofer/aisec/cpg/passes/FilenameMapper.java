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
import de.fraunhofer.aisec.cpg.processing.IVisitor;
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy;
import org.jetbrains.annotations.NotNull;

@PassIsLastPass
public class FilenameMapper extends Pass {
  @Override
  public void accept(TranslationResult translationResult) {
    for (TranslationUnitDeclaration tu : translationResult.getTranslationUnits()) {
      String name = tu.getName();
      tu.setFile(name);
      handle(tu, name);
    }
  }

  private void handle(Node node, String file) {
    // Using a visitor to avoid loops in the AST
    node.accept(
        Strategy::AST_FORWARD,
        new IVisitor<>() {
          @Override
          public void visit(@NotNull Node child) {
            child.setFile(file);
          }
        });
  }

  @Override
  public void cleanup() {}
}
