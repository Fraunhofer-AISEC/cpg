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
package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This interface denotes an AST-Node that can contain code. This code is stored as statements. This
 * includes Translation units namespaces and classes as some languages, mainly scripting languages
 * allow code placement outside of explicit functions.
 *
 * <p>The reason for not only using a statement property that encapsulates all code in an implicit
 * compound statements is that code can be distributed between functions and an encapsulating
 * compound statement would imply a block of code with a code region containing only the statements.
 */
public interface StatementHolder {

  /**
   * Getter to be implemented by implementing classes to gain read access to the classes member.
   *
   * @return List of property Edge statements
   */
  @NonNull
  List<PropertyEdge<Statement>> getStatementEdges();

  void setStatementEdges(@NonNull List<PropertyEdge<Statement>> statements);

  /**
   * Returns the list of contained statements.
   *
   * @return contained statements
   */
  @NonNull
  default List<Statement> getStatements() {
    return PropertyEdge.unwrap(getStatementEdges(), true);
  }

  @NonNull
  default List<PropertyEdge<Statement>> getStatementsPropertyEdge() {
    return getStatementEdges();
  }

  default void setStatements(@NonNull List<Statement> statements) {
    setStatementEdges(PropertyEdge.transformIntoOutgoingPropertyEdgeList(statements, (Node) this));
  }

  /**
   * Adds the specified statement to this statement holder. The statements have to be stored as a
   * list of statements as we try to avoid adding new AST-nodes that do not exist, e.g. a code body
   * to hold statements
   *
   * @param s the statement
   */
  default void addStatement(Statement s) {
    PropertyEdge<Statement> propertyEdge = new PropertyEdge<>((Node) this, s);
    propertyEdge.addProperty(Properties.INDEX, getStatementEdges().size());
    getStatementEdges().add(propertyEdge);
  }
}
