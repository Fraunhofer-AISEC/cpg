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
package de.fraunhofer.aisec.cpg.frontends.java;

import com.github.javaparser.JavaToken;
import com.github.javaparser.Range;
import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.utils.Pair;
import de.fraunhofer.aisec.cpg.frontends.Handler;
import de.fraunhofer.aisec.cpg.graph.*;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.*;
import de.fraunhofer.aisec.cpg.graph.statements.CatchClause;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*;
import de.fraunhofer.aisec.cpg.graph.types.Type;
import de.fraunhofer.aisec.cpg.graph.types.TypeParser;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import de.fraunhofer.aisec.cpg.sarif.Region;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatementHandler
    extends Handler<
        de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement, JavaLanguageFrontend> {

  private static final Logger log = LoggerFactory.getLogger(StatementHandler.class);

  public StatementHandler(JavaLanguageFrontend lang) {
    super(de.fraunhofer.aisec.cpg.graph.statements.Statement::new, lang);

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

  public de.fraunhofer.aisec.cpg.graph.statements.Statement handleExpressionStatement(
      Statement stmt) {
    var expression = lang.getExpressionHandler().handle(stmt.asExpressionStmt().getExpression());

    // update expression's code and location to match the statement

    lang.setCodeAndRegion(expression, stmt);

    return expression;
  }

  private de.fraunhofer.aisec.cpg.graph.statements.Statement handleThrowStmt(Statement stmt) {
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

    Expression expression = null;
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
    ifStatement.setCondition((Expression) lang.getExpressionHandler().handle(conditionExpression));

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
        (Expression) lang.getExpressionHandler().handle(conditionExpression));

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
        (Expression) lang.getExpressionHandler().handle(conditionExpression));
    lang.getScopeManager().leaveScope(whileStatement);

    return whileStatement;
  }

  private ForEachStatement handleForEachStatement(Statement stmt) {
    ForEachStatement statement = NodeBuilder.newForEachStatement(stmt.toString());
    lang.getScopeManager().enterScope(statement);

    ForEachStmt forEachStmt = stmt.asForEachStmt();
    de.fraunhofer.aisec.cpg.graph.statements.Statement variable =
        lang.getExpressionHandler().handle(forEachStmt.getVariable());
    de.fraunhofer.aisec.cpg.graph.statements.Statement iterable =
        lang.getExpressionHandler().handle(forEachStmt.getIterable());

    if (!(variable instanceof DeclarationStatement)) {
      log.error("Expected a DeclarationStatement but received: {}", variable.getName());
    } else {
      statement.setVariable(variable);
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
    lang.setCodeAndRegion(statement, stmt);
    lang.getScopeManager().enterScope(statement);

    if (forStmt.getInitialization().size() > 1) {
      PhysicalLocation ofExprList = null;

      // code will be set later
      ExpressionList initExprList = NodeBuilder.newExpressionList(null);

      for (com.github.javaparser.ast.expr.Expression initExpr : forStmt.getInitialization()) {
        de.fraunhofer.aisec.cpg.graph.statements.Statement s =
            lang.getExpressionHandler().handle(initExpr);

        // make sure location is set
        lang.setCodeAndRegion(s, initExpr);
        initExprList.addExpression(s);

        // can not update location
        if (s.getLocation() == null) {
          continue;
        }

        if (ofExprList == null) {
          ofExprList = s.getLocation();
        }

        ofExprList.setRegion(
            lang.mergeRegions(ofExprList.getRegion(), s.getLocation().getRegion()));
      }

      // set code and location of init list
      if (statement.getLocation() != null && ofExprList != null) {
        String initCode =
            lang.getCodeOfSubregion(
                statement, statement.getLocation().getRegion(), ofExprList.getRegion());
        initExprList.setLocation(ofExprList);
        initExprList.setCode(initCode);
      }
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
      Literal<?> literal =
          NodeBuilder.newLiteral(true, TypeParser.createFrom("boolean", true), "true");
      statement.setCondition(literal);
    }

    if (forStmt.getUpdate().size() > 1) {
      PhysicalLocation ofExprList = statement.getLocation();

      // code will be set later
      ExpressionList iterationExprList = NodeBuilder.newExpressionList(null);

      for (com.github.javaparser.ast.expr.Expression updateExpr : forStmt.getUpdate()) {
        de.fraunhofer.aisec.cpg.graph.statements.Statement s =
            lang.getExpressionHandler().handle(updateExpr);

        // make sure location is set
        lang.setCodeAndRegion(s, updateExpr);
        iterationExprList.addExpression(s);

        // can not update location
        if (s.getLocation() == null) {
          continue;
        }

        if (ofExprList == null) {
          ofExprList = s.getLocation();
        }

        ofExprList.setRegion(
            lang.mergeRegions(ofExprList.getRegion(), s.getLocation().getRegion()));
      }

      // set code and location of init list
      if (statement.getLocation() != null && ofExprList != null) {
        String updateCode =
            lang.getCodeOfSubregion(
                statement, statement.getLocation().getRegion(), ofExprList.getRegion());
        iterationExprList.setLocation(ofExprList);
        iterationExprList.setCode(updateCode);
      }
      statement.setIterationStatement(iterationExprList);
    } else if (forStmt.getUpdate().size() == 1) {
      statement.setIterationStatement(
          lang.getExpressionHandler().handle(forStmt.getUpdate().get(0)));
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
    doStatement.setCondition((Expression) lang.getExpressionHandler().handle(conditionExpression));
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
      de.fraunhofer.aisec.cpg.graph.statements.Statement statement = handle(child);

      compoundStatement.addStatement(statement);
    }
    lang.setCodeAndRegion(compoundStatement, stmt);

    lang.getScopeManager().leaveScope(compoundStatement);
    return compoundStatement;
  }

  public de.fraunhofer.aisec.cpg.graph.statements.Statement handleCaseDefaultStatement(
      com.github.javaparser.ast.expr.Expression caseExpression, SwitchEntry sEntry) {

    PhysicalLocation parentLocation = lang.getLocationFromRawNode(sEntry);

    Optional<TokenRange> optionalTokenRange = sEntry.getTokenRange();
    Pair<JavaToken, JavaToken> caseTokens = new Pair<>(null, null);
    if (optionalTokenRange.isEmpty()) {
      log.error("Token for Region for Default case not available");
    }

    if (caseExpression == null) {
      if (optionalTokenRange.isPresent()) {
        /*
        TODO: not sure if this is really necessary, it seems to be the same location as
         parentLocation, except that column starts 1 character later and I am not sure if
         this is correct anyway
        */
        // Compute region and code for self generated default statement to match the c++ versions

        caseTokens =
            new Pair<>(
                getNextTokenWith("default", optionalTokenRange.get().getBegin()),
                getNextTokenWith(":", optionalTokenRange.get().getBegin()));
      }
      DefaultStatement defaultStatement =
          NodeBuilder.newDefaultStatement(getCodeBetweenTokens(caseTokens.a, caseTokens.b));
      defaultStatement.setLocation(
          getLocationsFromTokens(parentLocation, caseTokens.a, caseTokens.b));
      return defaultStatement;
    }

    Optional<TokenRange> caseExprTokenRange = caseExpression.getTokenRange();
    if (optionalTokenRange.isPresent() && caseExprTokenRange.isPresent()) {
      // Compute region and code for self generated case statement to match the c++ versions
      caseTokens =
          new Pair<>(
              getPreviousTokenWith("case", optionalTokenRange.get().getBegin()),
              getNextTokenWith(":", caseExprTokenRange.get().getEnd()));
    }

    CaseStatement caseStatement =
        NodeBuilder.newCaseStatement(getCodeBetweenTokens(caseTokens.a, caseTokens.b));
    caseStatement.setCaseExpression(
        (Expression) lang.getExpressionHandler().handle(caseExpression));

    caseStatement.setLocation(getLocationsFromTokens(parentLocation, caseTokens.a, caseTokens.b));

    return caseStatement;
  }

  public JavaToken getPreviousTokenWith(String text, JavaToken token) {
    Optional<JavaToken> optional = token.getPreviousToken();
    while (!token.getText().equals(text) && optional.isPresent()) {
      token = optional.get();
      optional = token.getPreviousToken();
    }
    return token;
  }

  public JavaToken getNextTokenWith(String text, JavaToken token) {
    Optional<JavaToken> optional = token.getNextToken();
    while (!token.getText().equals(text) && optional.isPresent()) {
      token = optional.get();
      optional = token.getNextToken();
    }
    return token;
  }

  @Nullable
  public PhysicalLocation getLocationsFromTokens(
      PhysicalLocation parentLocation, JavaToken startToken, JavaToken endToken) {
    // cannot construct location without parent location
    if (parentLocation == null) {
      return null;
    }

    if (startToken != null && endToken != null) {
      Optional<Range> startOpt = startToken.getRange();
      Optional<Range> endOpt = endToken.getRange();
      if (startOpt.isPresent() && endOpt.isPresent()) {
        Range rstart = startOpt.get();
        Range rend = endOpt.get();

        Region region =
            new Region(rstart.begin.line, rstart.begin.column, rend.end.line, rend.end.column + 1);

        return new PhysicalLocation(parentLocation.getArtifactLocation().getUri(), region);
      }
    }

    return null;
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

    // make sure location is set
    lang.setCodeAndRegion(switchStatement, switchStmt);

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
    compoundStatement.setLocation(
        getLocationsFromTokens(switchStatement.getLocation(), start, end));

    for (SwitchEntry sentry : switchStmt.getEntries()) {

      if (sentry.getLabels().isEmpty()) {
        compoundStatement.addStatement(handleCaseDefaultStatement(null, sentry));
      }

      for (com.github.javaparser.ast.expr.Expression caseExp : sentry.getLabels()) {
        compoundStatement.addStatement(handleCaseDefaultStatement(caseExp, sentry));
      }

      for (Statement subStmt : sentry.getStatements()) {
        compoundStatement.addStatement(handle(subStmt));
      }
    }
    switchStatement.setStatement(compoundStatement);
    lang.getScopeManager().leaveScope(switchStatement);
    return switchStatement;
  }

  private ExplicitConstructorInvocation handleExplicitConstructorInvocation(Statement stmt) {
    ExplicitConstructorInvocationStmt eciStatement = stmt.asExplicitConstructorInvocationStmt();
    String containingClass = "";
    RecordDeclaration currentRecord = lang.getScopeManager().getCurrentRecord();
    if (currentRecord == null) {
      log.error("Explicit constructor invocation has to be located inside a record declaration!");
    } else {
      containingClass = currentRecord.getName();
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
    List<de.fraunhofer.aisec.cpg.graph.statements.Statement> resources =
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

    for (de.fraunhofer.aisec.cpg.graph.statements.Statement r : resources) {
      if (r instanceof DeclarationStatement) {
        for (Declaration d : r.getDeclarations()) {
          if (d instanceof VariableDeclaration) {
            lang.getScopeManager().addDeclaration(d);
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
      concreteType = TypeParser.createFrom("java.lang.Throwable", true);
      concreteType.setTypeOrigin(Type.Origin.GUESSED);
    } else {
      concreteType = lang.getTypeAsGoodAsPossible(catchCls.getParameter().getType());
      possibleTypes.add(concreteType);
    }

    VariableDeclaration parameter =
        NodeBuilder.newVariableDeclaration(
            catchCls.getParameter().getName().toString(),
            concreteType,
            catchCls.getParameter().toString(),
            false);
    parameter.setPossibleSubTypes(possibleTypes);
    CompoundStatement body = handleBlockStatement(catchCls.getBody());

    cClause.setBody(body);
    cClause.setParameter(parameter);

    lang.getScopeManager().addDeclaration(parameter);
    lang.getScopeManager().leaveScope(cClause);
    return cClause;
  }
}
