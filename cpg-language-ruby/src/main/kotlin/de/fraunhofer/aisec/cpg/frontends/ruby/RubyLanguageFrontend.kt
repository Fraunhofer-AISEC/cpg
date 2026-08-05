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
package de.fraunhofer.aisec.cpg.frontends.ruby

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsNewParse
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.file.Path
import org.jruby.Ruby
import org.jruby.ast.BlockNode
import org.jruby.ast.ClassNode
import org.jruby.ast.MethodDefNode
import org.jruby.ast.RootNode
import org.jruby.parser.Parser
import org.jruby.parser.ParserConfiguration

class RubyLanguageFrontend(ctx: TranslationContext, language: RubyLanguage) :
    LanguageFrontend<org.jruby.ast.Node, org.jruby.ast.Node>(ctx, language), SupportsNewParse {
    val declarationHandler: DeclarationHandler = DeclarationHandler(this)
    val expressionHandler: ExpressionHandler = ExpressionHandler(this)
    val statementHandler: StatementHandler = StatementHandler(this)

    override fun parse(file: File): TranslationUnit {
        return parse(file.readText(Charsets.UTF_8), file.toPath())
    }

    override fun parse(content: String, path: Path?): TranslationUnit {
        val ruby = Ruby.getGlobalRuntime()
        val parser = Parser(ruby)

        val node =
            parser.parse(
                if (path != null) {
                    path.toString()
                } else {
                    "unknown"
                },
                content.byteInputStream(),
                null,
                ParserConfiguration(ruby, 0, false, true, false),
            ) as RootNode

        return handleRootNode(node)
    }

    private fun handleRootNode(node: RootNode): TranslationUnit {
        val tu = newTranslationUnit(node.file, rawNode = node)

        scopeManager.resetToGlobal(tu)

        // The root node can either contain a single node or a block node. We normalize both cases
        // into a flat list of top-level nodes.
        val topLevelNodes =
            when (val body = node.bodyNode) {
                is BlockNode -> body.filterNotNull()
                null -> emptyList()
                else -> listOf(body)
            }

        for (innerNode in topLevelNodes) {
            // Method definitions (`def`) and class definitions (`class`) become declarations,
            // everything else is treated as a top-level statement. Modules (`module M ... end`,
            // a `ModuleNode`) share the same public/protected/private semantics for their instance
            // methods, but module member visibility is out of scope for now.
            if (innerNode is MethodDefNode || innerNode is ClassNode) {
                val decl = declarationHandler.handle(innerNode)
                scopeManager.addDeclaration(decl)
                tu.declarations += decl
            } else {
                val stmt = statementHandler.handle(innerNode)
                tu += stmt
            }
        }

        return tu
    }

    override fun codeOf(astNode: org.jruby.ast.Node): String? {
        return ""
    }

    override fun locationOf(astNode: org.jruby.ast.Node): PhysicalLocation? {
        return null
    }

    override fun typeOf(type: org.jruby.ast.Node): Type {
        return autoType()
    }

    override fun setComment(node: Node, astNode: org.jruby.ast.Node) {
        // not yet implemented
    }
}
