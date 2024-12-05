/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.backends.golang

import de.fraunhofer.aisec.cpg.LanguageBackend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type

private const val COMMA = ", "

class GoLanguageBackend : LanguageBackend() {

    override fun translate(
        decl: Declaration,
        indent: Int,
        predicate: ((Node) -> Boolean)?
    ): String? {
        if (decl is TranslationUnitDeclaration) {
            val builder = StringBuilder()

            writeTranslationDeclaration(builder, decl)

            return builder.toString()
        }

        return null
    }

    override fun translate(type: Type?): String? {
        return if (type is PointerType) {
            if (type.isArray) {
                "[]${translate(type.elementType)}"
            } else {
                "*${translate(type.elementType)}"
            }
        } else {
            type?.name.toString()
        }
    }

    fun writeTranslationDeclaration(builder: StringBuilder, tu: TranslationUnitDeclaration) {
        // We need to hack imports later (also ignore any std inferred namespaces)
        val declarations =
            tu.declarations.filter { it is NamespaceDeclaration && it.name.localName != "unsafe" }
        val imports = tu.declarations.filterIsInstance<IncludeDeclaration>()

        // The tu MUST only have a single namespace declaration
        val pkg =
            declarations.singleOrNull() as? NamespaceDeclaration
                ?: throw TranslationException(
                    "top-level TU must only consist of one namespace declaration"
                )
        writeNamespaceDeclaration(builder, pkg, imports)
    }

    fun writeNamespaceDeclaration(
        builder: StringBuilder,
        pkg: NamespaceDeclaration,
        imports: List<IncludeDeclaration>
    ) {
        builder.append("package ${pkg.path}\n\n")

        // Loop through declarations
        for (decl in pkg.declarations) {
            builder.append(writeDeclaration(decl))
        }
    }

    fun writeDeclaration(decl: Declaration): String {
        return when (decl) {
            is FunctionDeclaration -> {
                writeFunctionDeclaration(decl)
            }
            is VariableDeclaration -> {
                "${decl.name} ${translate(decl.type)}"
            }
            else -> {
                ""
            }
        }
    }

    private fun writeParameter(it: ParameterDeclaration): String {
        return "${it.name.localName.ifEmpty { "_" }} ${translate(it.type)}"
    }

    private fun writeFunctionDeclaration(decl: FunctionDeclaration): String {
        return "func ${decl.name.localName}${writeParameters(decl)} {\n${decl.body?.let { writeStatement(it) } ?: ""}}\n\n"
    }

    private fun writeParameters(decl: FunctionDeclaration): String {
        return decl.parameters.joinToString(COMMA, "(", ")") { writeParameter(it) }
    }

    fun writeStatement(stmt: Statement?): String {
        return when (stmt) {
            is Block -> {
                writeBlock(stmt)
            }
            is DeclarationStatement -> {
                writeDeclarationStatement(stmt)
            }
            is IfStatement -> {
                writeIfStatement(stmt)
            }
            is ReturnStatement -> {
                writeReturnStatement(stmt)
            }
            is Expression -> {
                // All expressions are statements, but we need to have them one an individual line
                "${writeExpression(stmt)}\n"
            }
            else -> {
                "\n"
            }
        }
    }

    private fun writeReturnStatement(stmt: ReturnStatement): String {
        return if (stmt.returnValues.isEmpty()) {
            "return\n"
        } else {
            "return ${stmt.returnValues.joinToString(COMMA) { writeExpression(it) }}\n"
        }
    }

    private fun writeIfStatement(stmt: IfStatement): String {
        return "if ${writeExpression(stmt.condition)} {\n${writeStatement(stmt.thenStatement)}\t}\n"
    }

    private fun writeAssignExpression(assign: AssignExpression): String {
        return "${assign.lhs.joinToString(COMMA) { writeExpression(it) }} = ${assign.rhs.joinToString(COMMA) { writeExpression(it) }}"
    }

    private fun writeExpression(expr: Expression?): String {
        return when (expr) {
            is Reference -> {
                writeReference(expr)
            }
            is AssignExpression -> {
                writeAssignExpression(expr)
            }
            is BinaryOperator -> {
                writeBinaryOperator(expr)
            }
            is CallExpression -> {
                writeCallExpression(expr)
            }
            is SubscriptExpression -> {
                writeSubscriptExpression(expr)
            }
            is Literal<*> -> {
                writeLiteral(expr)
            }
            else -> {
                ""
            }
        }
    }

    private fun writeSubscriptExpression(expr: SubscriptExpression): String {
        return "${writeExpression(expr.arrayExpression)}[${writeExpression(expr.subscriptExpression)}]"
    }

    private fun writeBinaryOperator(expr: BinaryOperator): String {
        return "${writeExpression(expr.lhs)} ${expr.operatorCode} ${writeExpression(expr.rhs)}"
    }

    private fun writeLiteral(expr: Literal<*>): String {
        return when (expr.value) {
            is String -> {
                "\"${expr.value}\""
            }
            is Number -> {
                expr.value.toString()
            }
            null -> {
                "nil"
            }
            else -> {
                ""
            }
        }
    }

    private fun writeCallExpression(expr: CallExpression): String {
        return "${expr.callee!!.name}${expr.arguments.joinToString(COMMA, prefix = "(", postfix = ")") { writeExpression(it)}}"
    }

    private fun writeReference(expr: Reference): String {
        return expr.name.toString()
    }

    private fun writeDeclarationStatement(stmt: DeclarationStatement): String {
        // We can only have value declarations at this point
        val valueDecls = stmt.declarations.filterIsInstance<ValueDeclaration>()
        // We can either make it a single definition of a multi-line
        val single = valueDecls.singleOrNull()
        return if (single != null) {
            "var ${writeDeclaration(single)}"
        } else {
            "var ${valueDecls.joinToString("\n\t\t", prefix = "(\n\t\t", postfix = "\n\t)") {writeDeclaration(it)}}\n"
        }
    }

    private fun writeBlock(block: Block): String {
        return block.statements.joinToString("\n") { "\t${writeStatement(it)}" }
    }
}
