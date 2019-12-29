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

import java.util.Set;

public interface HasType {
  Type getType();

  void setType(Type type);

  Set<Type> getPossibleSubTypes();

  void setPossibleSubTypes(Set<Type> possibleSubTypes);

  void registerTypeListener(TypeListener listener);

  void unregisterTypeListener(TypeListener listener);

  Set<TypeListener> getTypeListeners();

  default boolean shouldBeNotified(TypeListener listener) {
    return true;
  }

  void refreshType();

  /**
   * Used to set the type and clear the possible subtypes list for when a type is more precise than
   * the current.
   *
   * @param type the more precise type
   */
  void resetTypes(Type type);

  interface TypeListener {

    void typeChanged(HasType src, Type oldType);

    void possibleSubTypesChanged(HasType src, Set<Type> oldSubTypes);
  }
}
