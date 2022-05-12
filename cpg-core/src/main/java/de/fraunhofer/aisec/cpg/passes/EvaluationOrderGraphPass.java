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
package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.TranslationResult;
import de.fraunhofer.aisec.cpg.frontends.CallableInterface;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.*;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import de.fraunhofer.aisec.cpg.graph.statements.*;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker;
import de.fraunhofer.aisec.cpg.helpers.Util;
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
 * <p>An EOG is an intraprocedural directed graph whose vertices are executable AST nodes and edges
 * connect them in the order they would be executed when running the program.
 *
 * <p>An EOG always starts at the header of a method/function and ends in one (virtual) or multiple
 * return statements. A virtual return statement with a code location of (-1,-1) is used if the
 * actual source code does not have an explicit return statement.
 *
 * <p>The EOG is similar to the CFG {@code ControlFlowGraphPass}, but there are some subtle
 * differences:
 *
 * <ul>
 *   <li>For methods without explicit return statement, EOF will have an edge to a virtual return
 *       node with line number -1 which does not exist in the original code. A CFG will always end
 *       with the last reachable statement(s) and not insert any virtual return statements.
 *   <li>EOG considers an opening blocking ("CompoundStatement", indicated by a "{") as a separate
 *       node. A CFG will rather use the first actual executable statement within the block.
 *   <li>For IF statements, EOG treats the "if" keyword and the condition as separate nodes. CFG
 *       treats this as one "if" statement.
 *   <li>EOG considers a method header as a node. CFG will consider the first executable statement
 *       of the methods as a node.
 * </ul>
 */
