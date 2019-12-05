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

package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.BreakStatement;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.ContinueStatement;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DoStatement;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.MethodDeclaration;
import de.fraunhofer.aisec.cpg.graph.NamespaceDeclaration;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.WhileStatement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Creates a simple control flow graph (CFG) based on AST.
 *
 * <p>The CFG is a directed cyclic graph with Node representing statements and edges representing
 * the program control flow.
 *
 * <ul>
 *   <li>Return statements will have no successors.
 *   <li>Branch statements (if/switch/exception traps) will have multiple successors.
 *   <li>Simple statements will have their following statement as sole successor
 *   <li>break / continue will create respective edges
 * </ul>
 *
 * @author julian
 */
public class ControlFlowGraphPass implements Pass {
  private List<Statement> remaining = new CopyOnWriteArrayList<>();
  /** For keeping track of nested break/continue scopes. */
  private Deque<BreakContinueScope> breakContinueScopes = new ArrayDeque<>();

  @Override
  public void cleanup() {
    this.remaining.clear();
    this.breakContinueScopes.clear();
  }

  @Override
  public LanguageFrontend getLang() {
    return null;
  }

  public void setLang(LanguageFrontend lang) {}

  @Override
  public void accept(TranslationResult t) {
    for (TranslationUnitDeclaration tu : t.getTranslationUnits()) {
      for (Declaration decl : tu.getDeclarations()) {
        handleDeclaration(decl);
      }
    }
  }

  /**
   * We handle only declarations that contain basic blocks (Functions and Records).
   *
   * @param decl
   */
  private void handleDeclaration(Declaration decl) {
    if (decl instanceof RecordDeclaration) {
      handleRecordDeclaration((RecordDeclaration) decl);
    } else if (decl instanceof NamespaceDeclaration) {
      for (Declaration declaration : ((NamespaceDeclaration) decl).getDeclarations())
        handleDeclaration(declaration);
    } else if (decl instanceof FunctionDeclaration) {
      handleFunctionDeclaration((FunctionDeclaration) decl);
    }
  }

  /**
   * Create control flow graph for all statements in a function (this includes methods).
   *
   * @param decl
   */
  private void handleFunctionDeclaration(FunctionDeclaration decl) {
    Statement body = decl.getBody();
    decl.getNextCFG().add(body);
    if (body != null) handleBody((CompoundStatement) body);
  }

  /**
   * Create control flow graph for statements in a record.
   *
   * @param decl
   */
  private void handleRecordDeclaration(RecordDeclaration decl) {
    for (MethodDeclaration m : decl.getMethods()) {
      handleFunctionDeclaration(m);
    }
  }

  private void handleBody(CompoundStatement body) {
    // Create "CFG" link to first statement in body
    /* Compound stmts are never jump targets. Instead, jump target is the first
     * instruction in Compound (if any) */

    // Extend our todo list by all statements in Compound
    List<Statement> containedStmts = body.getStatements();
    body.getNextCFG().add(containedStmts.get(0));

    handleStatements(body);
  }

