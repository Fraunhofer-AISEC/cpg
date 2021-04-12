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

import static de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.unwrap;

import de.fraunhofer.aisec.cpg.graph.SubGraph;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.neo4j.ogm.annotation.Relationship;

public class TryStatement extends Statement {

  @Relationship(value = "RESOURCES", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<Statement>> resources = new ArrayList<>();

  @SubGraph("AST")
  private CompoundStatement tryBlock;

  @SubGraph("AST")
  private CompoundStatement finallyBlock;

  @Relationship(value = "CATCH_CLAUSES", direction = "OUTGOING")
  @SubGraph("AST")
  private List<PropertyEdge<CatchClause>> catchClauses = new ArrayList<>();

  @NonNull
  public List<Statement> getResources() {
    return unwrap(this.resources);
  }

  public List<PropertyEdge<Statement>> getResourcesPropertyEdge() {
    return this.resources;
  }

  public void setResources(List<Statement> resources) {
    this.resources = new ArrayList<>();
    int c = 0;
    for (Statement s : resources) {
      PropertyEdge<Statement> propertyEdge = new PropertyEdge<>(this, s);
      propertyEdge.addProperty(Properties.INDEX, c);
      this.resources.add(propertyEdge);
      c++;
    }
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

  @NonNull
  public List<CatchClause> getCatchClauses() {
    return unwrap(this.catchClauses);
  }

  public List<PropertyEdge<CatchClause>> getCatchClausesPropertyEdge() {
    return this.catchClauses;
  }

  public void setCatchClauses(List<CatchClause> catchClauses) {
    this.catchClauses = new ArrayList<>();
    int counter = 0;

    for (CatchClause c : catchClauses) {
      PropertyEdge<CatchClause> propertyEdge = new PropertyEdge<>(this, c);
      propertyEdge.addProperty(Properties.INDEX, counter);
      this.catchClauses.add(propertyEdge);
      counter++;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TryStatement)) {
      return false;
    }
    TryStatement that = (TryStatement) o;
    return super.equals(that)
        && Objects.equals(this.getResources(), that.getResources())
        && PropertyEdge.propertyEqualsList(resources, that.resources)
        && Objects.equals(tryBlock, that.tryBlock)
        && Objects.equals(finallyBlock, that.finallyBlock)
        && Objects.equals(this.getCatchClauses(), that.getCatchClauses())
        && PropertyEdge.propertyEqualsList(catchClauses, that.catchClauses);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
