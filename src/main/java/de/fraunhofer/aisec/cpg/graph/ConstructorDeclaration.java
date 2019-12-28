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
 * The declaration of a constructor within a {@link RecordDeclaration}. Is it essentially a special
 * case of a {@link MethodDeclaration}.
 */
public class ConstructorDeclaration extends MethodDeclaration {

  /**
   * Creates a constructor declaration from an existing {@link FunctionDeclaration}.
   *
   * @param functionDeclaration the {@link FunctionDeclaration}.
   */
  public static ConstructorDeclaration from(FunctionDeclaration functionDeclaration) {
    ConstructorDeclaration c = new ConstructorDeclaration();

    // constructors always have void type
    c.setType(Type.createFrom(VOID_TYPE_STRING));

    c.name = functionDeclaration.getName();
    c.body = functionDeclaration.getBody();
    c.code = functionDeclaration.getCode();
    c.region = functionDeclaration.getRegion();
    c.parameters = functionDeclaration.getParameters();

    return c;
  }
}
