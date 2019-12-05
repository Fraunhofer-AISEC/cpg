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

import java.util.Objects;

public class CatchClause extends Statement {

  @SubGraph("AST")
  private VariableDeclaration parameter;

  @SubGraph("AST")
  private CompoundStatement body;

  public VariableDeclaration getParameter() {
    return parameter;
  }

  public void setParameter(VariableDeclaration parameter) {
    this.parameter = parameter;
  }

  public CompoundStatement getBody() {
    return body;
  }

  public void setBody(CompoundStatement body) {
    this.body = body;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CatchClause)) {
      return false;
    }
    CatchClause that = (CatchClause) o;
    return super.equals(that)
        && Objects.equals(parameter, that.parameter)
        && Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
