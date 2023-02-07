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
package de.fraunhofer.aisec.cpg.graph.types;

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.frontends.Language;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * FunctionPointerType represents FunctionPointers in CPP containing a list of parameters and a
 * return type.
 */
public class FunctionPointerType extends Type {
  @Relationship(value = "PARAMETERS", direction = Relationship.Direction.OUTGOING)
  private List<PropertyEdge<Type>> parameters;

  private Type returnType;

  public void setParameters(List<Type> parameters) {
    this.parameters = PropertyEdge.transformIntoOutgoingPropertyEdgeList(parameters, this);
  }

  public void setReturnType(Type returnType) {
    this.returnType = returnType;
  }

  private FunctionPointerType() {}

  public FunctionPointerType(
      Type.Qualifier qualifier,
      Type.Storage storage,
      List<Type> parameters,
      Type returnType,
      Language<? extends LanguageFrontend> language) {
    super("", storage, qualifier, language);
    this.parameters = PropertyEdge.transformIntoOutgoingPropertyEdgeList(parameters, this);
    this.returnType = returnType;
  }

  public FunctionPointerType(
      Type type,
      List<Type> parameters,
      Type returnType,
      Language<? extends LanguageFrontend> language) {
    super(type);
    this.parameters = PropertyEdge.transformIntoOutgoingPropertyEdgeList(parameters, this);
    this.returnType = returnType;
    this.setLanguage(language);
  }

  public List<PropertyEdge<Type>> getParametersPropertyEdge() {
    return this.parameters;
  }

  public List<Type> getParameters() {
    return unwrap(this.parameters);
  }

  public Type getReturnType() {
    return returnType;
  }

  @Override
  public PointerType reference(PointerType.PointerOrigin pointerOrigin) {
    return new PointerType(this, pointerOrigin);
  }

  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type duplicate() {
    List<Type> copiedParameters = new ArrayList<>(unwrap(this.parameters));
    return new FunctionPointerType(this, copiedParameters, this.returnType, this.getLanguage());
  }

  @Override
  public boolean isSimilar(Type t) {
    if (t instanceof FunctionPointerType) {
      if (returnType == null || ((FunctionPointerType) t).returnType == null) {
        return this.parameters.equals(((FunctionPointerType) t).parameters)
            && (this.returnType == ((FunctionPointerType) t).returnType
                || returnType == ((FunctionPointerType) t).getReturnType());
      }
      return this.parameters.equals(((FunctionPointerType) t).parameters)
          && (this.returnType == ((FunctionPointerType) t).returnType
              || this.returnType.equals(((FunctionPointerType) t).returnType));
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FunctionPointerType)) return false;
    if (!super.equals(o)) return false;
    FunctionPointerType that = (FunctionPointerType) o;
    return Objects.equals(this.getParameters(), that.getParameters())
        && PropertyEdge.propertyEqualsList(parameters, that.parameters)
        && Objects.equals(returnType, that.returnType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), parameters, returnType);
  }

  @NotNull
  @Override
  public String toString() {
    return "FunctionPointerType{"
        + "parameters="
        + getParameters()
        + ", returnType="
        + returnType
        + ", typeName='"
        + getName()
        + '\''
        + ", storage="
        + this.getStorage()
        + ", qualifier="
        + this.getQualifier()
        + ", origin="
        + this.getTypeOrigin()
        + '}';
  }
}
