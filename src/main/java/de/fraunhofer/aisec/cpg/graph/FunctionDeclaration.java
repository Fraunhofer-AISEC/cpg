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
import de.fraunhofer.aisec.cpg.graph.type.UnknownType;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents the declaration or definition of a function. */
public class FunctionDeclaration extends ValueDeclaration {

  static final String VOID_TYPE_STRING = "void";
  private static final String INT_TYPE_STRING = "int";
  private static final String WHITESPACE = " ";
  private static final String BRACKET_LEFT = "(";
  private static final String COMMA = ",";
  private static final String BRACKET_RIGHT = ")";

  /** The function body. Usually a {@link CompoundStatement}. */
  @SubGraph("AST")
  protected Statement body;

  /** The list of function parameters. */
  @SubGraph("AST")
  protected List<ParamVariableDeclaration> parameters = new ArrayList<>();

  protected List<Type> throwsTypes = new ArrayList<>();

  @org.neo4j.ogm.annotation.Relationship(value = "OVERRIDES", direction = "INCOMING")
  private List<FunctionDeclaration> overriddenBy = new ArrayList<>();

  @org.neo4j.ogm.annotation.Relationship(value = "OVERRIDES", direction = "OUTGOING")
  private List<FunctionDeclaration> overrides = new ArrayList<>();

  public boolean hasBody() {
    return this.body != null;
  }

  public String getSignature() {
    return this.name
        + BRACKET_LEFT
        + this.parameters.stream()
            .map(x -> x.getType().getTypeName())
            .collect(Collectors.joining(COMMA + WHITESPACE))
        + BRACKET_RIGHT
        + Objects.requireNonNullElse(this.type, UnknownType.getUnknownType()).getTypeName();
  }

  public boolean hasSignature(List<Type> targetSignature) {
    List<ParamVariableDeclaration> signature =
        parameters.stream()
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
    return other.getName().equals(name)
        && other.getType().equals(type)
        && other.getSignature().equals(getSignature());
  }

  public List<FunctionDeclaration> getOverriddenBy() {
    return overriddenBy;
  }

  public List<FunctionDeclaration> getOverrides() {
    return overrides;
  }

  public List<Type> getThrowsTypes() {
    return throwsTypes;
  }

  public void setThrowsTypes(List<Type> throwsTypes) {
    this.throwsTypes = throwsTypes;
  }

  public Statement getBody() {
    return body;
  }

  @Nullable
  public <T> T getBodyStatementAs(int i, Class<T> clazz) {
    if (this.body instanceof CompoundStatement) {
      return clazz.cast(((CompoundStatement) this.body).getStatements().get(i));
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
    return parameters;
  }

  public void setParameters(List<ParamVariableDeclaration> parameters) {
    this.parameters = parameters;
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
        .append("body", body)
        .append("type", type)
        .append("parameters", parameters)
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
        && Objects.equals(parameters, that.parameters)
        && Objects.equals(throwsTypes, that.throwsTypes)
        && Objects.equals(overriddenBy, that.overriddenBy)
        && Objects.equals(overrides, that.overrides);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
