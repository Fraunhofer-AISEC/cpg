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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
  private final List<Declaration> declarations = new ArrayList<>();

  /** The list of statements. */
  @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
  @NotNull
  private @SubGraph("AST") List<PropertyEdge<Statement>> statementEdges = new ArrayList<>();

  public List<FieldDeclaration> getFields() {
    return declarations.stream()
        .filter(FieldDeclaration.class::isInstance)
        .map(FieldDeclaration.class::cast)
        .collect(Collectors.toList());
  }

  public List<FunctionDeclaration> getFunctions() {
    return declarations.stream()
        .filter(FunctionDeclaration.class::isInstance)
        .map(FunctionDeclaration.class::cast)
        .collect(Collectors.toList());
  }

  public List<RecordDeclaration> getRecords() {
    return declarations.stream()
        .filter(RecordDeclaration.class::isInstance)
        .map(RecordDeclaration.class::cast)
        .collect(Collectors.toList());
  }

  public List<NamespaceDeclaration> getNamespaces() {
    return declarations.stream()
        .filter(NamespaceDeclaration.class::isInstance)
        .map(NamespaceDeclaration.class::cast)
        .collect(Collectors.toList());
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
  @NotNull
  public <T extends Declaration> Set<T> getDeclarationsByName(
      @NotNull String name, @NotNull Class<T> clazz) {
    return getDeclarations().stream()
        .filter(declaration -> clazz.isAssignableFrom(declaration.getClass()))
        .map(clazz::cast)
        .filter(declaration -> Objects.equals(declaration.getName().toString(), name))
        .collect(Collectors.toSet());
  }

  public <T> T getDeclarationAs(int i, Class<T> clazz) {
    return clazz.cast(getDeclarations().get(i));
  }

  @Override
  public @NotNull List<PropertyEdge<Statement>> getStatementEdges() {
    return this.statementEdges;
  }

  @Override
  public void setStatementEdges(@NotNull List<PropertyEdge<Statement>> statements) {
    this.statementEdges = statements;
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
  public void addDeclaration(@NotNull Declaration declaration) {
    addIfNotContains(this.declarations, declaration);
  }

  @NotNull
  @Override
  public List<Statement> getStatements() {
    return StatementHolder.DefaultImpls.getStatements(this);
  }

  @Override
  public void setStatements(@NotNull List<? extends Statement> value) {
    StatementHolder.DefaultImpls.setStatements(this, value);
  }

  @Override
  public void addStatement(@NotNull Statement s) {
    StatementHolder.DefaultImpls.addStatement(this, s);
  }
}
