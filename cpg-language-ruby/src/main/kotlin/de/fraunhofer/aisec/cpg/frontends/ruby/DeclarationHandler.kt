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

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.newFunction
import de.fraunhofer.aisec.cpg.graph.newMethod
import de.fraunhofer.aisec.cpg.graph.newParameter
import de.fraunhofer.aisec.cpg.graph.newRecord
import de.fraunhofer.aisec.cpg.graph.newReturn
import org.jruby.ast.ArgumentNode
import org.jruby.ast.BlockNode
import org.jruby.ast.ClassNode
import org.jruby.ast.DefnNode
import org.jruby.ast.FCallNode
import org.jruby.ast.Node
import org.jruby.ast.SymbolNode
import org.jruby.ast.VCallNode

class DeclarationHandler(lang: RubyLanguageFrontend) :
    RubyHandler<Declaration, Node>({ ProblemDeclaration() }, lang) {

    override fun handleNode(node: Node): Declaration {
        return when (node) {
            is ArgumentNode -> handleArgumentNode(node)
            is DefnNode -> handleDefnNode(node)
            is ClassNode -> handleClassNode(node)
            else -> handleNotSupported(node, node::class.simpleName ?: "")
        }
    }

    private fun handleArgumentNode(node: ArgumentNode): Declaration {
        return newParameter(node.name.idString(), variadic = false)
    }

    /**
     * Handles a top-level `def`. Ruby method visibility only carries meaning for record members, so
     * a top-level function is left with the default
     * [de.fraunhofer.aisec.cpg.graph.Visibility.UNKNOWN]. Members defined inside a [ClassNode] are
     * handled by [handleClassNode].
     */
    private fun handleDefnNode(node: DefnNode): Function {
        val func = newFunction(node.name.idString(), rawNode = node)
        populateFunction(node, func)
        return func
    }

    /**
     * Handles a Ruby `class` definition. In addition to building the [Record] and its methods, this
     * tracks the *current visibility* while walking the class body: a bare `public`/`protected`/
     * `private` statement flips the default visibility for all subsequently-defined methods,
     * whereas the `private def ...` / `private :symbol` form applies to a single method only. See
     * [RubyLanguage.applyModifiers] for how the raw keyword is mapped onto the canonical
     * [de.fraunhofer.aisec.cpg.graph.Visibility].
     */
    private fun handleClassNode(node: ClassNode): Record {
        val record = newRecord(node.cPath.name.idString(), "class", rawNode = node)

        frontend.scopeManager.enterScope(record)

        // Ruby's default method visibility is public.
        var currentVisibility = RubyLanguage.PUBLIC

        for (child in bodyNodesOf(node.bodyNode)) {
            val keyword = visibilityKeyword(child)
            if (keyword != null) {
                val targets = visibilityTargets(child)
                if (targets.isEmpty()) {
                    // A bare `private`/`protected`/`public` flips the default for the methods that
                    // follow it in the class body.
                    currentVisibility = keyword
                } else {
                    // `private def foo ... end` or `private :foo` only affects the listed methods.
                    for (target in targets) {
                        when (target) {
                            is DefnNode -> addMethod(record, target, keyword)
                            is SymbolNode ->
                                applyVisibilityToExisting(record, target.name.idString(), keyword)
                            else -> {}
                        }
                    }
                }
                continue
            }

            if (child is DefnNode) {
                addMethod(record, child, currentVisibility)
            }
            // Other statements inside a class body (e.g. constant assignments) are not modeled yet.
        }

        frontend.scopeManager.leaveScope(record)

        return record
    }

    /**
     * Builds a [Method] for [node], records the raw [visibilityKeyword] and its canonical mapping.
     */
    private fun addMethod(record: Record, node: DefnNode, visibilityKeyword: String): Method {
        val method = newMethod(node.name.idString(), recordDeclaration = record, rawNode = node)

        // Keep the raw, lossless keyword in the modifiers and let the language project it onto the
        // canonical visibility, so passes such as the SymbolResolver can reason about access
        // control without knowing Ruby's concrete keywords.
        method.modifiers = method.modifiers + visibilityKeyword
        language.applyModifiers(method, frontend.scopeManager.currentScope)

        populateFunction(node, method)

        frontend.scopeManager.addDeclaration(method)
        record.addDeclaration(method)

        return method
    }

    /**
     * Retroactively applies [visibilityKeyword] to an already-built method with the given [name],
     * as produced by the `private :foo` form.
     */
    private fun applyVisibilityToExisting(record: Record, name: String, visibilityKeyword: String) {
        val method = record.methods.firstOrNull { it.name.localName == name } ?: return
        method.modifiers =
            method.modifiers
                .filterNotTo(mutableSetOf()) { it in RubyLanguage.visibilityModifiers }
                .apply { add(visibilityKeyword) }
        language.applyModifiers(method, frontend.scopeManager.currentScope)
    }

    /**
     * Returns the visibility keyword (`public`/`protected`/`private`) if [node] is a Ruby
     * visibility modifier statement, or `null` otherwise. Bare modifiers are parsed as
     * [VCallNode]s, the `private def ...` / `private :foo` forms as [FCallNode]s.
     */
    private fun visibilityKeyword(node: Node): String? {
        val name =
            when (node) {
                is VCallNode -> node.name.idString()
                is FCallNode -> node.name.idString()
                else -> return null
            }
        return name.takeIf { it in RubyLanguage.visibilityModifiers }
    }

    /** Returns the argument nodes of a visibility modifier call (empty for a bare modifier). */
    private fun visibilityTargets(node: Node): List<Node> {
        return when (node) {
            is FCallNode -> node.argsNode?.childNodes() ?: emptyList()
            else -> emptyList()
        }
    }

    /** Normalizes a (possibly `null` or single) body node into a flat list of child nodes. */
    private fun bodyNodesOf(body: Node?): List<Node> {
        return when (body) {
            null -> emptyList()
            is BlockNode -> body.filterNotNull()
            else -> listOf(body)
        }
    }

    /** Fills [func] with parameters and body extracted from the given [DefnNode]. */
    private fun populateFunction(node: DefnNode, func: Function) {
        frontend.scopeManager.enterScope(func)

        for (arg in node.argsNode.args) {
            val param = this.handle(arg) as? Parameter
            if (param == null) {
                continue
            }

            frontend.scopeManager.addDeclaration(param)
            func.parameters += param
        }

        val body = frontend.statementHandler.handle(node.bodyNode)
        if (body is Block) {
            // get the last statement
            val lastStatement = body.statements.lastOrNull()

            // add an implicit return statement, if there is no return statement
            if (lastStatement !is Return) {
                val returnStatement = newReturn()
                returnStatement.isImplicit = true
                body.statements += returnStatement

                // TODO: Ruby returns the last expression, if there is no explicit return
            }
        }
        func.body = body

        frontend.scopeManager.leaveScope(func)
    }
}
