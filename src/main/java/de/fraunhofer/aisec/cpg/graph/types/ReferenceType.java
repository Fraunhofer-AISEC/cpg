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

package de.fraunhofer.aisec.cpg.graph.types;

import java.util.Objects;

/**
 * ReferenceTypes describe CPP References (int&amp;), which are represent an alternative name for a
 * variable. It is necessary to make this distinction, and not just rely on the original type as it
 * is required for matching parameters in function arguments to discover which implementation is
 * called.
 */
public class ReferenceType extends Type implements SecondOrderType {

  private Type reference;

  private ReferenceType() {}

  public ReferenceType(Type reference) {
    super();
    this.name = reference.getName() + "&";
    this.reference = reference;
  }

  public ReferenceType(Type type, Type reference) {
    super(type);
    this.name = reference.getName() + "&";
    this.reference = reference;
  }

  public ReferenceType(Storage storage, Qualifier qualifier, Type reference) {
    super(reference.getName() + "&", storage, qualifier);
    this.reference = reference;
  }

  /** @return Referencing a ReferenceType results in a PointerType to the original ReferenceType */
  @Override
  public Type reference(PointerType.PointerOrigin pointerOrigin) {
    return new PointerType(this, pointerOrigin);
  }

  /**
   * @return Dereferencing a ReferenceType equals to dereferencing the original (non-reference) type
   */
  @Override
  public Type dereference() {
    return reference.dereference();
  }

  @Override
  public Type duplicate() {
    return new ReferenceType(this, this.reference);
  }

  public Type getElementType() {
    return reference;
  }

  public void setElementType(Type reference) {
    this.reference = reference;
  }

  @Override
  public boolean isSimilar(Type t) {
    return t instanceof ReferenceType
        && ((ReferenceType) t).getElementType().equals(this)
        && super.isSimilar(t);
  }

  public void refreshName() {
    this.name = this.getElementType().getName() + "&";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ReferenceType)) return false;
    if (!super.equals(o)) return false;
    ReferenceType that = (ReferenceType) o;
    return Objects.equals(reference, that.reference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), reference);
  }

  @Override
  public String toString() {
    return "ReferenceType{"
        + "reference="
        + reference
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
