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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.IncludeDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.order.ExecuteBefore

/**
 * This pass takes care of several things that we need to clean up, once all translation units are
 * successfully parsed, but before any of the remaining CPG passes, such as call resolving occurs.
 *
 * ## Add Type Listeners for Key/Value Variables in For-Each Statements
 *
 * In Go, a common idiom is to use a short assignment in a for-each statement to declare a key and
 * value object without explicitly specifying the type.
 *
 * ```go
 * var bytes = []byte{1,2,3,4}
 * for key, value := range bytes {
 *   // key is of type int; value of type byte
 *   fmt.Printf("bytes[%d]=%d\n", key, value)
 * }
 * ```
 *
 * The key variable is always of type `int`, whereas the value variable depends on the iterated
 * expression. Therefore, we set up type listeners based on the iterated object.
 *
 * ## Infer NamespaceDeclarations for Import Packages
 *
 * We want to infer namespace declarations for import packages, that are unknown to us. This allows
 * us to then infer functions in those packages as well.
 *
 * ## Declare Variables in Short Assignments
 *
 * We want to implicitly declare variables in a short assignment. We cannot do this in the frontend
 * itself, because some of the variables in the assignment might already exist, and those are not
 * declared, but just assigned. Only the non-defined variables are declared by the short assignment.
 *
 * The following short assignment (in the second line) only declares the variable `b` but assigns
 * `1` to the already existing variable `a` and `2` to the new variable `b`.
 *
 * ```go
 * var a int
 * a, b := 1, 2
 * ```
 *
 * In the frontend we only do the assignment, therefore we need to create a new
 * [VariableDeclaration] for `b` and inject a [DeclarationStatement].
 *
 * ## Converting Call Expressions into Cast Expressions
 *
 * In Go, it is possible to convert compatible types by "calling" the type name as a function, such
 * as
 *
 * ```go
 * var i = int(2.0)
 * ```
 *
 * This is also possible with more complex types, such as interfaces or aliased types, as long as
 * they are compatible. Because types in the same package can be defined in multiple files, we
 * cannot decide during the frontend run. Therefore, we need to execute this pass before the
 * [CallResolver] and convert certain [CallExpression] nodes into a [CastExpression].
 */
@ExecuteBefore(VariableUsageResolver::class)
@ExecuteBefore(CallResolver::class)
@ExecuteBefore(DFGPass::class)
class GoExtraPass : Pass(), ScopeProvider {

    override val scope: Scope?
        get() = scopeManager.currentScope

