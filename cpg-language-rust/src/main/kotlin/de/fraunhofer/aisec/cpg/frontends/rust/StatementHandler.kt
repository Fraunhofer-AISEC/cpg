/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import kotlin.collections.plusAssign
import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsLetStmt
import uniffi.cpgrust.RsPat
import uniffi.cpgrust.RsStmt

class StatementHandler(frontend: RustLanguageFrontend) :
    RustHandler<Statement, RsAst.RustStmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: RsAst.RustStmt): Statement {
        val unwrapped = node.v1
        return handleNode(unwrapped)
    }

    fun handleNode(node: RsStmt): Statement {
        return when (node) {
            is RsStmt.LetStmt -> handleLetStmt(node.v1)
            else ->
                newProblemExpression(
                    problem = "The statement of class ${node.javaClass} is not supported yet",
                    rawNode = RsAst.RustStmt(node),
                )
        }
    }

    fun handleLetStmt(letStmt: RsLetStmt): Statement {
        val raw = RsAst.RustStmt(RsStmt.LetStmt(letStmt))

        val declarationStatement = newDeclarationStatement(rawNode = raw)

        val variable =
            newVariableDeclaration(
                name = (letStmt.pat as? RsPat.IdentPat)?.v1?.name ?: "",
                type = letStmt.ty?.let { frontend.typeOf(it) } ?: unknownType(),
                rawNode = raw,
            )

        letStmt.initializer?.let {
            variable.initializer = frontend.expressionHandler.handle(RsAst.RustExpr(it))
        }

        declarationStatement.declarations += variable

        frontend.scopeManager.addDeclaration(variable)

        return declarationStatement
    }

    /*private fun handle...(node: ..., ...): ... {
        val statements = mutableListOf<Statement>()

        return statements
    }*/

}
