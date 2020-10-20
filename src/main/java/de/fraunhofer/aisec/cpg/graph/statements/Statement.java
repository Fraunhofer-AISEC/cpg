/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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

package de.fraunhofer.aisec.cpg.graph.statements;

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

/** A statement. */
public class Statement extends Node implements DeclarationHolder {

  /**
   * A list of local variables associated to this statement, defined by their {@link
   * VariableDeclaration} extracted from Block because for, while, if, switch can declare locals in
   * their condition or initializers
   */
  // TODO: This is actually an AST node just for a subset of nodes, i.e. initializers in for-loops
  @Relationship(value = "locals", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<PropertyEdge> locals = new ArrayList<>();

  public List<VariableDeclaration> getLocals() {
    List<VariableDeclaration> localsVariableDeclaration = new ArrayList<>();

    for (PropertyEdge propertyEdge : this.locals) {
      localsVariableDeclaration.add((VariableDeclaration) propertyEdge.getEnd());
    }

    return Collections.unmodifiableList(localsVariableDeclaration);
  }

  public List<PropertyEdge> getLocalsPropertyEdge() {
    return this.locals;
  }

  public void removeLocal(Declaration variableDeclaration) {
    this.locals = PropertyEdge.removeElementFromList(this.locals, variableDeclaration, true);
  }

  public void setLocals(List<VariableDeclaration> locals) {
    this.locals = PropertyEdge.transformIntoPropertyEdgeList(locals, this, true);
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
        && Objects.equals(this.locals, statement.locals);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof VariableDeclaration) {
      PropertyEdge propertyEdge = new PropertyEdge(this, (VariableDeclaration) declaration);
      propertyEdge.addProperty(Properties.Index, this.locals.size());
      this.locals.add(propertyEdge);
    }
  }
}
