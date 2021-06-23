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
import org.neo4j.ogm.annotation.Transient;

/** A declaration who has a type. */
public abstract class ValueDeclaration extends Declaration implements HasType {

  protected Type type = UnknownType.getUnknownType();

  protected Set<Type> possibleSubTypes = new HashSet<>();

  @Transient private Set<TypeListener> typeListeners = new HashSet<>();

  @Override
  public Type getType() {
    Type result;
    if (TypeManager.isTypeSystemActive()) {
      // just to make sure that we REALLY always return a valid type in case this somehow gets set
      // to
      // null
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
  public void setType(Type type, HasType root) {
    if (!TypeManager.isTypeSystemActive()) {
      TypeManager.getInstance().cacheType(this, type);
      return;
    }
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
  public void resetTypes(Type type) {
    Set<Type> oldSubTypes = new HashSet<>(getPossibleSubTypes());
    Type oldType = this.type;

    this.type = type;
    setPossibleSubTypes(new HashSet<>(List.of(type)));

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
    typeListeners.add(listener);
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
  public Set<Type> getPossibleSubTypes() {
    if (!TypeManager.isTypeSystemActive()) {
      return TypeManager.getInstance().getTypeCache().getOrDefault(this, Collections.emptySet());
    }
    return possibleSubTypes;
  }

  @Override
  public void setPossibleSubTypes(Set<Type> possibleSubTypes, HasType root) {
    possibleSubTypes =
        possibleSubTypes.stream()
            .filter(Predicate.not(TypeManager.getInstance()::isUnknown))
            .collect(Collectors.toSet());

    if (!TypeManager.isTypeSystemActive()) {
      possibleSubTypes.forEach(t -> TypeManager.getInstance().cacheType(this, t));
      return;
    }

    if (root == this) {
      return;
    }

    Set<Type> oldSubTypes = this.possibleSubTypes;
    this.possibleSubTypes = new HashSet<>(possibleSubTypes);

    if (!this.possibleSubTypes.equals(oldSubTypes)) {
      this.typeListeners.stream()
          .filter(l -> !l.equals(this))
          .forEach(l -> l.possibleSubTypesChanged(this, root == null ? this : root, oldSubTypes));
    }
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
