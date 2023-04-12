/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.openqasm

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.openqasm.astnodes.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.internal.core.dom.parser.cpp.*

class OpenQasmLanguageFrontend(
    language: Language<OpenQasmLanguageFrontend>,
    config: TranslationConfiguration,
    scopeManager: ScopeManager
) : LanguageFrontend(language, config, scopeManager) {

    companion object {
        @kotlin.jvm.JvmField var OPENQASM_EXTENSIONS: List<String> = listOf(".qasm")
    }

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        return parseInternal(file.readText(Charsets.UTF_8), file.path)
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        if (astNode is ASTNode) {
            return "TODO" // TODO
        } else {
            TODO()
        }
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        // will be invoked by native function
        return null
    }

    override fun <S, T> setComment(s: S, ctx: T) {
        // will be invoked by native function
    }

    private fun parseInternal(code: String, path: String): TranslationUnitDeclaration {
        val lexer = OpenQASMLexer(File(path))
        lexer.run()
        val parser = OpenQASMParser(lexer.tokens)
        val ast = parser.parse()
        return toCpg(ast)
    }

    private fun toCpg(ast: ProgramNode): TranslationUnitDeclaration {
        val tu =
            newTranslationUnitDeclaration(
                ast.location.artifactLocation.uri.path,
                code = getCodeFromRawNode(ast),
                rawNode = ast
            )
        scopeManager.resetToGlobal(tu)
        val nsd = newNamespaceDeclaration("OpenQASM", code = getCodeFromRawNode(ast), rawNode = ast)
        scopeManager.addDeclaration(nsd)
        tu.addDeclaration(nsd)
        scopeManager.enterScope(nsd)
        for (stmt in ast.stmts) {
            nsd.addStatement(handleStatement(stmt))
        }
        scopeManager.leaveScope(nsd)
        scopeManager.addDeclaration(nsd)

        return tu
    }

    private fun handleStatement(stmt: StatementNode): Statement {
        return when (stmt) {
            is IncludeStatementNode -> newEmptyStatement() // TODO
            is GateStatementNode -> handleGateStatement(stmt)
            is GateCallStatementNode -> handleGateCall(stmt)
            is QuantumDeclarationStatementNode -> handleQBitDecl(stmt)
            is ClassicalDeclarationStatementNode -> handleClassicalBitDecl(stmt)
            is ResetStatementNode -> newEmptyStatement() // TODO
            is ForStatementNode -> handleForStatement(stmt)
            is MeasureArrowAssignmentStatementNode -> handleMeasure(stmt)
            else -> TODO()
        }
    }

    private fun handleMeasure(stmt: MeasureArrowAssignmentStatementNode): Statement {
        val ret = newCompoundStatement()

        // TODO third expr, first()

        val lhsName =
            ((stmt.measureExpr.payload as? GateOperandNode)?.payload as? IndexedIdentifierNode)
                ?.identifier
                ?.payload
                ?: TODO()

        val lhsRange =
            ((stmt.measureExpr.payload as? GateOperandNode)?.payload as? IndexedIdentifierNode)
                ?.indexOperators
                ?.first()
                ?.exprs
                ?.first()

        val lhsStartIdx =
            when (lhsRange) {
                is RangeExpressionNode ->
                    (lhsRange.firstExpr as? DecimalIntegerLiteralExpressionNode)?.payload
                is DecimalIntegerLiteralExpressionNode -> lhsRange.payload
                else -> TODO()
            }
                ?: TODO()

        val lhsEndIdx =
            when (lhsRange) {
                is RangeExpressionNode ->
                    (lhsRange.secondExpr as? DecimalIntegerLiteralExpressionNode)?.payload
                is DecimalIntegerLiteralExpressionNode -> lhsRange.payload
                else -> TODO()
            }
                ?: TODO()

        val rhsName = (stmt.indexedIdentifier?.identifier as? IdentifierNode)?.payload ?: TODO()
        val rhsRange =
            (stmt.indexedIdentifier.indexOperators.first() as? IndexOperatorNode)?.exprs?.first()
        val rhsStartIdx =
            when (rhsRange) {
                is RangeExpressionNode ->
                    (rhsRange.firstExpr as? DecimalIntegerLiteralExpressionNode)?.payload
                is DecimalIntegerLiteralExpressionNode -> rhsRange.payload
                else -> TODO()
            }
                ?: TODO()
        val rhsEndIdx =
            when (rhsRange) {
                is RangeExpressionNode ->
                    (rhsRange.secondExpr as? DecimalIntegerLiteralExpressionNode)?.payload
                is DecimalIntegerLiteralExpressionNode -> rhsRange.payload
                else -> TODO()
            }
                ?: TODO()

        if (lhsEndIdx.toInt() - lhsStartIdx.toInt() != rhsEndIdx.toInt() - rhsStartIdx.toInt()) {
            TODO()
        }

        for (i in 0 until (lhsEndIdx.toInt() - lhsStartIdx.toInt())) {
            val lhsIdx = lhsStartIdx.toInt() + i
            val lhsIdxName = "$lhsName[$lhsIdx]"
            val rhsIdx = rhsStartIdx.toInt() + i
            val rhsIdxName = "$rhsName[$rhsIdx]"

            val m =
                newAssignExpression(
                    lhs = listOf(newDeclaredReferenceExpression(lhsIdxName)),
                    rhs = listOf(newDeclaredReferenceExpression(rhsIdxName))
                )
            ret.addStatement(m)
        }

        return ret
    }

    private fun handleForStatement(stmt: ForStatementNode): Statement {
        val f = newForStatement(code = getCodeFromRawNode(stmt), rawNode = stmt)
        // TODO
        return f
    }

    private fun handleClassicalBitDecl(stmt: ClassicalDeclarationStatementNode): Statement {
        val name = stmt.identifier.payload
        val cnt =
            when (stmt.tpe) {
                is ScalarTypeBitNode ->
                    (stmt.tpe.designatorNode?.payload as? DecimalIntegerLiteralExpressionNode)
                        ?.payload
                        ?: TODO()
                is ScalarTypeUIntNode ->
                    (stmt.tpe.designatorNode?.payload as? DecimalIntegerLiteralExpressionNode)
                        ?.payload
                        ?: TODO()
                else -> TODO()
            }

        val collector = newCompoundStatement()
        for (i in 0 until cnt as Int) {
            val v =
                newVariableDeclaration("$name[$i]", code = getCodeFromRawNode(stmt), rawNode = stmt)
            scopeManager.addDeclaration(v)
            collector.addDeclaration(v)
            // TODO type
        }
        // TODO initializer (declExpression)
        return collector
    }

    private fun handleQBitDecl(stmt: QuantumDeclarationStatementNode): Statement {
        val name = stmt.identifier.payload
        val cnt: Number =
            (stmt.qubitType.designator?.payload as? DecimalIntegerLiteralExpressionNode)?.payload
                ?: TODO()
        val collector = newCompoundStatement()
        for (i in 0 until cnt as Int) {
            val v =
                newVariableDeclaration("$name[$i]", code = getCodeFromRawNode(stmt), rawNode = stmt)
            scopeManager.addDeclaration(v)
            collector.addDeclaration(v)
            // TODO type
        }
        return collector
    }

    private fun handleGateCall(stmt: GateCallStatementNode): Statement {
        // handle gate call as function call

        val callee =
            newDeclaredReferenceExpression(
                stmt.identifier?.payload,
                code = getCodeFromRawNode(stmt),
                rawNode = stmt
            )
        val call = newCallExpression(callee, code = getCodeFromRawNode(stmt), rawNode = stmt)
        if (stmt.gateOperandList != null) {
            for (p in stmt.gateOperandList.payload) {
                call.addArgument(handleCallArg(p))
            }
        }
        return call
    }

    private fun handleCallArg(p: GateOperandNode): Expression {
        return when (p.payload) {
            is IndexedIdentifierNode ->
                newDeclaredReferenceExpression(
                    p.payload.identifier.payload,
                    code = getCodeFromRawNode(p),
                    rawNode = p
                )
            else -> TODO()
        }
    }

    private fun handleGateStatement(stmt: GateStatementNode): Statement {
        // handle gates as "functions"
        val func = newFunctionDeclaration(stmt.identifier, rawNode = stmt)
        scopeManager.enterScope(func)
        if (stmt.identifierList != null) {
            for (p in stmt.identifierList.identifiers) {
                func.addParameter(
                    newParamVariableDeclaration(
                        p,
                        code = getCodeFromRawNode(stmt),
                        rawNode = stmt.identifierList
                    )
                )
            }
        }

        func.body = handleScopeNode(stmt.scope)
        val declStmt = newDeclarationStatement()
        scopeManager.leaveScope(func)
        scopeManager.addDeclaration(func)
        declStmt.addDeclaration(func)
        return declStmt
    }

    private fun handleScopeNode(scope: ScopeNode): Statement {
        val ret = newCompoundStatement(code = getCodeFromRawNode(scope), rawNode = scope)
        for (s in scope.stmtList) {
            ret.addStatement(handleStatement(s))
        }
        return ret
    }
}
