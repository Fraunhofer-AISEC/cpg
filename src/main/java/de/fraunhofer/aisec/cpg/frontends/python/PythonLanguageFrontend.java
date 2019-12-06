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
import de.fraunhofer.aisec.cpg.graph.Region;
import de.fraunhofer.aisec.cpg.graph.TranslationUnitDeclaration;
import io.github.oxisto.reticulated.ParserResult;
import io.github.oxisto.reticulated.PythonParser;
import io.github.oxisto.reticulated.ast.FileInput;
import io.github.oxisto.reticulated.ast.statement.Definition;
import io.github.oxisto.reticulated.ast.statement.Statement;
import java.io.File;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The language frontend for translating python language into the graph. It uses antlr to parse the
 * actual source code into an AST.
 */
public class PythonLanguageFrontend extends LanguageFrontend {

  private StatementHandler statementHandler = new StatementHandler(this);
  private StatementListHandler statementListHandler = new StatementListHandler(this);
  private DefinitionHandler definitionHandler = new DefinitionHandler(this);
  private SuiteHandler suiteHandler = new SuiteHandler(this);
  private ExpressionHandler expressionHandler = new ExpressionHandler(this);
  private SimpleStatementHandler simpleStatementHandler = new SimpleStatementHandler(this);

  public PythonLanguageFrontend(TranslationConfiguration config) {
    super(config, "");
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
  public @NonNull <T> Region getRegionFromRawNode(T astNode) {
    return Region.UNKNOWN_REGION;
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
