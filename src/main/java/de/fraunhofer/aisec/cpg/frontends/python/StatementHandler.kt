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

import de.fraunhofer.aisec.cpg.frontends.Handler
import io.github.oxisto.reticulated.ast.simple.SimpleStatement
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.statements.Statement

class StatementHandler(lang: PythonLanguageFrontend) :
    Handler<Statement?, io.github.oxisto.reticulated.ast.statement.Statement, PythonLanguageFrontend>(
        ::Statement, lang
    ) {

    init {
        map[SimpleStatement::class.java] = HandlerInterface { handleSimpleStatement(it as SimpleStatement) }
    }

    private fun handleSimpleStatement(statement: SimpleStatement): Statement? {
        return lang.simpleStatementHandler.handle(statement)
    }

}