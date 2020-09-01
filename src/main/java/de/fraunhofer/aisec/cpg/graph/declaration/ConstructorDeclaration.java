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
import de.fraunhofer.aisec.cpg.graph.type.TypeParser;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The declaration of a constructor within a {@link RecordDeclaration}. Is it essentially a special
 * case of a {@link MethodDeclaration}.
 */
public class ConstructorDeclaration extends MethodDeclaration {

  /**
   * Creates a constructor declaration from an existing {@link MethodDeclaration}.
   *
   * @param methodDeclaration the {@link MethodDeclaration}.
   * @return the constructor declaration
   */
  public static ConstructorDeclaration from(MethodDeclaration methodDeclaration) {
    RecordDeclaration recordDeclaration = methodDeclaration.getRecordDeclaration();

    ConstructorDeclaration c =
        NodeBuilder.newConstructorDeclaration(
            methodDeclaration.getName(), methodDeclaration.getCode(), recordDeclaration);

    c.setBody(methodDeclaration.getBody());
    c.setLocation(methodDeclaration.getLocation());
    c.setParameters(methodDeclaration.getParameters());
    c.addAnnotations(methodDeclaration.getAnnotations());
    c.setIsDefinition(methodDeclaration.isDefinition());

    if (!c.isDefinition()) {
      // do not call getDefinition if this is a definition itself, otherwise this
      // will return a 'this' to the old method declaration
      c.setDefinition(methodDeclaration.getDefinition());
    }

    return c;
  }

  @Override
  public void setRecordDeclaration(@Nullable RecordDeclaration recordDeclaration) {
    super.setRecordDeclaration(recordDeclaration);
    if (recordDeclaration != null) {
      // constructors always have implicitly the return type of their class
      setType(TypeParser.createFrom(recordDeclaration.getName(), true));
    }
  }
}
