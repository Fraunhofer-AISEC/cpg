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

import static de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation;

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TypedefDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Is a scope where local variables can be declared and independent from specific language
 * constructs. Works for if, for, and extends to the block scope
 */
public class ValueDeclarationScope extends Scope {

  protected static final Logger log = LoggerFactory.getLogger(ValueDeclarationScope.class);

  @NotNull private List<ValueDeclaration> valueDeclarations = new ArrayList<>();

  /** A map of typedefs keyed by their alias. */
  @NotNull private final Map<Type, TypedefDeclaration> typedefs = new HashMap<>();

  public ValueDeclarationScope(Node node) {
    this.astNode = node;
  }

  @NotNull
  public List<ValueDeclaration> getValueDeclarations() {
    return valueDeclarations;
  }

  public void setValueDeclarations(@NotNull List<ValueDeclaration> valueDeclarations) {
    this.valueDeclarations = valueDeclarations;
  }

  public @NotNull Map<Type, TypedefDeclaration> getTypedefs() {
    return typedefs;
  }

  public void addTypedef(TypedefDeclaration typedef) {
    this.typedefs.put(typedef.getAlias(), typedef);
  }

  public void addDeclaration(@NotNull Declaration declaration, boolean addToAST) {
    if (declaration instanceof ValueDeclaration) {
      addValueDeclaration((ValueDeclaration) declaration, addToAST);
    } else {
      errorWithFileLocation(
          declaration, log, "A non ValueDeclaration can not be added to a DeclarationScope");
    }
  }

  /**
   * THe value declarations are only set in the ast node if the handler of the ast node may not know
   * the outer
   *
   * @param valueDeclaration the {@link ValueDeclaration}
   * @param addToAST whether to also add the declaration to the AST of its holder.
   */
  void addValueDeclaration(ValueDeclaration valueDeclaration, boolean addToAST) {
    this.valueDeclarations.add(valueDeclaration);

    if (addToAST) {
      if (astNode instanceof DeclarationHolder) {
        var holder = (DeclarationHolder) astNode;
        holder.addDeclaration(valueDeclaration);
      } else {
        errorWithFileLocation(
            valueDeclaration,
            log,
            "Trying to add a value declaration to a scope which does not have a declaration holder AST node");
      }
    }
    /*
     There are nodes where we do not set the declaration when storing them in the scope,
     mostly for structures that have a single value-declaration: WhileStatement, DoStatement,
     ForStatement, SwitchStatement; and others where the location of declaration is somewhere
     deeper in the AST-subtree: CompoundStatement, AssertStatement.
    */
  }
}
