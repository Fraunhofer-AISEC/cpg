/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CatchClause extends Statement {

  @SubGraph("AST")
  @Nullable
  private VariableDeclaration parameter;

  @SubGraph("AST")
  private CompoundStatement body;

  @Nullable
  public VariableDeclaration getParameter() {
    return parameter;
  }

  public void setParameter(@NonNull VariableDeclaration parameter) {
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
