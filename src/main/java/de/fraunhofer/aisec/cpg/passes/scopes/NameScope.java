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
package de.fraunhofer.aisec.cpg.passes.scopes;

import de.fraunhofer.aisec.cpg.graph.Node;

public class NameScope extends StructureDeclarationScope {

  private String namePrefix;

  public NameScope(Node node, String currentPrefix, String delimiter) {
    super(node);
    if (currentPrefix == null || !currentPrefix.isEmpty()) {
      String nodeName = node.getName();
      // If the name already contains some form of prefix we have to remove it.
      nodeName =
          nodeName.contains(delimiter)
              ? nodeName.substring(nodeName.lastIndexOf(delimiter) + delimiter.length())
              : nodeName;
      this.namePrefix = currentPrefix + delimiter + nodeName;
    } else {
      this.namePrefix = node.getName();
    }
    this.astNode = node;
  }

  public String getNamePrefix() {
    return namePrefix;
  }

  public void setNamePrefix(String namePrefix) {
    this.namePrefix = namePrefix;
  }
}
