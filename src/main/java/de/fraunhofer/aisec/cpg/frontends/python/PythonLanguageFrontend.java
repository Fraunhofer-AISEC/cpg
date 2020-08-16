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

package de.fraunhofer.aisec.cpg.frontends.python;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation;
import io.github.oxisto.reticulated.ParserResult;
import io.github.oxisto.reticulated.PythonParser;
import io.github.oxisto.reticulated.ast.FileInput;
import io.github.oxisto.reticulated.ast.statement.Definition;
import io.github.oxisto.reticulated.ast.statement.Statement;
import java.io.File;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The language frontend for translating python language into the graph. It uses antlr to parse the
 * actual source code into an AST.
 */
public class PythonLanguageFrontend extends LanguageFrontend {

  private final StatementHandler statementHandler = new StatementHandler(this);
  private final StatementListHandler statementListHandler = new StatementListHandler(this);
  private final DefinitionHandler definitionHandler = new DefinitionHandler(this);
  private final SuiteHandler suiteHandler = new SuiteHandler(this);
  private final ExpressionHandler expressionHandler = new ExpressionHandler(this);
  private final SimpleStatementHandler simpleStatementHandler = new SimpleStatementHandler(this);

  public PythonLanguageFrontend(TranslationConfiguration config, ScopeManager scopeManager) {
    super(config, scopeManager, "");
  }

  @Override
  public TranslationUnitDeclaration parse(File file) throws TranslationException {
    PythonParser app = new PythonParser();
    ParserResult result = app.parse(file.getPath());
    FileInput input = result.getRoot();

    TranslationUnitDeclaration tu = new TranslationUnitDeclaration();

    for (Statement ctx : input.getStatements()) {
      // now things get a little tricky, since python does not distinguish between statements and
      // declarations, but the python parser has an utility class called 'Definition' to distinguish
      // class and function definitions from other statements such as 'if' and 'for'.
      if (ctx instanceof Definition) {
        tu.add(definitionHandler.handle((Definition) ctx));
      }

      // additionally, python allows statement on a global level, something we also do not allow, so
      // we need to put them into a virtual function

    }

    return tu;
  }

  @Override
  public <T> String getCodeFromRawNode(T astNode) {
    return null;
  }

  @Override
  public @Nullable <T> PhysicalLocation getLocationFromRawNode(T astNode) {
    return null;
  }

  @Override
  public <S, T> void setComment(S s, T ctx) {}

  public StatementHandler getStatementHandler() {
    return statementHandler;
  }

  public DefinitionHandler getDefinitionHandler() {
    return definitionHandler;
  }

  public SuiteHandler getSuiteHandler() {
    return suiteHandler;
  }

  public StatementListHandler getStatementListHandler() {
    return statementListHandler;
  }

  public ExpressionHandler getExpressionHandler() {
    return expressionHandler;
  }

  public SimpleStatementHandler getSimpleStatementHandler() {
    return simpleStatementHandler;
  }
}
