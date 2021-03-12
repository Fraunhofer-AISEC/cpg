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
import io.github.oxisto.reticulated.ast.simple.ExpressionStatement
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import io.github.oxisto.reticulated.ast.simple.AssignmentStatement

class SimpleStatementHandler(lang: PythonLanguageFrontend?) :
    Handler<Statement?, SimpleStatement?, PythonLanguageFrontend>(
        ::Statement, lang!!
    ) {

    init {
        map[ExpressionStatement::class.java] = HandlerInterface { handleExpressionStatement(it as ExpressionStatement) }
        map[AssignmentStatement::class.java] = HandlerInterface { handleAssignmentStatement(it as AssignmentStatement) }
    }

    private fun handleAssignmentStatement(assignment: AssignmentStatement): Statement? {
        // for now, lets just always create an binary operator, in reality would need
        // to check, if this implicitly declares a variable as well

        val binOp = NodeBuilder.newBinaryOperator("=", lang.getCodeFromRawNode(assignment))

        // just one target for now
        val target = assignment.targets[0]

        binOp.lhs = lang.expressionHandler.handle(target)
        binOp.rhs = lang.expressionHandler.handle(assignment.expression)

        return binOp
    }

    private fun handleExpressionStatement(expressionStatement: ExpressionStatement): Statement {
        // un-wrap it
        return lang.expressionHandler.handle(expressionStatement.expression)
    }

}