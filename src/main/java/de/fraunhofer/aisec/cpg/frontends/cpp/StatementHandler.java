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

package de.fraunhofer.aisec.cpg.frontends.cpp;

import de.fraunhofer.aisec.cpg.frontends.Handler;
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
import de.fraunhofer.aisec.cpg.graph.Expression;
import de.fraunhofer.aisec.cpg.graph.ForEachStatement;
import de.fraunhofer.aisec.cpg.graph.ForStatement;
import de.fraunhofer.aisec.cpg.graph.GotoStatement;
import de.fraunhofer.aisec.cpg.graph.IfStatement;
import de.fraunhofer.aisec.cpg.graph.LabelStatement;
import de.fraunhofer.aisec.cpg.graph.Literal;
import de.fraunhofer.aisec.cpg.graph.NodeBuilder;
import de.fraunhofer.aisec.cpg.graph.Region;
import de.fraunhofer.aisec.cpg.graph.ReturnStatement;
import de.fraunhofer.aisec.cpg.graph.Statement;
import de.fraunhofer.aisec.cpg.graph.SwitchStatement;
import de.fraunhofer.aisec.cpg.graph.TryStatement;
import de.fraunhofer.aisec.cpg.graph.Type;
import de.fraunhofer.aisec.cpg.graph.VariableDeclaration;
import de.fraunhofer.aisec.cpg.graph.WhileStatement;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.ILabel;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTASMDeclaration;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTBreakStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCaseStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCatchHandler;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTContinueStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDefaultStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDoStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTForStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTGotoStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIfStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLabelStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNullStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTRangeBasedForStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTReturnStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSwitchStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTryBlockStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTWhileStatement;

class StatementHandler extends Handler<Statement, IASTStatement, CXXLanguageFrontend> {

  StatementHandler(CXXLanguageFrontend lang) {
    super(Statement::new, lang);

    map.put(
        CPPASTCompoundStatement.class,
        ctx -> handleCompoundStatement((CPPASTCompoundStatement) ctx));
    map.put(CPPASTReturnStatement.class, ctx -> handleReturnStatement((CPPASTReturnStatement) ctx));
    map.put(
        CPPASTDeclarationStatement.class,
        ctx -> handleDeclarationStatement((CPPASTDeclarationStatement) ctx));
    map.put(
        CPPASTExpressionStatement.class,
        ctx -> handleExpressionStatement((CPPASTExpressionStatement) ctx));
    map.put(CPPASTIfStatement.class, ctx -> handleIfStatement((CPPASTIfStatement) ctx));
    map.put(CPPASTWhileStatement.class, ctx -> handleWhileStatement((CPPASTWhileStatement) ctx));
    map.put(CPPASTDoStatement.class, ctx -> handleDoStatement((CPPASTDoStatement) ctx));
    map.put(CPPASTForStatement.class, ctx -> handleForStatement((CPPASTForStatement) ctx));
    map.put(
        CPPASTRangeBasedForStatement.class,
        ctx -> handleForEachStatement((CPPASTRangeBasedForStatement) ctx));
    map.put(
        CPPASTContinueStatement.class,
        ctx -> handleContinueStatement((CPPASTContinueStatement) ctx));
    map.put(CPPASTBreakStatement.class, ctx -> handleBreakStatement((CPPASTBreakStatement) ctx));

    map.put(CPPASTLabelStatement.class, ctx -> handleLabelStatement((CPPASTLabelStatement) ctx));
    map.put(CPPASTSwitchStatement.class, ctx -> handleSwitchStatement((CPPASTSwitchStatement) ctx));
    map.put(CPPASTCaseStatement.class, ctx -> handleCaseStatement((CPPASTCaseStatement) ctx));
    map.put(
        CPPASTDefaultStatement.class, ctx -> handleDefaultStatement((CPPASTDefaultStatement) ctx));
    map.put(CPPASTNullStatement.class, ctx -> handleEmptyStatement((CPPASTNullStatement) ctx));
    map.put(CPPASTGotoStatement.class, ctx -> handleGotoStatement((CPPASTGotoStatement) ctx));
    map.put(
        CPPASTTryBlockStatement.class,
        ctx -> handleTryBlockStatement((CPPASTTryBlockStatement) ctx));
    map.put(CPPASTCatchHandler.class, ctx -> handleCatchHandler((ICPPASTCatchHandler) ctx));
  }

  private EmptyStatement handleEmptyStatement(CPPASTNullStatement emptyStmt) {
    return NodeBuilder.newEmptyStatement(emptyStmt.getRawSignature());
  }

  private TryStatement handleTryBlockStatement(CPPASTTryBlockStatement tryStmt) {
    TryStatement tryStatement = NodeBuilder.newTryStatement(tryStmt.toString());
    lang.getScopeManager().enterScope(tryStatement);
    CompoundStatement statement = (CompoundStatement) handle(tryStmt.getTryBody());

    List<CatchClause> catchClauses =
        Arrays.stream(tryStmt.getCatchHandlers())
            .map(this::handleCatchHandler)
            .collect(Collectors.toList());
    tryStatement.setTryBlock(statement);
    tryStatement.setCatchClauses(catchClauses);
    lang.getScopeManager().leaveScope(tryStatement);

    return tryStatement;
  }

