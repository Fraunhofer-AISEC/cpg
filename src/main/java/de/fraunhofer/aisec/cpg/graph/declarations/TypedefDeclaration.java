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

import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.UnknownType;
import java.util.Objects;

/**
 * Represents a type alias definition as found in C/C++: <code>typedef unsigned long ulong;</code>
 */
public class TypedefDeclaration extends Declaration {

  /** The already existing type that is to be aliased */
  private Type type = UnknownType.getUnknownType();

  /** The newly created alias to be defined */
  private Type alias = UnknownType.getUnknownType();

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Type getAlias() {
    return alias;
  }

  public void setAlias(Type alias) {
    this.alias = alias;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TypedefDeclaration)) {
      return false;
    }
    TypedefDeclaration that = (TypedefDeclaration) o;
    return Objects.equals(type, that.type) && Objects.equals(alias, that.alias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, alias);
  }
}
