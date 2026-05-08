/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.php

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Handles PHP declaration nodes: functions, classes, namespaces, and parameters. Top-level
 * statements are dispatched through [handleTopStatement].
 */
class DeclarationHandler(frontend: PHPLanguageFrontend) :
    PHPHandler<Declaration, ParserRuleContext>({ ProblemDeclaration() }, frontend) {

    /** Dispatches a top-level statement into the given [TranslationUnit]. */
    fun handleTopStatement(stmt: PhpParser.TopStatementContext, tu: TranslationUnit) {
        when {
            stmt.functionDeclaration() != null ->
                handleFunctionDeclaration(stmt.functionDeclaration()).also {
                    frontend.scopeManager.addDeclaration(it)
                    tu.declarations += it
                }
            stmt.classDeclaration() != null ->
                handleClassDeclaration(stmt.classDeclaration()).also {
                    frontend.scopeManager.addDeclaration(it)
                    tu.declarations += it
                }
            stmt.namespaceDeclaration() != null ->
                handleNamespaceDeclaration(stmt.namespaceDeclaration(), tu)
            stmt.statement() != null -> {
                val stmtNode = frontend.statementHandler.handle(stmt.statement())
                tu += stmtNode
            }
            else -> {
                // useDeclaration, globalConstantDeclaration, enumDeclaration – stub
            }
        }
    }

    override fun handleNode(node: ParserRuleContext): Declaration {
        return when (node) {
            is PhpParser.FunctionDeclarationContext -> handleFunctionDeclaration(node)
            is PhpParser.ClassDeclarationContext -> handleClassDeclaration(node)
            is PhpParser.FormalParameterContext -> handleFormalParameter(node)
            else -> handleNotSupported(node, node::class.simpleName ?: "unknown")
        }
    }

    // ── Functions ────────────────────────────────────────────────────────────

    private fun handleFunctionDeclaration(ctx: PhpParser.FunctionDeclarationContext): Function {
        val name = ctx.identifier()?.text ?: ""
        val func = frontend.newFunction(name, rawNode = ctx)

        frontend.scopeManager.enterScope(func)

        // Return type
        ctx.typeHint()?.let { func.returnTypes = listOf(frontend.typeOf(it.text)) }

        // Parameters
        ctx.formalParameterList()?.formalParameter()?.forEach { param ->
            val p = handleFormalParameter(param)
            frontend.scopeManager.addDeclaration(p)
            func.parameters += p
        }

        // Body
        func.body = frontend.statementHandler.handle(ctx.blockStatement())

        frontend.scopeManager.leaveScope(func)
        return func
    }

    // ── Classes ──────────────────────────────────────────────────────────────

    private fun handleClassDeclaration(ctx: PhpParser.ClassDeclarationContext): Record {
        // In the grammar, identifier() returns a single context (one class name)
        val name = ctx.identifier()?.text ?: ""
        val kind =
            when {
                ctx.Interface() != null -> "interface"
                ctx.classEntryType()?.Trait() != null -> "class" // treat traits as classes
                else -> "class"
            }
        val record = frontend.newRecord(name, kind, rawNode = ctx)

        frontend.scopeManager.enterScope(record)

        // Superclass (Extends clause – qualifiedStaticTypeRef() returns single context)
        ctx.qualifiedStaticTypeRef()?.let { superType ->
            record.superClasses += listOf(frontend.objectType(superType.text))
        }

        // Members
        ctx.classStatement()?.forEach { stmt -> handleClassStatement(stmt, record) }

        frontend.scopeManager.leaveScope(record)
        return record
    }

    private fun handleClassStatement(ctx: PhpParser.ClassStatementContext, record: Record) {
        when {
            // method
            ctx.Function_() != null -> {
                val name = ctx.identifier()?.text ?: ""
                val method = frontend.newMethod(name, rawNode = ctx)
                method.recordDeclaration = record

                frontend.scopeManager.enterScope(method)

                ctx.returnTypeDecl()?.typeHint()?.let {
                    method.returnTypes = listOf(frontend.typeOf(it.text))
                }

                ctx.formalParameterList()?.formalParameter()?.forEach { param ->
                    val p = handleFormalParameter(param)
                    frontend.scopeManager.addDeclaration(p)
                    method.parameters += p
                }

                ctx.methodBody()?.blockStatement()?.let {
                    method.body = frontend.statementHandler.handle(it)
                }

                frontend.scopeManager.leaveScope(method)
                frontend.scopeManager.addDeclaration(method)
                record.methods += method
            }
            // property
            ctx.variableInitializer().isNotEmpty() -> {
                ctx.variableInitializer().forEach { vi ->
                    val fieldName = vi.VarName()?.text?.removePrefix("$") ?: return@forEach
                    val initExpr =
                        if (vi.constantInitializer() != null)
                            frontend.expressionHandler.handleConstantInitializer(
                                vi.constantInitializer()
                            )
                        else null
                    val field = frontend.newField(fieldName, initializer = initExpr, rawNode = vi)
                    frontend.scopeManager.addDeclaration(field)
                    record.fields += field
                }
            }
            else -> {
                // const, trait use – skip
            }
        }
    }

    // ── Parameters ───────────────────────────────────────────────────────────

    internal fun handleFormalParameter(ctx: PhpParser.FormalParameterContext): Parameter {
        val varInit = ctx.variableInitializer()
        val rawName = varInit?.VarName()?.text ?: ""
        val name = rawName.removePrefix("$")
        val isVariadic = ctx.children?.any { it.text == "..." } == true

        val type = ctx.typeHint()?.let { frontend.typeOf(it.text) } ?: frontend.autoType()

        val param = frontend.newParameter(name, type, variadic = isVariadic, rawNode = ctx)

        // default value
        if (varInit?.constantInitializer() != null) {
            param.default =
                frontend.expressionHandler.handleConstantInitializer(varInit.constantInitializer())
        }

        return param
    }

    // ── Namespaces ───────────────────────────────────────────────────────────

    private fun handleNamespaceDeclaration(
        ctx: PhpParser.NamespaceDeclarationContext,
        tu: TranslationUnit,
    ) {
        val namespaceName = ctx.namespaceNameList()?.text ?: ""
        val ns = frontend.newNamespace(namespaceName, rawNode = ctx)

        frontend.scopeManager.enterScope(ns)

        ctx.namespaceStatement()?.forEach { nsStmt ->
            when {
                nsStmt.functionDeclaration() != null ->
                    handleFunctionDeclaration(nsStmt.functionDeclaration()).also {
                        frontend.scopeManager.addDeclaration(it)
                        ns.declarations += it
                    }
                nsStmt.classDeclaration() != null ->
                    handleClassDeclaration(nsStmt.classDeclaration()).also {
                        frontend.scopeManager.addDeclaration(it)
                        ns.declarations += it
                    }
                nsStmt.statement() != null -> {
                    val stmtNode = frontend.statementHandler.handle(nsStmt.statement())
                    ns += stmtNode
                }
                else -> {}
            }
        }

        frontend.scopeManager.leaveScope(ns)
        frontend.scopeManager.addDeclaration(ns)
        tu.declarations += ns
    }
}