  /**
   * Create CFG for a block of statements (CompoundStatement).
   *
   * @param body
   */
  private void handleStatements(CompoundStatement body) {
    // Used to mark Nodes which should not automatically be linked to their successors (e.g. last
    // stmt in THEN block should not be linked to first stmt in ELSE block)
    Set<Node> doNotLinkToFollowingStmt = new HashSet<>();

    // Store all statements of the block in our todo list.
    this.remaining.addAll(body.getStatements());

    // Iterate in old school style to allow adding to list during iteration
    for (int i = 0; i < this.remaining.size(); i++) {

      Statement stmt = this.remaining.get(i);
      if (stmt == null) {
        continue;
      }

      updateBreakContinueScopes(i, stmt);

      // Analyze jump targets of current statement.
      Collection<Node> targets = new ArrayList<>();
      if (isIfStmt(stmt)) {

        // IF stmt has two jump targets: true and either: false branch or statement after IF.
        List<Node> ifThenBlocks = getJumpTargets((IfStatement) stmt);
        targets.addAll(ifThenBlocks);
        if (ifThenBlocks.size() < 2) {
          targets.add(this.remaining.get(i + 1));
        }

        Statement thenStmt = ((IfStatement) stmt).getThenStatement();
        doNotLinkToFollowingStmt.add(lastStatementInCompound(thenStmt));
        if (!isBreakOrContinue(thenStmt)) {
          thenStmt.getNextCFG().add(this.remaining.get(i + 1));
        }
        addTodo(i, thenStmt);

        Statement elseStmt = ((IfStatement) stmt).getElseStatement();
        if (elseStmt != null) {
          addTodo(i + 1, elseStmt);
        }

      } else if (stmt instanceof ContinueStatement) {

        ContinueStatement contStmt = (ContinueStatement) stmt;
        if (!this.breakContinueScopes.isEmpty()) {
          BreakContinueScope scope = this.breakContinueScopes.peek();
          contStmt.getNextCFG().add(scope.start);
        }

      } else if (stmt instanceof BreakStatement) {

        BreakStatement breakStmt = (BreakStatement) stmt;
        if (!this.breakContinueScopes.isEmpty()) {
          BreakContinueScope scope = this.breakContinueScopes.peek();
          breakStmt.getNextCFG().add(scope.breakLocation);
        }

      } else if (isCompoundStmt(stmt)) {
        /* Compound stmts are never jump targets. Instead, jump target is the first
         * instruction in Compound (if any) */

        // Extend our todo list by all statements in Compound
        List<Statement> containedStmts = ((CompoundStatement) stmt).getStatements();
        addTodo(i, containedStmts);

      } else if (stmt instanceof WhileStatement) {

        WhileStatement whileStmt = (WhileStatement) stmt;

        targets.add(firstStatementInCompound(whileStmt.getStatement()));
        targets.add(this.remaining.get(i + 1));

        addTodo(i, whileStmt.getStatement());

        Node lastInBlock = lastStatementInCompound(whileStmt.getStatement());
        if (lastInBlock != null) {
          lastInBlock.getNextCFG().add(whileStmt);
        }
        doNotLinkToFollowingStmt.add(lastStatementInCompound(whileStmt.getStatement()));

      } else if (stmt instanceof DoStatement) {

        // Add inner block to todo list and to target
        DoStatement doStmt = (DoStatement) stmt;
        addTodo(i, doStmt.getStatement());
        targets.add(firstStatementInCompound(doStmt.getStatement()));

        // Connect condition to first stmt in compound (when re-iterating the do-while-loop)
        doStmt.getCondition().getNextCFG().add(firstStatementInCompound(doStmt.getStatement()));
        addTodo(i + 1, doStmt.getCondition());

      } else if (!(stmt instanceof ReturnStatement)
          && i + 1 < this.remaining.size() - 1
          && !doNotLinkToFollowingStmt.contains(stmt)) {
        // Next stmt (if any) is "jump" target
        Node nextRealStmt = this.remaining.get(i + 1);
        if (isCompoundStmt(nextRealStmt)) {
          nextRealStmt = firstStatementInCompound(nextRealStmt);
        }
        if (nextRealStmt != null) {
          targets.add(nextRealStmt);
        }
      }
      stmt.getNextCFG().addAll(targets);
    }
  }

  private boolean isBreakOrContinue(Statement stmt) {
    return stmt instanceof BreakStatement || stmt instanceof ContinueStatement;
  }

  private void updateBreakContinueScopes(int index, Statement stmt) {
    if (isLoopingStmt(stmt)) {
      String label = ""; // TODO Handle labels

      // Determine begin (=continue location) of block
      Node beginOfScope = null;
      Node endOfScope = null;

      if (stmt instanceof DoStatement) {
        if (isCompoundStmt(((DoStatement) stmt).getStatement())) {
          beginOfScope = firstStatementInCompound(((DoStatement) stmt).getStatement());
          endOfScope = lastStatementInCompound(((DoStatement) stmt).getStatement());
        } else {
          beginOfScope = stmt;
          endOfScope = stmt;
        }
      } else if (stmt instanceof WhileStatement) {
        if (isCompoundStmt(((WhileStatement) stmt).getStatement())) {
          beginOfScope = firstStatementInCompound(((WhileStatement) stmt).getStatement());
          endOfScope = lastStatementInCompound(((WhileStatement) stmt).getStatement());
        } else {
          beginOfScope = stmt;
          endOfScope = stmt;
        }
      } // TODO Handle ForStatement

      Statement after = this.remaining.get(index + 1);
      BreakContinueScope scope = new BreakContinueScope(beginOfScope, endOfScope, after, label);
      this.breakContinueScopes.add(scope);
    }

    Optional<BreakContinueScope> oScope =
        this.breakContinueScopes.stream().filter(scope -> stmt.equals(scope.end)).findAny();

    oScope.ifPresent(breakContinueScope -> this.breakContinueScopes.remove(breakContinueScope));
  }

