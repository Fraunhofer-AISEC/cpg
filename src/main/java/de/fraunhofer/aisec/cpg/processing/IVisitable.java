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

import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An object that can be visited by a visitor.
 *
 * @param <V>
 */
public interface IVisitable<V extends IVisitable> {

  /**
   * @param strategy Traversal strategy.
   * @param visitor Instance of the visitor to call.
   */
  default void accept(IStrategy<V> strategy, IVisitor<V> visitor) {
    if (!visitor.getVisited().contains(this)) {
      visitor.getVisited().add((V) this);
      visitor.visit((V) this);
      @NonNull Iterator<V> it = strategy.getIterator((V) this);
      while (it.hasNext()) {
        it.next().accept(strategy, visitor);
      }
    }
  }
}
