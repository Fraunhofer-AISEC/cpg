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

package de.fraunhofer.aisec.cpg.passes.scopes;

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.TypedefDeclaration;
import de.fraunhofer.aisec.cpg.graph.ValueDeclaration;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Is a scope where local variables can be declared and independent from specific language
 * constructs. Works for if, for, and extends to the block scope
 */
public class DeclarationScope extends Scope {

  @NonNull private List<ValueDeclaration> valueDeclarations = new ArrayList<>();
  @NonNull private List<TypedefDeclaration> typedefs = new ArrayList<>();

  public DeclarationScope(Node node) {
    this.astNode = node;
  }

  @NonNull
  public List<ValueDeclaration> getValueDeclarations() {
    return valueDeclarations;
  }

  public void setValueDeclarations(@NonNull List<ValueDeclaration> valueDeclarations) {
    this.valueDeclarations = valueDeclarations;
  }

  public void addValueDeclaration(ValueDeclaration valueDeclaration) {
    this.valueDeclarations.add(valueDeclaration);
  }

  public List<TypedefDeclaration> getTypedefs() {
    return typedefs;
  }

  public void setTypedefs(List<TypedefDeclaration> typedefs) {
    this.typedefs = typedefs;
  }

  public void addTypedef(TypedefDeclaration typedef) {
    this.typedefs.add(typedef);
  }
}
