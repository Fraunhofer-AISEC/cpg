/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

public class StructureDeclarationScope extends ValueDeclarationScope {

  public List<Declaration> getStructureDeclarations() {
    return structureDeclarations;
  }

  public void setStructureDeclarations(@NonNull List<Declaration> structureDeclarations) {
    this.structureDeclarations = structureDeclarations;
  }

  @NonNull private List<Declaration> structureDeclarations = new ArrayList<>();

  public StructureDeclarationScope(Node node) {
    super(node);
  }

  private void addStructureDeclaration(@NonNull Declaration declaration) {
    structureDeclarations.add(declaration);

    if (astNode instanceof DeclarationHolder) {
      var holder = (DeclarationHolder) astNode;
      holder.addDeclaration(declaration);
    } else {
      log.error(
          "Trying to add a value declaration to a scope which does not have a declaration holder AST node");
    }
  }

  @Override
  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof ValueDeclaration) {
      addValueDeclaration((ValueDeclaration) declaration);
    } else {
      addStructureDeclaration(declaration);
    }
  }
}
