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
package de.fraunhofer.aisec.cpg.processing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;

/**
 * Reflective visitor that visits the most specific implementation of visit() methods.
 *
 * @param <V> V must implement {@code IVisitable}.
 */
public abstract class IVisitor<V extends IVisitable> {
  private final Collection<V> visited = new HashSet<>();

  public Collection<V> getVisited() {
    return visited;
  }

  public void visit(@NotNull V t) {
    try {
      Method mostSpecificVisit = this.getClass().getMethod("visit", t.getClass());

      if (mostSpecificVisit != null) {
        mostSpecificVisit.setAccessible(true);
        mostSpecificVisit.invoke(this, t);
      }
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      // Nothing to do here
    }
  }
}
