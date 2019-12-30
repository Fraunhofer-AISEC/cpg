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

/**
 * A method declaration is a {@link FunctionDeclaration} tied to a specific {@link
 * RecordDeclaration}.
 */
public class MethodDeclaration extends FunctionDeclaration {

  private boolean isStatic;

  /**
   * Creates a method declaration from an existing {@link FunctionDeclaration}.
   *
   * @param functionDeclaration the {@link FunctionDeclaration}.
   * @return the new method declaration
   */
  public static MethodDeclaration from(FunctionDeclaration functionDeclaration) {
    MethodDeclaration md = new MethodDeclaration();

    md.setName(functionDeclaration.getName());
    md.setCode(functionDeclaration.getCode());
    md.setRegion(functionDeclaration.getRegion());
    md.setParameters(functionDeclaration.getParameters());
    md.setBody(functionDeclaration.getBody());
    md.setType(functionDeclaration.getType());

    return md;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean isStatic) {
    this.isStatic = isStatic;
  }
}
