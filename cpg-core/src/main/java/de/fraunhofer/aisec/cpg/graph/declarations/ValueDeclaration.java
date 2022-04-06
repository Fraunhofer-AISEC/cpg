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

import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
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

/** A declaration who has a type. */
public abstract class ValueDeclaration extends Declaration implements HasType {

  protected Type type = UnknownType.getUnknownType();

  protected Set<Type> possibleSubTypes = new HashSet<>();

  @Transient private final Set<TypeListener> typeListeners = new HashSet<>();

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

  /**
   * There is no case in which we would want to propagate a referenceType as in this case always the
   * underlying ObjectType should be propagated
   *
   * @return Type that should be propagated
   */
  @Override
  public Type getPropagationType() {
    if (this.type instanceof ReferenceType) {
      return ((ReferenceType) this.type).getElementType();
    }
    return getType();
  }

  @Override
  public void setType(Type type, Collection<HasType> root) {
    if (!TypeManager.isTypeSystemActive()) {
      TypeManager.getInstance().cacheType(this, type);
      return;
    }

    if (root == null) {
      root = new ArrayList<>();
    }

    if (type == null
        || root.contains(this)
        || TypeManager.getInstance().isUnknown(type)
        || (this.type instanceof FunctionPointerType && !(type instanceof FunctionPointerType))) {
      return;
    }

    Type oldType = this.type;

    type = type.duplicate();
    type.setQualifier(this.type.getQualifier().merge(type.getQualifier()));

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
  public void resetTypes(Type type) {
    Set<Type> oldSubTypes = new HashSet<>(getPossibleSubTypes());
    Type oldType = this.type;

    this.type = type;
    setPossibleSubTypes(new HashSet<>(List.of(type)));

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
    typeListeners.add(listener);
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

    if (root.contains(this)) {
      return;
    }
    root.add(this);

    Set<Type> oldSubTypes = this.possibleSubTypes;
    this.possibleSubTypes = possibleSubTypes;

    if (!this.possibleSubTypes.equals(oldSubTypes)) {
      for (var listener : this.typeListeners) {
        if (!listener.equals(this)) {
          listener.possibleSubTypesChanged(this, root, oldSubTypes);
        }
      }
    }
  }

  @Override
  public void refreshType() {
    List<HasType> root = new ArrayList<>(List.of(this));
    for (var l : this.typeListeners) {
      l.typeChanged(this, root, type);
      l.possibleSubTypesChanged(this, root, possibleSubTypes);
    }
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE).appendSuper(super.toString()).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ValueDeclaration)) {
      return false;
    }
    ValueDeclaration that = (ValueDeclaration) o;
    return super.equals(that)
        && Objects.equals(type, that.type)
        && Objects.equals(possibleSubTypes, that.possibleSubTypes);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public void updateType(Type type) {
    this.type = type;
  }

  @Override
  public void updatePossibleSubtypes(Set<Type> types) {
    this.possibleSubTypes = types;
  }
}