public class EvaluationOrderGraphPass extends Pass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationOrderGraphPass.class);

  protected final Map<Class<? extends Node>, CallableInterface<? extends Node>> map =
      new HashMap<>();

  protected List<Node> currentEOG = new ArrayList<>();
  protected final EnumMap<Properties, Object> currentProperties = new EnumMap<>(Properties.class);

  // Some nodes will have no incoming nor outgoing edges but still need to be associated to the next
  // eog relevant node.
  protected final List<Node> intermediateNodes = new ArrayList<>();

  public EvaluationOrderGraphPass() {
    map.put(TranslationUnitDeclaration.class, this::handleTranslationUnitDeclaration);
    map.put(NamespaceDeclaration.class, this::handleNamespaceDeclaration);
    map.put(RecordDeclaration.class, this::handleRecordDeclaration);
    map.put(FunctionDeclaration.class, this::handleFunctionDeclaration);
    map.put(VariableDeclaration.class, this::handleVariableDeclaration);
    map.put(CallExpression.class, this::handleCallExpression);
    map.put(MemberExpression.class, this::handleMemberExpression);
    map.put(ArraySubscriptionExpression.class, this::handleArraySubscriptionExpression);
    map.put(ArrayCreationExpression.class, this::handleArrayCreationExpression);
    map.put(DeclarationStatement.class, this::handleDeclarationStatement);
    map.put(ReturnStatement.class, this::handleReturnStatement);
    map.put(BinaryOperator.class, this::handleBinaryOperator);
    map.put(UnaryOperator.class, this::handleUnaryOperator);
    map.put(CompoundStatement.class, this::handleCompoundStatement);
    map.put(CompoundStatementExpression.class, this::handleCompoundStatementExpression);
    map.put(IfStatement.class, this::handleIfStatement);
    map.put(AssertStatement.class, this::handleAssertStatement);
    map.put(WhileStatement.class, this::handleWhileStatement);
    map.put(DoStatement.class, this::handleDoStatement);
    map.put(ForStatement.class, this::handleForStatement);
    map.put(ForEachStatement.class, this::handleForEachStatement);
    map.put(TryStatement.class, this::handleTryStatement);
    map.put(ContinueStatement.class, this::handleContinueStatement);
    map.put(DeleteExpression.class, this::handleDeleteExpression);
    map.put(BreakStatement.class, this::handleBreakStatement);
    map.put(SwitchStatement.class, this::handleSwitchStatement);
    map.put(LabelStatement.class, this::handleLabelStatement);
    map.put(GotoStatement.class, this::handleGotoStatement);
    map.put(CaseStatement.class, this::handleCaseStatement);
    map.put(SynchronizedStatement.class, this::handleSynchronizedStatement);
    map.put(NewExpression.class, this::handleNewExpression);
    map.put(CastExpression.class, this::handleCastExpression);
    map.put(ExpressionList.class, this::handleExpressionList);
    map.put(ConditionalExpression.class, this::handleConditionalExpression);
    map.put(InitializerListExpression.class, this::handleInitializerListExpression);
    map.put(ConstructExpression.class, this::handleConstructExpression);
    map.put(EmptyStatement.class, this::handleDefault);
    map.put(Literal.class, this::handleDefault);
    map.put(UninitializedValue.class, this::handleDefault);
    map.put(DefaultStatement.class, this::handleDefault);
    map.put(TypeIdExpression.class, this::handleDefault);
    map.put(DeclaredReferenceExpression.class, this::handleDefault);
  }

  /**
   * Searches backwards in the EOG Graph on whether or not there is a path from a function
   * declaration to the given node. After the construction phase some unreachable nodes may have EOG
   * edges. This function also serves to truncate the EOG graph by unreachable paths.
   *
   * @param node - That lies on the reachable or unreachable path
   * @return true if the node can bea reached from a function declaration
   */
  protected static boolean reachableFromValidEOGRoot(@NonNull Node node) {
    Set<Node> passedBy = new HashSet<>();
    List<Node> workList = new ArrayList<>(node.getPrevEOG());
    while (!workList.isEmpty()) {
      Node toProcess = workList.get(0);
      workList.remove(toProcess);
      passedBy.add(toProcess);
      if (toProcess instanceof FunctionDeclaration) {
        return true;
      }
      for (Node pred : toProcess.getPrevEOG()) {
        if (!passedBy.contains(pred) && !workList.contains(pred)) {
          workList.add(pred);
        }
      }
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
    if (lang == null) {
      Util.errorWithFileLocation(result, log, "Could not create EOG: language frontend is null");

      return;
    }

    for (TranslationUnitDeclaration tu : result.getTranslationUnits()) {
      createEOG(tu);
      removeUnreachableEOGEdges(tu);
      // checkEOGInvariant(tu); To insert when trying to check if the invariant holds
    }
  }

  /**
   * Checks if every node that has another node in its next or previous EOG List is also contained
   * in that nodes previous or next EOG list to ensure the bidirectionality of the relation in both
   * lists.
   *
   * @param n
   * @return
   */
  public static boolean checkEOGInvariant(Node n) {
    List<Node> allNodes = SubgraphWalker.flattenAST(n);
    boolean ret = true;
    for (Node node : allNodes) {
      for (Node next : node.getNextEOG()) {
        if (!next.getPrevEOG().contains(node)) {
          LOGGER.warn(
              "Violation to EOG invariant found: Node {} does not have a backreference to his EOG-redecesor {}.",
              node,
              next);
          ret = false;
        }
      }
      for (Node prev : node.getPrevEOG()) {
        if (!prev.getNextEOG().contains(node)) {
          LOGGER.warn(
              "Violation to EOG invariant found: Node {} does not have a reference to his EOG-successor {}.",
              node,
              prev);
          ret = false;
        }
      }
    }
    return ret;
  }

  /**
   * Use with 'SubgraphWalker.flattenAST(tu).stream() .filter(node -> node.getPrevEOG().isEmpty() &&
   * !node.getNextEOG().isEmpty())' to eliminate edges starting from nodes that have no incoming
   * edge and are no function declarations. ======= To eliminate edges starting from nodes that have
   * no incoming edge and are no function declarations.
   */
  private void truncateLooseEdges(@NonNull List<Node> eogSources) {
    for (Node eogSourceNode : eogSources) {
      if (eogSourceNode instanceof FunctionDeclaration) {
        continue;
      }
      List<Node> nextNodes = new ArrayList<>(eogSourceNode.getNextEOG());
      eogSourceNode.clearNextEOG();
      nextNodes.forEach(node -> node.removePrevEOGEntry(eogSourceNode));
      truncateLooseEdges(
          nextNodes.stream()
              .filter(node -> node.getPrevEOG().isEmpty() && !node.getNextEOG().isEmpty())
              .collect(Collectors.toList()));
    }
  }

  /**
   * Removes EOG edges by first building the negative set of nodes that cannot be visited and then
   * remove there outgoing edges.In contrast to truncateLooseEdges this also removes cycles.
   */
  protected void removeUnreachableEOGEdges(@NonNull TranslationUnitDeclaration tu) {
    List<Node> eognodes =
        SubgraphWalker.flattenAST(tu).stream()
            .filter(node -> !(node.getPrevEOG().isEmpty() && node.getNextEOG().isEmpty()))
            .collect(Collectors.toList());
    Set<Node> validStarts =
        eognodes.stream()
            .filter(
                node ->
                    node instanceof FunctionDeclaration
                        || node instanceof RecordDeclaration
                        || node instanceof NamespaceDeclaration
                        || node instanceof TranslationUnitDeclaration)
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
      unvisitedNode
          .getNextEOGEdges()
          .forEach(next -> next.getEnd().removePrevEOGEntry(unvisitedNode));
      unvisitedNode.getNextEOGEdges().clear();
    }
  }

  protected void handleTranslationUnitDeclaration(@NonNull Node node) {
    TranslationUnitDeclaration declaration = (TranslationUnitDeclaration) node;

    handleStatementHolder((StatementHolder) node);

    // loop through functions
    for (Declaration child : declaration.getDeclarations()) {
      createEOG(child);
    }
    lang.clearProcessed();
  }

  protected void handleNamespaceDeclaration(@NonNull Node node) {
    NamespaceDeclaration declaration = (NamespaceDeclaration) node;

    handleStatementHolder(declaration);

    // loop through functions
    for (Declaration child : declaration.getDeclarations()) {
      createEOG(child);
    }
    lang.clearProcessed();
  }

  protected void handleVariableDeclaration(@NonNull Node node) {
    Declaration declaration = (Declaration) node;
    // analyze the initializer
    createEOG(((VariableDeclaration) declaration).getInitializer());
    pushToEOG(declaration);
  }

  protected void handleRecordDeclaration(@NonNull Node node) {
    RecordDeclaration declaration = (RecordDeclaration) node;
    lang.getScopeManager().enterScope(declaration);

    handleStatementHolder(declaration);

    this.currentEOG.clear();

    for (ConstructorDeclaration constructor : declaration.getConstructors()) {
      createEOG(constructor);
    }

    for (MethodDeclaration method : declaration.getMethods()) {
      createEOG(method);
    }

    for (RecordDeclaration records : declaration.getRecords()) {
      createEOG(records);
    }
    lang.getScopeManager().leaveScope(declaration);
  }

  protected void handleStatementHolder(StatementHolder statementHolder) {
    // separate code into static and non-static parts as they are executed in different moments
    // although they can be
    // be placed in the same enclosing declaration.
    List<Statement> code =
        statementHolder.getStatementsPropertyEdge().stream()
            .map(PropertyEdge.class::cast)
            .map(PropertyEdge::getEnd)
            .map(Statement.class::cast)
            .collect(Collectors.toList());

    List<Statement> nonStaticCode =
        code.stream()
            .filter(CompoundStatement.class::isInstance)
            .map(CompoundStatement.class::cast)
            .filter(block -> !block.isStaticBlock())
            .collect(Collectors.toList());
    List<Statement> staticCode =
        code.stream().filter(node -> !nonStaticCode.contains(node)).collect(Collectors.toList());

    pushToEOG((Node) statementHolder);
    for (Statement staticStatement : staticCode) {
      createEOG(staticStatement);
    }

    currentEOG.clear();
    pushToEOG((Node) statementHolder);

    for (Statement nonStaticStatement : nonStaticCode) {
      createEOG(nonStaticStatement);
    }

    currentEOG.clear();
  }

  protected void handleFunctionDeclaration(@NonNull Node node) {
    FunctionDeclaration funcDecl = (FunctionDeclaration) node;

    // reset EOG
    this.currentEOG.clear();

    var needToLeaveRecord = false;

    if (node instanceof MethodDeclaration
        && ((MethodDeclaration) node).getRecordDeclaration() != null
        && ((MethodDeclaration) node).getRecordDeclaration()
            != lang.getScopeManager().getCurrentRecord()) {
      // This is a method declaration outside of the AST of the record, as its possible in
      // languages, such as C++. Therefore we need to enter the record scope as well
      lang.getScopeManager().enterScope(((MethodDeclaration) node).getRecordDeclaration());
      needToLeaveRecord = true;
    }

    lang.getScopeManager().enterScope(funcDecl);
    // push the function declaration
    pushToEOG(funcDecl);

    // analyze the body
    if (funcDecl.hasBody()) {
      createEOG(funcDecl.getBody());
    }

    var currentScope = lang.getScopeManager().getCurrentScope();

    if (!(currentScope instanceof FunctionScope)) {
      Util.errorWithFileLocation(
          node,
          log,
          "Scope of function declaration is not a function scope. EOG of function might be incorrect.");
      // try to recover at least a little bit
      lang.getScopeManager().leaveScope(funcDecl);

      this.currentEOG.clear();
      return;
    }

    FunctionScope scope = (FunctionScope) currentScope;
    List<Node> uncaughtEOGThrows =
        scope.getCatchesOrRelays().values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    // Connect uncaught throws to block node
    addMultipleIncomingEOGEdges(uncaughtEOGThrows, funcDecl.getBody());

    lang.getScopeManager().leaveScope(funcDecl);

    if (node instanceof MethodDeclaration
        && ((MethodDeclaration) node).getRecordDeclaration() != null
        && needToLeaveRecord) {
      lang.getScopeManager().leaveScope(((MethodDeclaration) node).getRecordDeclaration());
    }

    // Set default argument evaluation nodes
    List<Node> funcDeclNextEOG = funcDecl.getNextEOG();
    this.currentEOG.clear();
    this.currentEOG.add(funcDecl);
    Expression defaultArg = null;

    for (ParamVariableDeclaration paramVariableDeclaration : funcDecl.getParameters()) {
      if (paramVariableDeclaration.getDefault() != null) {
        defaultArg = paramVariableDeclaration.getDefault();
        pushToEOG(defaultArg);
        this.currentEOG.clear();
        this.currentEOG.add(defaultArg);
        this.currentEOG.add(funcDecl);
      }
    }

    if (defaultArg != null) {
      for (Node nextEOG : funcDeclNextEOG) {
        this.currentEOG.clear();
        this.currentEOG.add(defaultArg);
        pushToEOG(nextEOG);
      }
    }

    this.currentEOG.clear();
  }

  protected void createEOG(@Nullable Node node) {
    if (node == null) {
      return;
    }
    this.intermediateNodes.add(node);
    Class<?> toHandle = node.getClass();
    CallableInterface callable = map.get(toHandle);
    while (callable == null) {
      toHandle = toHandle.getSuperclass();
      callable = map.get(toHandle);
      if (toHandle == Node.class || !Node.class.isAssignableFrom(toHandle)) break;
    }
    if (callable != null) {
      callable.dispatch(node);
    } else {
      LOGGER.info("Parsing of type " + node.getClass() + " is not supported (yet)");
    }
  }

  protected void handleDefault(@NonNull Node node) {
    pushToEOG(node);
  }

  protected void handleCallExpression(@NonNull Node node) {
    CallExpression callExpression = (CallExpression) node;

    // Todo add call as throwexpression to outer scope of call can throw (which is trivial to find
    // out for java, but impossible for c++)

    // evaluate base first, if there is one
    if (callExpression instanceof MemberCallExpression
        && callExpression.getBase() instanceof Statement) {
      createEOG(callExpression.getBase());
    }

    // first the arguments
    for (Expression arg : callExpression.getArguments()) {
      createEOG(arg);
    }
    // then the call itself
    pushToEOG(callExpression);
  }

  protected void handleMemberExpression(@NonNull Node node) {
    MemberExpression memberExpression = (MemberExpression) node;
    createEOG(memberExpression.getBase());
    pushToEOG(memberExpression);
  }

  protected void handleArraySubscriptionExpression(@NonNull Node node) {
    ArraySubscriptionExpression arraySubs = (ArraySubscriptionExpression) node;

    // Connect according to evaluation order, first the array reference, then the contained index.
    createEOG(arraySubs.getArrayExpression());
    createEOG(arraySubs.getSubscriptExpression());

    pushToEOG(arraySubs);
  }

  protected void handleArrayCreationExpression(@NonNull Node node) {
    ArrayCreationExpression arrayCreate = (ArrayCreationExpression) node;

    for (Expression dimension : arrayCreate.getDimensions()) {
      if (dimension != null) {
        createEOG(dimension);
      }
    }
    createEOG(arrayCreate.getInitializer());

    pushToEOG(arrayCreate);
  }

  protected void handleDeclarationStatement(@NonNull Node node) {
    DeclarationStatement declarationStatement = (DeclarationStatement) node;
    // loop through declarations
    for (Declaration declaration : declarationStatement.getDeclarations()) {
      if (declaration instanceof VariableDeclaration) {
        // analyze the initializers if there is one
        createEOG(declaration);
      } else if (declaration instanceof FunctionDeclaration) {
        // save the current EOG stack, because we can have a function declaration within an
        // existing function and the EOG handler for handling function declarations will reset the
        // stack
        var oldEOG = new ArrayList<>(this.currentEOG);

        // analyze the defaults
        createEOG(declaration);

        // reset the oldEOG stack
        this.currentEOG = oldEOG;
      }
    }

    // push statement itself
    pushToEOG(declarationStatement);
  }

  protected void handleReturnStatement(@NonNull Node node) {
    ReturnStatement returnStatement = (ReturnStatement) node;
    // analyze the return value
    createEOG(returnStatement.getReturnValue());

    // push the statement itself
    pushToEOG(returnStatement);

    // reset the state afterwards, we're done with this function
    currentEOG.clear();
  }

  protected void handleBinaryOperator(@NonNull Node node) {
    BinaryOperator binOp = (BinaryOperator) node;
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
    pushToEOG(binOp);
  }

  protected void handleCompoundStatement(@NonNull Node node) {
    CompoundStatement compoundStatement = (CompoundStatement) node;

    // not all language handle compound statements as scoping blocks, so we need to avoid creating
    // new scopes here
    lang.getScopeManager().enterScopeIfExists(compoundStatement);

    // analyze the contained statements
    for (Statement child : compoundStatement.getStatements()) {
      createEOG(child);
    }

    if (lang.getScopeManager().getCurrentScope() instanceof BlockScope) {
      lang.getScopeManager().leaveScope(compoundStatement);
    }

    pushToEOG(compoundStatement);
  }

  protected void handleUnaryOperator(@NonNull Node node) {
    UnaryOperator unaryOperator = (UnaryOperator) node;
    Expression input = unaryOperator.getInput();
    createEOG(input);
    if (unaryOperator.getOperatorCode().equals("throw")) {
      Type throwType;
      Scope catchingScope =
          lang.getScopeManager()
              .firstScopeOrNull(
                  scope -> scope instanceof TryScope || scope instanceof FunctionScope);

      if (input != null) {
        throwType = input.getType();
      } else {
        // do not check via instanceof, since we do not want to allow subclasses of
        // DeclarationScope here
        Scope decl =
            lang.getScopeManager()
                .firstScopeOrNull(scope -> scope.getClass().equals(ValueDeclarationScope.class));
        if (decl != null
            && decl.getAstNode() instanceof CatchClause
            && ((CatchClause) decl.getAstNode()).getParameter() != null) {
          VariableDeclaration param = ((CatchClause) decl.getAstNode()).getParameter();
          assert param != null;
          throwType = param.getType();
        } else {
          LOGGER.info("Unknown throw type, potentially throw; in a method");
          throwType = TypeParser.createFrom("UNKNOWN_THROW_TYPE", true);
        }
      }
      pushToEOG(unaryOperator);

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
      pushToEOG(unaryOperator);
    }
  }

  protected void handleCompoundStatementExpression(@NonNull Node node) {
    createEOG(((CompoundStatementExpression) node).getStatement());
    pushToEOG(node);
  }

  protected void handleAssertStatement(@NonNull Node node) {
    AssertStatement ifs = (AssertStatement) node;
    createEOG(ifs.getCondition());
    List<Node> openConditionEOGs = new ArrayList<>(currentEOG);
    createEOG(ifs.getMessage());
    setCurrentEOGs(openConditionEOGs);
    pushToEOG(node);
  }

  protected void handleTryStatement(@NonNull Node node) {
    TryStatement tryStatement = (TryStatement) node;
    lang.getScopeManager().enterScope(tryStatement);
    TryScope tryScope = (TryScope) lang.getScopeManager().getCurrentScope();
    TryStatement tryStmt = tryStatement;
    if (tryStmt.getResources() != null) {
      tryStmt.getResources().forEach(this::createEOG);
    }
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
            .firstScopeOrNull(
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
    lang.getScopeManager().leaveScope(tryStatement);
    // To Avoid edges out of the finally block to the next regular statement.
    if (!canTerminateExceptionfree) {
      currentEOG.clear();
    }
    pushToEOG(tryStatement);
  }

  protected void handleContinueStatement(@NonNull Node node) {
    pushToEOG(node);
    lang.getScopeManager().addContinueStatement((ContinueStatement) node);
    currentEOG.clear();
  }

  protected void handleDeleteExpression(@NonNull Node node) {
    createEOG(((DeleteExpression) node).getOperand());
    pushToEOG(node);
  }

  protected void handleBreakStatement(@NonNull Node node) {
    pushToEOG(node);
    lang.getScopeManager().addBreakStatement((BreakStatement) node);
    currentEOG.clear();
  }

  protected void handleLabelStatement(@NonNull Node node) {
    LabelStatement labelStatement = (LabelStatement) node;
    lang.getScopeManager().addLabelStatement(labelStatement);
    createEOG(labelStatement.getSubStatement());
  }

  protected void handleGotoStatement(@NonNull Node node) {
    GotoStatement gotoStatement = (GotoStatement) node;
    pushToEOG(gotoStatement);
    if (gotoStatement.getTargetLabel() != null) {
      lang.registerObjectListener(
          gotoStatement.getTargetLabel(), (from, to) -> addEOGEdge(gotoStatement, (Node) to));
    }
    currentEOG.clear();
  }

  protected void handleCaseStatement(@NonNull Node node) {
    createEOG(((CaseStatement) node).getCaseExpression());
    pushToEOG(node);
  }

  protected void handleNewExpression(@NonNull Node node) {
    NewExpression newStmt = (NewExpression) node;
    createEOG(newStmt.getInitializer());

    pushToEOG(node);
  }

  protected void handleCastExpression(@NonNull Node node) {
    CastExpression castExpr = (CastExpression) node;
    createEOG(castExpr.getExpression());
    pushToEOG(castExpr);
  }

  protected void handleExpressionList(@NonNull Node node) {
    ExpressionList exprList = (ExpressionList) node;
    for (Statement expr : exprList.getExpressions()) {
      createEOG(expr);
    }

    pushToEOG(exprList);
  }

  protected void handleInitializerListExpression(@NonNull Node node) {
    InitializerListExpression initList = (InitializerListExpression) node;

    // first the arguments
    for (Expression inits : initList.getInitializers()) {
      createEOG(inits);
    }

    pushToEOG(initList);
  }

  protected void handleConstructExpression(@NonNull Node node) {
    ConstructExpression constructExpr = (ConstructExpression) node;
    // first the arguments
    for (Expression arg : constructExpr.getArguments()) {
      createEOG(arg);
    }
    pushToEOG(constructExpr);
  }

  /**
   * Creates an EOG-edge between the given argument node and the saved currentEOG Edges.
   *
   * @param node node that gets the incoming edge
   */
  public void pushToEOG(@NonNull Node node) {
    LOGGER.trace("Pushing {} {} to EOG", node.getClass().getSimpleName(), node);
    for (Node intermediate : intermediateNodes) {
      lang.process(intermediate, node);
    }

    addMultipleIncomingEOGEdges(this.currentEOG, node);
    intermediateNodes.clear();
    this.currentEOG.clear();
    this.currentProperties.clear();
    this.currentEOG.add(node);
  }

  public List<Node> getCurrentEOG() {
    return this.currentEOG;
  }

  public void setCurrentEOG(List<Node> currentEOG) {
    this.currentEOG = currentEOG;
  }

  public void setCurrentEOG(Node node) {
    LOGGER.trace("Setting {} to EOG", node);

    this.currentEOG = new ArrayList<>();
    this.currentEOG.add(node);
  }

  public <T extends Node> void setCurrentEOGs(List<T> nodes) {
    LOGGER.trace("Setting {} to EOGs", nodes);

    this.currentEOG = new ArrayList<>(nodes);
  }

  public void addToCurrentEOG(List<Node> nodes) {
    LOGGER.trace("Adding {} to current EOG", nodes);

    this.currentEOG.addAll(nodes);
  }

  /**
   * Connects the current EOG leaf nodes to the last stacked node, e.g. loop head, and removes the
   * nodes.
   *
   * @param loopStatement the loop statement
   * @param loopScope the loop scope
   */
  protected void exitLoop(@NonNull Statement loopStatement, @NonNull LoopScope loopScope) {
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
  protected void connectCurrentToLoopStart() {
    if (lang == null) {
      // Avoid null checks in every if/else branch
      LOGGER.warn(
          "Skipping connection of EOG loop to start - no information about frontend available.");
      return;
    }

    LoopScope loopScope =
        (LoopScope) lang.getScopeManager().firstScopeOrNull(scope -> scope instanceof LoopScope);
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
  protected void addEOGEdge(Node prev, Node next) {
    PropertyEdge<Node> propertyEdge = new PropertyEdge<>(prev, next);
    propertyEdge.addProperties(this.currentProperties);
    propertyEdge.addProperty(Properties.INDEX, prev.getNextEOG().size());
    propertyEdge.addProperty(Properties.UNREACHABLE, false);
    prev.addNextEOG(propertyEdge);
    next.addPrevEOG(propertyEdge);
  }

  protected void addMultipleIncomingEOGEdges(List<Node> prevs, Node next) {
    prevs.forEach(prev -> addEOGEdge(prev, next));
  }

  protected void handleSynchronizedStatement(@NonNull Node node) {
    SynchronizedStatement synch = (SynchronizedStatement) node;
    createEOG(synch.getExpression());
    pushToEOG(synch);
    createEOG(synch.getBlockStatement());
  }

  protected void handleConditionalExpression(@NonNull Node node) {
    ConditionalExpression conditionalExpression = (ConditionalExpression) node;
    List<Node> openBranchNodes = new ArrayList<>();
    createEOG(conditionalExpression.getCondition());
    pushToEOG(conditionalExpression); // To have semantic information after the condition evaluation
    List<Node> openConditionEOGs = new ArrayList<>(currentEOG);
    createEOG(conditionalExpression.getThenExpr());
    openBranchNodes.addAll(currentEOG);

    setCurrentEOGs(openConditionEOGs);
    createEOG(conditionalExpression.getElseExpr());
    openBranchNodes.addAll(currentEOG);

    setCurrentEOGs(openBranchNodes);
  }

  protected void handleDoStatement(@NonNull Node node) {
    DoStatement doStatement = (DoStatement) node;
    lang.getScopeManager().enterScope(doStatement);

    createEOG(doStatement.getStatement());

    createEOG(doStatement.getCondition());

    doStatement.addPrevDFG(doStatement.getCondition());
    pushToEOG(doStatement); // To have semantic information after the condition evaluation

    connectCurrentToLoopStart();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(doStatement);
    if (currentLoopScope != null) {
      exitLoop(doStatement, currentLoopScope);
    } else {
      LOGGER.error("Trying to exit do loop, but no loop scope: {}", doStatement);
    }
  }

  protected void handleForEachStatement(@NonNull Node node) {
    ForEachStatement forEachStatement = (ForEachStatement) node;
    lang.getScopeManager().enterScope(forEachStatement);

    createEOG(forEachStatement.getIterable());
    createEOG(forEachStatement.getVariable());

    forEachStatement.addPrevDFG(forEachStatement.getVariable());
    pushToEOG(forEachStatement); // To have semantic information after the variable declaration

    List<Node> tmpEOGNodes = new ArrayList<>(currentEOG);

    createEOG(forEachStatement.getStatement());

    connectCurrentToLoopStart();
    currentEOG.clear();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(forEachStatement);
    if (currentLoopScope != null) {
      exitLoop(forEachStatement, currentLoopScope);
    } else {
      LOGGER.error("Trying to exit foreach loop, but not in loop scope: {}", forEachStatement);
    }

    currentEOG.addAll(tmpEOGNodes);
  }

  protected void handleForStatement(@NonNull Node node) {

    ForStatement forStatement = (ForStatement) node;
    lang.getScopeManager().enterScope(forStatement);
    ForStatement forStmt = forStatement;

    createEOG(forStmt.getInitializerStatement());
    createEOG(forStmt.getConditionDeclaration());
    createEOG(forStmt.getCondition());

    Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
        forStatement, forStatement.getCondition(), forStatement.getConditionDeclaration());

    pushToEOG(forStatement); // To have semantic information after the condition evaluation

    List<Node> tmpEOGNodes = new ArrayList<>(currentEOG);

    createEOG(forStmt.getStatement());
    createEOG(forStmt.getIterationStatement());

    connectCurrentToLoopStart();
    currentEOG.clear();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(forStatement);
    if (currentLoopScope != null) {
      exitLoop(forStatement, currentLoopScope);
    } else {
      LOGGER.error("Trying to exit for loop, but no loop scope: {}", forStatement);
    }

    currentEOG.addAll(tmpEOGNodes);
  }

  protected void handleIfStatement(@NonNull Node node) {
    IfStatement ifStatement = (IfStatement) node;
    List<Node> openBranchNodes = new ArrayList<>();
    lang.getScopeManager().enterScopeIfExists(ifStatement);
    createEOG(ifStatement.getInitializerStatement());
    createEOG(ifStatement.getConditionDeclaration());
    createEOG(ifStatement.getCondition());

    Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
        ifStatement, ifStatement.getCondition(), ifStatement.getConditionDeclaration());

    pushToEOG(ifStatement); // To have semantic information after the condition evaluation
    List<Node> openConditionEOGs = new ArrayList<>(currentEOG);
    currentProperties.put(de.fraunhofer.aisec.cpg.graph.edge.Properties.BRANCH, true);
    createEOG(ifStatement.getThenStatement());
    openBranchNodes.addAll(currentEOG);

    if (ifStatement.getElseStatement() != null) {
      setCurrentEOGs(openConditionEOGs);
      currentProperties.put(Properties.BRANCH, false);
      createEOG(ifStatement.getElseStatement());
      openBranchNodes.addAll(currentEOG);
    } else {
      openBranchNodes.addAll(openConditionEOGs);
    }

    lang.getScopeManager().leaveScope(ifStatement);

    setCurrentEOGs(openBranchNodes);
  }

  protected void handleSwitchStatement(@NonNull Node node) {
    SwitchStatement switchStatement = (SwitchStatement) node;
    lang.getScopeManager().enterScopeIfExists(switchStatement);

    createEOG(switchStatement.getInitializerStatement());

    createEOG(switchStatement.getSelectorDeclaration());

    createEOG(switchStatement.selector);

    Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
        switchStatement, switchStatement.getSelector(), switchStatement.getSelectorDeclaration());

    pushToEOG(switchStatement); // To have semantic information after the condition evaluation

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
      if (subStatement instanceof CaseStatement || subStatement instanceof DefaultStatement) {
        currentEOG.addAll(tmp);
      }
      createEOG(subStatement);
    }
    pushToEOG(compound);

    SwitchScope switchScope = (SwitchScope) lang.getScopeManager().leaveScope(switchStatement);
    if (switchScope != null) {
      this.currentEOG.addAll(switchScope.getBreakStatements());
    } else {
      LOGGER.error("Handling switch statement, but not in switch scope: {}", switchStatement);
    }
  }

  protected void handleWhileStatement(@NonNull Node node) {
    WhileStatement whileStatement = (WhileStatement) node;
    lang.getScopeManager().enterScope(whileStatement);

    createEOG(whileStatement.getConditionDeclaration());

    createEOG(whileStatement.getCondition());

    Util.addDFGEdgesForMutuallyExclusiveBranchingExpression(
        whileStatement, whileStatement.getCondition(), whileStatement.getConditionDeclaration());

    pushToEOG(whileStatement); // To have semantic information after the condition evaluation

    List<Node> tmpEOGNodes = new ArrayList<>(currentEOG);
    createEOG(whileStatement.getStatement());
    connectCurrentToLoopStart();

    // Replace current EOG nodes without triggering post setEOG ... processing
    currentEOG.clear();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(whileStatement);
    if (currentLoopScope != null) {
      exitLoop(whileStatement, currentLoopScope);
    } else {
      LOGGER.error("Trying to exit while loop, but no loop scope: {}", whileStatement);
    }

    currentEOG.addAll(tmpEOGNodes);
  }
}
