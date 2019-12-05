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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A statement. */
public class Statement extends Node {

  /**
   * A list of local variables associated to this statement, defined by their {@link
   * VariableDeclaration} extracted from Block because for, while, if, switch can declare locals in
   * their condition or initializers
   */
  private @SubGraph("AST") List<VariableDeclaration> locals = new ArrayList<>();

  public List<VariableDeclaration> getLocals() {
    return locals;
  }

  public void setLocals(List<VariableDeclaration> locals) {
    this.locals = locals;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Statement)) {
      return false;
    }
    Statement statement = (Statement) o;
    return super.equals(statement) && Objects.equals(locals, statement.locals);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