  private CatchClause handleCatchHandler(ICPPASTCatchHandler catchHandler) {
    CatchClause catchClause = NodeBuilder.newCatchClause(catchHandler.getRawSignature());
    lang.getScopeManager().enterScope(catchClause);
    Statement body = this.lang.getStatementHandler().handle(catchHandler.getCatchBody());
    Declaration decl = null;
    if (catchHandler.getDeclaration() != null) { // can be null for "catch(...)"
      decl = this.lang.getDeclarationHandler().handle(catchHandler.getDeclaration());
    }
    catchClause.setBody((CompoundStatement) body);
    if (decl != null) {
      catchClause.setParameter((VariableDeclaration) decl);
    }
    lang.getScopeManager().leaveScope(catchClause);
    return catchClause;
  }

  private IfStatement handleIfStatement(CPPASTIfStatement ctx) {
    IfStatement statement = NodeBuilder.newIfStatement(ctx.getRawSignature());
    lang.getScopeManager().enterScope(statement);
    if (ctx.getInitializerStatement() != null) {
      statement.setInitializerStatement(handle(ctx.getInitializerStatement()));
    }
    if (ctx.getConditionDeclaration() != null) {
      statement.setConditionDeclaration(
          this.lang.getDeclarationHandler().handle(ctx.getConditionDeclaration()));
    }
    statement.setConstExpression(ctx.isConstexpr());
    if (ctx.getConditionExpression() != null)
      statement.setCondition(this.lang.getExpressionHandler().handle(ctx.getConditionExpression()));
    statement.setThenStatement(handle(ctx.getThenClause()));

    if (ctx.getElseClause() != null) {
      statement.setElseStatement(handle(ctx.getElseClause()));
    }
    lang.getScopeManager().leaveScope(statement);
    return statement;
  }

  private LabelStatement handleLabelStatement(CPPASTLabelStatement ctx) {
    LabelStatement statement = NodeBuilder.newLabelStatement(ctx.getRawSignature());

    statement.setSubStatement(handle(ctx.getNestedStatement()));
    statement.setLabel(ctx.getName().toString());

    return statement;
  }

  private GotoStatement handleGotoStatement(CPPASTGotoStatement ctx) {
    GotoStatement statement = NodeBuilder.newGotoStatement(ctx.getRawSignature());
    BiConsumer assigneTargetLabel = (from, to) -> statement.setTargetLabel((LabelStatement) to);
    IBinding b = null;
    try {
      b = ctx.getName().resolveBinding();
      if (b instanceof ILabel) {
        ILabel label = (ILabel) b;
        label.getLabelStatement();
        // If the bound AST node is/or was transformed into a CPG node the cpg node is bound to the
        // CPG goto statement
        lang.registerObjectListener(label.getLabelStatement(), assigneTargetLabel);
      }
    } catch (Exception e) {
      // If the Label AST node was could not be resolved, the matchign is done based on label names
      // of CPG nodes using the predicate listeners
      lang.registerPredicateListener(
          (from, to) ->
              to instanceof LabelStatement
                  && ((LabelStatement) to).getLabel().equals(statement.getLabelName()),
          assigneTargetLabel);
    }
    return statement;
  }

  private WhileStatement handleWhileStatement(CPPASTWhileStatement ctx) {
    WhileStatement statement = NodeBuilder.newWhileStatement(ctx.getRawSignature());
    lang.getScopeManager().enterScope(statement);
    if (ctx.getConditionDeclaration() != null)
      statement.setConditionDeclaration(
          this.lang.getDeclarationHandler().handle(ctx.getConditionDeclaration()));
    if (ctx.getCondition() != null)
      statement.setCondition(this.lang.getExpressionHandler().handle(ctx.getCondition()));
    statement.setStatement(handle(ctx.getBody()));
    lang.getScopeManager().leaveScope(statement);
    return statement;
  }

  private DoStatement handleDoStatement(CPPASTDoStatement ctx) {
    DoStatement statement = NodeBuilder.newDoStatement(ctx.getRawSignature());
    lang.getScopeManager().enterScope(statement);
    statement.setCondition(this.lang.getExpressionHandler().handle(ctx.getCondition()));
    statement.setStatement(handle(ctx.getBody()));
    lang.getScopeManager().leaveScope(statement);

    return statement;
  }

