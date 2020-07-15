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
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * UnknownType describe the case in which it is not possible for the CPG to determine which Type is
 * used. E.g.: This occurs when the type is inferred by the compiler automatically when using
 * keywords such as auto in cpp
 */
public class UnknownType extends Type {

  // Only one instance of UnknownType for better representation in the graph
  private static final UnknownType unknownType = new UnknownType();

  private UnknownType() {
    super();
    this.name = "UNKNOWN";
  }

  /**
   * Use this function to obtain an UnknownType or call the TypeParser with the typeString UNKNOWN
   *
   * @return UnknownType instance
   */
  public static UnknownType getUnknownType() {
    return unknownType;
  }

  /**
   * This is only intended to be used by {@link TypeParser} for edge cases like distinct unknown
   * types, such as "UNKNOWN1", thus the package-private visibility. Other users should see {@link
   * #getUnknownType()} instead
   *
   * @param typeName The name of this unknown type, usually a variation of UNKNOWN
   */
  UnknownType(String typeName) {
    super(typeName);
  }

  /**
   * @return Same UnknownType, as it is makes no sense to obtain a pointer/reference to an
   *     UnknownType
   */
  @Override
  public Type reference(PointerType.PointerOrigin pointerOrigin) {
    return this;
  }

  /** @return Same UnknownType, */
  @Override
  public Type dereference() {
    return this;
  }

  @Override
  public Type duplicate() {
    return unknownType;
  }

  @Override
  public int hashCode() {

    return Objects.hash(super.hashCode());
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof UnknownType;
  }

  @Override
  public String toString() {
    return "UNKNOWN";
  }

  @Override
  public void setStorage(@NonNull Storage storage) {
    // Only one instance of UnknownType, use default values
  }

  @Override
  public void setQualifier(Qualifier qualifier) {
    // Only one instance of UnknownType, use default values
  }

  @Override
  public void setTypeOrigin(Origin origin) {
    // Only one instance of UnknownType, use default values
  }
}
