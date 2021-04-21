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

import java.util.Objects;

/**
 * IncompleteTypes are defined as object with unknown size. For instance: void, arrays of unknown
 * length, forward declarated classes in C++
 *
 * <p>Right now we are only dealing with void for objects with unknown size, therefore the name is
 * fixed to void. However, this can be changed in future, in order to support other objects with
 * unknown size apart from void. Therefore this Type is not called VoidType
 */
public class IncompleteType extends Type {

  public IncompleteType() {
    super("void", Storage.AUTO, new Qualifier(false, false, false, false));
  }

  public IncompleteType(Type type) {
    super(type);
  }

  /** @return PointerType to a IncompleteType, e.g. void* */
  @Override
  public Type reference(PointerType.PointerOrigin pointerOrigin) {
    return new PointerType(this, pointerOrigin);
  }

  /** @return dereferencing void results in void therefore the same type is returned */
  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type duplicate() {
    return new IncompleteType(this);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof IncompleteType;
  }

  @Override
  public int hashCode() {

    return Objects.hash(super.hashCode());
  }

  @Override
  public String toString() {
    return "IncompleteType{"
        + "typeName='"
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
