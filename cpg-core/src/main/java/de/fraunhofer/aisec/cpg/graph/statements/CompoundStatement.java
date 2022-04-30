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

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.StatementHolder;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.ScopeHolder;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.passes.scopes.BlockScope;
import de.fraunhofer.aisec.cpg.passes.scopes.Scope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.neo4j.ogm.annotation.Relationship;

/**
 * A statement which contains a list of statements. A common example is a function body within a
 * {@link FunctionDeclaration}.
 */
public class CompoundStatement extends Statement
    implements StatementHolder, ScopeHolder<BlockScope> {

  /** The list of statements. */
  @Relationship(value = "STATEMENTS", direction = "OUTGOING")
  @NotNull
  private @SubGraph("AST") List<PropertyEdge<Statement>> statements = new ArrayList<>();

  /**
   * This variable helps to differentiate between static and non static initializer blocks. Static
   * initializer blocks are executed when the enclosing declaration is first referred to, e.g.
   * loaded into the jvm or parsed. Non static initializers are executed on Record construction.
   *
   * <p>If a compound statement is part of a method body, this notion is not relevant.
   */
  private boolean staticBlock = false;

  @Override
  public @NotNull List<PropertyEdge<Statement>> getStatementEdges() {
    return this.statements;
  }

  @Override
  public void setStatementEdges(@NotNull List<PropertyEdge<Statement>> statements) {
    this.statements = statements;
  }

  public boolean isStaticBlock() {
    return staticBlock;
  }

  public void setStaticBlock(boolean staticBlock) {
    this.staticBlock = staticBlock;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE).appendSuper(super.toString()).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CompoundStatement)) {
      return false;
    }
    CompoundStatement that = (CompoundStatement) o;
    return super.equals(that)
        && Objects.equals(this.getStatements(), that.getStatements())
        && PropertyEdge.propertyEqualsList(statements, that.statements);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public void newScope(@Nullable Scope parent) {
    // TODO(oxisto): This functionality should be called somewhere else, the class should only
    // create the new scope
    var scope = new BlockScope(this);

    if (parent != null) {
      scope.setParent(parent);
      parent.getChildren().add(scope);
    }

    this.setScope(scope);
  }
}
