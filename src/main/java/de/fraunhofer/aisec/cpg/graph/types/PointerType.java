/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import java.util.Objects;
import org.neo4j.ogm.annotation.Relationship;

/**
 * PointerTypes represent all references to other Types. For C/CPP this includes pointers, as well
 * as arrays, since technically arrays are pointers. For JAVA the only use case are arrays as there
 * is no such pointer concept.
 */
public class PointerType extends Type implements SecondOrderType {

  @Relationship(value = "ELEMENT_TYPE")
  private Type elementType;

  public enum PointerOrigin {
    POINTER,
    ARRAY,
  }

  private PointerOrigin pointerOrigin;

  private PointerType() {}

  public PointerType(Type elementType, PointerOrigin pointerOrigin) {
    super();
    if (pointerOrigin == PointerOrigin.ARRAY) {
      this.setName(elementType.getName() + "[]");
      this.pointerOrigin = PointerOrigin.ARRAY;
    } else {
      this.setName(elementType.getName() + "*");
      this.pointerOrigin = PointerOrigin.POINTER;
    }
    this.elementType = elementType;
  }

  public PointerType(Type type, Type elementType, PointerOrigin pointerOrigin) {
    super(type);
    if (pointerOrigin == PointerOrigin.ARRAY) {
      this.setName(elementType.getName() + "[]");
    } else {
      this.setName(elementType.getName() + "*");
    }
    this.elementType = elementType;
    this.pointerOrigin = pointerOrigin;
  }

  /**
   * @return referencing a PointerType results in another PointerType wrapping the first
   *     PointerType, e.g. int**
   */
  @Override
  public PointerType reference(PointerOrigin origin) {
    if (origin == null) {
      origin = PointerOrigin.ARRAY;
    }
    return new PointerType(this, origin);
  }

  /** @return dereferencing a PointerType yields the type the pointer was pointing towards */
  @Override
  public Type dereference() {
    return elementType;
  }

  @Override
  public void refreshNames() {
    if (this.getElementType() instanceof PointerType) {
      ((PointerType) this.getElementType()).refreshNames();
    }
    if (this.pointerOrigin == PointerOrigin.ARRAY) {
      this.setName(this.getElementType().getName() + "[]");
    } else {
      this.setName(this.getElementType().getName() + "*");
    }
  }

  @Override
  public Type duplicate() {
    return new PointerType(this, this.elementType.duplicate(), this.pointerOrigin);
  }

  public boolean isArray() {
    return this.pointerOrigin == PointerOrigin.ARRAY;
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

  public PointerOrigin getPointerOrigin() {
    return pointerOrigin;
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
