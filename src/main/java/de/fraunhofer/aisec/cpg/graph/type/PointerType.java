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

package de.fraunhofer.aisec.cpg.graph.type;

import java.util.Objects;

/**
 * PointerTypes represent all references to other Types. For C/CPP this includes pointers, as well
 * as arrays, since technically arrays are pointers. For JAVA the only use case are arrays as there
 * is no such pointer concept.
 */
public class PointerType extends Type {
  private Type elementType;

  public PointerType(Type elementType) {
    super();
    this.name = elementType.getName() + "*";
    this.elementType = elementType;
  }

  public PointerType(Type type, Type elementType) {
    super(type);
    this.name = elementType.getName() + "*";
    this.elementType = elementType;
  }

  /**
   * @return referencing a PointerType results in another PointerType wrapping the first
   *     PointerType, e.g. int**
   */
  @Override
  public PointerType reference() {
    return new PointerType(this);
  }

  /** @return dereferencing a PointerType yields the type the pointer was pointing towards */
  @Override
  public Type dereference() {
    return elementType;
  }

  @Override
  public Type duplicate() {
    return new PointerType(this, this.elementType.duplicate());
  }

  @Override
  public boolean isSimilar(Type t) {
    if (!(t instanceof PointerType)) {
      return false;
    }

    PointerType pointerType = (PointerType) t;

    return this.getReferenceDepth() == pointerType.getReferenceDepth()
        && this.getElementType().isSimilar(pointerType.getRoot())
        && super.isSimilar(t);
  }

  @Override
  public Type getRoot() {
    return this.elementType.getRoot();
  }

  @Override
  public Type getFollowingLevel() {
    return elementType;
  }

  public Type getElementType() {
    return elementType;
  }

  @Override
  public int getReferenceDepth() {
    int depth = 1;
    Type containedType = this.elementType;
    while (containedType instanceof PointerType) {
      depth++;
      containedType = ((PointerType) containedType).getElementType();
    }
    return depth;
  }

  @Override
  public void setRoot(Type newRoot) {
    if (this.elementType.isFirstOrderType()) {
      this.elementType = newRoot;
    } else {
      this.elementType.setRoot(newRoot);
    }
  }

  public void setElementType(Type elementType) {
    this.elementType = elementType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PointerType)) return false;
    if (!super.equals(o)) return false;
    PointerType that = (PointerType) o;
    return Objects.equals(elementType, that.elementType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), elementType);
  }

  @Override
  public String toString() {
    return "PointerType{"
        + "elementType="
        + elementType
        + ", typeName='"
        + name
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
