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

import de.fraunhofer.aisec.cpg.helpers.TypeConverter;
import de.fraunhofer.aisec.cpg.helpers.TypeSetConverter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.annotation.typeconversion.Convert;

/** A declaration who has a type. */
public abstract class ValueDeclaration extends Declaration implements HasType {

  @Convert(TypeConverter.class)
  protected Type type = Type.UNKNOWN;

  @Convert(TypeSetConverter.class)
  protected Set<Type> possibleSubTypes = new HashSet<>();

  @Transient private Set<TypeListener> typeListeners = new HashSet<>();

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public void setType(Type type) {
    if (type == null || TypeManager.getInstance().isUnknown(type)) {
      return;
    }
    if (TypeManager.getInstance().isPrimitive(type)
        && !TypeManager.getInstance().isUnknown(this.type)) {
      return;
    }

    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.add(type);

    Type oldType = this.type;

    this.type = TypeManager.getInstance().getCommonType(subTypes).orElse(type);
    setPossibleSubTypes(subTypes);

    if (!Objects.equals(oldType, type)) {
      this.typeListeners.stream()
          .filter(this::shouldBeNotified)
          .forEach(l -> l.typeChanged(this, oldType));
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
          .filter(this::shouldBeNotified)
          .forEach(l -> l.typeChanged(this, oldType));
    }
    if (oldSubTypes.size() != 1 || !oldSubTypes.contains(type))
      this.typeListeners.stream()
          .filter(this::shouldBeNotified)
          .forEach(l -> l.possibleSubTypesChanged(this, oldSubTypes));
  }

  @Override
  public void registerTypeListener(TypeListener listener) {
    typeListeners.add(listener);
    if (shouldBeNotified(listener)) {
      listener.typeChanged(this, this.type);
      listener.possibleSubTypesChanged(this, this.possibleSubTypes);
    }
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
    return possibleSubTypes;
  }

  @Override
  public void setPossibleSubTypes(Set<Type> possibleSubTypes) {
    if (possibleSubTypes.stream().allMatch(TypeManager.getInstance()::isPrimitive)
        && !this.possibleSubTypes.isEmpty()) {
      return;
    }
    Set<Type> oldSubTypes = this.possibleSubTypes;

    if (this.possibleSubTypes.addAll(possibleSubTypes)) {
      this.typeListeners.stream()
          .filter(this::shouldBeNotified)
          .forEach(l -> l.possibleSubTypesChanged(this, oldSubTypes));
    }
  }

  @Override
  public void refreshType() {
    this.typeListeners.stream()
        .filter(this::shouldBeNotified)
        .forEach(
            l -> {
              l.typeChanged(this, type);
              l.possibleSubTypesChanged(this, possibleSubTypes);
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
}