  private boolean isLoopingStmt(Statement stmt) {
    return stmt instanceof DoStatement
        || stmt instanceof WhileStatement; // TODO FOR statement missing
  }

  public void addTodo(int index, List<Statement> stmts) {
    if (index >= this.remaining.size()) {
      this.remaining.addAll(stmts);
    } else {
      this.remaining.addAll(index + 1, stmts);
    }
  }

  /**
   * Adds a statement to this.remaining, omitting null.
   *
   * @param index
   * @param stmts
   */
  public void addTodo(int index, Statement... stmts) {
    addTodo(index, Arrays.asList(stmts));
  }

  /**
   * Returns the first "real" (non-compound) statement in a CompoundStatement.
   *
   * @param stmt
   * @return
   */
  @Nullable
  private Node firstStatementInCompound(Node stmt) {
    if (!(stmt instanceof CompoundStatement)) {
      return stmt;
    }

    CompoundStatement comp = (CompoundStatement) stmt;
    Statement first = null;
    boolean found = false;

    while (!found) {
      // Get first statement in compound or return if does not exist.
      List<Statement> containedStmts = comp.getStatements();
      if (!containedStmts.isEmpty()) {
        first = containedStmts.get(0);
      } else {
        return null;
      }

      // If the stmt is again a (nested) compound, iterate. Otherwise return
      if (first instanceof CompoundStatement) {
        comp = (CompoundStatement) first;
      } else {
        found = true;
      }
    }
    return first;
  }

  /**
   * Returns the last "real" (non-compound) statement in a CompoundStatement.
   *
   * @param stmt
   * @return
   */
  @Nullable
  private Node lastStatementInCompound(Node stmt) {
    if (!(stmt instanceof CompoundStatement)) {
      return stmt;
    }

    CompoundStatement comp = (CompoundStatement) stmt;
    Statement last = null;
    boolean found = false;

    while (!found) {
      // Get first statement in compound or return if does not exist.
      List<Statement> containedStmts = comp.getStatements();
      if (!containedStmts.isEmpty()) {
        last = containedStmts.get(containedStmts.size() - 1);
      } else {
        return null;
      }

      // If the stmt is again a (nested) compound, iterate. Otherwise return
      if (last instanceof CompoundStatement) {
        comp = (CompoundStatement) last;
      } else {
        found = true;
      }
    }
    return last;
  }

  private List<Node> getJumpTargets(IfStatement stmt) {
    ArrayList<Node> result = new ArrayList<>();
    Node thenTarget = stmt.getThenStatement();
    if (isCompoundStmt(thenTarget)) {
      thenTarget = firstStatementInCompound(thenTarget);
    }
    if (thenTarget != null) {
      result.add(thenTarget);
    }

    Node elseTarget = stmt.getElseStatement();
    if (isCompoundStmt(elseTarget)) {
      elseTarget = firstStatementInCompound(elseTarget);
    }

    if (elseTarget != null) {
      result.add(elseTarget);
    }

    return result;
  }

  private boolean isIfStmt(Statement stmt) {
    return stmt instanceof IfStatement;
  }

  private boolean isCompoundStmt(Node stmt) {
    return stmt instanceof CompoundStatement;
  }

  private static class BreakContinueScope {
    Node start;
    Node end;
    String label;
    Node breakLocation;

    public BreakContinueScope(Node start, Node end, Node breakLocation, String label) {
      super();
      this.start = start;
      this.end = end;
      this.label = label;
      this.breakLocation = breakLocation;
    }

    @Override
    public String toString() {
      return this.label
          + ": "
          + this.start.getRegion().getStartLine()
          + " to "
          + this.end.getRegion().getStartLine()
          + ", will break to "
          + this.breakLocation.getRegion().getStartLine();
    }
  }
}
