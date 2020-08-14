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

package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.helpers.NodeComparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Declares the scope of a namespace and appends its own name to the current namespace-ürefix to
 * form a new namespace prefix. While RecordDeclarations in C++ and Java have their own namespace,
 * namespace declarations can be declared multiple times. At the beginning of a Java-file, a
 * namespace declaration is used to represent the package name as namespace. In its explicit
 * appearance a namespace declaration can contain {@link FunctionDeclaration}, {@link
 * FieldDeclaration} and {@link RecordDeclaration} similar to a {@link RecordDeclaration} and the
 * semantical difference between NamespaceDeclaration and {@link RecordDeclaration} lies in the
 * non-instantiabillity of a namespace.
 */
public class NamespaceDeclaration extends Declaration {

  /** Edges to a {@link FieldDeclaration} defined in a namespace, these fields are static. */
  @SubGraph("AST")
  private List<FieldDeclaration> fields = new ArrayList<>();

  /** Edge to {@link FunctionDeclaration}s defined in a namespace, these functions are static. */
  @SubGraph("AST")
  private List<FunctionDeclaration> functions = new ArrayList<>();

  /** Edges to {@link RecordDeclaration}s defined in a namespace. */
  @SubGraph("AST")
  private List<RecordDeclaration> records = new ArrayList<>();

  /** Edges to nested namespaces contained in the current namespace. */
  @SubGraph("AST")
  private List<NamespaceDeclaration> namespaces = new ArrayList<>();

  public List<FieldDeclaration> getFields() {
    return fields;
  }

  public void setFields(List<FieldDeclaration> fields) {
    this.fields = fields;
  }

  public List<FunctionDeclaration> getFunctions() {
    return functions;
  }

  public void setFunctions(List<FunctionDeclaration> functions) {
    this.functions = functions;
  }

  public List<RecordDeclaration> getRecords() {
    return records;
  }

  public void setRecords(List<RecordDeclaration> records) {
    this.records = records;
  }

  public List<NamespaceDeclaration> getNamespaces() {
    return namespaces;
  }

  public void setNamespaces(List<NamespaceDeclaration> namespaces) {
    this.namespaces = namespaces;
  }

  public List<Declaration> getDeclarations() {
    ArrayList<Declaration> ret = new ArrayList<>();
    ret.addAll(getFields());
    ret.addAll(getRecords());
    ret.addAll(getFunctions());
    ret.addAll(getNamespaces());
    ret.sort(new NodeComparator());
    return ret;
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
}
