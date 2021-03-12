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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import io.github.oxisto.reticulated.PythonParser
import io.github.oxisto.reticulated.ast.statement.Definition
import java.io.File

/**
 * The language frontend for translating python language into the graph. It uses antlr to parse the
 * actual source code into an AST.
 */
class PythonLanguageFrontend(config: TranslationConfiguration?, scopeManager: ScopeManager?) : LanguageFrontend(
    config!!, scopeManager, ""
) {
    val statementHandler = StatementHandler(this)
    val statementListHandler = StatementListHandler(this)
    val definitionHandler = DefinitionHandler(this)
    val suiteHandler = SuiteHandler(this)
    val expressionHandler = ExpressionHandler(this)
    val simpleStatementHandler = SimpleStatementHandler(this)

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        val app = PythonParser()
        val (input) = app.parse(file.path)
        val tu = TranslationUnitDeclaration()

        for (ctx in input.statements) {
            // now things get a little tricky, since python does not distinguish between statements and
            // declarations, but the python parser has an utility class called 'Definition' to distinguish
            // class and function definitions from other statements such as 'if' and 'for'.
            if (ctx is Definition) {
                val d = definitionHandler.handle(ctx)
                if (d != null) tu.addDeclaration(d)
            }

            // additionally, python allows statement on a global level, something we also do not allow, so
            // we need to put them into a virtual function
        }
        return tu
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        return null
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        return null
    }

    override fun <S, T> setComment(s: S, ctx: T) {}
}