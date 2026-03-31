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
import de.fraunhofer.aisec.cpg.graph.expressions.*
import kotlin.collections.plusAssign
import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsExprStmt
import uniffi.cpgrust.RsItem
import uniffi.cpgrust.RsLetStmt
import uniffi.cpgrust.RsPat
import uniffi.cpgrust.RsStmt

class StatementHandler(frontend: RustLanguageFrontend) :
    RustHandler<Expression, RsAst.RustStmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: RsAst.RustStmt): Expression {
        val unwrapped = node.v1
        return handleNode(unwrapped)
    }

    fun handleNode(node: RsStmt): Expression {
        return when (node) {
            is RsStmt.LetStmt -> handleLetStmt(node.v1)
            is RsStmt.ExprStmt -> handleExprStmt(node.v1)
            is RsStmt.Item -> handleItem(node.v1)
            else ->
                newProblemExpression(
                    problem = "The statement of class ${node.javaClass} is not supported yet",
                    rawNode = RsAst.RustStmt(node),
                )
        }
    }

    fun handleLetStmt(letStmt: RsLetStmt): Expression {
        val raw = RsAst.RustStmt(RsStmt.LetStmt(letStmt))

        val declarationStatement = newDeclarationStatement(rawNode = raw)

        val variable =
            newVariable(
                name = (letStmt.pat as? RsPat.IdentPat)?.v1?.name ?: "",
                type = letStmt.ty?.let { frontend.typeOf(it) } ?: unknownType(),
                rawNode = raw,
            )

        letStmt.initializer?.let {
            // Todo If this is a tuple struct, rust analyzer will actually make a call out of it
            variable.initializer = frontend.expressionHandler.handle(RsAst.RustExpr(it))

            // Here, if we have the classical pattern for initializers we set the base of the
            // contained member access. This part needs to be made more precise.
            val initializingExpressions =
                when (variable.initializer) {
                    is Construction -> (variable.initializer as Construction).arguments
                    is InitializerList -> (variable.initializer as InitializerList).initializers
                    else -> listOf()
                }
            initializingExpressions.forEach {
                (it as? Assign)?.lhs?.forEach {
                    val targetRef = (it as? MemberAccess)?.base ?: it
                    (targetRef as? Reference)?.let {
                        if (it.name.toString() == "null") {
                            it.name = variable.name
                        }
                    }
                }
            }
        }

        declarationStatement.declarations += variable

        frontend.scopeManager.addDeclaration(variable)

        return declarationStatement
    }

    fun handleExprStmt(exprStmt: RsExprStmt): Expression {
        val raw = RsAst.RustStmt(RsStmt.ExprStmt(exprStmt))

        exprStmt.expr.getOrNull(0)?.let {
            return frontend.expressionHandler.handle(RsAst.RustExpr(it)).also {
                it.usedAsExpression = false
            }
        }

        return newProblemExpression(
            "${exprStmt.javaClass.simpleName} does not contain an expression",
            rawNode = raw,
        )
    }

    fun handleItem(item: RsItem): Expression {

        val declarationStatement = newDeclarationStatement(rawNode = RsAst.RustItem(item))

        val declaration = frontend.declarationHandler.handle(RsAst.RustItem(item))
        ((declaration as? DeclarationSequence)?.declarations ?: listOf(declaration)).forEach { declItem ->
            declarationStatement.declarations += declItem
            frontend.scopeManager.addDeclaration(declItem)
        }

        return declarationStatement
    }
}