  private ForStatement handleForStatement(CPPASTForStatement ctx) {
    ForStatement statement = NodeBuilder.newForStatement(ctx.getRawSignature());
    lang.getScopeManager().enterScope(statement);
    statement.setInitializerStatement(handle(ctx.getInitializerStatement()));
    if (ctx.getConditionDeclaration() != null)
      statement.setConditionDeclaration(
          this.lang.getDeclarationHandler().handle(ctx.getConditionDeclaration()));
    if (ctx.getConditionExpression() != null)
      statement.setCondition(this.lang.getExpressionHandler().handle(ctx.getConditionExpression()));

    // Adds true expression node where default empty condition evaluates to true, remove here and in
    // java StatementAnalyzer
    if (statement.getConditionDeclaration() == null && statement.getCondition() == null) {
      Literal literal = NodeBuilder.newLiteral(true, new Type("bool"), "true");
      literal.setRegion(new Region());
      statement.setCondition(literal);
    }
    if (ctx.getIterationExpression() != null)
      statement.setIterationExpression(
          this.lang.getExpressionHandler().handle(ctx.getIterationExpression()));
    statement.setStatement(handle(ctx.getBody()));
    lang.getScopeManager().leaveScope(statement);

    return statement;
  }

  private ForEachStatement handleForEachStatement(CPPASTRangeBasedForStatement ctx) {
    ForEachStatement statement = NodeBuilder.newForEachStatement(ctx.getRawSignature());
    lang.getScopeManager().enterScope(statement);

    Declaration var = this.lang.getDeclarationHandler().handle(ctx.getDeclaration());
    Statement iterable = this.lang.getExpressionHandler().handle(ctx.getInitializerClause());

    statement.setVariable(var);
    statement.setIterable(iterable);
    statement.setStatement(handle(ctx.getBody()));
    lang.getScopeManager().leaveScope(statement);

    return statement;
  }

  private BreakStatement handleBreakStatement(CPPASTBreakStatement ctx) {
    return NodeBuilder.newBreakStatement(ctx.getRawSignature());
    // C++ has no labeled break
  }

  private ContinueStatement handleContinueStatement(CPPASTContinueStatement ctx) {
    return NodeBuilder.newContinueStatement(ctx.getRawSignature());
    // C++ has no labeled continue
  }

  private Expression handleExpressionStatement(CPPASTExpressionStatement ctx) {
    return this.lang.getExpressionHandler().handle(ctx.getExpression());
  }

  private DeclarationStatement handleDeclarationStatement(CPPASTDeclarationStatement ctx) {
    if (ctx.getDeclaration() instanceof CPPASTASMDeclaration) {
      return NodeBuilder.newASMDeclarationStatement(ctx.getRawSignature());
    } else {
      DeclarationStatement declarationStatement =
          NodeBuilder.newDeclarationStatement(ctx.getRawSignature());

      declarationStatement.setDeclarations(
          this.lang.getDeclarationListHandler().handle(ctx.getDeclaration()));

      return declarationStatement;
    }
  }

  private ReturnStatement handleReturnStatement(CPPASTReturnStatement ctx) {
    ReturnStatement returnStatement = NodeBuilder.newReturnStatement(ctx.getRawSignature());
    // parse the return value
    // Todo Handle ReturnArgument
    if (ctx.getReturnValue() != null)
      returnStatement.setReturnValue(this.lang.getExpressionHandler().handle(ctx.getReturnValue()));

    return returnStatement;
  }

  private CompoundStatement handleCompoundStatement(CPPASTCompoundStatement ctx) {
    CompoundStatement compoundStatement = NodeBuilder.newCompoundStatement(ctx.getRawSignature());

    lang.getScopeManager().enterScope(compoundStatement);

    for (IASTStatement statement : ctx.getStatements()) {
      compoundStatement.getStatements().add(handle(statement));
    }
    lang.getScopeManager().leaveScope(compoundStatement);
    return compoundStatement;
  }

  private SwitchStatement handleSwitchStatement(CPPASTSwitchStatement ctx) {
    SwitchStatement switchStatement = NodeBuilder.newSwitchStatement(ctx.getRawSignature());
    lang.getScopeManager().enterScope(switchStatement);
    if (ctx.getInitializerStatement() != null)
      switchStatement.setInitializerStatement(handle(ctx.getInitializerStatement()));
    if (ctx.getControllerDeclaration() != null)
      switchStatement.setSelectorDeclaration(
          this.lang.getDeclarationHandler().handle(ctx.getControllerDeclaration()));
    if (ctx.getControllerExpression() != null)
      switchStatement.setSelector(
          this.lang.getExpressionHandler().handle(ctx.getControllerExpression()));
    switchStatement.setStatement(handle(ctx.getBody()));
    lang.getScopeManager().leaveScope(switchStatement);
    return switchStatement;
  }

  private CaseStatement handleCaseStatement(CPPASTCaseStatement ctx) {
    CaseStatement caseStatement = NodeBuilder.newCaseStatement(ctx.getRawSignature());
    caseStatement.setCaseExpression(this.lang.getExpressionHandler().handle(ctx.getExpression()));
    return caseStatement;
  }

  private DefaultStatement handleDefaultStatement(CPPASTDefaultStatement ctx) {
    return NodeBuilder.newDefaultStatement(ctx.getRawSignature());
  }
}
