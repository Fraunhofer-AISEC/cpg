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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsNewParse
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Namespace
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token

/**
 * A PHP language frontend that uses the ANTLR4-based PHP grammar (MIT-licensed, from
 * antlr/grammars-v4) to parse PHP 8 source code into a CPG.
 */
class PHPLanguageFrontend(ctx: TranslationContext, language: PHPLanguage) :
    LanguageFrontend<ParserRuleContext, ParserRuleContext>(ctx, language), SupportsNewParse {

    val declarationHandler: DeclarationHandler = DeclarationHandler(this)
    val statementHandler: StatementHandler = StatementHandler(this)
    val expressionHandler: ExpressionHandler = ExpressionHandler(this)

    /** The source file path – kept to build [PhysicalLocation] objects. */
    internal var filePath: Path? = null

    /** Parses a PHP source file into a [TranslationUnit]. */
    override fun parse(file: File): TranslationUnit {
        return parse(file.readText(StandardCharsets.UTF_8), file.toPath())
    }

    /** Parses PHP source code and models top-level namespace sections and declarations. */
    override fun parse(content: String, path: Path?): TranslationUnit {
        filePath = path

        val charStream = CharStreams.fromString(content)
        val lexer = PhpLexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = PhpParser(tokens)

        val document = parser.htmlDocument()
        val tu = newTranslationUnit(path?.toString() ?: "unknown.php", rawNode = document)

        scopeManager.resetToGlobal(tu)
        var activeNamespace: Namespace? = null

        for (block in document.phpBlock()) {
            for (stmt in block.topStatement()) {
                val namespaceDeclaration = declarationHandler.handleNamespaceTopStatement(stmt, tu)
                if (namespaceDeclaration != null) {
                    activeNamespace?.let { scopeManager.leaveScope(it) }
                    activeNamespace = namespaceDeclaration
                    scopeManager.enterScope(namespaceDeclaration)
                } else {
                    declarationHandler.handleTopStatement(stmt, tu, activeNamespace)
                }
            }
        }

        activeNamespace?.let { scopeManager.leaveScope(it) }
        return tu
    }

    /** Returns the raw source snippet represented by the parser node. */
    override fun codeOf(astNode: ParserRuleContext): String? {
        return astNode.text
    }

    /** Computes the physical location of the parser node within the current source file. */
    override fun locationOf(astNode: ParserRuleContext): PhysicalLocation? {
        val start = astNode.start ?: return null
        val stop = astNode.stop ?: start
        val uri = filePath?.toUri() ?: return null
        // ANTLR uses 1-based line and 0-based column; CPG Region uses 1-based both
        val region =
            Region(
                start.line,
                start.charPositionInLine + 1,
                stop.line,
                stop.charPositionInLine + stop.text.length + 1,
            )
        return PhysicalLocation(uri, region)
    }

    /** Resolves a parser rule that denotes a type to a CPG [Type]. */
    override fun typeOf(type: ParserRuleContext): Type {
        return typeOf(type.text)
    }

    /**
     * Resolves a PHP type-hint string (e.g. "string", "int", "\\App\\Foo") to a CPG [Type]. Falls
     * back to [autoType] for unknown or compound types.
     */
    fun typeOf(typeName: String?): Type {
        if (typeName == null || typeName.contains("|") || typeName.contains("?")) {
            return autoType()
        }
        val stripped = typeName.trimStart('\\')
        return language.builtInTypes[stripped] ?: objectType(stripped)
    }

    /**
     * Attaches comments from the parser node to the CPG node once comment handling is implemented.
     */
    override fun setComment(node: Node, astNode: ParserRuleContext) {
        // not yet implemented
    }

    /** Convenience to get the raw text of a [Token]. */
    fun tokenText(token: Token?): String = token?.text ?: ""
}
