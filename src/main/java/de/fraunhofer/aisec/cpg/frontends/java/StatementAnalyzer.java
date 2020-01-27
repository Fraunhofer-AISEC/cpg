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

package de.fraunhofer.aisec.cpg.frontends.java;

import com.github.javaparser.JavaToken;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.utils.Pair;
import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.AssertStatement;
import de.fraunhofer.aisec.cpg.graph.BreakStatement;
import de.fraunhofer.aisec.cpg.graph.CaseStatement;
import de.fraunhofer.aisec.cpg.graph.CatchClause;
import de.fraunhofer.aisec.cpg.graph.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.ContinueStatement;
import de.fraunhofer.aisec.cpg.graph.Declaration;
import de.fraunhofer.aisec.cpg.graph.DeclarationStatement;
import de.fraunhofer.aisec.cpg.graph.DefaultStatement;
import de.fraunhofer.aisec.cpg.graph.DoStatement;
import de.fraunhofer.aisec.cpg.graph.EmptyStatement;
import de.fraunhofer.aisec.cpg.graph.ExplicitConstructorInvocation;
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.ExpressionList;
import de.fraunhofer.aisec.cpg.graph.ForEachStatement;
import de.fraunhofer.aisec.cpg.graph.ForStatement;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.LabelStatement;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.Region;
import de.fraunhofer.aisec.cpg.graph.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.SynchronizedStatement;
import de.fraunhofer.aisec.cpg.graph.TryStatement;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.UnaryOperator;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.WhileStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatementAnalyzer
    extends Handler<de.fraunhofer.aisec.cpg.graph.Statement, Statement, JavaLanguageFrontend> {

  private static final Logger log = LoggerFactory.getLogger(StatementAnalyzer.class);

  public StatementAnalyzer(JavaLanguageFrontend lang) {
    super(de.fraunhofer.aisec.cpg.graph.Statement::new, lang);

    map.put(IfStmt.class, this::handleIfStatement);
    map.put(AssertStmt.class, this::handleAssertStatement);
    map.put(WhileStmt.class, this::handleWhileStatement);
    map.put(DoStmt.class, this::handleDoStatement);
    map.put(ForEachStmt.class, this::handleForEachStatement);
    map.put(ForStmt.class, this::handleForStatement);
    map.put(BreakStmt.class, this::handleBreakStatement);
    map.put(ContinueStmt.class, this::handleContinueStatement);
    map.put(ReturnStmt.class, this::handleReturnStatement);
    map.put(BlockStmt.class, this::handleBlockStatement);
    map.put(LabeledStmt.class, this::handleLabelStatement);
    map.put(ExplicitConstructorInvocationStmt.class, this::handleExplicitConstructorInvocation);
    map.put(ExpressionStmt.class, this::handleExpressionStatement);
    map.put(SwitchStmt.class, this::handleSwitchStatement);
    map.put(EmptyStmt.class, this::handleEmptyStatement);
    map.put(SynchronizedStmt.class, this::handleSynchronizedStatement);
    map.put(TryStmt.class, this::handleTryStatement);
    map.put(ThrowStmt.class, this::handleThrowStmt);
  }

  public de.fraunhofer.aisec.cpg.graph.Statement handleExpressionStatement(Statement stmt) {
    return lang.getExpressionHandler().handle(stmt.asExpressionStmt().getExpression());
  }

  private de.fraunhofer.aisec.cpg.graph.Statement handleThrowStmt(Statement stmt) {
    ThrowStmt throwStmt = (ThrowStmt) stmt;
    UnaryOperator throwOperation =
        NodeBuilder.newUnaryOperator("throw", false, true, throwStmt.toString());
    throwOperation.setInput(
        (Expression) lang.getExpressionHandler().handle(throwStmt.getExpression()));
    return throwOperation;
  }

  private ReturnStatement handleReturnStatement(Statement stmt) {
    ReturnStmt returnStmt = stmt.asReturnStmt();

    Optional<com.github.javaparser.ast.expr.Expression> optionalExpression =
        returnStmt.getExpression();

    de.fraunhofer.aisec.cpg.graph.Expression expression = null;
    if (optionalExpression.isPresent()) {
      com.github.javaparser.ast.expr.Expression expr = optionalExpression.get();

      // handle the expression as the first argument
      expression = (Expression) lang.getExpressionHandler().handle(expr);
    }

    ReturnStatement returnStatement = NodeBuilder.newReturnStatement(returnStmt.toString());

    // expressionRefersToDeclaration to arguments, if there are any
    if (expression != null) {
      returnStatement.setReturnValue(expression);
    }
    lang.setCodeAndRegion(returnStatement, stmt);
    return returnStatement;
  }

  private IfStatement handleIfStatement(Statement stmt) {
    IfStmt ifStmt = stmt.asIfStmt();

    com.github.javaparser.ast.expr.Expression conditionExpression = ifStmt.getCondition();
    Statement thenStatement = ifStmt.getThenStmt();
    Optional<Statement> optionalElseStatement = ifStmt.getElseStmt();

    IfStatement ifStatement = NodeBuilder.newIfStatement(ifStmt.toString());
    lang.getScopeManager().enterScope(ifStatement);

    ifStatement.setThenStatement(handle(thenStatement));
    ifStatement.setCondition(
        (de.fraunhofer.aisec.cpg.graph.Expression)
            lang.getExpressionHandler().handle(conditionExpression));

    optionalElseStatement.ifPresent(statement -> ifStatement.setElseStatement(handle(statement)));

    lang.getScopeManager().leaveScope(ifStatement);
    return ifStatement;
  }

  private AssertStatement handleAssertStatement(Statement stmt) {
    AssertStmt assertStmt = stmt.asAssertStmt();

    com.github.javaparser.ast.expr.Expression conditionExpression = assertStmt.getCheck();
    Optional<com.github.javaparser.ast.expr.Expression> thenStatement = assertStmt.getMessage();

    AssertStatement assertStatement = NodeBuilder.newAssertStatement(stmt.toString());

    assertStatement.setCondition(
        (de.fraunhofer.aisec.cpg.graph.Expression)
            lang.getExpressionHandler().handle(conditionExpression));

    thenStatement.ifPresent(
        statement ->
            assertStatement.setMessage(lang.getExpressionHandler().handle(thenStatement.get())));

    return assertStatement;
  }

  private WhileStatement handleWhileStatement(Statement stmt) {
    WhileStmt whileStmt = stmt.asWhileStmt();

    com.github.javaparser.ast.expr.Expression conditionExpression = whileStmt.getCondition();
    Statement statement = whileStmt.getBody();

    WhileStatement whileStatement = NodeBuilder.newWhileStatement(whileStmt.toString());
    lang.getScopeManager().enterScope(whileStatement);

    whileStatement.setStatement(handle(statement));
    whileStatement.setCondition(
        (de.fraunhofer.aisec.cpg.graph.Expression)
            lang.getExpressionHandler().handle(conditionExpression));
    lang.getScopeManager().leaveScope(whileStatement);

    return whileStatement;
  }

  private ForEachStatement handleForEachStatement(Statement stmt) {
    ForEachStatement statement = NodeBuilder.newForEachStatement(stmt.toString());
    lang.getScopeManager().enterScope(statement);

    ForEachStmt forEachStmt = stmt.asForEachStmt();
    de.fraunhofer.aisec.cpg.graph.Statement var =
        lang.getExpressionHandler().handle(forEachStmt.getVariable());
    de.fraunhofer.aisec.cpg.graph.Statement iterable =
        lang.getExpressionHandler().handle(forEachStmt.getIterable());

    if (!(var instanceof DeclarationStatement
        && ((DeclarationStatement) var).isSingleDeclaration())) {
      log.error("more or unknown decl in foreach");
    } else {
      statement.setVariable(((DeclarationStatement) var).getSingleDeclaration());
    }

    statement.setIterable(iterable);
    statement.setStatement(handle(forEachStmt.getBody()));
    lang.getScopeManager().leaveScope(statement);
    return statement;
  }

  private ForStatement handleForStatement(Statement stmt) {

    ForStmt forStmt = stmt.asForStmt();
    String code;
    Optional<TokenRange> tokenRange = forStmt.getTokenRange();
    if (tokenRange.isPresent()) {
      code = tokenRange.get().toString();
    } else {
      code = stmt.toString();
    }
    ForStatement statement = NodeBuilder.newForStatement(code);
    lang.getScopeManager().enterScope(statement);

    if (forStmt.getInitialization().size() > 1) {
      // Include artificial Expressionlist and initializer statement.
      Region ofExprList = null;
      for (com.github.javaparser.ast.expr.Expression initExpr : forStmt.getInitialization()) {
        if (ofExprList == null) {
          ofExprList = lang.getRegionFromRawNode(initExpr);
        } else {
          ofExprList = lang.mergeRegions(ofExprList, lang.getRegionFromRawNode(initExpr));
        }
      }

      String initString =
          lang.getCodeOfSubregion(statement, lang.getRegionFromRawNode(stmt), ofExprList);
      ExpressionList initExprList = NodeBuilder.newExpressionList(initString);
      initExprList.setRegion(ofExprList);
      forStmt
          .getInitialization()
          .forEach(expr -> initExprList.addExpression(lang.getExpressionHandler().handle(expr)));
      statement.setInitializerStatement(initExprList);
    } else if (forStmt.getInitialization().size() == 1) {
      statement.setInitializerStatement(
          lang.getExpressionHandler().handle(forStmt.getInitialization().get(0)));
    }

    forStmt
        .getCompare()
        .ifPresent(
            condition ->
                statement.setCondition((Expression) lang.getExpressionHandler().handle(condition)));

    // Adds true expression node where default empty condition evaluates to true, remove here and in
    // cpp StatementHandler
    if (statement.getCondition() == null) {
      Literal literal = NodeBuilder.newLiteral(true, new Type("boolean"), "true");
      literal.setRegion(new Region());
      statement.setCondition(literal);
    }

    if (forStmt.getUpdate().size() > 1) {
      // Include artificial Expressionlist and initializer statement.
      Region ofExprList = null;
      for (com.github.javaparser.ast.expr.Expression initExpr : forStmt.getUpdate()) {
        if (ofExprList == null) {
          ofExprList = lang.getRegionFromRawNode(initExpr);
        } else {
          ofExprList = lang.mergeRegions(ofExprList, lang.getRegionFromRawNode(initExpr));
        }
      }

      String updateString =
          lang.getCodeOfSubregion(statement, lang.getRegionFromRawNode(stmt), ofExprList);
      ExpressionList updateExprList = NodeBuilder.newExpressionList(updateString);
      updateExprList.setRegion(ofExprList);
      forStmt
          .getUpdate()
          .forEach(
              expr ->
                  updateExprList.addExpression(
                      (Expression) lang.getExpressionHandler().handle(expr)));
      statement.setIterationExpression(updateExprList);
    } else if (forStmt.getUpdate().size() == 1) {
      statement.setIterationExpression(
          (Expression) lang.getExpressionHandler().handle(forStmt.getUpdate().get(0)));
    }

    statement.setStatement(handle(forStmt.getBody()));
    lang.getScopeManager().leaveScope(statement);
    return statement;
  }

  private DoStatement handleDoStatement(Statement stmt) {
    DoStmt doStmt = stmt.asDoStmt();

    com.github.javaparser.ast.expr.Expression conditionExpression = doStmt.getCondition();
    Statement statement = doStmt.getBody();

    DoStatement doStatement = NodeBuilder.newDoStatement(doStmt.toString());
    lang.getScopeManager().enterScope(doStatement);

    doStatement.setStatement(handle(statement));
    doStatement.setCondition(
        (de.fraunhofer.aisec.cpg.graph.Expression)
            lang.getExpressionHandler().handle(conditionExpression));
    lang.getScopeManager().leaveScope(doStatement);
    return doStatement;
  }

  private EmptyStatement handleEmptyStatement(Statement stmt) {
    EmptyStmt emptyStmt = stmt.asEmptyStmt();
    return NodeBuilder.newEmptyStatement(emptyStmt.toString());
  }

  private SynchronizedStatement handleSynchronizedStatement(Statement stmt) {
    SynchronizedStmt synchronizedJava = stmt.asSynchronizedStmt();
    SynchronizedStatement synchronizedCPG = NodeBuilder.newSynchronizedStatement(stmt.toString());
    synchronizedCPG.setExpression(
        (Expression) lang.getExpressionHandler().handle(synchronizedJava.getExpression()));
    synchronizedCPG.setBlockStatement((CompoundStatement) handle(synchronizedJava.getBody()));
    return synchronizedCPG;
  }

  private LabelStatement handleLabelStatement(Statement stmt) {
    LabeledStmt labelStmt = stmt.asLabeledStmt();

    String label = labelStmt.getLabel().getIdentifier();
    Statement statement = labelStmt.getStatement();

    LabelStatement labelStatement = NodeBuilder.newLabelStatement(labelStmt.toString());

    labelStatement.setSubStatement(handle(statement));
    labelStatement.setLabel(label);

    return labelStatement;
  }

  private BreakStatement handleBreakStatement(Statement stmt) {
    BreakStmt breakStmt = stmt.asBreakStmt();
    BreakStatement breakStatement = new BreakStatement();
    breakStmt.getLabel().ifPresent(label -> breakStatement.setLabel(label.toString()));

    return breakStatement;
  }

  private ContinueStatement handleContinueStatement(Statement stmt) {
    ContinueStmt continueStmt = stmt.asContinueStmt();
    ContinueStatement continueStatement = new ContinueStatement();
    continueStmt.getLabel().ifPresent(label -> continueStatement.setLabel(label.toString()));

    return continueStatement;
  }

  public CompoundStatement handleBlockStatement(Statement stmt) {
    BlockStmt blockStmt = stmt.asBlockStmt();

    // first of, all we need a compound statement
    CompoundStatement compoundStatement = NodeBuilder.newCompoundStatement(stmt.toString());

    lang.getScopeManager().enterScope(compoundStatement);

    for (Statement child : blockStmt.getStatements()) {
      de.fraunhofer.aisec.cpg.graph.Statement statement = handle(child);

      compoundStatement.getStatements().add(statement);
    }
    lang.setCodeAndRegion(compoundStatement, stmt);

    lang.getScopeManager().leaveScope(compoundStatement);
    return compoundStatement;
  }

  public de.fraunhofer.aisec.cpg.graph.Statement handleCaseDefaultStatement(
      com.github.javaparser.ast.expr.Expression caseExpression, SwitchEntry sEntry) {

    Optional<TokenRange> optionalTokenRange = sEntry.getTokenRange();
    Pair<JavaToken, JavaToken> caseTokens = null;
    if (optionalTokenRange.isEmpty()) {
      caseTokens = new Pair<>(null, null);
      log.error("Token for Region for Default case not available");
    }

    if (caseExpression == null) {
      if (optionalTokenRange.isPresent()) {
        // Compute region and code for self generated default statement to match the c++ versions
        caseTokens =
            getOuterTokensWithText(
                "default",
                ":",
                optionalTokenRange.get().getBegin(),
                optionalTokenRange.get().getEnd());
      }
      DefaultStatement defaultStatement =
          NodeBuilder.newDefaultStatement(getCodeBetweenTokens(caseTokens.a, caseTokens.b));
      defaultStatement.setRegion(getRegionFromTokens(caseTokens.a, caseTokens.b));
      return defaultStatement;
    }

    if (optionalTokenRange.isPresent()) {
      // Compute region and code for self generated case statement to match the c++ versions
      caseTokens =
          getOuterTokensWithText(
              "case", ":", optionalTokenRange.get().getBegin(), optionalTokenRange.get().getEnd());
    }
    CaseStatement caseStatement =
        NodeBuilder.newCaseStatement(getCodeBetweenTokens(caseTokens.a, caseTokens.b));
    caseStatement.setCaseExpression(
        (Expression) lang.getExpressionHandler().handle(caseExpression));

    caseStatement.setRegion(getRegionFromTokens(caseTokens.a, caseTokens.b));

    return caseStatement;
  }

  public JavaToken getPreviousTokenWith(String text, JavaToken token) {
    Optional<JavaToken> optional = token.getPreviousToken();
    while (token.getText().equals(text) && optional.isPresent()) {
      token = optional.get();
      optional = token.getPreviousToken();
    }
    return token;
  }

  public JavaToken getNextTokenWith(String text, JavaToken token) {
    Optional<JavaToken> optional = token.getNextToken();
    while (token.getText().equals(text) && optional.isPresent()) {
      token = optional.get();
      optional = token.getNextToken();
    }
    return token;
  }

  public Pair<JavaToken, JavaToken> getOuterTokensWithText(
      String startDelim, String endDelim, JavaToken start, JavaToken end) {
    return new Pair<>(getPreviousTokenWith(startDelim, start), getNextTokenWith(endDelim, end));
  }

  public Region getRegionFromTokens(JavaToken startToken, JavaToken endToken) {
    if (startToken != null && endToken != null) {
      Optional<Range> startOpt = startToken.getRange();
      Optional<Range> endOpt = endToken.getRange();
      if (startOpt.isPresent() && endOpt.isPresent()) {
        Range rstart = startOpt.get();
        Range rend = endOpt.get();
        return new Region(
            rstart.begin.line, rstart.begin.column, rend.end.line, rend.end.column + 1);
      }
    }
    return new Region();
  }

  public String getCodeBetweenTokens(JavaToken startToken, JavaToken endToken) {
    if (startToken == null || endToken == null) {
      return Type.UNKNOWN_TYPE_STRING;
    }
    StringBuilder newCode = new StringBuilder(startToken.getText());
    JavaToken current = startToken;
    do {
      current = current.getNextToken().orElse(null);
      if (current == null) {
        break;
      }
      newCode.append(current.getText());
    } while (current != endToken);
    return newCode.toString();
  }

  public SwitchStatement handleSwitchStatement(Statement stmt) {
    SwitchStmt switchStmt = stmt.asSwitchStmt();
    SwitchStatement switchStatement = NodeBuilder.newSwitchStatement(stmt.toString());

    lang.getScopeManager().enterScope(switchStatement);

    switchStatement.setSelector(
        (Expression) lang.getExpressionHandler().handle(switchStmt.getSelector()));

    // Compute region and code for self generated compound statement to match the c++ versions
    JavaToken start = null;
    JavaToken end = null;
    Optional<TokenRange> tokenRange = switchStmt.getTokenRange();
    Optional<TokenRange> tokenRangeSelector = switchStmt.getSelector().getTokenRange();
    if (tokenRange.isPresent() && tokenRangeSelector.isPresent()) {
      start = getNextTokenWith("{", tokenRangeSelector.get().getEnd());
      end = getPreviousTokenWith("}", tokenRange.get().getEnd());
    }

    CompoundStatement compoundStatement =
        NodeBuilder.newCompoundStatement(getCodeBetweenTokens(start, end));
    compoundStatement.setRegion(getRegionFromTokens(start, end));

    for (SwitchEntry sentry : switchStmt.getEntries()) {

      if (sentry.getLabels().isEmpty()) {
        compoundStatement.getStatements().add(handleCaseDefaultStatement(null, sentry));
      }

      for (com.github.javaparser.ast.expr.Expression caseExp : sentry.getLabels()) {
        compoundStatement.getStatements().add(handleCaseDefaultStatement(caseExp, sentry));
      }

      for (Statement subStmt : sentry.getStatements()) {
        compoundStatement.getStatements().add(handle(subStmt));
      }
    }
    switchStatement.setStatement(compoundStatement);
    lang.getScopeManager().leaveScope(switchStatement);
    return switchStatement;
  }

  private ExplicitConstructorInvocation handleExplicitConstructorInvocation(Statement stmt) {
    ExplicitConstructorInvocationStmt eciStatement = stmt.asExplicitConstructorInvocationStmt();
    String containingClass;
    try {
      containingClass = eciStatement.resolve().declaringType().getQualifiedName();
    } catch (RuntimeException | NoClassDefFoundError e) {
      containingClass = lang.recoverTypeFromUnsolvedException(e);
      // base can be null here
    }

    ExplicitConstructorInvocation node =
        NodeBuilder.newExplicitConstructorInvocation(containingClass, eciStatement.toString());

    List<Expression> arguments =
        eciStatement.getArguments().stream()
            .map(lang.getExpressionHandler()::handle)
            .map(Expression.class::cast)
            .collect(Collectors.toList());
    node.setArguments(arguments);

    return node;
  }

  private TryStatement handleTryStatement(Statement stmt) {
    TryStmt tryStmt = stmt.asTryStmt();
    TryStatement tryStatement = NodeBuilder.newTryStatement(stmt.toString());
    lang.getScopeManager().enterScope(tryStatement);
    List<de.fraunhofer.aisec.cpg.graph.Statement> resources =
        tryStmt.getResources().stream()
            .map(lang.getExpressionHandler()::handle)
            .collect(Collectors.toList());
    CompoundStatement tryBlock = handleBlockStatement(tryStmt.getTryBlock());
    List<CatchClause> catchClauses =
        tryStmt.getCatchClauses().stream()
            .map(this::handleCatchClause)
            .collect(Collectors.toList());
    CompoundStatement finallyBlock =
        tryStmt.getFinallyBlock().map(this::handleBlockStatement).orElse(null);
    lang.getScopeManager().leaveScope(tryStatement);
    tryStatement.setResources(resources);
    tryStatement.setTryBlock(tryBlock);
    tryStatement.setFinallyBlock(finallyBlock);
    tryStatement.setCatchClauses(catchClauses);

    for (de.fraunhofer.aisec.cpg.graph.Statement r : resources) {
      if (r instanceof DeclarationStatement) {
        for (Declaration d : ((DeclarationStatement) r).getDeclarations()) {
          if (d instanceof VariableDeclaration) {
            lang.getScopeManager().addValueDeclaration((VariableDeclaration) d);
          }
        }
      }
    }
    return tryStatement;
  }

  private CatchClause handleCatchClause(com.github.javaparser.ast.stmt.CatchClause catchCls) {
    CatchClause cClause = NodeBuilder.newCatchClause(catchCls.toString());
    lang.getScopeManager().enterScope(cClause);

    HashSet<Type> possibleTypes = new HashSet<>();
    Type concreteType;
    if (catchCls.getParameter().getType() instanceof UnionType) {
      for (ReferenceType t : ((UnionType) catchCls.getParameter().getType()).getElements()) {
        possibleTypes.add(lang.getTypeAsGoodAsPossible(t));
      }
      // we do not know which of the exceptions was actually thrown, so we assume this might be any
      concreteType = new Type("java.lang.Throwable", Type.Origin.GUESSED);
    } else {
      concreteType = lang.getTypeAsGoodAsPossible(catchCls.getParameter().getType());
      possibleTypes.add(concreteType);
    }

    VariableDeclaration parameter =
        NodeBuilder.newVariableDeclaration(
            catchCls.getParameter().getName().toString(),
            concreteType,
            catchCls.getParameter().toString());
    parameter.setPossibleSubTypes(possibleTypes);
    CompoundStatement body = handleBlockStatement(catchCls.getBody());

    cClause.setBody(body);
    cClause.setParameter(parameter);

    lang.getScopeManager().addValueDeclaration(parameter);
    lang.getScopeManager().leaveScope(cClause);
    return cClause;
  }
}
