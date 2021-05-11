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

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.edge.AstPropertyEdge;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.neo4j.ogm.annotation.Relationship;

/** Represents the declaration or definition of a function. */
public class FunctionDeclaration extends ValueDeclaration implements DeclarationHolder {

  private static final String WHITESPACE = " ";
  private static final String BRACKET_LEFT = "(";
  private static final String COMMA = ",";
  private static final String BRACKET_RIGHT = ")";

  /** The function body. Usually a {@link CompoundStatement}. */
  @SubGraph("AST")
  protected Statement body;

  /**
   * Classes and Structs can be declared inside a function and are only valid within the function.
   */
  @Relationship(value = "RECORDS", direction = "OUTGOING")
  protected List<PropertyEdge<RecordDeclaration>> records = new ArrayList<>();

  /** The list of function parameters. */
  @Relationship(value = "PARAMETERS", direction = "OUTGOING")
  @SubGraph("AST")
  protected List<AstPropertyEdge<ParamVariableDeclaration>> parameters = new ArrayList<>();

  @Relationship(value = "THROWS_TYPES", direction = "OUTGOING")
  protected List<PropertyEdge<Type>> throwsTypes = new ArrayList<>();

  @org.neo4j.ogm.annotation.Relationship(value = "OVERRIDES", direction = "INCOMING")
  private final List<PropertyEdge<FunctionDeclaration>> overriddenBy = new ArrayList<>();

  @org.neo4j.ogm.annotation.Relationship(value = "OVERRIDES", direction = "OUTGOING")
  private final List<PropertyEdge<FunctionDeclaration>> overrides = new ArrayList<>();

  /**
   * Specifies, whether this function declaration is also a definition, i.e. has a function body
   * definition.
   */
  private boolean isDefinition;

  /** If this is only a declaration, this provides a link to the definition of the function. */
  @Relationship(value = "DEFINES")
  private FunctionDeclaration definition;

  public boolean hasBody() {
    return this.body != null;
  }

  public String getSignature() {
    return this.getName()
        + BRACKET_LEFT
        + this.parameters.stream()
            .map(PropertyEdge::getEnd)
            .map(x -> x.getType().getTypeName())
            .collect(Collectors.joining(COMMA + WHITESPACE))
        + BRACKET_RIGHT
        + Objects.requireNonNullElse(this.type, UnknownType.getUnknownType()).getTypeName();
  }

  public boolean hasSameSignature(FunctionDeclaration targetFunctionDeclaration) {
    return targetFunctionDeclaration.getName().equals(this.getName())
        && targetFunctionDeclaration.getSignatureTypes().equals(this.getSignatureTypes());
  }

  public boolean hasSignature(List<Type> targetSignature) {
    List<ParamVariableDeclaration> signature =
        parameters.stream()
            .map(PropertyEdge::getEnd)
            .sorted(Comparator.comparingInt(ParamVariableDeclaration::getArgumentIndex))
            .collect(Collectors.toList());
    if (targetSignature.size() < signature.size()) {
      return false;
    } else {
      // signature is a collection of positional arguments, so the order must be preserved
      for (int i = 0; i < signature.size(); i++) {
        ParamVariableDeclaration declared = signature.get(i);
        if (declared.isVariadic() && targetSignature.size() >= signature.size()) {
          // Everything that follows is collected by this param, so the signature is fulfilled no
          // matter what comes now (potential FIXME: in Java, we could have overloading with
          // different vararg types, in C++ we can't, as vararg types are not defined here anyways)
          return true;
        }

        Type provided = targetSignature.get(i);
        if (!TypeManager.getInstance().isSupertypeOf(declared.getType(), provided)) {
          return false;
        }
      }
      // Longer target signatures are only allowed with varargs. If we reach this point, no
      // vararg has been encountered
      return targetSignature.size() == signature.size();
    }
  }

  public boolean isOverrideCandidate(FunctionDeclaration other) {
    return other.getName().equals(getName())
        && other.getType().equals(type)
        && other.getSignature().equals(getSignature());
  }

