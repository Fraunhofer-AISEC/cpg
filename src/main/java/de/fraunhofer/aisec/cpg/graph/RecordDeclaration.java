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

import de.fraunhofer.aisec.cpg.graph.type.Type;
import java.util.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Represents a C++ union/struct/class or Java class */
public class RecordDeclaration extends Declaration {

  /** The kind, i.e. struct, class, union or enum. */
  private String kind;

  @SubGraph("AST")
  private List<FieldDeclaration> fields = new ArrayList<>();

  @SubGraph("AST")
  private List<MethodDeclaration> methods = new ArrayList<>();

  @SubGraph("AST")
  private List<ConstructorDeclaration> constructors = new ArrayList<>();

  @SubGraph("AST")
  private List<RecordDeclaration> records = new ArrayList<>();

  private List<Type> superTypes = new ArrayList<>();

  @org.neo4j.ogm.annotation.Relationship
  private Set<RecordDeclaration> superTypeDeclarations = new HashSet<>();

  private List<String> importStatements = new ArrayList<>();
  @org.neo4j.ogm.annotation.Relationship private Set<Declaration> imports = new HashSet<>();
  // Methods and fields can be imported statically
  private List<String> staticImportStatements = new ArrayList<>();

  @org.neo4j.ogm.annotation.Relationship
  private Set<ValueDeclaration> staticImports = new HashSet<>();

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public List<FieldDeclaration> getFields() {
    return fields;
  }

  public void setFields(List<FieldDeclaration> fields) {
    this.fields = fields;
  }

  public FieldDeclaration getThis() {
    return fields.stream().filter(f -> f.getName().equals("this")).findFirst().orElse(null);
  }

  public List<MethodDeclaration> getMethods() {
    return methods;
  }

  public void setMethods(List<MethodDeclaration> methods) {
    this.methods = methods;
  }

  public List<ConstructorDeclaration> getConstructors() {
    return constructors;
  }

  public void setConstructors(List<ConstructorDeclaration> constructors) {
    this.constructors = constructors;
  }

  public List<RecordDeclaration> getRecords() {
    return records;
  }

  public void setRecords(List<RecordDeclaration> records) {
    this.records = records;
  }

  public List<Type> getSuperTypes() {
    return superTypes;
  }

  public void setSuperTypes(List<Type> superTypes) {
    this.superTypes = superTypes;
  }

  public Set<RecordDeclaration> getSuperTypeDeclarations() {
    return superTypeDeclarations;
  }

  public void setSuperTypeDeclarations(Set<RecordDeclaration> superTypeDeclarations) {
    this.superTypeDeclarations = superTypeDeclarations;
  }

  public Set<Declaration> getImports() {
    return imports;
  }

  public void setImports(Set<Declaration> imports) {
    this.imports = imports;
  }

  public Set<ValueDeclaration> getStaticImports() {
    return staticImports;
  }

  public void setStaticImports(Set<ValueDeclaration> staticImports) {
    this.staticImports = staticImports;
  }

  public List<String> getImportStatements() {
    return importStatements;
  }

  public void setImportStatements(List<String> importStatements) {
    this.importStatements = importStatements;
  }

  public List<String> getStaticImportStatements() {
    return staticImportStatements;
  }

  public void setStaticImportStatements(List<String> staticImportStatements) {
    this.staticImportStatements = staticImportStatements;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("name", name)
        .append("kind", kind)
        .append("superTypeDeclarations", superTypeDeclarations)
        .append("fields", fields)
        .append("methods", methods)
        .append("constructors", constructors)
        .append("records", records)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RecordDeclaration)) {
      return false;
    }
    RecordDeclaration that = (RecordDeclaration) o;
    return super.equals(that)
        && Objects.equals(kind, that.kind)
        && Objects.equals(fields, that.fields)
        && Objects.equals(methods, that.methods)
        && Objects.equals(constructors, that.constructors)
        && Objects.equals(records, that.records)
        && Objects.equals(superTypes, that.superTypes)
        && Objects.equals(superTypeDeclarations, that.superTypeDeclarations);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
