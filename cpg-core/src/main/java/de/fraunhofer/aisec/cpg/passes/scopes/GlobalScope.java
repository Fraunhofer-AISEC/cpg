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
package de.fraunhofer.aisec.cpg.passes.scopes;

import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import java.util.Collection;

public class GlobalScope extends StructureDeclarationScope {

  /**
   * This should ideally only be called once. It constructs a new global scope, which is not
   * associated to any AST node. However, depending on the language, a language frontend can
   * explicitly set the ast node using {@link
   * ScopeManager#resetToGlobal(TranslationUnitDeclaration)} if the language needs a global scope
   * that is restricted to a translation unit, i.e. C++ while still maintaing a unique list of
   * global variables.
   */
  public GlobalScope() {
    super(null);
  }

  public void mergeFrom(Collection<GlobalScope> others) {
    for (GlobalScope other : others) {
      this.getStructureDeclarations().addAll(other.getStructureDeclarations());
      this.getValueDeclarations().addAll(other.getValueDeclarations());
      this.getTypedefs().putAll(other.getTypedefs());
      // TODO what to do with astNode?
      for (Scope child : other.getChildren()) {
        child.setParent(this);
        this.children.add(child);
      }
    }
  }
}