    override fun accept(t: TranslationResult) {
        scopeManager = t.scopeManager

        val walker = SubgraphWalker.ScopedWalker(scopeManager)
        walker.registerHandler { _, parent, node ->
            when (node) {
                is CallExpression -> handleCall(node, parent)
                is IncludeDeclaration -> handleInclude(node)
                is AssignExpression -> handleAssign(node, parent)
                is ForEachStatement -> handleForEachStatement(node)
            }
        }

        for (tu in t.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * handleForEachStatement adds a [HasType.TypeListener] to the [ForEachStatement.iterable] of an
     * [ForEachStatement] in order to determine the types used in [ForEachStatement.variable] (index
     * and iterated value).
     */
    private fun handleForEachStatement(forEach: ForEachStatement) {
        (forEach.iterable as HasType).registerTypeListener(
            object : HasType.TypeListener {
                override fun typeChanged(src: HasType, root: MutableList<HasType>, oldType: Type) {
                    if (src.type is UnknownType) {
                        return
                    }

                    val variable = forEach.variable
                    if (variable is DeclarationStatement) {
                        // The key is the first variable. It is always an int
                        val keyVariable =
                            variable.declarations.firstOrNull() as? VariableDeclaration
                        keyVariable?.type = TypeParser.createFrom("int", forEach.language)

                        // The value is the second one. Its type depends on the array type
                        val valueVariable =
                            variable.declarations.getOrNull(1) as? VariableDeclaration
                        ((forEach.iterable as? HasType)?.type as? PointerType)?.let {
                            valueVariable?.type = it.elementType
                        }
                    }
                }

                override fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>) {
                    // Nothing to do
                }
            }
        )
    }

    /**
     * This function gets called for every [AssignExpression], to check, whether we need to
     * implicitly define any variables assigned in the statement.
     */
    private fun handleAssign(assign: AssignExpression, parent: Node?) {
        // Only filter nodes that could potentially declare
        if (assign.operatorCode != ":=") {
            return
        }

        // Loop through the target variables (left-hand side)
        for (expr in assign.lhs) {
            if (expr is DeclaredReferenceExpression) {
                // And try to resolve it
                val ref = scopeManager.resolveReference(expr)
                if (ref == null) {
                    // We need to implicitly declare it, if its not declared before.
                    val decl = newVariableDeclaration(expr.name, expr.type)
                    decl.location = expr.location
                    decl.isImplicit = true
                    decl.initializer = assign.findValue(expr)

                    assign.declarations += decl

                    // Add it to the scope, so other assignments / references can "see" it.
                    scopeManager.addDeclaration(decl)
                }
            }
        }
    }

    /**
     * This function gets called for every [IncludeDeclaration] (which in Go imports a whole
     * package) and checks, if we need to infer a [NamespaceDeclaration] for this particular
     * include.
     */
    // TODO: Somehow, this gets called twice?!
    private fun handleInclude(include: IncludeDeclaration) {
        // Try to see if we already know about this namespace somehow
        val namespace =
            scopeManager.resolve<NamespaceDeclaration>(scopeManager.globalScope, true) {
                it.name == include.name && it.path == include.filename
            }

        // If not, we can infer a namespace declaration, so we can bundle all inferred function
        // declarations in there
        if (namespace.isEmpty()) {
            scopeManager.globalScope
                ?.astNode
                ?.startInference(scopeManager)
                ?.createInferredNamespaceDeclaration(include.name, include.filename)
        }
    }

    /**
     * This function gets called for every [CallExpression] and checks, whether this is actually a
     * "calling" a type and is thus a [CastExpression] rather than a [CallExpression].
     */
    private fun handleCall(call: CallExpression, parent: Node?) {
        // We need to check, whether the "callee" refers to a type and if yes, convert it into a
        // cast expression. And this is only really necessary, if the function call has a single
        // argument.
        val callee = call.callee
        if (parent != null && callee is DeclaredReferenceExpression && call.arguments.size == 1) {
            val language = parent.language ?: GoLanguage()

            // First, check if this is a built-in type
            if (language.builtInTypes.contains(callee.name.toString())) {
                replaceCallWithCast(callee.name.toString(), language, parent, call)
            } else {
                // If not, then this could still refer to an existing type. We need to make sure
                // that we take the current namespace into account
                val fqn =
                    if (callee.name.parent == null) {
                        scopeManager.currentNamespace.fqn(callee.name.localName)
                    } else {
                        callee.name
                    }

                if (TypeManager.getInstance().typeExists(fqn.toString())) {
                    replaceCallWithCast(fqn, language, parent, call)
                }
            }
        }
    }

    private fun replaceCallWithCast(
        typeName: CharSequence,
        language: Language<out LanguageFrontend>,
        parent: Node,
        call: CallExpression,
    ) {
        val cast = parent.newCastExpression(call.code)
        cast.location = call.location
        cast.castType = TypeParser.createFrom(typeName, false, language)
        cast.expression = call.arguments.single()

        if (parent !is ArgumentHolder) {
            log.error(
                "Parent AST node of call expression is not an argument holder. Cannot convert to cast expression. Further analysis might not be entirely accurate."
            )
            return
        }

        val success = parent.replaceArgument(call, cast)
        if (!success) {
            log.error(
                "Replacing call expression with cast expression was not successful. Further analysis might not be entirely accurate."
            )
        } else {
            call.disconnectFromGraph()
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
