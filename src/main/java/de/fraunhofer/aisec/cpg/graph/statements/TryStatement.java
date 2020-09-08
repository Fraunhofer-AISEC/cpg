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

package de.fraunhofer.aisec.cpg.graph.statements;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import java.util.List;
import java.util.Objects;

public class TryStatement extends Statement {

  @SubGraph("AST")
  private List<Statement> resources;

  @SubGraph("AST")
  private CompoundStatement tryBlock;

  @SubGraph("AST")
  private CompoundStatement finallyBlock;

  @SubGraph("AST")
  private List<CatchClause> catchClauses;

  public List<Statement> getResources() {
    return resources;
  }

  public void setResources(List<Statement> resources) {
    this.resources = resources;
  }

  public CompoundStatement getTryBlock() {
    return tryBlock;
  }

  public void setTryBlock(CompoundStatement tryBlock) {
    this.tryBlock = tryBlock;
  }

  public CompoundStatement getFinallyBlock() {
    return finallyBlock;
  }

  public void setFinallyBlock(CompoundStatement finallyBlock) {
    this.finallyBlock = finallyBlock;
  }

  public List<CatchClause> getCatchClauses() {
    return catchClauses;
  }

  public void setCatchClauses(List<CatchClause> catchClauses) {
    this.catchClauses = catchClauses;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TryStatement that = (TryStatement) o;
    return Objects.equals(resources, that.resources)
        && Objects.equals(tryBlock, that.tryBlock)
        && Objects.equals(finallyBlock, that.finallyBlock)
        && Objects.equals(catchClauses, that.catchClauses);
  }
}
