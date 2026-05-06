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
import uniffi.cpgrust.RsBlockExpr
import uniffi.cpgrust.RsExpr
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
        // for us, a let expression is an assigment with a deconstruction

        var initializer =
            letStmt.initializer?.let { frontend.expressionHandler.handle(RsAst.RustExpr(it)) }
                ?: newProblemExpression("Let statement does not have an initializer", rawNode = raw)

        letStmt.letElse?.let {
            return handleLetElse(letStmt, it, raw)
        }

        // If the Pattern is a simple identity pattern we make it a declaration statement
        (letStmt.pat as? RsPat.IdentPat)?.let {
            val declarationStatement = newDeclarationStatement(rawNode = raw)

            val variable =
                newVariable(
                    name = it.v1.name ?: "",
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

        val assign: Assign =
            newAssign(
                operatorCode = "=",
                lhs =
                    letStmt.pat?.let { listOf(frontend.patternHandler.handle(RsAst.RustPat(it))) }
                        ?: emptyList(),
                rhs =
                    letStmt.initializer?.let {
                        listOf(frontend.expressionHandler.handle(RsAst.RustExpr(it)))
                    } ?: emptyList(),
                rawNode = raw,
            )

        assign.usedAsExpression = true
        return assign
    }

    fun handleLetElse(letStmt: RsLetStmt, blockExpr: RsBlockExpr, raw: RsAst.RustStmt): Expression {

        val patternResult =
            letStmt.pat?.let { frontend.patternHandler.handle(RsAst.RustPat(it)) }
                ?: newProblemExpression("Pattern cannot be parsed.", rawNode = raw)

        val declarations = patternResult.nodes.filterIsInstance<DeclarationStatement>()

        val variableDeconstruction =
            newObjectDeconstruction(raw).also { obj ->
                letStmt.ty?.let { obj.type = frontend.typeOf(it) }
                    ?: run { obj.type = unknownType() }
                declarations.forEach { declStmt -> obj.components += declStmt }
            }

        // Handle the pattern, extract the variable declarations, put them into an object
        // deconstruction,
        // are they already added to the scope?, for every variable, make a tuple expression with a
        // reference for each
        //    variable and put that as the return expression
        // Translate the pattern a second time as case expression

        val switch =
            newSwitch(rawNode = raw).also { switch ->
                switch.selector =
                    letStmt.initializer?.let {
                        frontend.expressionHandler.handle(RsAst.RustExpr(it))
                    }
                        ?: newProblemExpression(
                            "Let statement does not have an initializer",
                            rawNode = raw,
                        )
                frontend.scopeManager.enterScope(switch)

                // Create a block to hold two case statements
                val caseBlock = newBlock(raw)
                caseBlock.usedAsExpression = true

                caseBlock.statements +=
                    newCase(raw).also { value ->
                        value.caseExpression =
                            letStmt.pat?.let { frontend.patternHandler.handle(RsAst.RustPat(it)) }
                                ?: newProblemExpression("Pattern cannot be parsed.", rawNode = raw)
                    }

                val bindingsList = newInitializerList(rawNode = raw)

                declarations
                    .flatMap { it.variables }
                    .forEach { variable ->
                        val reference = newReference(variable.name.toString(), rawNode = raw)
                        reference.refersTo = variable
                        bindingsList.initializers += reference
                    }

                val breakExpr = newBreak(raw)
                breakExpr.expr = bindingsList
                breakExpr.usedAsExpression = true

                caseBlock.statements += breakExpr

                caseBlock.statements += newDefault(raw)
                caseBlock.statements +=
                    frontend.expressionHandler.handleNode(RsExpr.BlockExpr(blockExpr))

                switch.statement = caseBlock

                frontend.scopeManager.leaveScope(switch)

                switch.usedAsExpression = true
            }

        return newAssign(
            operatorCode = "=",
            lhs = listOf(variableDeconstruction),
            rhs = listOf(switch),
            rawNode = raw,
        )
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
        ((declaration as? DeclarationSequence)?.declarations ?: listOf(declaration)).forEach {
            declItem ->
            declarationStatement.declarations += declItem
            frontend.scopeManager.addDeclaration(declItem)
        }

        return declarationStatement
    }
}
