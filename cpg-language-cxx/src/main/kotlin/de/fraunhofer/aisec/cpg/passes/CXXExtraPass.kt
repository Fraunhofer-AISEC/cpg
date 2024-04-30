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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.ValueDeclarationScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore

/**
 * This [Pass] executes certain C++ specific conversions on initializers, that are only possible
 * once we know all the types. It may be extended in the future with other things that we currently
 * still do in the frontend, but might be more accurate to do once we parsed all files and have all
 * type information.
 */
@ExecuteBefore(EvaluationOrderGraphPass::class)
@ExecuteBefore(ReplaceCallCastPass::class)
@DependsOn(TypeResolver::class)
class CXXExtraPass(ctx: TranslationContext) : ComponentPass(ctx) {

    private lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(component: Component) {
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)

        walker.registerHandler(::fixInitializers)
        walker.registerHandler { _, parent, node ->
            when (node) {
                is BinaryOperator -> convertOperators(node, parent)
            }
        }
        walker.registerHandler(::connectDefinitions)

        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * In C++ there is an ambiguity between the combination of a cast + unary operator or a binary
     * operator where some arguments are wrapped in parenthesis. This function tries to resolve
     * this. Note: This is done especially for the C++ frontend. [ReplaceCallCastPass.handleCall]
     * handles the more general case (which also applies to C++), in which a cast and a call are
     * indistinguishable and need to be resolved once all types are known.
     */
    private fun convertOperators(binOp: BinaryOperator, parent: Node?) {
        val cast = binOp.lhs
        if (cast is CastExpression) {
            // We need to check, if the supposed cast expression is really referring to a type or
            // not
            val type = typeManager.typeOf(cast.name)
            if (type != null) {
                // If the name of the cast expression is really a type, then this is really a unary
                // expression instead of a binary operator. We need to perform the following steps:
                // * create a unary operator with the rhs of the binary operator (and the same
                //   operator code)
                // * set this unary operator as the "expression" of the cast
                // * actually set the type of cast, since before we only set the name (so we do not
                //   accidentally create the type)
                // * replace the binary operator with the cast expression in the parent argument
                //   holder
                val unaryOp =
                    newUnaryOperator(binOp.operatorCode ?: "", postfix = false, prefix = true)
                        .codeAndLocationFrom(binOp.rhs)
                unaryOp.input = binOp.rhs

                // disconnect the old expression
                cast.expression.disconnectFromGraph()
                // set the unary operator as the new expression
                cast.expression = unaryOp
                cast.type = type
                cast.castType = type

                // replace the node in the parent of the bin/unary op
                walker.replaceArgument(parent, binOp, cast)
            } else {
                // Otherwise, the supposed cast expression was really just parenthesis around an
                // identifier, but we can only make this distinction now. In this case we can just
                // unwrap the reference and exchange the cast expression in the arguments of the
                // binary op
                walker.replaceArgument(binOp, cast, cast.expression)
            }
        }
    }

    protected fun fixInitializers(node: Node?) {
        if (node is VariableDeclaration) {
            // check if we have the corresponding class for this type
            val record = node.type.root.recordDeclaration
            val typeString = node.type.root.name
            if (record != null) {
                val currInitializer = node.initializer
                if (currInitializer == null && node.isImplicitInitializerAllowed) {
                    val initializer =
                        newConstructExpression(typeString).implicit(code = "$typeString()")
                    initializer.language = node.language
                    initializer.type = node.type
                    node.initializer = initializer
                    node.templateParameters?.let {
                        SymbolResolver.addImplicitTemplateParametersToCall(it, initializer)
                    }
                } else if (
                    currInitializer !is ConstructExpression &&
                        currInitializer is CallExpression &&
                        currInitializer.name.localName == node.type.root.name.localName
                ) {
                    // This should actually be a construct expression, not a call!
                    val arguments = currInitializer.arguments
                    val signature = arguments.map(Node::code).joinToString(", ")
                    val initializer =
                        newConstructExpression(typeString)
                            .implicit(code = "$typeString($signature)")
                    initializer.language = node.language
                    initializer.type = node.type
                    initializer.arguments = mutableListOf(*arguments.toTypedArray())
                    node.initializer = initializer
                    currInitializer.disconnectFromGraph()
                }
            }
        }
    }

    /**
     * This function connects a [FunctionDeclaration] that is a definition (i.e., has a body) to
     * possible declarations of the same function (has [FunctionDeclaration.isDefinition] set to
     * false) pointing to it by setting the field [FunctionDeclaration.definition].
     *
     * This works across the whole [Component].
     */
    private fun connectDefinitions(declaration: Node?) {
        if (declaration !is FunctionDeclaration) {
            return
        }

        // We only need to look at functions that are definitions, i.e., have a body
        if (!declaration.isDefinition) {
            return
        }

        var scope = scopeManager.currentScope
        // This is a rather stupid workaround since because of incorrect scope merging, there exist
        // multiple instances of "global" scopes...
        if (scope is GlobalScope) {
            scope = scopeManager.globalScope
        }
        if (scope is ValueDeclarationScope) {
            // Update the definition
            val candidates =
                scope.valueDeclarations.filterIsInstance<FunctionDeclaration>().filter {
                    // We should only connect methods to methods, functions to functions and
                    // constructors to constructors.
                    it::class == declaration::class &&
                        !it.isDefinition &&
                        it.name == declaration.name &&
                        it.signature == declaration.signature
                }
            for (candidate in candidates) {
                candidate.definition = declaration

                // Do some additional magic with default parameters, which I do not really
                // understand
                for (i in declaration.parameters.indices) {
                    if (candidate.parameters[i].default != null) {
                        declaration.parameters[i].default = candidate.parameters[i].default
                    }
                }
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
