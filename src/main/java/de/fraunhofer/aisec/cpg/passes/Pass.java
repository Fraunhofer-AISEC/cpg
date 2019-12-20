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

package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a class that enhances the graph before it is persisted.
 *
 * <p>Passes are expected to mutate the {@code TranslationResult}.
 */
public interface Pass extends Consumer<TranslationResult> {

  /** @return might be null */
  @Nullable
  LanguageFrontend getLang();

  /**
   * Passes may need information about what source language they are parsing.
   *
   * @param lang May be null
   */
  void setLang(LanguageFrontend lang);

  void cleanup();

  default TranslationUnitDeclaration createUnknownTranslationUnit(TranslationResult result) {
    TranslationUnitDeclaration declaration =
        NodeBuilder.newTranslationUnitDeclaration("unknown declarations", "");
    declaration.setDummy(true);
    result.getTranslationUnits().add(declaration);
    return declaration;
  }
}
