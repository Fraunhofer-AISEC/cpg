/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  $$$$$$\  $$$$$$$\   $$$$$$\
 * $$  __$$\ $$  __$$\ $$  __$$\
 * $$ /  \__|$$ |  $$ |$$ /  \__|
 * $$ |      $$$$$$$  |$$ |$$$$\
 * $$ |      $$  ____/ $$ |\_$$ |
 * $$ |  $$\ $$ |      $$ |  $$ |
 * \$$$$$   |$$ |      \$$$$$   |
 *  \______/ \__|       \______/
 *
 */

package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.ArrayCreationExpression;
import de.fraunhofer.aisec.cpg.graph.ArraySubscriptionExpression;
import de.fraunhofer.aisec.cpg.graph.AssertStatement;
import de.fraunhofer.aisec.cpg.graph.BinaryOperator;
import de.fraunhofer.aisec.cpg.graph.BreakStatement;
import de.fraunhofer.aisec.cpg.graph.CallExpression;
import de.fraunhofer.aisec.cpg.graph.CaseStatement;
import de.fraunhofer.aisec.cpg.graph.CastExpression;
import de.fraunhofer.aisec.cpg.graph.CatchClause;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.CompoundStatementExpression;
import de.fraunhofer.aisec.cpg.graph.ConditionalExpression;
import de.fraunhofer.aisec.cpg.graph.ConstructExpression;
import de.fraunhofer.aisec.cpg.graph.ContinueStatement;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.DeclaredReferenceExpression;
import de.fraunhofer.aisec.cpg.graph.DefaultStatement;
import de.fraunhofer.aisec.cpg.graph.DeleteExpression;
import de.fraunhofer.aisec.cpg.graph.DoStatement;
import de.fraunhofer.aisec.cpg.graph.EmptyStatement;
import de.fraunhofer.aisec.cpg.graph.ExpressionList;
import de.fraunhofer.aisec.cpg.graph.ForEachStatement;
import de.fraunhofer.aisec.cpg.graph.ForStatement;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.GotoStatement;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.InitializerListExpression;
import de.fraunhofer.aisec.cpg.graph.LabelStatement;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.MemberCallExpression;
import de.fraunhofer.aisec.cpg.graph.MemberExpression;
import de.fraunhofer.aisec.cpg.graph.NewExpression;
import de.fraunhofer.aisec.cpg.graph.Node;
import de.fraunhofer.aisec.cpg.graph.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.SynchronizedStatement;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.TryStatement;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.TypeIdExpression;
import de.fraunhofer.aisec.cpg.graph.TypeManager;
import de.fraunhofer.aisec.cpg.graph.UnaryOperator;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.WhileStatement;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker;
import de.fraunhofer.aisec.cpg.passes.scopes.DeclarationScope;
import de.fraunhofer.aisec.cpg.passes.scopes.FunctionScope;
import de.fraunhofer.aisec.cpg.passes.scopes.LoopScope;
import de.fraunhofer.aisec.cpg.passes.scopes.Scope;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import de.fraunhofer.aisec.cpg.passes.scopes.SwitchScope;
import de.fraunhofer.aisec.cpg.passes.scopes.TryScope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
 *
 * @author julian and konrad
 */
