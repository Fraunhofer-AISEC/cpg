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
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.passes.scopes.*;
import java.util.*;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates an Evaluation Order Graph (EOG) based on AST.
 *
 * <p>EOG is similar to the CFG {@code ControlFlowGraphPass}, but its nodes are not limited to
 * *executable* statements but also include (some) *evaluated* expressions and CompoundStatements.
 * This leads to subtle differences:
 *
 * <ul>
 *   <li>For methods without explicit return statement, EOF will have an edge to a virtual return
 *       node with line number -1 which does not exist in the original code. In CFG, the last
 *       reachable statement(s) will not have any further nextCFG edges.
 *   <li>For IF statements, EOG treats the "if" keyword and the condition as separate nodes. CFG
 *       treats this as one "if" statement.
 *   <li>EOG considers an opening blocking ("CompoundStatement", indicated by a "{") as a separate
 *       node. CFG will rather use the first actual executable statement within the block.
 *   <li>EOG considers a method header as a node. CFG will consider the first executable statement
 *       of the methods as a node.
 * </ul>
 */
public class EvaluationOrderGraphPass extends Pass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationOrderGraphPass.class);

  protected List<Node> currentEOG = new ArrayList<>();

  // Some nodes will have no incoming nor outgoing edges but still need to be associated to the next
  // eog relevant node.
  protected List<Node> intermediateNodes = new ArrayList<>();

  /**
   * Searches backwards in the EOG Graph on whether or not there is a path from a function
   * declaration to the given node. After the construction phase some unreachable nodes may have EOG
   * edges. This function also serves to truncate the EOG graph by unreachable paths.
   *
   * @param node - That lies on the reachable or unreachable path
   * @return true if the node can bea reached from a function declaration
   */
  private static boolean reachableFromValidEOGRoot(@NonNull Node node) {
    Set<Node> passedBy = new HashSet<>();
    List<Node> workList = new ArrayList<>(node.getPrevEOG());
    while (!workList.isEmpty()) {
      Node toProcess = workList.get(0);
      workList.remove(toProcess);
      passedBy.add(toProcess);
      if (toProcess instanceof FunctionDeclaration) return true;
      for (Node pred : toProcess.getPrevEOG())
        if (!passedBy.contains(pred) && !workList.contains(pred)) workList.add(pred);
    }
    return false;
  }

  @Override
  public void cleanup() {
    this.intermediateNodes.clear();
    this.currentEOG.clear();
  }

  @Override
  public void accept(TranslationResult result) {
    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      handleDeclaration(tu);
      removeUnreachableEOGEdges(tu);
    }
  }

  /**
   * To eliminate edges starting from nodes that have no incoming
   * edge and are no function declarations.
   *
   * @param eogSources
   */
  protected void truncateLooseEdges(@NonNull List<Node> eogSources) {
    for (Node eogSourceNode : eogSources) {
      if (eogSourceNode instanceof FunctionDeclaration) continue;
      List<Node> nextNodes = new ArrayList<>(eogSourceNode.getNextEOG());
      eogSourceNode.getNextEOG().clear();
      nextNodes.forEach(node -> node.getPrevEOG().remove(eogSourceNode));
      truncateLooseEdges(
          nextNodes.stream()
              .filter(node -> node.getPrevEOG().isEmpty() && !node.getNextEOG().isEmpty())
              .collect(Collectors.toList()));
    }
  }

  /**
   * Removes EOG edges by first building the negative set of nodes that cannot be visited and then
   * remove there outgoing edges.In contrast to truncateLooseEdges this also removes cycles.
   *
   * @param tu
   */
  private void removeUnreachableEOGEdges(@NonNull TranslationUnitDeclaration tu) {
    List<Node> eognodes =
        SubgraphWalker.flattenAST(tu).stream()
            .filter(node -> !(node.getPrevEOG().isEmpty() && node.getNextEOG().isEmpty()))
            .collect(Collectors.toList());
    Set<Node> validStarts =
        eognodes.stream()
            .filter(node -> node instanceof FunctionDeclaration)
            .collect(Collectors.toSet());
    while (!validStarts.isEmpty()) {
      eognodes.removeAll(validStarts);
      validStarts =
          validStarts.stream()
              .flatMap(node -> node.getNextEOG().stream())
              .filter(eognodes::contains)
              .collect(Collectors.toSet());
    }
    // remaining eognodes were not visited and have to be removed from the EOG
    for (Node unvisitedNode : eognodes) {
      unvisitedNode.getNextEOG().forEach(next -> next.getPrevEOG().remove(unvisitedNode));
      unvisitedNode.getNextEOG().clear();
    }
  }

  /**
   * Handles declarations and is mainly used to propagate EOG construction to actually interesting
   * nodes.
   *
   * @param declaration
   */
  protected void handleDeclaration(@Nullable Declaration declaration) {
    if (declaration == null) {
      return;
    }
    if (lang == null) {
      // Avoid null checks in every if/else branch
      LOGGER.warn("Will not handle declaration - no information about frontend available.");
      return;
    }
    this.intermediateNodes.add(declaration);
    // todo FieldDeclarations have initializers that may be appropriate to
    // expressionRefersToDeclaration to the
    // constructor body over eog edges
    if (declaration instanceof TranslationUnitDeclaration) {
      // loop through functions
      for (Declaration child : ((TranslationUnitDeclaration) declaration).getDeclarations()) {
        handleDeclaration(child);
      }
      lang.clearProcessed();
    } else if (declaration instanceof RecordDeclaration) {

      lang.getScopeManager().enterScope(declaration);
      this.currentEOG.clear();
      for (ConstructorDeclaration constructor :
          ((RecordDeclaration) declaration).getConstructors()) {
        handleDeclaration(constructor);
      }

      for (MethodDeclaration method : ((RecordDeclaration) declaration).getMethods()) {
        handleDeclaration(method);
      }
      lang.getScopeManager().leaveScope(declaration);
    } else if (declaration instanceof FunctionDeclaration) {
      FunctionDeclaration funcDecl = (FunctionDeclaration) declaration;
      // reset EOG
      this.currentEOG.clear();

      lang.getScopeManager().enterScope(declaration);
      // push the function declaration
      pushToEOG(declaration);

      // analyze the body
      if (funcDecl.hasBody()) createEOG(((FunctionDeclaration) declaration).getBody());
      FunctionScope scope = ((FunctionScope) lang.getScopeManager().getCurrentScope());
      List<Node> uncaughtEOGThrows =
          scope.getCatchesOrRelays().values().stream()
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      // Connect uncaught throws to block node
      addMultipleIncomingEOGEdges(uncaughtEOGThrows, funcDecl.getBody());
      lang.getScopeManager().leaveScope(declaration);
    } else if (declaration instanceof VariableDeclaration) {
      // analyze the initializer
      createEOG(((VariableDeclaration) declaration).getInitializer());
      pushToEOG(declaration);
    } else {
      // In this case the ast -> cpg translation has to implement the cpg node creation
      pushToEOG(declaration);
    }
  }

  /**
   * Builds the EOG by explicitly adding edges. Every Statement that can contain EOG nodes, must
   * propagate constructions to the child nodes, e.g. If has to propagate to its CONDITION and to
   * both branches THENSTATEMENT and ELSESTATEMENT. EOG Edges are built in AST order and only with
   * first level statements, conditions, blocks, children of blocks etc. No sub-expressions of
   * statements themselves. The edges are added to the nextEOG member and thus only forward
   * exploration is possible.
   *
   * @param statement
   */
  protected void createEOG(@Nullable Statement statement) {
    if (statement == null) {
      return; // For null statements, and to avoid null checks in every if/else branch
    }

    if (lang == null) {
      // Avoid null checks in every if/else branch
      LOGGER.warn("Skipping EOG construction - no information about frontend available.");
      return;
    }
    this.intermediateNodes.add(statement);
    if (statement instanceof CallExpression) {
      CallExpression callExpression = (CallExpression) statement;

      // Todo add call as throwexpression to outer scope of call can throw (which is trivial to find
      // out for java, but impossible for c++)

      // evaluate base first, if there is one
      if (callExpression instanceof MemberCallExpression
          && ((MemberCallExpression) callExpression).getBase() instanceof Statement) {
        createEOG((Statement) ((MemberCallExpression) callExpression).getBase());
      }

      // first the arguments
      for (Expression arg : callExpression.getArguments()) {
        createEOG(arg);
      }

      // then the call itself
      pushToEOG(statement);

      // look, whether the function is known to us
      /*

      State state = State.getInstance();

       todo Reconsider if this is the right thing to do "Do we want to expressionRefersToDeclaration to the call target?
       todo We might not resolve the appropriate function". In addition the Return may better expressionRefersToDeclaration to the block
       todo root node instead of just leading to nowhere.
      functionDeclaration = state.findMethod(callExpression);
      if (functionDeclaration != null) {
        // expressionRefersToDeclaration call to function
        State.getInstance().addEOGEdge(callExpression, functionDeclaration);

        // expressionRefersToDeclaration all return statements of function to statement after call expression
        State.getInstance().setCurrentEOGs(functionDeclaration.getReturnStatements());
      }*/

    } else if (statement instanceof MemberExpression) {
      // analyze the base
      if (((MemberExpression) statement).getBase() instanceof Statement) {
        createEOG((Statement) ((MemberExpression) statement).getBase());
      }

      // analyze the member
      if (((MemberExpression) statement).getMember() instanceof Statement) {
        createEOG((Statement) ((MemberExpression) statement).getMember());
      }

      pushToEOG(statement);

    } else if (statement instanceof ArraySubscriptionExpression) {
      ArraySubscriptionExpression arraySubs = (ArraySubscriptionExpression) statement;

      // Connect according to evaluation order, first the array reference, then the contained index.
      createEOG(arraySubs.getArrayExpression());
      createEOG(arraySubs.getSubscriptExpression());

      pushToEOG(statement);

    } else if (statement instanceof ArrayCreationExpression) {
      ArrayCreationExpression arrayCreate = (ArrayCreationExpression) statement;

      for (Expression dimension : arrayCreate.getDimensions())
        if (dimension != null) createEOG(dimension);
      createEOG(arrayCreate.getInitializer());

      pushToEOG(statement);

    } else if (statement instanceof DeclarationStatement) {
      // loop through declarations
      for (Declaration declaration : ((DeclarationStatement) statement).getDeclarations()) {
        if (declaration instanceof VariableDeclaration) {
          // analyze the initializers if there is one
          handleDeclaration(declaration);
        }
      }

      // push statement itself
      pushToEOG(statement);

    } else if (statement instanceof ReturnStatement) {
      // analyze the return value
      createEOG(((ReturnStatement) statement).getReturnValue());

      // push the statement itself
      pushToEOG(statement);

      // reset the state afterwards, we're done with this function
      currentEOG.clear();

    } else if (statement instanceof BinaryOperator) {

      BinaryOperator binOp = (BinaryOperator) statement;
      createEOG(binOp.getLhs());

      List<Node> shortCircuitNodes = new ArrayList<>();

      // Two operators that don't evaluate the second operator if the first evaluates to a certain
      // value.
      if (binOp.getOperatorCode().equals("&&") || binOp.getOperatorCode().equals("||")) {
        shortCircuitNodes.addAll(currentEOG);
      }

      createEOG(binOp.getRhs());

      shortCircuitNodes.addAll(currentEOG);
      setCurrentEOGs(shortCircuitNodes);
      // push the statement itself
      pushToEOG(statement);

    } else if (statement instanceof UnaryOperator) {

      Expression input = ((UnaryOperator) statement).getInput();
      createEOG(input);
      if (((UnaryOperator) statement).getOperatorCode().equals("throw")) {
        Type throwType;
        Scope catchingScope =
            lang.getScopeManager()
                .getFirstScopeThat(
                    scope -> scope instanceof TryScope || scope instanceof FunctionScope);

        if (input != null) {
          throwType = input.getType();
        } else {
          // do not check via instanceof, since we do not want to allow subclasses of
          // DeclarationScope here
          Scope decl =
              lang.getScopeManager()
                  .getFirstScopeThat(scope -> scope.getClass().equals(DeclarationScope.class));
          if (decl != null
              && decl.getAstNode() instanceof CatchClause
              && ((CatchClause) decl.getAstNode()).getParameter() != null) {
            VariableDeclaration param = ((CatchClause) decl.getAstNode()).getParameter();
            assert param != null;
            throwType = param.getType();
          } else {
            LOGGER.info("Unknown throw type, potentially throw; in a method");
            throwType = new Type("UKNOWN_THROW_TYPE");
          }
        }
        pushToEOG(statement);

        if (catchingScope instanceof TryScope) {
          ((TryScope) catchingScope)
              .getCatchesOrRelays()
              .put(throwType, new ArrayList<>(this.currentEOG));

        } else if (catchingScope instanceof FunctionScope) {
          ((FunctionScope) catchingScope)
              .getCatchesOrRelays()
              .put(throwType, new ArrayList<>(this.currentEOG));
        }
        currentEOG.clear();
      } else {
        pushToEOG(statement);
      }

    } else if (statement instanceof CompoundStatement) {
      lang.getScopeManager().enterScope(statement);
      // analyze the contained statements
      for (Statement child : ((CompoundStatement) statement).getStatements()) {
        createEOG(child);
      }
      lang.getScopeManager().leaveScope(statement);
      pushToEOG(statement);

    } else if (statement instanceof CompoundStatementExpression) {
      createEOG(((CompoundStatementExpression) statement).getStatement());
      pushToEOG(statement);

    } else if (statement instanceof IfStatement) {
      handleIfStatement((IfStatement) statement);

    } else if (statement instanceof AssertStatement) {
      AssertStatement ifs = (AssertStatement) statement;
      createEOG(ifs.getCondition());
      List<Node> openConditionEOGs = new ArrayList<>(currentEOG);
      createEOG(ifs.getMessage());
      setCurrentEOGs(openConditionEOGs);
      pushToEOG(statement);

    } else if (statement instanceof WhileStatement) {
      handleWhileStatement((WhileStatement) statement);

    } else if (statement instanceof DoStatement) {
      handleDoStatement((DoStatement) statement);

    } else if (statement instanceof ForStatement) {
      handleForStatement((ForStatement) statement);
    } else if (statement instanceof ForEachStatement) {
      handleForEachStatement((ForEachStatement) statement);

    } else if (statement instanceof TryStatement) {
      lang.getScopeManager().enterScope(statement);
      TryScope tryScope = (TryScope) lang.getScopeManager().getCurrentScope();
      TryStatement tryStmt = (TryStatement) statement;

      if (tryStmt.getResources() != null) tryStmt.getResources().forEach(this::createEOG);
      createEOG(tryStmt.getTryBlock());

      List<Node> tmpEOGNodes = new ArrayList<>(currentEOG);

      Map<Type, List<Node>> catchesOrRelays = tryScope.getCatchesOrRelays();

      for (CatchClause catchClause : tryStmt.getCatchClauses()) {
        currentEOG.clear();
        // Try to catch all internally thrown exceptions under the catching clause and remove caught
        // ones
        HashSet<Type> toRemove = new HashSet<>();
        for (Map.Entry entry : catchesOrRelays.entrySet()) {
          Type throwType = (Type) entry.getKey();
          List<Node> eogEdges = (List<Node>) entry.getValue();
          if (catchClause.getParameter() == null) { // e.g. catch (...)
            currentEOG.addAll(eogEdges);
          } else if (TypeManager.getInstance()
              .isSupertypeOf(catchClause.getParameter().getType(), throwType)) {
            currentEOG.addAll(eogEdges);
            toRemove.add(throwType);
          }
        }
        toRemove.forEach(catchesOrRelays::remove);

        createEOG(catchClause.getBody());
        tmpEOGNodes.addAll(currentEOG);
      }
      boolean canTerminateExceptionfree =
          tmpEOGNodes.stream().anyMatch(EvaluationOrderGraphPass::reachableFromValidEOGRoot);

      currentEOG.clear();
      currentEOG.addAll(tmpEOGNodes);
      // connect all try-block, catch-clause and uncought throws eog points to finally start if
      // finally exists
      if (tryStmt.getFinallyBlock() != null) {
        // extends current EOG by all value EOG from open throws
        currentEOG.addAll(
            catchesOrRelays.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList()));
        createEOG(tryStmt.getFinallyBlock());

        //  all current-eog edges , result of finally execution as value List of uncought
        // catchesOrRelaysThrows
        for (Map.Entry entry : catchesOrRelays.entrySet()) {
          ((List) entry.getValue()).clear();
          ((List) entry.getValue()).addAll(this.currentEOG);
        }
      }
      // Forwards all open and uncaught throwing nodes to the outer scope that may handle them
      Scope outerScope =
          lang.getScopeManager()
              .getFirstScopeThat(
                  lang.getScopeManager().getCurrentScope().getParent(),
                  scope -> scope instanceof TryScope || scope instanceof FunctionScope);
      if (outerScope != null) {
        Map outerCatchesOrRelays =
            outerScope instanceof TryScope
                ? ((TryScope) outerScope).getCatchesOrRelays()
                : ((FunctionScope) outerScope).getCatchesOrRelays();
        for (Map.Entry entry : catchesOrRelays.entrySet()) {
          List<Node> catches =
              (List<Node>) outerCatchesOrRelays.getOrDefault(entry.getKey(), new ArrayList<Node>());
          catches.addAll((List<Node>) entry.getValue());
          outerCatchesOrRelays.put(entry.getKey(), catches);
        }
      }
      lang.getScopeManager().leaveScope(statement);
      // To Avoid edges out of the finally block to the next regular statement.
      if (!canTerminateExceptionfree) {
        currentEOG.clear();
      }

      pushToEOG(statement);

    } else if (statement instanceof ContinueStatement) {
      pushToEOG(statement);

      lang.getScopeManager().addContinueStatement((ContinueStatement) statement);

      currentEOG.clear();

    } else if (statement instanceof DeleteExpression) {

      createEOG(((DeleteExpression) statement).getOperand());
      pushToEOG(statement);

    } else if (statement instanceof BreakStatement) {
      pushToEOG(statement);

      lang.getScopeManager().addBreakStatement((BreakStatement) statement);

      currentEOG.clear();

    } else if (statement instanceof SwitchStatement) {
      handleSwitchStatement((SwitchStatement) statement);
    } else if (statement instanceof LabelStatement) {
      lang.getScopeManager().addLabelStatement((LabelStatement) statement);
      createEOG(((LabelStatement) statement).getSubStatement());
    } else if (statement instanceof GotoStatement) {
      GotoStatement gotoStatement = (GotoStatement) statement;
      pushToEOG(gotoStatement);
      if (gotoStatement.getTargetLabel() != null)
        lang.registerObjectListener(
            gotoStatement.getTargetLabel(), (from, to) -> addEOGEdge(gotoStatement, (Node) to));
      currentEOG.clear();
    } else if (statement instanceof CaseStatement) {
      createEOG(((CaseStatement) statement).getCaseExpression());
      pushToEOG(statement);
    } else if (statement instanceof SynchronizedStatement) {
      handleSynchronizedStatement((SynchronizedStatement) statement);
    } else if (statement instanceof EmptyStatement) {
      pushToEOG(statement);
    } else if (statement instanceof Literal) {
      pushToEOG(statement);
    } else if (statement instanceof DefaultStatement) {
      pushToEOG(statement);
    } else if (statement instanceof TypeIdExpression) {
      pushToEOG(statement);
    } else if (statement instanceof NewExpression) {
      NewExpression newStmt = (NewExpression) statement;
      createEOG(newStmt.getInitializer());

      pushToEOG(statement);
    } else if (statement instanceof CastExpression) {
      CastExpression castExpr = (CastExpression) statement;
      createEOG(castExpr.getExpression());
      pushToEOG(castExpr);
    } else if (statement instanceof ExpressionList) {
      ExpressionList exprList = (ExpressionList) statement;
      for (Statement expr : exprList.getExpressions()) createEOG(expr);

      pushToEOG(statement);
    } else if (statement instanceof ConditionalExpression) {
      handleConditionalExpression((ConditionalExpression) statement);
    } else if (statement instanceof InitializerListExpression) {
      InitializerListExpression initList = (InitializerListExpression) statement;

      // first the arguments
      for (Expression inits : initList.getInitializers()) {
        createEOG(inits);
      }

      pushToEOG(statement);
    } else if (statement instanceof ConstructExpression) {
      ConstructExpression constructExpr = (ConstructExpression) statement;

      // first the arguments
      for (Expression arg : constructExpr.getArguments()) {
        createEOG(arg);
      }

      pushToEOG(statement);
    } else if (statement instanceof DeclaredReferenceExpression) {
      pushToEOG(statement);
    } else {
      // In this case the ast -> cpg translation has to implement the cpg node creation
      pushToEOG(statement);
    }
  }

  protected void handleSynchronizedStatement(SynchronizedStatement synch) {
    createEOG(synch.getExpression());
    createEOG(synch.getBlockStatement());
    pushToEOG(synch);
  }

  protected void handleWhileStatement(WhileStatement whileStatement) {
    lang.getScopeManager().enterScope(whileStatement);

    handleDeclaration(whileStatement.getConditionDeclaration());

    createEOG(whileStatement.getCondition());
    List<Node> tmpEOGNodes = new ArrayList<>(currentEOG);
    createEOG(whileStatement.getStatement());
    connectCurrentToLoopStart();

    // Replace current EOG nodes without triggering post setEOG ... processing
    currentEOG.clear();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(whileStatement);
    if (currentLoopScope != null) {
      exitLoop(whileStatement, currentLoopScope);
    } else {
      LOGGER.error("Trying to exit while loop, but no loop scope: {}", whileStatement.toString());
    }

    currentEOG.addAll(tmpEOGNodes);

    pushToEOG(whileStatement); // Todo Remove root, if not wanted
  }

  protected void handleDoStatement(DoStatement doStatement) {
    lang.getScopeManager().enterScope(doStatement);

    createEOG(doStatement.getStatement());

    createEOG(doStatement.getCondition());
    connectCurrentToLoopStart();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(doStatement);
    if (currentLoopScope != null) {
      exitLoop(doStatement, currentLoopScope);
    } else {
      LOGGER.error("Trying to exit do loop, but no loop scope: {}", doStatement.toString());
    }

    pushToEOG(doStatement); // Todo Remove root, if not wanted
  }

  protected void handleIfStatement(IfStatement ifStatement) {

    List<Node> openBranchNodes = new ArrayList<>();
    lang.getScopeManager().enterScope(ifStatement);
    createEOG(ifStatement.getInitializerStatement());
    handleDeclaration(ifStatement.getConditionDeclaration());
    createEOG(ifStatement.getCondition());
    List<Node> openConditionEOGs = new ArrayList<>(currentEOG);
    createEOG(ifStatement.getThenStatement());
    openBranchNodes.addAll(currentEOG);

    if (ifStatement.getElseStatement() != null) {
      setCurrentEOGs(openConditionEOGs);
      createEOG(ifStatement.getElseStatement());
      openBranchNodes.addAll(currentEOG);
    } else openBranchNodes.addAll(openConditionEOGs);

    lang.getScopeManager().leaveScope(ifStatement);

    setCurrentEOGs(openBranchNodes);
    pushToEOG(ifStatement); // Todo Remove root, if not wanted
  }

  protected void handleSwitchStatement(SwitchStatement switchStatement) {

    lang.getScopeManager().enterScope(switchStatement);

    createEOG(switchStatement.getInitializerStatement());

    handleDeclaration(switchStatement.getSelectorDeclaration());

    createEOG(switchStatement.selector);

    CompoundStatement compound;
    List<Node> tmp = new ArrayList<>(currentEOG);
    if (switchStatement.getStatement() instanceof DoStatement) {
      createEOG(switchStatement.getStatement());
      compound = (CompoundStatement) ((DoStatement) switchStatement.getStatement()).getStatement();
    } else {
      compound = (CompoundStatement) switchStatement.getStatement();
    }
    currentEOG = new ArrayList<>();

    for (Statement subStatement : compound.getStatements()) {
      if (subStatement instanceof CaseStatement || subStatement instanceof DefaultStatement)
        currentEOG.addAll(tmp);
      createEOG(subStatement);
    }
    pushToEOG(compound);

    SwitchScope switchScope = (SwitchScope) lang.getScopeManager().leaveScope(switchStatement);
    if (switchScope != null) {
      this.currentEOG.addAll(switchScope.getBreakStatements());
    } else {
      LOGGER.error(
          "Handling switch statement, but not in switch scope: {}", switchStatement.toString());
    }

    pushToEOG(switchStatement);
  }

  protected void handleConditionalExpression(ConditionalExpression conditionalExpression) {

    List<Node> openBranchNodes = new ArrayList<>();
    createEOG(conditionalExpression.getCondition());
    List<Node> openConditionEOGs = new ArrayList<>(currentEOG);
    createEOG(conditionalExpression.getThenExpr());
    openBranchNodes.addAll(currentEOG);

    setCurrentEOGs(openConditionEOGs);
    createEOG(conditionalExpression.getElseExpr());
    openBranchNodes.addAll(currentEOG);

    setCurrentEOGs(openBranchNodes);
    pushToEOG(conditionalExpression); // Todo Remove root, if not wanted
  }

  protected void handleForStatement(ForStatement forStatement) {

    lang.getScopeManager().enterScope(forStatement);
    ForStatement forStmt = (ForStatement) forStatement;

    createEOG(forStmt.getInitializerStatement());
    handleDeclaration(forStmt.getConditionDeclaration());
    createEOG(forStmt.getCondition());

    List<Node> tmpEOGNodes = new ArrayList<>(currentEOG);

    createEOG(forStmt.getStatement());
    createEOG(forStmt.getIterationExpression());

    connectCurrentToLoopStart();
    currentEOG.clear();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(forStatement);
    if (currentLoopScope != null) {
      exitLoop(forStatement, currentLoopScope);
    } else {
      LOGGER.error("Trying to exit for loop, but no loop scope: {}", forStatement.toString());
    }

    currentEOG.addAll(tmpEOGNodes);

    pushToEOG(forStatement); // Todo Remove root, if not wanted
  }

  protected void handleForEachStatement(ForEachStatement forEachStatement) {

    lang.getScopeManager().enterScope(forEachStatement);

    createEOG(forEachStatement.getIterable());
    handleDeclaration(forEachStatement.getVariable());

    List<Node> tmpEOGNodes = new ArrayList<>(currentEOG);

    createEOG(forEachStatement.getStatement());

    connectCurrentToLoopStart();
    currentEOG.clear();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(forEachStatement);
    if (currentLoopScope != null) {
      exitLoop(forEachStatement, currentLoopScope);
    } else {
      LOGGER.error(
          "Trying to exit foreach loop, but not in loop scope: {}", forEachStatement.toString());
    }

    currentEOG.addAll(tmpEOGNodes);

    pushToEOG(forEachStatement); // Todo Remove root, if not wanted
  }

  public <T extends Node> void pushToEOG(@NonNull T node) {
    if (lang == null) {
      // Avoid null checks in every if/else branch
      LOGGER.warn("Not pushing to EOG - no information about frontend available.");
      return;
    }

    LOGGER.debug("Pushing {} {} to EOG", node.getClass().getSimpleName(), node);
    for (Node intermediate : intermediateNodes) {
      lang.process(intermediate, node);
    }
    addMultipleIncomingEOGEdges(this.currentEOG, node);
    intermediateNodes.clear();
    this.currentEOG.clear();
    this.currentEOG.add(node);
  }

  public List<Node> getCurrentEOG() {
    return this.currentEOG;
  }

  public void setCurrentEOG(List<Node> currentEOG) {
    this.currentEOG = currentEOG;
  }

  public void setCurrentEOG(Node node) {
    LOGGER.debug("Setting {} to EOG", node);

    this.currentEOG = new ArrayList<>();
    this.currentEOG.add(node);
  }

  public <T extends Node> void setCurrentEOGs(List<T> nodes) {
    LOGGER.debug("Setting {} to EOGs", nodes);

    this.currentEOG = new ArrayList<>(nodes);
  }

  public void addToCurrentEOG(List<Node> nodes) {
    LOGGER.debug("Adding {} to current EOG", nodes);

    this.currentEOG.addAll(nodes);
  }

  /**
   * Connects the current EOG leaf nodes to the last stacked node, e.g. loop head, and removes the
   * nodes.
   *
   * @param loopStatement the loop statement
   * @param loopScope the loop scope
   */
  public void exitLoop(@NonNull Statement loopStatement, @NonNull LoopScope loopScope) {
    // Breaks are connected to the NEXT EOG node and therefore temporarily stored after the loop
    // context is destroyed
    this.currentEOG.addAll(loopScope.getBreakStatements());

    List<Node> continues = new ArrayList<>(loopScope.getContinueStatements());
    if (!continues.isEmpty()) {
      Node condition;
      if (loopStatement instanceof DoStatement) {
        condition = ((DoStatement) loopStatement).getCondition();
      } else if (loopStatement instanceof ForStatement) {
        condition = ((ForStatement) loopStatement).getCondition();
      } else if (loopStatement instanceof ForEachStatement) {
        condition = loopStatement;
      } else if (loopStatement instanceof AssertStatement) {
        condition = loopStatement;
      } else {
        condition = ((WhileStatement) loopStatement).getCondition();
      }
      List<Node> conditions = SubgraphWalker.getEOGPathEdges(condition).getEntries();
      conditions.forEach(node -> addMultipleIncomingEOGEdges(continues, node));
    }
  }

  /**
   * Connects current EOG nodes to the previously saved loop start to mimic control flow of loops
   */
  public void connectCurrentToLoopStart() {
    if (lang == null) {
      // Avoid null checks in every if/else branch
      LOGGER.warn(
          "Skipping connection of EOG loop to start - no information about frontend available.");
      return;
    }

    LoopScope loopScope =
        (LoopScope) lang.getScopeManager().getFirstScopeThat(scope -> scope instanceof LoopScope);
    if (loopScope == null) {
      LOGGER.error("I am unexpectedly not in a loop, cannot add edge to loop start");
      return;
    }
    loopScope.starts().forEach(node -> addMultipleIncomingEOGEdges(this.currentEOG, node));
  }

  /**
   * Builds an EOG edge from prev to next. 'eogDirection' defines how the node instances save the
   * references constituting the edge. 'FORWARD': only the nodes nextEOG member contains references,
   * an points to the next nodes. 'BACKWARD': only the nodes prevEOG member contains references and
   * points to the previous nodes. 'BIDIRECTIONAL': nextEOG and prevEOG contain references and point
   * to the previous and the next nodes.
   *
   * @param prev the previous node
   * @param next the next node
   */
  public void addEOGEdge(Node prev, Node next) {
    prev.getNextEOG().add(next);
    next.getPrevEOG().add(prev);
  }

  public void addMultipleIncomingEOGEdges(List<Node> prevs, Node next) {
    prevs.forEach(prev -> addEOGEdge(prev, next));
  }
}
