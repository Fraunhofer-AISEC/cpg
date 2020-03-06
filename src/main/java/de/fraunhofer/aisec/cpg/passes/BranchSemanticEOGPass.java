package de.fraunhofer.aisec.cpg.passes;

import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.passes.scopes.LoopScope;
import de.fraunhofer.aisec.cpg.passes.scopes.SwitchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BranchSemanticEOGPass extends EvaluationOrderGraphPass {

  private static final Logger LOGGER = LoggerFactory.getLogger(BranchSemanticEOGPass.class);

  @Override
  protected void handleSynchronizedStatement(SynchronizedStatement synch) {

    createEOG(synch.getExpression());
    pushToEOG(synch);
    createEOG(synch.getBlockStatement());
  }

  @Override
  protected void handleConditionalExpression(ConditionalExpression conditionalExpression) {

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

  @Override
  protected void handleDoStatement(DoStatement doStatement) {
    lang.getScopeManager().enterScope(doStatement);

    createEOG(doStatement.getStatement());

    createEOG(doStatement.getCondition());
    pushToEOG(doStatement); // To have semantic information after the condition evaluation
    connectCurrentToLoopStart();
    LoopScope currentLoopScope = (LoopScope) lang.getScopeManager().leaveScope(doStatement);
    if (currentLoopScope != null) {
      exitLoop(doStatement, currentLoopScope);
    } else {
      LOGGER.error("Trying to exit do loop, but no loop scope: {}", doStatement.toString());
    }
  }

  @Override
  protected void handleForEachStatement(ForEachStatement forEachStatement) {

    lang.getScopeManager().enterScope(forEachStatement);

    createEOG(forEachStatement.getIterable());
    handleDeclaration(forEachStatement.getVariable());

    pushToEOG(forEachStatement); // To have semantic information after the variable declaration

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
  }

  @Override
  protected void handleForStatement(ForStatement forStatement) {

    lang.getScopeManager().enterScope(forStatement);
    ForStatement forStmt = (ForStatement) forStatement;

    createEOG(forStmt.getInitializerStatement());
    handleDeclaration(forStmt.getConditionDeclaration());
    createEOG(forStmt.getCondition());

    pushToEOG(forStatement); // To have semantic information after the condition evaluation

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
  }

  @Override
  protected void handleIfStatement(IfStatement ifStatement) {

    List<Node> openBranchNodes = new ArrayList<>();
    lang.getScopeManager().enterScope(ifStatement);
    createEOG(ifStatement.getInitializerStatement());
    handleDeclaration(ifStatement.getConditionDeclaration());
    createEOG(ifStatement.getCondition());

    pushToEOG(ifStatement); // To have semantic information after the condition evaluation
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
  }

  @Override
  protected void handleSwitchStatement(SwitchStatement switchStatement) {

    lang.getScopeManager().enterScope(switchStatement);

    createEOG(switchStatement.getInitializerStatement());

    handleDeclaration(switchStatement.getSelectorDeclaration());

    createEOG(switchStatement.selector);

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
  }

  @Override
  protected void handleWhileStatement(WhileStatement whileStatement) {
    lang.getScopeManager().enterScope(whileStatement);

    handleDeclaration(whileStatement.getConditionDeclaration());

    createEOG(whileStatement.getCondition());

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
      LOGGER.error("Trying to exit while loop, but no loop scope: {}", whileStatement.toString());
    }

    currentEOG.addAll(tmpEOGNodes);
  }
}
