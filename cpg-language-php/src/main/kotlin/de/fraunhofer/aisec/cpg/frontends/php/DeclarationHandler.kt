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
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Handles PHP declaration nodes: functions, classes, namespaces, and parameters. Top-level
 * statements are dispatched through [handleTopStatement].
 */
class DeclarationHandler(frontend: PHPLanguageFrontend) :
    PHPHandler<Declaration, ParserRuleContext>({ ProblemDeclaration() }, frontend) {

    /**
     * Extracts a namespace declaration from a top-level statement, including the grammar fallback
     * where `namespace Foo\Bar;` is parsed as an expression statement.
     */
    fun handleNamespaceTopStatement(
        stmt: PhpParser.TopStatementContext,
        tu: TranslationUnit,
    ): Namespace? {
        stmt.namespaceDeclaration()?.let {
            return handleNamespaceDeclaration(it, tu)
        }
        return handleImplicitNamespaceDeclaration(stmt, tu)
    }

    /** Dispatches a top-level statement into the translation unit or the active namespace. */
    fun handleTopStatement(
        stmt: PhpParser.TopStatementContext,
        tu: TranslationUnit,
        namespace: Namespace? = null,
    ) {
        when {
            stmt.functionDeclaration() != null ->
                handleFunctionDeclaration(stmt.functionDeclaration()).also {
                    frontend.scopeManager.addDeclaration(it)
                    addDeclarationToContainer(it, tu, namespace)
                }
            stmt.classDeclaration() != null ->
                handleClassDeclaration(stmt.classDeclaration()).also {
                    frontend.scopeManager.addDeclaration(it)
                    addDeclarationToContainer(it, tu, namespace)
                }
            stmt.namespaceDeclaration() != null ->
                handleNamespaceDeclaration(stmt.namespaceDeclaration(), tu)
            stmt.statement() != null -> {
                val stmtNode = frontend.statementHandler.handle(stmt.statement())
                addStatementToContainer(stmtNode, tu, namespace)
            }
            else -> {
                // useDeclaration, globalConstantDeclaration, enumDeclaration – stub
            }
        }
    }

    /** Dispatches a parser node to the corresponding declaration handler. */
    override fun handleNode(node: ParserRuleContext): Declaration {
        return when (node) {
            is PhpParser.FunctionDeclarationContext -> handleFunctionDeclaration(node)
            is PhpParser.ClassDeclarationContext -> handleClassDeclaration(node)
            is PhpParser.FormalParameterContext -> handleFormalParameter(node)
            else -> handleNotSupported(node, node::class.simpleName ?: "unknown")
        }
    }

    // ── Functions ────────────────────────────────────────────────────────────

    /** Models a PHP function declaration including parameters, return type, and body. */
    private fun handleFunctionDeclaration(ctx: PhpParser.FunctionDeclarationContext): Function {
        val name = ctx.identifier()?.text ?: ""
        val func = newFunction(name, rawNode = ctx)

        frontend.scopeManager.enterScope(func)

        // Return type
        resolveTypeName(ctx.typeHint(), ctx.QuestionMark() != null)?.let {
            func.returnTypes = listOf(frontend.typeOf(it))
        }

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

    /** Models a PHP class-like declaration and its members. */
    private fun handleClassDeclaration(ctx: PhpParser.ClassDeclarationContext): Record {
        // In the grammar, identifier() returns a single context (one class name)
        val name = ctx.identifier()?.text ?: ""
        val kind =
            when {
                ctx.Interface() != null -> "interface"
                ctx.classEntryType()?.Trait() != null -> "class" // treat traits as classes
                else -> "class"
            }
        val record = newRecord(name, kind, rawNode = ctx)

        frontend.scopeManager.enterScope(record)

        // Superclass (Extends clause – qualifiedStaticTypeRef() returns single context)
        ctx.qualifiedStaticTypeRef()?.let { superType ->
            record.superClasses += listOf(objectType(superType.text))
        }

        // Members
        ctx.classStatement()?.forEach { stmt -> handleClassStatement(stmt, record) }

        frontend.scopeManager.leaveScope(record)
        return record
    }

    /** Models a class member declaration and adds it to the owning [record]. */
    private fun handleClassStatement(ctx: PhpParser.ClassStatementContext, record: Record) {
        when {
            // method
            ctx.Function_() != null -> {
                val name = ctx.identifier()?.text ?: ""
                val method = newMethod(name, rawNode = ctx)
                method.recordDeclaration = record

                frontend.scopeManager.enterScope(method)

                resolveTypeName(
                        ctx.returnTypeDecl()?.typeHint(),
                        ctx.returnTypeDecl()?.QuestionMark() != null,
                    )
                    ?.let { method.returnTypes = listOf(frontend.typeOf(it)) }

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
                    val field = newField(fieldName, initializer = initExpr, rawNode = vi)
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

    /** Models a formal parameter including its type, variadic flag, and default value. */
    internal fun handleFormalParameter(ctx: PhpParser.FormalParameterContext): Parameter {
        val varInit = ctx.variableInitializer()
        val rawName = varInit?.VarName()?.text ?: ""
        val name = rawName.removePrefix("$")
        val isVariadic = ctx.children?.any { it.text == "..." } == true

        val type =
            resolveTypeName(ctx.typeHint(), ctx.QuestionMark() != null)?.let { frontend.typeOf(it) }
                ?: autoType()

        val param = newParameter(name, type, variadic = isVariadic, rawNode = ctx)

        // default value
        if (varInit?.constantInitializer() != null) {
            param.default =
                frontend.expressionHandler.handleConstantInitializer(varInit.constantInitializer())
        }

        return param
    }

    // ── Namespaces ───────────────────────────────────────────────────────────

    /** Models a namespace declaration and returns it when it stays active after the statement. */
    fun handleNamespaceDeclaration(
        ctx: PhpParser.NamespaceDeclarationContext,
        tu: TranslationUnit,
    ): Namespace? {
        val ns = createNamespace(ctx.namespaceNameList()?.text ?: "", ctx, tu)

        if (ctx.OpenCurlyBracket() == null) {
            return ns
        }

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
        return null
    }

    /**
     * Models semicolon-style namespace declarations that the grammar currently exposes as
     * expression statements.
     */
    private fun handleImplicitNamespaceDeclaration(
        stmt: PhpParser.TopStatementContext,
        tu: TranslationUnit,
    ): Namespace? {
        val statementText = stmt.statement()?.expressionStatement()?.text ?: return null
        if (!statementText.startsWith("namespace") || !statementText.endsWith(";")) {
            return null
        }

        val namespaceName = statementText.removePrefix("namespace").removeSuffix(";")
        return createNamespace(namespaceName, stmt, tu)
    }

    /** Creates a namespace node, registers it in the current scope, and attaches it to the TU. */
    private fun createNamespace(
        namespaceName: String,
        rawNode: ParserRuleContext,
        tu: TranslationUnit,
    ): Namespace {
        val ns = newNamespace(namespaceName, rawNode = rawNode)
        frontend.scopeManager.addDeclaration(ns)
        tu.declarations += ns
        return ns
    }

    /** Adds a declaration either to the active namespace or directly to the translation unit. */
    private fun addDeclarationToContainer(
        declaration: Declaration,
        tu: TranslationUnit,
        namespace: Namespace?,
    ) {
        if (namespace != null) {
            namespace.declarations += declaration
        } else {
            tu.declarations += declaration
        }
    }

    /** Adds a statement either to the active namespace body or directly to the translation unit. */
    private fun addStatementToContainer(
        statement: Expression,
        tu: TranslationUnit,
        namespace: Namespace?,
    ) {
        if (namespace != null) {
            namespace += statement
        } else {
            tu += statement
        }
    }

    /** Returns the normalized type text including nullable marker when present. */
    private fun resolveTypeName(typeHint: PhpParser.TypeHintContext?, nullable: Boolean): String? {
        val typeName = typeHint?.text ?: return null
        return when {
            nullable && !typeName.startsWith("?") -> "?$typeName"
            !nullable && typeName.startsWith("?") -> typeName.removePrefix("?")
            else -> typeName
        }
    }
}
