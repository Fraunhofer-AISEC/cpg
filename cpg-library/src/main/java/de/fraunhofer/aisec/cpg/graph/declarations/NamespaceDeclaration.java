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
package de.fraunhofer.aisec.cpg.graph.declarations;

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder;
import de.fraunhofer.aisec.cpg.graph.StatementHolder;
import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.helpers.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * Declares the scope of a namespace and appends its own name to the current namespace-prefix to
 * form a new namespace prefix. While RecordDeclarations in C++ and Java have their own namespace,
 * namespace declarations can be declared multiple times. At the beginning of a Java-file, a
 * namespace declaration is used to represent the package name as namespace. In its explicit
 * appearance a namespace declaration can contain {@link FunctionDeclaration}, {@link
 * FieldDeclaration} and {@link RecordDeclaration} similar to a {@link RecordDeclaration} and the
 * semantic difference between NamespaceDeclaration and {@link RecordDeclaration} lies in the
 * non-instantiabillity of a namespace.
 *
 * <p>The name property of this node need to be a FQN for propery resolution.
 */
public class NamespaceDeclaration extends Declaration
    implements DeclarationHolder, StatementHolder {

  /**
   * Edges to nested namespaces, records, functions, fields etc. contained in the current namespace.
   */
  @SubGraph("AST")
  private List<Declaration> declarations = new ArrayList<>();

  /** The list of statements. */
  @Relationship(value = "STATEMENTS", direction = "OUTGOING")
  @NonNull
  private @SubGraph("AST") List<PropertyEdge<Statement>> statements = new ArrayList<>();

  public List<FieldDeclaration> getFields() {
    return Util.filterCast(declarations, FieldDeclaration.class);
  }

  public List<FunctionDeclaration> getFunctions() {
    return Util.filterCast(declarations, FunctionDeclaration.class);
  }

  public List<RecordDeclaration> getRecords() {
    return Util.filterCast(declarations, RecordDeclaration.class);
  }

  public List<NamespaceDeclaration> getNamespaces() {
    return Util.filterCast(declarations, NamespaceDeclaration.class);
  }

  @NotNull
  public List<Declaration> getDeclarations() {
    return declarations;
  }

  /**
   * Returns a non-null, possibly empty {@code Set} of the declaration of a specified type and
   * clazz.
   *
   * @param name the name to search for
   * @param clazz the declaration class, such as {@link FunctionDeclaration}.
   * @param <T> the type of the declaration
   * @return a {@code Set} containing the declarations, if any.
   */
  @NonNull
  public <T extends Declaration> Set<T> getDeclarationsByName(
      @NonNull String name, @NonNull Class<T> clazz) {
    return getDeclarations().stream()
        .filter(declaration -> clazz.isAssignableFrom(declaration.getClass()))
        .map(clazz::cast)
        .filter(declaration -> Objects.equals(declaration.getName(), name))
        .collect(Collectors.toSet());
  }

  public <T> T getDeclarationAs(int i, Class<T> clazz) {
    return clazz.cast(getDeclarations().get(i));
  }

  @Override
  public @NonNull List<PropertyEdge<Statement>> getStatementEdges() {
    return this.statements;
  }

  @Override
  public void setStatementEdges(@NonNull List<PropertyEdge<Statement>> statements) {
    this.statements = statements;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NamespaceDeclaration)) {
      return false;
    }
    NamespaceDeclaration that = (NamespaceDeclaration) o;
    return super.equals(that) && Objects.equals(declarations, that.declarations);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public void addDeclaration(@NonNull Declaration declaration) {
    addIfNotContains(this.declarations, declaration);
  }
}