  public List<FunctionDeclaration> getOverriddenBy() {
    return unwrap(this.overriddenBy, false);
  }

  public List<PropertyEdge<FunctionDeclaration>> getOverriddenByPropertyEdge() {
    return this.overriddenBy;
  }

  public void addOverriddenBy(Collection<? extends FunctionDeclaration> c) {
    for (FunctionDeclaration functionDeclaration : c) {
      addOverriddenBy(functionDeclaration);
    }
  }

  public void addOverriddenBy(FunctionDeclaration functionDeclaration) {
    addIfNotContains(this.overriddenBy, functionDeclaration, false);
  }

  public List<FunctionDeclaration> getOverrides() {
    return unwrap(this.overrides);
  }

  public List<PropertyEdge<FunctionDeclaration>> getOverridesPropertyEdge() {
    return this.overrides;
  }

  public void addOverrides(FunctionDeclaration functionDeclaration) {
    addIfNotContains(this.overrides, functionDeclaration);
  }

  public List<Type> getThrowsTypes() {
    return unwrap(this.throwsTypes);
  }

  public List<PropertyEdge<Type>> getThrowsTypesPropertyEdge() {
    return this.throwsTypes;
  }

  public void setThrowsTypes(List<Type> throwsTypes) {
    this.throwsTypes = PropertyEdge.transformIntoOutgoingPropertyEdgeList(throwsTypes, this);
  }

  public void addThrowTypes(Type type) {
    PropertyEdge<Type> propertyEdge = new PropertyEdge<>(this, type);
    propertyEdge.addProperty(Properties.INDEX, this.throwsTypes.size());
    this.throwsTypes.add(propertyEdge);
  }

  public void addThrowTypes(Collection<Type> collection) {
    for (Type type : collection) {
      addThrowTypes(type);
    }
  }

  public Statement getBody() {
    return body;
  }

  @Nullable
  public <T> T getBodyStatementAs(int i, Class<T> clazz) {
    if (this.body instanceof CompoundStatement) {
      Statement statement = ((CompoundStatement) this.body).getStatements().get(i);

      if (statement == null) {
        return null;
      }

      return statement.getClass().isAssignableFrom(clazz) ? clazz.cast(statement) : null;
    }

    return null;
  }

  public void setBody(Statement body) {
    if (this.body instanceof ReturnStatement) {
      this.removePrevDFG(this.body);
    } else if (this.body instanceof CompoundStatement) {
      ((CompoundStatement) this.body)
          .getStatements().stream()
              .filter(ReturnStatement.class::isInstance)
              .forEach(this::removePrevDFG);
    }

    this.body = body;

    if (body instanceof ReturnStatement) {
      this.addPrevDFG(body);
    } else if (body instanceof CompoundStatement) {
      ((CompoundStatement) body)
          .getStatements().stream()
              .filter(ReturnStatement.class::isInstance)
              .forEach(this::addPrevDFG);
    }
  }

  public List<ParamVariableDeclaration> getParameters() {
    return unwrap(this.parameters);
  }

  public List<Expression> getDefaultParameters() {
    List<Expression> defaultParameters = new ArrayList<>();
    for (ParamVariableDeclaration param : getParameters()) {
      defaultParameters.add(param.getDefault());
    }
    return defaultParameters;
  }

  public List<Type> getDefaultParameterSignature() {
    List<Type> signature = new ArrayList<>();
    for (ParamVariableDeclaration paramVariableDeclaration : getParameters()) {
      if (paramVariableDeclaration.getDefault() != null) {
        signature.add(paramVariableDeclaration.getType());
      } else {
        signature.add(UnknownType.getUnknownType());
      }
    }
    return signature;
  }

  public List<Type> getSignatureTypes() {
    List<Type> signatureTypes = new ArrayList<>();
    for (ParamVariableDeclaration paramVariableDeclaration : unwrap(this.parameters)) {
      signatureTypes.add(paramVariableDeclaration.getType());
    }
    return signatureTypes;
  }

  public List<AstPropertyEdge<ParamVariableDeclaration>> getParametersPropertyEdge() {
    return this.parameters;
  }

