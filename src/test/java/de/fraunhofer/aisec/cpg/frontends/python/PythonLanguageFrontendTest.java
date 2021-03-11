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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.frontends.TranslationException;
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration;
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement;
import de.fraunhofer.aisec.cpg.graph.statements.Statement;
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression;
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PythonLanguageFrontendTest {

  private TranslationConfiguration config;

  @BeforeEach
  void setUp() {
    config = TranslationConfiguration.builder().build();
  }

  @Test
  void testSimple() throws TranslationException {
    TranslationUnitDeclaration declaration =
        new PythonLanguageFrontend(config, new ScopeManager())
            .parse(new File("src/test/resources/main.py"));

    assertNotNull(declaration);

    List<Declaration> declarations = declaration.getDeclarations();

    assertEquals(1, declarations.size());

    // first declaration is the function declaration
    assertTrue(declarations.get(0) instanceof FunctionDeclaration);
    FunctionDeclaration functionDeclaration = (FunctionDeclaration) declarations.get(0);

    assertEquals("test", functionDeclaration.getName());

    CompoundStatement body = (CompoundStatement) functionDeclaration.getBody();
    List<Statement> statements = body.getStatements();
    assertEquals(3, statements.size());

    Statement stmt = statements.get(0);
    assertTrue(stmt instanceof CallExpression);

    CallExpression call = (CallExpression) stmt;
    assertEquals("print", call.getName());
  }
}