public class EvaluationOrderGraphPass implements Pass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationOrderGraphPass.class);

  // Mapping from AST nodes to the first EOG node, important for
  // private Map<Node, Node> firstEOG = new HashMap<>();
  // private List<Node> openNodes =
  //    new ArrayList<>(); // All AST nodes that have been visited since adding the last EOG edge

  private List<Node> currentEOG = new ArrayList<>();

  // Some nodes will have no incoming nor outgoing edges but still need to be associated to the next
  // eog relevant node.
  private List<Node> intermediateNodes = new ArrayList<>();

  // TODO @KW: we need to remove lang here, as we might have multiple language frontends.
  //  Can we move the scopemanager somewhere else?
  private LanguageFrontend lang;
  private Map<Node, State> intermediateStates = new IdentityHashMap<>();

  private static class State {
    List<Node> tmpEOGNodes = new ArrayList<>();
  }

  private static class BranchState extends State {
    List<Node> openConditionEOGs = new ArrayList<>();
    List<Node> openBranchNodes = new ArrayList<>();
  }

  private static class TryState extends State {
    Map<Type, List<Node>> catchesOrRelays = new HashMap<>();
    boolean canTerminateExceptionfree = false;
  }

  private static class ParentState<P extends Node> extends State {
    P parent;
  }

  @Override
  public void cleanup() {
    this.intermediateNodes.clear();
    this.currentEOG.clear();
    this.lang = null;
  }

  @Override
  public LanguageFrontend getLang() {
    return lang;
  }

  @Override
  public void setLang(LanguageFrontend lang) {
    this.lang = lang;
  }

  public void setCurrentEOG(List<Node> currentEOG) {
    this.currentEOG = currentEOG;
  }

  @Override
  public void accept(TranslationResult result) {
    lang.setScopeManager(new ScopeManager(lang));
    ScopedWalker walker = new ScopedWalker();
    walker.setUnseenChildrenProvider(this::getEOGOrderedChildren);
    walker.registerOnNodeVisit(this::onEnter);
    walker.registerOnScopeExit(this::onExit);
    walker.registerOnChildExit(this::onChildExit);

    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      walker.iterate(tu);
      removeUnreachableEOGEdges(tu);
    }
  }

  private List<Node> getEOGOrderedChildren(Node current, Set<Node> seen) {
    List<Node> result = new ArrayList<>();
    if (current instanceof TranslationUnitDeclaration) {
      result.addAll(((TranslationUnitDeclaration) current).getDeclarations());
    } else if (current instanceof RecordDeclaration) {
      result.addAll(((RecordDeclaration) current).getConstructors());
      result.addAll(((RecordDeclaration) current).getMethods());
    } else if (current instanceof FunctionDeclaration) {
      result.add(((FunctionDeclaration) current).getBody());
    } else if (current instanceof VariableDeclaration) {
      result.add(((VariableDeclaration) current).getInitializer());
    } else if (current instanceof CallExpression) {
      // evaluate base first, if there is one
      if (current instanceof MemberCallExpression
          && ((MemberCallExpression) current).getBase() instanceof Statement) {
        result.add(((MemberCallExpression) current).getBase());
      }

      // first the arguments
      result.addAll(((CallExpression) current).getArguments());
    } else if (current instanceof MemberExpression) {
      result.add(((MemberExpression) current).getBase());
      result.add(((MemberExpression) current).getMember());
    } else if (current instanceof ArraySubscriptionExpression) {
      // Connect according to evaluation order, first the array reference, then the contained index.
      result.add(((ArraySubscriptionExpression) current).getArrayExpression());
      result.add(((ArraySubscriptionExpression) current).getSubscriptExpression());
    } else if (current instanceof ArrayCreationExpression) {
      result.addAll(((ArrayCreationExpression) current).getDimensions());
      result.add(((ArrayCreationExpression) current).getInitializer());
    } else if (current instanceof DeclarationStatement) {
      result.addAll(((DeclarationStatement) current).getDeclarations());
    } else if (current instanceof ReturnStatement) {
      result.add(((ReturnStatement) current).getReturnValue());
    } else if (current instanceof BinaryOperator) {
      result.add(((BinaryOperator) current).getLhs());
      result.add(((BinaryOperator) current).getRhs());
    } else if (current instanceof UnaryOperator) {
      result.add(((UnaryOperator) current).getInput());
    } else if (current instanceof CompoundStatement) {
      result.addAll(((CompoundStatement) current).getStatements());
    } else if (current instanceof CompoundStatementExpression) {
      result.add(((CompoundStatementExpression) current).getStatement());
    } else if (current instanceof IfStatement) {
      result.add(((IfStatement) current).getInitializerStatement());
      result.add(((IfStatement) current).getCondition());
      result.add(((IfStatement) current).getThenStatement());
      result.add(((IfStatement) current).getElseStatement());
    } else if (current instanceof AssertStatement) {
      result.add(((AssertStatement) current).getCondition());
      result.add(((AssertStatement) current).getMessage());
    } else if (current instanceof WhileStatement) {
      result.add(((WhileStatement) current).getConditionDeclaration());
      result.add(((WhileStatement) current).getCondition());
      result.add(((WhileStatement) current).getStatement());
    } else if (current instanceof DoStatement) {
      result.add(((DoStatement) current).getStatement());
      result.add(((DoStatement) current).getCondition());
    } else if (current instanceof ForStatement) {
      result.add(((ForStatement) current).getInitializerStatement());
      result.add(((ForStatement) current).getConditionDeclaration());
      result.add(((ForStatement) current).getCondition());
      result.add(((ForStatement) current).getStatement());
      result.add(((ForStatement) current).getIterationExpression());
    } else if (current instanceof ForEachStatement) {
      result.add(((ForEachStatement) current).getIterable());
      result.add(((ForEachStatement) current).getVariable());
      result.add(((ForEachStatement) current).getStatement());
    } else if (current instanceof TryStatement) {
      result.addAll(((TryStatement) current).getResources());
      result.add(((TryStatement) current).getTryBlock());
      result.addAll(((TryStatement) current).getCatchClauses());
      result.add(((TryStatement) current).getFinallyBlock());
    } else if (current instanceof DeleteExpression) {
      result.add(((DeleteExpression) current).getOperand());
    } else if (current instanceof SwitchStatement) {
      result.add(((SwitchStatement) current).getInitializerStatement());
      result.add(((SwitchStatement) current).getSelectorDeclaration());
      result.add(((SwitchStatement) current).getSelector());
      Statement statement = ((SwitchStatement) current).getStatement();
      CompoundStatement compound;
      if (statement instanceof DoStatement) {
        result.add(statement);
        compound = (CompoundStatement) ((DoStatement) statement).getStatement();
      } else {
        compound = (CompoundStatement) statement;
      }
      result.addAll(compound.getStatements());

      if (statement instanceof DoStatement) {
        compound = (CompoundStatement) ((DoStatement) statement).getStatement();
      } else {
        compound = (CompoundStatement) statement;
      }
      ParentState<SwitchStatement> parentState = new ParentState<>();
      parentState.parent = (SwitchStatement) current;
      for (Statement subStatement : compound.getStatements()) {
        if (subStatement instanceof CaseStatement || subStatement instanceof DefaultStatement) {
          // tell the case/default statements who their parent switch is
          intermediateStates.put(subStatement, parentState);
        }
      }
    } else if (current instanceof LabelStatement) {
      result.add(((LabelStatement) current).getSubStatement());
    } else if (current instanceof CaseStatement) {
      result.add(((CaseStatement) current).getCaseExpression());
    } else if (current instanceof SynchronizedStatement) {
      result.add(((SynchronizedStatement) current).getExpression());
      result.add(((SynchronizedStatement) current).getBlockStatement());
    } else if (current instanceof NewExpression) {
      result.add(((NewExpression) current).getInitializer());
    } else if (current instanceof CastExpression) {
      result.add(((CastExpression) current).getExpression());
    } else if (current instanceof ExpressionList) {
      result.addAll(((ExpressionList) current).getExpressions());
    } else if (current instanceof ConditionalExpression) {
      result.add(((ConditionalExpression) current).getCondition());
      result.add(((ConditionalExpression) current).getThenExpr());
      result.add(((ConditionalExpression) current).getElseExpr());
    } else if (current instanceof InitializerListExpression) {
      result.addAll(((InitializerListExpression) current).getInitializers());
    } else if (current instanceof ConstructExpression) {
      result.addAll(((ConstructExpression) current).getArguments());
    }

    return result.stream()
        .filter(Objects::nonNull)
        .filter(Predicate.not(seen::contains))
        .collect(Collectors.toList());
  }

  private void onEnter(Type currentClass, Node currentScope, Node current) {
    if (current == null) {
      return;
    }
    if (current instanceof CatchClause) {
      // need to do some tasks before handling the node. Get the prepared state to start
      TryStatement parent = ((ParentState<TryStatement>) intermediateStates.get(current)).parent;
      TryState state = (TryState) intermediateStates.get(parent);
      currentEOG.clear();
      // Try to catch all internally thrown exceptions under the catching clause and remove caught
      // ones
      HashSet<Type> toRemove = new HashSet<>();
      for (Map.Entry<Type, List<Node>> entry : state.catchesOrRelays.entrySet()) {
        Type throwType = entry.getKey();
        List<Node> eogEdges = entry.getValue();
        if (((CatchClause) current).getParameter() == null) { // e.g. catch (...)
          currentEOG.addAll(eogEdges);
        } else if (TypeManager.getInstance()
            .isSupertypeOf(((CatchClause) current).getParameter().getType(), throwType)) {
          currentEOG.addAll(eogEdges);
          toRemove.add(throwType);
        }
      }
      toRemove.forEach(state.catchesOrRelays::remove);
    } else if (current instanceof CaseStatement || current instanceof DefaultStatement) {
      SwitchStatement parent =
          ((ParentState<SwitchStatement>) intermediateStates.get(current)).parent;
      State state = intermediateStates.get(parent);
      currentEOG.addAll(state.tmpEOGNodes);
    }

    this.intermediateNodes.add(current);
    if (shouldEnterScope(current)) {
      lang.getScopeManager().enterScope(current);
    }

    if (current instanceof Declaration) {
      handleDeclaration((Declaration) current);
    } else if (current instanceof Statement) {
      handleStatement((Statement) current);
    }
  }

  private void handleDeclaration(Declaration current) {
    if (current instanceof TranslationUnitDeclaration || current instanceof RecordDeclaration) {
      // nothing to do here
    } else if (current instanceof FunctionDeclaration) {
      // reset EOG
      this.currentEOG.clear();
      // push the function declaration
      pushToEOG(current);
    } else {
      // In this case the ast -> cpg translation has to implement the cpg node creation
      pushToEOG(current);
    }
  }

  private void handleStatement(Statement current) {
    if (current instanceof ContinueStatement) {
      pushToEOG(current);
      lang.getScopeManager().addContinueStatment((ContinueStatement) current);
      currentEOG.clear();
    } else if (current instanceof BreakStatement) {
      pushToEOG(current);
      lang.getScopeManager().addBreakStatment((BreakStatement) current);
      currentEOG.clear();
    } else if (current instanceof LabelStatement) {
      lang.getScopeManager().addLabelStatement((LabelStatement) current);
    } else if (current instanceof GotoStatement) {
      GotoStatement gotoStatement = (GotoStatement) current;
      pushToEOG(gotoStatement);
      if (gotoStatement.getTargetLabel() != null)
        lang.registerObjectListener(
            gotoStatement.getTargetLabel(), (from, to) -> addEOGEdge(gotoStatement, (Node) to));
      currentEOG.clear();
    }
  }

  private void onChildExit(Node parent, Node child) {
    if (parent instanceof IfStatement) {
      if (!intermediateStates.containsKey(parent)) {
        intermediateStates.put(parent, new BranchState());
      }
      BranchState state = (BranchState) intermediateStates.get(parent);

      // see at which step we currently are
      if (child == ((IfStatement) parent).getCondition()) {
        state.openConditionEOGs = new ArrayList<>(currentEOG);
      } else if (child == ((IfStatement) parent).getThenStatement()) {
        state.openBranchNodes = new ArrayList<>(currentEOG);
        if (((IfStatement) parent).getElseStatement() == null) {
          state.openBranchNodes.addAll(state.openConditionEOGs);
        } else {
          setCurrentEOGs(state.openConditionEOGs);
        }
      } else if (child == ((IfStatement) parent).getElseStatement()) {
        state.openBranchNodes.addAll(currentEOG);
      }
    } else if (parent instanceof AssertStatement) {
      if (child == ((AssertStatement) parent).getCondition()) {
        if (!intermediateStates.containsKey(parent)) {
          intermediateStates.put(parent, new State());
        }
        State state = intermediateStates.get(parent);
        state.tmpEOGNodes = new ArrayList<>(currentEOG);
      }
    } else if (parent instanceof WhileStatement) {
      if (child == ((WhileStatement) parent).getCondition()) {
        if (!intermediateStates.containsKey(parent)) {
          intermediateStates.put(parent, new State());
        }
        State state = intermediateStates.get(parent);
        state.tmpEOGNodes = new ArrayList<>(currentEOG);
      }
    } else if (parent instanceof ForStatement) {
      if (!intermediateStates.containsKey(parent)) {
        intermediateStates.put(parent, new State());
      }
      if (child == ((ForStatement) parent).getCondition()) {
        State state = intermediateStates.get(parent);
        state.tmpEOGNodes = new ArrayList<>(currentEOG);
      } else if (((ForStatement) parent).getCondition() == null
          && child == ((ForStatement) parent).getConditionDeclaration()) {
        State state = intermediateStates.get(parent);
        state.tmpEOGNodes = new ArrayList<>(currentEOG);
      }
    } else if (parent instanceof ForEachStatement) {
      if (!intermediateStates.containsKey(parent)) {
        intermediateStates.put(parent, new State());
      }
      State state = intermediateStates.get(parent);
      if (child == ((ForEachStatement) parent).getVariable()) {
        state.tmpEOGNodes = new ArrayList<>(currentEOG);
      }
    } else if (parent instanceof TryStatement) {
      if (!intermediateStates.containsKey(parent)) {
        intermediateStates.put(parent, new TryState());
      }
      TryState state = (TryState) intermediateStates.get(parent);
      List<CatchClause> catchClauses = ((TryStatement) parent).getCatchClauses();

      if (child == ((TryStatement) parent).getTryBlock()) {
        state.tmpEOGNodes = new ArrayList<>(currentEOG);
        TryScope scope = (TryScope) lang.getScopeManager().getScopeOfStatment(parent);
        state.catchesOrRelays = scope.getCatchesOrRelays();

        // prepare states for catch clauses so that they know which try statement is their parent
        for (CatchClause clause : catchClauses) {
          ParentState<TryStatement> parentState = new ParentState<>();
          parentState.parent = (TryStatement) parent;
          intermediateStates.put(clause, parentState);
        }
      } else if (child instanceof CatchClause && catchClauses.contains(child)) {
        state.tmpEOGNodes.addAll(currentEOG);

        // was this the last one?
        if (catchClauses.indexOf(child) == catchClauses.size() - 1) {
          state.canTerminateExceptionfree =
              state.tmpEOGNodes.stream()
                  .anyMatch(EvaluationOrderGraphPass::reachableFromValidEOGRoot);

          currentEOG.clear();
          currentEOG.addAll(state.tmpEOGNodes);
          // connect all try-block, catch-clause and uncought throws eog points to finally start if
          // finally exists
          if (((TryStatement) parent).getFinallyBlock() != null) {
            // extends current EOG by all value EOG from open throws
            currentEOG.addAll(
                state.catchesOrRelays.entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toList()));
          }
        }
      } else if (child == ((TryStatement) parent).getFinallyBlock()) {
        //  all current-eog edges , result of finally execution as value List of uncought
        // catchesOrRelaysThrows
        for (Map.Entry<Type, List<Node>> entry : state.catchesOrRelays.entrySet()) {
          entry.getValue().clear();
          entry.getValue().addAll(this.currentEOG);
        }
      }
    } else if (parent instanceof SwitchStatement) {
      if (!intermediateStates.containsKey(parent)) {
        intermediateStates.put(parent, new State());
      }
      State state = intermediateStates.get(parent);

      if (child == ((SwitchStatement) parent).getSelector()) {
        state.tmpEOGNodes = new ArrayList<>(currentEOG);
        currentEOG = new ArrayList<>();
      }
    } else if (parent instanceof ConditionalExpression) {
      if (!intermediateStates.containsKey(parent)) {
        intermediateStates.put(parent, new BranchState());
      }
      BranchState state = (BranchState) intermediateStates.get(parent);

      // see at which step we currently are
      if (child == ((ConditionalExpression) parent).getCondition()) {
        state.openConditionEOGs = new ArrayList<>(currentEOG);
      } else if (child == ((ConditionalExpression) parent).getThenExpr()) {
        state.openBranchNodes = new ArrayList<>(currentEOG);
        setCurrentEOGs(state.openConditionEOGs);
      } else if (child == ((ConditionalExpression) parent).getElseExpr()) {
        state.openBranchNodes.addAll(currentEOG);
      }
    }
  }

  private void onExit(Node exiting) {
    if (exiting instanceof TranslationUnitDeclaration) {
      lang.clearProcessed();
    } else if (exiting instanceof FunctionDeclaration) {
      FunctionScope scope = ((FunctionScope) lang.getScopeManager().getCurrentScope());
      List<Node> uncaughtEOGThrows =
          scope.getCatchesOrRelays().values().stream()
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      // Connect uncaught throws to block node
      addMultipleIncomingEOGEdges(uncaughtEOGThrows, ((FunctionDeclaration) exiting).getBody());
    } else if (exiting instanceof VariableDeclaration
        || exiting instanceof ArrayCreationExpression
        || exiting instanceof ArraySubscriptionExpression
        || exiting instanceof BinaryOperator
        || exiting instanceof CallExpression
        || exiting instanceof CaseStatement
        || exiting instanceof CastExpression
        || exiting instanceof CompoundStatement
        || exiting instanceof CompoundStatementExpression
        || exiting instanceof ConstructExpression
        || exiting instanceof DeleteExpression
        || exiting instanceof EmptyStatement
        || exiting instanceof ExpressionList
        || exiting instanceof InitializerListExpression
        || exiting instanceof DeclarationStatement
        || exiting instanceof DeclaredReferenceExpression
        || exiting instanceof DefaultStatement
        || exiting instanceof Literal
        || exiting instanceof MemberExpression
        || exiting instanceof NewExpression
        || exiting instanceof SynchronizedStatement
        || exiting instanceof TypeIdExpression) {
      pushToEOG(exiting);
    } else if (exiting instanceof ReturnStatement) {
      pushToEOG(exiting);
      // we're done with this function
      currentEOG.clear();
    } else if (exiting instanceof UnaryOperator) {
      if (((UnaryOperator) exiting).getOperatorCode().equals("throw")) {
        Type throwType;
        Scope catchingScope =
            lang.getScopeManager()
                .getFirstScopeThat(
                    scope -> scope instanceof TryScope || scope instanceof FunctionScope);

        if (((UnaryOperator) exiting).getInput() != null) {
          throwType = ((UnaryOperator) exiting).getInput().getType();
        } else {
          // do not check via instanceof, since we do not want to allow subclasses of
          // DeclarationScope here
          Scope decl =
              lang.getScopeManager()
                  .getFirstScopeThat(scope -> scope.getClass().equals(DeclarationScope.class));
          if (decl != null
              && decl.getAstNode() instanceof CatchClause
              && ((CatchClause) decl.getAstNode()).getParameter() != null) {
            throwType = ((CatchClause) decl.getAstNode()).getParameter().getType();
          } else {
            LOGGER.info("Unknown throw type, potentially throw; in a method");
            throwType = new Type("UKNOWN_THROW_TYPE");
          }
        }

        pushToEOG(exiting);
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
        pushToEOG(exiting);
      }
    } else if (exiting instanceof IfStatement) {
      BranchState state = (BranchState) intermediateStates.get(exiting);
      setCurrentEOGs(state.openBranchNodes);
      pushToEOG(exiting); // Todo Remove root, if not wanted
    } else if (exiting instanceof ConditionalExpression) {
      BranchState state = (BranchState) intermediateStates.get(exiting);
      setCurrentEOGs(state.openBranchNodes);
      pushToEOG(exiting); // Todo Remove root, if not wanted
    } else if (exiting instanceof AssertStatement) {
      State state = intermediateStates.get(exiting);
      setCurrentEOGs(state.tmpEOGNodes);
      pushToEOG(exiting);
    } else if (exiting instanceof ForStatement) {
      State state = intermediateStates.get(exiting);
      connectCurrentToLoopStart();
      currentEOG.clear();
      exitLoop((Statement) exiting, (LoopScope) lang.getScopeManager().leaveScope(exiting));

      currentEOG.addAll(state.tmpEOGNodes);

      pushToEOG(exiting); // Todo Remove root, if not wanted
    } else if (exiting instanceof WhileStatement) {
      State state = intermediateStates.get(exiting);
      connectCurrentToLoopStart();

      // Replace current EOG nodes without triggering post setEOG ... processing
      currentEOG.clear();
      exitLoop((Statement) exiting, (LoopScope) lang.getScopeManager().leaveScope(exiting));

      currentEOG.addAll(state.tmpEOGNodes);

      pushToEOG(exiting); // Todo Remove root, if not wanted
    } else if (exiting instanceof DoStatement) {
      connectCurrentToLoopStart();
      exitLoop((Statement) exiting, (LoopScope) lang.getScopeManager().leaveScope(exiting));

      pushToEOG(exiting); // Todo Remove root, if not wanted
    } else if (exiting instanceof ForEachStatement) {
      State state = intermediateStates.get(exiting);
      connectCurrentToLoopStart();
      currentEOG.clear();
      exitLoop((Statement) exiting, (LoopScope) lang.getScopeManager().leaveScope(exiting));

      currentEOG.addAll(state.tmpEOGNodes);

      pushToEOG(exiting); // Todo Remove root, if not wanted
    } else if (exiting instanceof TryStatement) {
      TryState state = (TryState) intermediateStates.get(exiting);
      // Forwards all open and uncought throwing nodes to the outer scope that may handle them
      Scope outerScope =
          lang.getScopeManager()
              .getFirstScopeThat(
                  lang.getScopeManager().getCurrentScope().getParent(),
                  scope -> scope instanceof TryScope || scope instanceof FunctionScope);
      Map<Type, List<Node>> outerCatchesOrRelays =
          outerScope instanceof TryScope
              ? ((TryScope) outerScope).getCatchesOrRelays()
              : ((FunctionScope) outerScope).getCatchesOrRelays();
      for (Map.Entry<Type, List<Node>> entry : state.catchesOrRelays.entrySet()) {
        List<Node> catches =
            outerCatchesOrRelays.getOrDefault(entry.getKey(), new ArrayList<Node>());
        catches.addAll(entry.getValue());
        outerCatchesOrRelays.put(entry.getKey(), catches);
      }

      // To Avoid edges out of the finally block to the next regular statement.
      if (!state.canTerminateExceptionfree) {
        currentEOG.clear();
      }

      pushToEOG(exiting);
    } else if (exiting instanceof SwitchStatement) {
      SwitchScope switchScope = (SwitchScope) lang.getScopeManager().leaveScope(exiting);
      this.currentEOG.addAll(switchScope.getBreakStatements());

      pushToEOG(exiting);
    } else {
      // In this case the ast -> cpg translation has to implement the cpg node creation
      pushToEOG(exiting);
    }

    if (shouldExitScope(exiting)) {
      lang.getScopeManager().leaveScope(exiting);
    }
  }

  private boolean shouldEnterScope(Node node) {
    return node instanceof RecordDeclaration
        || node instanceof FunctionDeclaration
        || node instanceof CompoundStatement
        || node instanceof IfStatement
        || node instanceof WhileStatement
        || node instanceof DoStatement
        || node instanceof ForStatement
        || node instanceof ForEachStatement
        || node instanceof TryStatement
        || node instanceof SwitchStatement;
  }

  private boolean shouldExitScope(Node node) {
    return node instanceof RecordDeclaration
        || node instanceof FunctionDeclaration
        || node instanceof CompoundStatement
        || node instanceof TryStatement;
  }

  /**
   * Use with 'SubgraphWalker.flattenAST(tu).stream() .filter(node -> node.getPrevEOG().isEmpty() &&
   * !node.getNextEOG().isEmpty())' to eliminate edges starting from nodes that have no incoming
   * edge and are no function declarations.
   *
   * @param eogSources
   */
  private void truncateLooseEdges(List<Node> eogSources) {
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
  private void removeUnreachableEOGEdges(TranslationUnitDeclaration tu) {
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

  public <T extends Node> void pushToEOG(T node) {
    LOGGER.debug("Pushing {} {} to EOG", node.getClass().getSimpleName(), node);
    for (Node intermediate : intermediateNodes) lang.process(intermediate, node);
    addMultipleIncomingEOGEdges(this.currentEOG, node);
    intermediateNodes.clear();
    this.currentEOG.clear();
    this.currentEOG.add(node);
  }

  private static boolean reachableFromValidEOGRoot(Node node) {
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

  public List<Node> getCurrentEOG() {
    return this.currentEOG;
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
   */
  public void exitLoop(Statement loopStatement, LoopScope loopScope) {

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
    LoopScope loopScope =
        (LoopScope) lang.getScopeManager().getFirstScopeThat(scope -> scope instanceof LoopScope);
    loopScope.starts().forEach(node -> addMultipleIncomingEOGEdges(this.currentEOG, node));
  }

  /**
   * Builds an EOG edge from prev to next. 'eogDirection' defines how the node instances save the
   * references constituting the edge. 'FORWARD': only the nodes nextEOG member contains references,
   * an points to the next nodes. 'BACKWARD': only the nodes prevEOG member contains references and
   * points to the previous nodes. 'BIDIRECTIONAL': nextEOG and prevEOG contain references and point
   * to the previous and the next nodes.
   *
   * @param prev -> next
   * @param next <- prev
   */
  public void addEOGEdge(Node prev, Node next) {
    prev.getNextEOG().add(next);
    next.getPrevEOG().add(prev);
  }

  public void addMultipleIncomingEOGEdges(List<Node> prevs, Node next) {
    prevs.forEach(prev -> addEOGEdge(prev, next));
  }
}
