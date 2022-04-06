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
package de.fraunhofer.aisec.cpg.graph.statements.expressions;

import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType;
import de.fraunhofer.aisec.cpg.graph.types.ReferenceType;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
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

  @Transient private final Set<TypeListener> typeListeners = new HashSet<>();

  private Set<Type> possibleSubTypes = new HashSet<>();

  @Override
  public Type getType() {
    Type result;
    if (TypeManager.isTypeSystemActive()) {
      // just to make sure that we REALLY always return a valid type in case this somehow gets set
      // to null
      result = type != null ? type : UnknownType.getUnknownType();
    } else {
      result =
          TypeManager.getInstance()
              .getTypeCache()
              .computeIfAbsent(this, n -> Collections.emptySet())
              .stream()
              .findAny()
              .orElse(UnknownType.getUnknownType());
    }

    return result;
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
  public void setType(Type type, Collection<HasType> root) {
    // TODO Document this method. It is called very often (potentially for each AST node) and
    // performs less than optimal.

    if (!TypeManager.isTypeSystemActive()) {
      this.type = type;
      TypeManager.getInstance().cacheType(this, type);
      return;
    }

    if (root == null) {
      root = new ArrayList<>();
    }

    // No (or only unknown) type given, loop detected? Stop early because there's nothing we can do.
    if (type == null
        || root.contains(this)
        || TypeManager.getInstance().isUnknown(type)
        || TypeManager.getInstance().stopPropagation(this.type, type)
        || (this.type instanceof FunctionPointerType && !(type instanceof FunctionPointerType))) {
      return;
    }

    Type oldType = this.type; // Backup to check if something changed

    type = type.duplicate();
    type.setQualifier(this.type.getQualifier().merge(type.getQualifier()));

    Set<Type> subTypes = new HashSet<>();

    // Check all current subtypes and consider only those which are "different enough" to type.
    for (Type t : getPossibleSubTypes()) {
      if (!t.isSimilar(type)) {
        subTypes.add(t);
      }
    }

    subTypes.add(type);

    // Probably tries to get something like the best supertype of all possible subtypes.
    this.type =
        TypeManager.getInstance()
            .registerType(TypeManager.getInstance().getCommonType(subTypes).orElse(type));

    // TODO: Why do we need this loop? Shouldn't the condition be ensured by the previous line
    // getting the common type??
    Set<Type> newSubtypes = new HashSet<>();
    for (var s : subTypes) {
      if (TypeManager.getInstance().isSupertypeOf(this.type, s)) {
        newSubtypes.add(TypeManager.getInstance().registerType(s));
      }
    }

    setPossibleSubTypes(newSubtypes);

    if (Objects.equals(oldType, type)) {
      // Nothing changed, so we do not have to notify the listeners.
      return;
    }
    root.add(this); // Add current node to the set of "triggers" to detect potential loops.
    // Notify all listeners about the changed type
    for (var l : typeListeners) {
      if (!l.equals(this)) {
        l.typeChanged(this, root, oldType);
      }
    }
  }

  @Override
  public Set<Type> getPossibleSubTypes() {
    if (!TypeManager.isTypeSystemActive()) {
      return TypeManager.getInstance().getTypeCache().getOrDefault(this, Collections.emptySet());
    }
    return possibleSubTypes;
  }

  @Override
  public void setPossibleSubTypes(Set<Type> possibleSubTypes, @NotNull Collection<HasType> root) {
    possibleSubTypes =
        possibleSubTypes.stream()
            .filter(Predicate.not(TypeManager.getInstance()::isUnknown))
            .collect(Collectors.toSet());

    if (!TypeManager.isTypeSystemActive()) {
      possibleSubTypes.forEach(t -> TypeManager.getInstance().cacheType(this, t));
      return;
    }

    // Loop detected or only primitive types (which cannot have a subtype)
    if (root.contains(this)
        || (possibleSubTypes.stream().allMatch(TypeManager.getInstance()::isPrimitive)
            && !this.possibleSubTypes.isEmpty())) {
      return;
    }

    Set<Type> oldSubTypes = this.possibleSubTypes;
    this.possibleSubTypes = possibleSubTypes;

    if (getPossibleSubTypes().equals(oldSubTypes)) {
      // Nothing changed, so we do not have to notify the listeners.
      return;
    }
    root.add(this); // Add current node to the set of "triggers" to detect potential loops.
    // Notify all listeners about the changed type
    for (var listener : typeListeners) {
      if (!listener.equals(this)) {
        listener.possibleSubTypesChanged(this, root, oldSubTypes);
      }
    }
  }

  @Override
  public void resetTypes(Type type) {

    Set<Type> oldSubTypes = new HashSet<>(getPossibleSubTypes());

    Type oldType = this.type;

    this.type = type;
    possibleSubTypes = new HashSet<>();

    List<HasType> root = new ArrayList<>(List.of(this));
    if (!Objects.equals(oldType, type)) {
      this.typeListeners.stream()
          .filter(l -> !l.equals(this))
          .forEach(l -> l.typeChanged(this, root, oldType));
    }
    if (oldSubTypes.size() != 1 || !oldSubTypes.contains(type))
      this.typeListeners.stream()
          .filter(l -> !l.equals(this))
          .forEach(l -> l.possibleSubTypesChanged(this, root, oldSubTypes));
  }

  @Override
  public void registerTypeListener(TypeListener listener) {
    List<HasType> root = new ArrayList<>(List.of(this));
    this.typeListeners.add(listener);
    listener.typeChanged(this, root, this.type);
    listener.possibleSubTypesChanged(this, root, this.possibleSubTypes);
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
    List<HasType> root = new ArrayList<>(List.of(this));
    for (var l : typeListeners) {
      l.typeChanged(this, root, type);
      l.possibleSubTypesChanged(this, root, possibleSubTypes);
    }
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .appendSuper(super.toString())
        .append("type", type)
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
