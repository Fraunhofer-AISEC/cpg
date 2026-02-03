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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.openqasm.astnodes.*
import de.fraunhofer.aisec.cpg.frontends.openqasm.passes.OpenQASMPass
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumNodeBuilder.newClassicIf
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.ClassicBitType
import de.fraunhofer.aisec.cpg.graph.types.quantumcpg.QuantumBitType
import de.fraunhofer.aisec.cpg.passes.configuration.RegisterExtraPass
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumDFGPass
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumEOGPass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File

@RegisterExtraPass(OpenQASMPass::class)
@RegisterExtraPass(QuantumEOGPass::class)
@RegisterExtraPass(QuantumDFGPass::class)
class OpenQasmLanguageFrontend(
    ctx: TranslationContext,
    language: Language<OpenQasmLanguageFrontend>,
) : LanguageFrontend<ASTNode, Any>(ctx = ctx, language = language) { // TODO

    companion object {
        @kotlin.jvm.JvmField var OPENQASM_EXTENSIONS: List<String> = listOf(".qasm")
    }

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        return parseInternal(file.readText(Charsets.UTF_8), file.path)
    }

    override fun setComment(node: Node, astNode: ASTNode) {
        TODO("Not yet implemented")
    }

    override fun locationOf(astNode: ASTNode): PhysicalLocation? {
        return null // TODO
    }

    override fun codeOf(astNode: ASTNode): String {
        return "TODO" // TODO
    }

    override fun typeOf(type: Any): Type {
        TODO("Not yet implemented")
    }

    private fun parseInternal(code: String, path: String): TranslationUnitDeclaration {
        val lexer = OpenQASMLexer(File(path))
        lexer.run()
        val parser = OpenQASMParser(lexer.tokens)
        val ast = parser.parse()
        val cpg = toCpg(ast)
        return cpg
    }

    private fun toCpg(ast: ProgramNode): TranslationUnitDeclaration {
        val tu =
            newTranslationUnitDeclaration(
                ast.location.artifactLocation.uri?.path ?: Name(""),
                rawNode = ast,
            )
        scopeManager.resetToGlobal(tu)
        val nsd = newNamespaceDeclaration("OpenQASM", rawNode = ast)
        scopeManager.addDeclaration(nsd)
        tu.addDeclaration(nsd)
        scopeManager.enterScope(nsd)
        for (stmt in ast.stmts) {
            nsd.statements += handleStatement(stmt)
        }
        scopeManager.leaveScope(nsd)
        scopeManager.addDeclaration(nsd)

        return tu
    }

    private fun handleExpression(expr: ExpressionNode): Expression {
        return when (expr) {
            is MultiplicativeExpressionNode -> handleMultiplicativeExpressionNode(expr)
            is IdentifierNode -> handleIdentifierNode(expr)
            is DecimalIntegerLiteralExpressionNode -> handleInteger(expr)
            else -> TODO()
        }
    }

    private fun handleInteger(expr: DecimalIntegerLiteralExpressionNode): Expression {
        return newLiteral(expr.payload, rawNode = expr)
    }

    private fun handleIdentifierNode(expr: IdentifierNode): Reference {
        return newReference(expr.payload, rawNode = expr)
    }

    private fun handleMultiplicativeExpressionNode(expr: MultiplicativeExpressionNode): Expression {
        val operator =
            when (expr) {
                is MultiplicativeSlashExpressionNode -> "/"
                is MultiplicativePercentExpressionNode -> "%"
                is MultiplicativeAsteriskExpressionNode -> "*"
                else -> {
                    TODO("Unknown multiplicative operator $expr")
                }
            }
        val binOp = newBinaryOperator(operator, rawNode = expr)
        binOp.lhs = handleExpression(expr.lhs)
        binOp.rhs = handleExpression(expr.rhs)
        return binOp
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
            is IfStatementNode -> handleIfStatement(stmt)
            is MeasureArrowAssignmentStatementNode -> handleMeasure(stmt)
            is OldStyleDeclarationStatementNode -> handleOldStylDeclStmt(stmt)
            is BarrierStatement ->
                newEmptyStatement() // TODO: Does nothing really useful. Maybe use "BlockStmt"
            // instead?
            else -> newProblemExpression("Expression $stmt is not supported yet")
        }
    }

    private fun handleOldStylDeclStmt(stmt: OldStyleDeclarationStatementNode): Statement {
        val name = stmt.idNode.payload
        val cnt: Number =
            (stmt.designator?.payload as? DecimalIntegerLiteralExpressionNode)?.payload ?: TODO()
        val collector = newBlock(rawNode = stmt)
        val tpe =
            when (stmt.type) {
                "QREG" -> QuantumBitType(language = language)
                "CREG" -> ClassicBitType(language = language)
                else -> TODO()
            }
        for (i in 0 until cnt as Int) {
            val v = newVariableDeclaration("$name[$i]", type = tpe, rawNode = stmt)
            scopeManager.addDeclaration(v)
            collector.addDeclaration(v)
            // TODO type
        }
        return collector
    }

    private fun handleMeasure(stmt: MeasureArrowAssignmentStatementNode): Statement {
        val ret = newBlock(rawNode = stmt)

        // TODO third expr, first()

        val lhsName =
            ((stmt.measureExpr.payload as? GateOperandNode)?.payload as? IndexedIdentifierNode)
                ?.identifier
                ?.payload ?: TODO()

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
            } ?: TODO()

        val lhsEndIdx =
            when (lhsRange) {
                is RangeExpressionNode ->
                    (lhsRange.secondExpr as? DecimalIntegerLiteralExpressionNode)?.payload
                is DecimalIntegerLiteralExpressionNode -> lhsRange.payload
                else -> TODO()
            } ?: TODO()

        val rhsName = (stmt.indexedIdentifier?.identifier as? IdentifierNode)?.payload ?: TODO()
        val rhsRange =
            (stmt.indexedIdentifier.indexOperators.first() as? IndexOperatorNode)?.exprs?.first()
        val rhsStartIdx =
            when (rhsRange) {
                is RangeExpressionNode ->
                    (rhsRange.firstExpr as? DecimalIntegerLiteralExpressionNode)?.payload
                is DecimalIntegerLiteralExpressionNode -> rhsRange.payload
                else -> TODO()
            } ?: TODO()
        val rhsEndIdx =
            when (rhsRange) {
                is RangeExpressionNode ->
                    (rhsRange.secondExpr as? DecimalIntegerLiteralExpressionNode)?.payload
                is DecimalIntegerLiteralExpressionNode -> rhsRange.payload
                else -> TODO()
            } ?: TODO()

        if (lhsEndIdx.toInt() - lhsStartIdx.toInt() != rhsEndIdx.toInt() - rhsStartIdx.toInt()) {
            TODO()
        }

        for (i in 0..(lhsEndIdx.toInt() - lhsStartIdx.toInt())) {
            val lhsIdx = lhsStartIdx.toInt() + i
            val lhsIdxName = "$lhsName[$lhsIdx]"
            val rhsIdx = rhsStartIdx.toInt() + i
            val rhsIdxName = "$rhsName[$rhsIdx]"

            val m = newCallExpression(newReference("measure"))
            m.addArgument(newReference(lhsIdxName))
            m.addArgument(newReference(rhsIdxName))
            /* newAssignExpression(
                lhs = listOf(newDeclaredReferenceExpression(lhsIdxName)),
                rhs = listOf(newDeclaredReferenceExpression(rhsIdxName))
            )*/
            ret.statements += m
        }

        return ret
    }

    private fun handleForStatement(stmt: ForStatementNode): Statement {
        val f = newForStatement(rawNode = stmt)
        // TODO
        return f
    }

    private fun handleIfStatement(stmt: IfStatementNode): Statement {
        val node = newClassicIf()

        return node
    }

    private fun handleClassicalBitDecl(stmt: ClassicalDeclarationStatementNode): Statement {
        val name = stmt.identifier.payload
        val cnt =
            when (stmt.tpe) {
                is ScalarTypeBitNode ->
                    (stmt.tpe.designatorNode?.payload as? DecimalIntegerLiteralExpressionNode)
                        ?.payload ?: TODO()
                is ScalarTypeUIntNode ->
                    (stmt.tpe.designatorNode?.payload as? DecimalIntegerLiteralExpressionNode)
                        ?.payload ?: TODO()
                else -> TODO()
            }

        val collector = newBlock()
        for (i in 0 until cnt as Int) {
            val v = newVariableDeclaration("$name[$i]", rawNode = stmt)
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
        val collector = newBlock()
        for (i in 0 until cnt as Int) {
            val v = newVariableDeclaration("$name[$i]", rawNode = stmt)
            scopeManager.addDeclaration(v)
            collector.addDeclaration(v)
            // TODO type
        }
        return collector
    }

    private fun handleGateCall(stmt: GateCallStatementNode): CallExpression {
        // handle gate call as function call

        val callee = newReference(stmt.identifier?.payload, rawNode = stmt)
        val call = newCallExpression(callee, rawNode = stmt)
        if (stmt.exprList != null) {
            for (e in stmt.exprList) {
                call.addArgument(handleExpression(e))
            }
        }
        if (stmt.gateOperandList != null) {
            for (p in stmt.gateOperandList.payload) {
                call.addArgument(handleCallArg(p))
            }
        }
        return call
    }

    private fun handleCallArg(p: GateOperandNode): Expression {
        return when (p.payload) {
            is IndexedIdentifierNode -> handleIndexedIdentifier(p.payload)
            else -> TODO()
        }
    }

    private fun handleIndexedIdentifier(indexedIdentifier: IndexedIdentifierNode): Expression {
        var name = indexedIdentifier.identifier.payload
        (indexedIdentifier.indexOperators.firstOrNull()?.exprs?.firstOrNull()
                as? DecimalIntegerLiteralExpressionNode)
            ?.payload
            ?.let { name += "[$it]" }
        return newReference(name, rawNode = indexedIdentifier)
    }

    private fun handleGateStatement(stmt: GateStatementNode): Statement {
        // handle gates as "functions"
        val func = newFunctionDeclaration(stmt.identifier, rawNode = stmt)
        scopeManager.enterScope(func)
        if (stmt.identifierList != null) {
            for (p in stmt.identifierList.identifiers) {
                func.parameters += newParameterDeclaration(p, rawNode = stmt.identifierList)
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
        val ret = newBlock(rawNode = scope)
        for (s in scope.stmtList) {
            ret.statements += handleStatement(s)
        }
        return ret
    }
}
