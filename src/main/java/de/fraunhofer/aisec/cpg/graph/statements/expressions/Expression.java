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

package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType;
import de.fraunhofer.aisec.cpg.graph.types.ReferenceType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Transient;

/**
 * Represents one expression. It is used as a base class for multiple different types of
 * expressions. The only constraint is, that each expression has a type.
 *
 * <p>Note: In our graph, {@link Expression} is inherited from {@link Statement}. This is a
 * constraint of the C++ language. In C++, it is valid to have an expression (for example a {@link
 * Literal}) as part of a function body, even though the expression value is not used. Consider the
 * following code: <code>
 *   int main() {
 *     1;
 *   }
 * </code>
 *
 * <p>This is not possible in Java, the aforementioned code example would prompt a compile error.
 */
public class Expression extends Statement implements HasType {

  /** The type of the value after evaluation. */
  protected Type type = UnknownType.getUnknownType();

  @Transient private Set<TypeListener> typeListeners = new HashSet<>();

  private Set<Type> possibleSubTypes = new HashSet<>();

  @Override
  public Type getType() {
    // just to make sure that we REALLY always return a valid type in case this somehow gets set to
    // null
    return type != null ? type : UnknownType.getUnknownType();
  }

  @Override
  public Type getPropagationType() {
    if (this.type instanceof ReferenceType) {
      return ((ReferenceType) this.type).getElementType();
    }
    return getType();
  }

  @Override
  public void updateType(Type type) {
    this.type = type;
  }

  @Override
  public void updatePossibleSubtypes(Set<Type> types) {
    this.possibleSubTypes = types;
  }

  @Override
  public void setType(Type type, HasType root) {
    // TODO Document this method. It is called very often (potentially for each AST node) and
    // performs less than optimal.
    if (type == null || root == this) {
      return;
    }

    if (this.type instanceof FunctionPointerType && !(type instanceof FunctionPointerType)) {
      return;
    }

    Type oldType = this.type;

    if (TypeManager.getInstance().isUnknown(type)) {
      return;
    }

    Set<Type> subTypes = new HashSet<>();

    for (Type t : getPossibleSubTypes()) {
      if (!t.isSimilar(type)) {
        subTypes.add(t);
      }
    }

    subTypes.add(type);

    this.type =
        TypeManager.getInstance()
            .registerType(TypeManager.getInstance().getCommonType(subTypes).orElse(type));

    subTypes =
        subTypes.stream()
            .filter(s -> TypeManager.getInstance().isSupertypeOf(this.type, s))
            .collect(Collectors.toSet());

    subTypes =
        subTypes.stream()
            .map(s -> TypeManager.getInstance().registerType(s))
            .collect(Collectors.toSet());

    setPossibleSubTypes(subTypes);

    if (!Objects.equals(oldType, type)) {
      this.typeListeners.stream()
          .filter(l -> !l.equals(this))
          .forEach(l -> l.typeChanged(this, root == null ? this : root, oldType));
    }
  }

  @Override
  public Set<Type> getPossibleSubTypes() {
    return possibleSubTypes;
  }

  @Override
  public void setPossibleSubTypes(Set<Type> possibleSubTypes, HasType root) {
    if (root == this) {
      return;
    }

    if (possibleSubTypes.stream().allMatch(TypeManager.getInstance()::isPrimitive)
        && !this.possibleSubTypes.isEmpty()) {
      return;
    }
    Set<Type> oldSubTypes = this.possibleSubTypes;
    this.possibleSubTypes = new HashSet<>(possibleSubTypes);

    if (!this.getPossibleSubTypes().equals(oldSubTypes)) {
      this.typeListeners.stream()
          .filter(l -> !l.equals(this))
          .forEach(l -> l.possibleSubTypesChanged(this, root == null ? this : root, oldSubTypes));
    }
  }

  @Override
  public void resetTypes(Type type) {

    Set<Type> oldSubTypes = new HashSet<>(getPossibleSubTypes());

    Type oldType = this.type;

    this.type = type;
    possibleSubTypes = new HashSet<>();

    if (!Objects.equals(oldType, type)) {
      this.typeListeners.stream()
          .filter(l -> !l.equals(this))
          .forEach(l -> l.typeChanged(this, this, oldType));
    }
    if (oldSubTypes.size() != 1 || !oldSubTypes.contains(type))
      this.typeListeners.stream()
          .filter(l -> !l.equals(this))
          .forEach(l -> l.possibleSubTypesChanged(this, this, oldSubTypes));
  }

  @Override
  public void registerTypeListener(TypeListener listener) {
    this.typeListeners.add(listener);
    listener.typeChanged(this, this, this.type);
    listener.possibleSubTypesChanged(this, this, this.possibleSubTypes);
  }

  @Override
  public void unregisterTypeListener(TypeListener listener) {
    this.typeListeners.remove(listener);
  }

  @Override
  public Set<TypeListener> getTypeListeners() {
    return typeListeners;
  }

  @Override
  public void refreshType() {
    this.typeListeners.forEach(
        l -> {
          l.typeChanged(this, this, type);
          l.possibleSubTypesChanged(this, this, possibleSubTypes);
        });
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("type", type)
        .append("possibleSubTypes", possibleSubTypes)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Expression)) {
      return false;
    }
    Expression that = (Expression) o;
    return super.equals(that)
        && Objects.equals(type, that.type)
        && Objects.equals(possibleSubTypes, that.possibleSubTypes);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
