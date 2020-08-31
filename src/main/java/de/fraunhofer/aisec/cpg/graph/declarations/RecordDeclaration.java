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

package de.fraunhofer.aisec.cpg.graph.declarations;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Transient;

/** Represents a C++ union/struct/class or Java class */
public class RecordDeclaration extends Declaration
    implements DeclarationHolder, HasDefinition<RecordDeclaration> {

  /** The kind, i.e. struct, class, union or enum. */
  private String kind;

  @Relationship(value = "FIELDS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<FieldDeclaration>> fields = new ArrayList<>();

  @Relationship(value = "METHODS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<MethodDeclaration>> methods = new ArrayList<>();

  @Relationship(value = "CONSTRUCTORS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<ConstructorDeclaration>> constructors = new ArrayList<>();

  @Relationship(value = "RECORDS", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<RecordDeclaration>> records = new ArrayList<>();

  @Transient private List<Type> superClasses = new ArrayList<>();
  @Transient private List<Type> implementedInterfaces = new ArrayList<>();

  @org.neo4j.ogm.annotation.Relationship
  private Set<RecordDeclaration> superTypeDeclarations = new HashSet<>();

  private List<String> importStatements = new ArrayList<>();
  @org.neo4j.ogm.annotation.Relationship private Set<Declaration> imports = new HashSet<>();
  // Methods and fields can be imported statically
  private List<String> staticImportStatements = new ArrayList<>();

  @org.neo4j.ogm.annotation.Relationship
  private Set<ValueDeclaration> staticImports = new HashSet<>();

  private final DeclarationChain<RecordDeclaration> chain = new DeclarationChain<>();

  /** If this is only a declaration, this provides a link to the definition of the function. */
  @Relationship(value = "DEFINES")
  private RecordDeclaration definition;

  /**
   * Specifies, whether this function declaration is also a definition, i.e. has a function body
   * definition.
   */
  private boolean isDefinition;

  @Override
  public void setName(@NonNull String name) {
    // special case for record declarations! Constructor names need to match
    super.setName(name);
    for (PropertyEdge<ConstructorDeclaration> constructorEdge : constructors) {
      constructorEdge.getEnd().setName(name);
    }
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public List<FieldDeclaration> getFields() {
    return unwrap(this.fields);
  }

  public List<PropertyEdge<FieldDeclaration>> getFieldsPropertyEdge() {
    return this.fields;
  }

  public void addField(FieldDeclaration fieldDeclaration) {
    addIfNotContains(this.fields, fieldDeclaration);
  }

  public void removeField(FieldDeclaration fieldDeclaration) {
    this.fields.removeIf(propertyEdge -> propertyEdge.getEnd().equals(fieldDeclaration));
  }

  @Nullable
  public FieldDeclaration getField(String name) {
    return fields.stream()
        .map(PropertyEdge::getEnd)
        .filter(f -> f.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  public void setFields(List<FieldDeclaration> fields) {
    this.fields = PropertyEdge.transformIntoOutgoingPropertyEdgeList(fields, this);
  }

  public FieldDeclaration getThis() {
    return fields.stream()
        .map(PropertyEdge::getEnd)
        .filter(f -> f.getName().equals("this"))
        .findFirst()
        .orElse(null);
  }

  public List<MethodDeclaration> getMethods() {
    return unwrap(this.methods);
  }

  public List<PropertyEdge<MethodDeclaration>> getMethodsPropertyEdge() {
    return this.methods;
  }

  public void addMethod(MethodDeclaration methodDeclaration) {
    addIfNotContains(this.methods, methodDeclaration);
  }

  public void removeMethod(MethodDeclaration methodDeclaration) {
    this.methods.removeIf(propertyEdge -> propertyEdge.getEnd().equals(methodDeclaration));
  }

  public void setMethods(List<MethodDeclaration> methods) {
    this.methods = PropertyEdge.transformIntoOutgoingPropertyEdgeList(methods, this);
  }

  public List<ConstructorDeclaration> getConstructors() {
    return unwrap(this.constructors);
  }

  public List<PropertyEdge<ConstructorDeclaration>> getConstructorsPropertyEdge() {
    return this.constructors;
  }

  public void setConstructors(List<ConstructorDeclaration> constructors) {
    this.constructors = PropertyEdge.transformIntoOutgoingPropertyEdgeList(constructors, this);
  }

  public void addConstructor(ConstructorDeclaration constructorDeclaration) {
    addIfNotContains(this.constructors, constructorDeclaration);
  }

  public void removeConstructor(ConstructorDeclaration constructorDeclaration) {
    this.constructors.removeIf(
        propertyEdge -> propertyEdge.getEnd().equals(constructorDeclaration));
  }

  public List<RecordDeclaration> getRecords() {
    return unwrap(this.records);
  }

  public List<PropertyEdge<RecordDeclaration>> getRecordsPropertyEdge() {
    return this.records;
  }

  public void setRecords(List<RecordDeclaration> records) {
    this.records = PropertyEdge.transformIntoOutgoingPropertyEdgeList(records, this);
  }

  public void removeRecord(RecordDeclaration recordDeclaration) {
    this.records.removeIf(propertyEdge -> propertyEdge.getEnd().equals(recordDeclaration));
  }

  /**
   * Combines both implemented interfaces and extended classes. This is most commonly what you are
   * looking for when looking for method call targets etc.
   *
   * @return concatenation of {@link #getSuperClasses()} and {@link #getImplementedInterfaces()}
   */
  public List<Type> getSuperTypes() {
    return Stream.of(superClasses, implementedInterfaces)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  /**
   * The classes that are extended by this one. Usually zero or one, but in C++ this can contain
   * multiple classes
   *
   * @return extended classes
   */
  public List<Type> getSuperClasses() {
    return superClasses;
  }

  public void setSuperClasses(List<Type> superClasses) {
    this.superClasses = superClasses;
  }

  /**
   * Interfaces implemented by this class. This concept is not present in C++
   *
   * @return the list of implemented interfaces
   */
  public List<Type> getImplementedInterfaces() {
    return implementedInterfaces;
  }

  public void setImplementedInterfaces(List<Type> implementedInterfaces) {
    this.implementedInterfaces = implementedInterfaces;
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
        && Objects.equals(this.getFields(), that.getFields())
        && Objects.equals(methods, that.methods)
        && Objects.equals(this.getMethods(), that.getMethods())
        && Objects.equals(constructors, that.constructors)
        && Objects.equals(this.getConstructors(), that.getConstructors())
        && Objects.equals(records, that.records)
        && Objects.equals(this.getRecords(), that.getRecords())
        && Objects.equals(superClasses, that.superClasses)
        && Objects.equals(implementedInterfaces, that.implementedInterfaces)
        && Objects.equals(superTypeDeclarations, that.superTypeDeclarations);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof ConstructorDeclaration) {
      addIfNotContains(this.constructors, (ConstructorDeclaration) declaration);
    } else if (declaration instanceof MethodDeclaration) {
      addIfNotContains(this.methods, (MethodDeclaration) declaration);
    } else if (declaration instanceof FieldDeclaration) {
      addIfNotContains(this.fields, (FieldDeclaration) declaration);
    } else if (declaration instanceof RecordDeclaration) {
      addIfNotContains(this.records, (RecordDeclaration) declaration);
    }
  }

  public @NonNull DeclarationChain<RecordDeclaration> getChain() {
    return chain;
  }

  @Nullable
  public RecordDeclaration getDefinition() {
    return definition;
  }

  @Override
  public void setDefinition(HasDefinition<RecordDeclaration> definition) {
    this.definition = (RecordDeclaration) definition;
  }

  public boolean isDefinition() {
    return isDefinition;
  }

  @Override
  public RecordDeclaration getNextDeclaration() {
    return (RecordDeclaration) this.getPrevious();
  }

  public void setIsDefinition(boolean isDefinition) {
    this.isDefinition = isDefinition;
  }

  @Override
  public boolean isSameDeclaration(@NonNull RecordDeclaration declaration) {
    // for records, we just compare the name
    return Objects.equals(this.name, declaration.name);
  }
}
