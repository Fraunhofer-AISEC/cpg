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

import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * A statement which contains a list of statements. A common example is a function body within a
 * {@link FunctionDeclaration}.
 */
public class CompoundStatement extends Statement {

  /** The list of statements. */
  @Relationship(value = "STATEMENTS", direction = "OUTGOING")
  @NonNull
  private @SubGraph("AST") List<PropertyEdge> statements = new ArrayList<>();

  @NonNull
  public List<Statement> getStatements() {
    List<Statement> targets = new ArrayList<>();
    for (PropertyEdge propertyEdge : this.statements) {
      targets.add((Statement) propertyEdge.getEnd());
    }
    return Collections.unmodifiableList(targets);
  }

  @NonNull
  public List<PropertyEdge> getStatementsPropertyEdge() {
    return this.statements;
  }

  public void setStatements(@NonNull List<Statement> statements) {
    this.statements = PropertyEdge.transformIntoPropertyEdgeList(statements, this, true);
  }

  @NonNull
  public List<PropertyEdge> getStatementEdges() {
    return this.statements;
  }

  public void addStatement(Statement s) {
    PropertyEdge propertyEdge = new PropertyEdge(this, s);
    propertyEdge.addProperty(Properties.INDEX, this.statements.size());
    this.statements.add(propertyEdge);
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
        && Objects.equals(statements, that.statements)
        && Objects.equals(this.getStatements(), that.getStatements());
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
