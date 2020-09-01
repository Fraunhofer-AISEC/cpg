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

package de.fraunhofer.aisec.cpg.graph.declaration;

import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A method declaration is a {@link FunctionDeclaration} tied to a specific {@link
 * RecordDeclaration}.
 */
public class MethodDeclaration extends FunctionDeclaration {

  private boolean isStatic;

  /**
   * The {@link RecordDeclaration} this method is tied to. This can be empty if we do not know about
   * the type.
   */
  @Nullable private RecordDeclaration recordDeclaration;

  /**
   * Creates a method declaration from an existing {@link FunctionDeclaration}.
   *
   * @param functionDeclaration the {@link FunctionDeclaration}.
   * @param recordDeclaration the {@link RecordDeclaration} this constructor belongs to.
   * @return the new method declaration
   */
  public static MethodDeclaration from(
      FunctionDeclaration functionDeclaration, @Nullable RecordDeclaration recordDeclaration) {
    MethodDeclaration md =
        NodeBuilder.newMethodDeclaration(
            functionDeclaration.getName(), functionDeclaration.getCode(), false, recordDeclaration);

    md.setLocation(functionDeclaration.getLocation());
    md.setParameters(functionDeclaration.getParameters());
    md.setBody(functionDeclaration.getBody());
    md.setType(functionDeclaration.getType());
    md.addAnnotations(functionDeclaration.getAnnotations());
    md.setRecordDeclaration(recordDeclaration);
    md.setIsDefinition(functionDeclaration.isDefinition());

    if (!md.isDefinition()) {
      // do not call getDefinition if this is a definition itself, otherwise this
      // will return a 'this' to the old function declaration
      md.setDefinition(functionDeclaration.getDefinition());
    }

    return md;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean isStatic) {
    this.isStatic = isStatic;
  }

  @Nullable
  public RecordDeclaration getRecordDeclaration() {
    return recordDeclaration;
  }

  public void setRecordDeclaration(@Nullable RecordDeclaration recordDeclaration) {
    this.recordDeclaration = recordDeclaration;
  }
}
