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
import de.fraunhofer.aisec.cpg.graph.statements.BreakStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ContinueStatement;
import de.fraunhofer.aisec.cpg.graph.statements.DoStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement;
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopScope extends ValueDeclarationScope implements Breakable, Continuable {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoopScope.class);

  /**
   * Statements that constitute the start of the Loop depending on the used pass, mostly of size 1
   */
  private List<Node> starts = new ArrayList<>();
  /** Statements that constitute the start of the Loop condition evaluation, mostly of size 1 */
  private List<Node> conditions = new ArrayList<>();

  private List<BreakStatement> breaks = new ArrayList<>();
  private List<ContinueStatement> continues = new ArrayList<>();

  public LoopScope(Statement loopStatement) {
    super(loopStatement);
  }

  public List<Node> getStarts() {
    return starts;
  }

  public void setStarts(List<Node> starts) {
    this.starts = starts;
  }

  public List<Node> getConditions() {
    return conditions;
  }

  public void setConditions(List<Node> conditions) {
    this.conditions = conditions;
  }

  public void addBreakStatement(BreakStatement breakStatement) {
    this.breaks.add(breakStatement);
  }

  public void addContinueStatement(ContinueStatement continueStatement) {
    this.continues.add(continueStatement);
  }

  public List<BreakStatement> getBreakStatements() {
    return this.breaks;
  }

  public List<ContinueStatement> getContinueStatements() {
    return this.continues;
  }

  public List<Node> starts() {
    if (astNode instanceof WhileStatement) {
      WhileStatement ws = (WhileStatement) astNode;
      if (ws.getConditionDeclaration() != null)
        return SubgraphWalker.getEOGPathEdges(ws.getConditionDeclaration()).getEntries();
      else if (ws.getCondition() != null)
        return SubgraphWalker.getEOGPathEdges(ws.getCondition()).getEntries();
      return SubgraphWalker.getEOGPathEdges(ws.getStatement()).getEntries();
    } else if (astNode instanceof ForStatement) {
      ForStatement fs = (ForStatement) astNode;
      if (fs.getConditionDeclaration() != null)
        return SubgraphWalker.getEOGPathEdges(fs.getConditionDeclaration()).getEntries();
      else if (fs.getCondition() != null)
        return SubgraphWalker.getEOGPathEdges(fs.getCondition()).getEntries();
      return SubgraphWalker.getEOGPathEdges(fs.getStatement()).getEntries();
    } else if (astNode instanceof ForEachStatement) {
      ForEachStatement fs = (ForEachStatement) astNode;
      return SubgraphWalker.getEOGPathEdges(fs).getEntries();
    } else if (astNode instanceof DoStatement) {
      return SubgraphWalker.getEOGPathEdges(((DoStatement) astNode).getStatement()).getEntries();
    } else {
      LOGGER.error(
          "Currently the component {} is not supported as loop scope.", astNode.getClass());
      return new ArrayList<>();
    }
  }
}
