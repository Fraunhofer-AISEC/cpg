/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes;

import java.util.*;

public class PassOrderingWorkingList {
  private List<Map.Entry<Pass, Set<Class<? extends Pass>>>> workingList;

  public PassOrderingWorkingList() {
    workingList = new ArrayList<>();
  }

  public List<Map.Entry<Pass, Set<Class<? extends Pass>>>> getWorkingList() {
    return workingList;
  }

  public void addToWorkingList(
      AbstractMap.SimpleEntry<Pass, Set<Class<? extends Pass>>> newElement) {
    workingList.add(newElement);
  }

  public boolean isEmpty() {
    return workingList.isEmpty();
  }

  public int size() {
    return workingList.size();
  }

  public void removeDependencyByClass(Class<? extends Pass> cls) {
    for (Map.Entry<Pass, Set<Class<? extends Pass>>> currentElement : workingList) {
      currentElement.getValue().remove(cls);
    }
  }

  public String toString() {
    return workingList.toString();
  }
}
