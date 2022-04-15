/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/** A statement. */
public abstract class Statement extends Node implements DeclarationHolder {

  /**
   * A list of local variables associated to this statement, defined by their {@link
   * VariableDeclaration} extracted from Block because for, while, if, switch can declare locals in
   * their condition or initializers
   */
  // TODO: This is actually an AST node just for a subset of nodes, i.e. initializers in for-loops
  @Relationship(value = "LOCALS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge<VariableDeclaration>> locals = new ArrayList<>();

  public List<VariableDeclaration> getLocals() {
    return unwrap(this.locals);
  }

  public List<PropertyEdge<VariableDeclaration>> getLocalsPropertyEdge() {
    return this.locals;
  }

  public void removeLocal(VariableDeclaration variableDeclaration) {
    this.locals = PropertyEdge.removeElementFromList(this.locals, variableDeclaration, true);
  }

  public void setLocals(List<VariableDeclaration> locals) {
    this.locals = PropertyEdge.transformIntoOutgoingPropertyEdgeList(locals, this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Statement)) {
      return false;
    }
    Statement statement = (Statement) o;
    return super.equals(statement)
        && Objects.equals(this.getLocals(), statement.getLocals())
        && PropertyEdge.propertyEqualsList(this.locals, statement.locals);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof VariableDeclaration) {
      addIfNotContains(this.locals, (VariableDeclaration) declaration);
    }
  }

  @NotNull
  public List<Declaration> getDeclarations() {
    var list = new ArrayList<Declaration>(this.getLocals());

    return list;
  }
}