  public void addParameter(ParamVariableDeclaration paramVariableDeclaration) {
    var propertyEdge = new AstPropertyEdge<>(this, paramVariableDeclaration);
    propertyEdge.addProperty(Properties.INDEX, this.parameters.size());
    this.parameters.add(propertyEdge);
  }

  public void removeParameter(ParamVariableDeclaration paramVariableDeclaration) {
    this.parameters.removeIf(
        propertyEdge -> propertyEdge.getEnd().equals(paramVariableDeclaration));
  }

  public void setParameters(List<ParamVariableDeclaration> parameters) {
    this.parameters =
        PropertyEdge
            .<ParamVariableDeclaration, AstPropertyEdge<ParamVariableDeclaration>>
                transformIntoOutgoingPropertyEdgeList(parameters, this, AstPropertyEdge.class);
  }

  /**
   * Looks for a variable declaration by the given name.
   *
   * @param name the name of the variable
   * @return an optional value containing the variable declaration if found
   */
  public Optional<VariableDeclaration> getVariableDeclarationByName(String name) {
    if (body instanceof CompoundStatement) {
      return ((CompoundStatement) body)
          .getStatements().stream()
              // only declaration statements
              .filter(statement -> statement instanceof DeclarationStatement)
              // cast them
              .map(DeclarationStatement.class::cast)
              // flatten the declarations (could be more than one) of the statements so we can
              // search them
              .flatMap(declarationStatement -> declarationStatement.getDeclarations().stream())
              // only variable declarations
              .filter(declaration -> declaration instanceof VariableDeclaration)
              // cast them
              .map(VariableDeclaration.class::cast)
              // filter them by name
              .filter(declaration -> Objects.equals(declaration.getName(), name))
              // return the first (later ones should not exist anyway)
              .findFirst();
    }

    return Optional.empty();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("type", type)
        .append(
            "parameters",
            parameters.stream()
                .map(PropertyEdge::getEnd)
                .map(ParamVariableDeclaration::getName)
                .collect(Collectors.joining(", ")))
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FunctionDeclaration)) {
      return false;
    }
    FunctionDeclaration that = (FunctionDeclaration) o;
    return super.equals(that)
        && Objects.equals(body, that.body)
        && Objects.equals(this.getParameters(), that.getParameters())
        && PropertyEdge.propertyEqualsList(parameters, that.parameters)
        && Objects.equals(this.getThrowsTypes(), that.getThrowsTypes())
        && PropertyEdge.propertyEqualsList(throwsTypes, that.throwsTypes)
        && Objects.equals(this.getOverriddenBy(), that.getOverriddenBy())
        && PropertyEdge.propertyEqualsList(overriddenBy, that.overriddenBy)
        && Objects.equals(this.getOverrides(), that.getOverrides())
        && PropertyEdge.propertyEqualsList(overrides, that.overrides);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public FunctionDeclaration getDefinition() {
    return isDefinition ? this : definition;
  }

  public boolean isDefinition() {
    return isDefinition;
  }

  public void setIsDefinition(boolean definition) {
    this.isDefinition = definition;
  }

  public void setDefinition(FunctionDeclaration definition) {
    this.definition = definition;
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

  @Override
  public void addDeclaration(@NonNull Declaration declaration) {
    if (declaration instanceof ParamVariableDeclaration) {
      // TODO: make addIfNotContains support the subclass
      
      // addIfNotContains(parameters, (ParamVariableDeclaration) declaration);
      var edge = new AstPropertyEdge<>(this, (ParamVariableDeclaration) declaration);

      // set the index property
      edge.addProperty(Properties.INDEX, parameters.size());

      boolean contains = false;
      for (var element : parameters) {
        if (element.getEnd().equals(edge.getEnd())) {
          contains = true;
          break;
        }
      }
      if (!contains) {
        parameters.add(edge);
      }
    }

    if (declaration instanceof RecordDeclaration) {
      addIfNotContains(records, (RecordDeclaration) declaration);
    }
  }
}
