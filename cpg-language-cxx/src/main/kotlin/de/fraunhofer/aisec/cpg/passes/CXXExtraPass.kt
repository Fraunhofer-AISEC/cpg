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
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.replace
import de.fraunhofer.aisec.cpg.nameIsType
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore

/**
 * This [Pass] executes certain C++ specific conversions on initializers, that are only possible
 * once we know all the types. It may be extended in the future with other things that we currently
 * still do in the frontend, but might be more accurate to do once we parsed all files and have all
 * type information.
 */
@ExecuteBefore(EvaluationOrderGraphPass::class)
@ExecuteBefore(ResolveCallExpressionAmbiguityPass::class)
@DependsOn(TypeResolver::class)
class CXXExtraPass(ctx: TranslationContext) : ComponentPass(ctx) {

    private lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(component: Component) {
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)

        walker.registerHandler(::fixInitializers)
        walker.registerHandler { node ->
            when (node) {
                is UnaryOperator -> removeBracketOperators(node)
                is BinaryOperator -> convertOperators(node)
            }
        }
        walker.registerHandler(::connectDefinitions)

        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * In the frontend, we keep parenthesis around some expressions, so we can decide whether they
     * are [CastExpression] nodes or just simply brackets with no syntactic value. The
     * [CastExpression] conversion is done in [convertOperators], but in this function we are trying
     * to get rid of those ()-unary operators that are meaningless, in order to reduce clutter to
     * the graph.
     */
    private fun removeBracketOperators(node: UnaryOperator) {
        val input = node.input
        if (node.operatorCode == "()" && input is Reference && input.nameIsType() == null) {
            // It was really just parenthesis around an identifier, but we can only make this
            // distinction now.
            //
            // In theory, we could just keep this meaningless unary expression, but it in order
            // to reduce nodes, we unwrap the reference and exchange it in the arguments of the
            // binary op
            walker.replace(node.astParent, node, node.input)
        }
    }

    /**
     * In C++ there is an ambiguity between the combination of a cast + unary operator or a binary
     * operator where some arguments are wrapped in parentheses. This function tries to resolve
     * this.
     *
     * Note: This is done especially for the C++ frontend.
     * [ResolveCallExpressionAmbiguityPass.handleCall] handles the more general case (which also
     * applies to C++), in which a cast and a call are indistinguishable and need to be resolved
     * once all types are known.
     */
    private fun convertOperators(binOp: BinaryOperator) {
        val fakeUnaryOp = binOp.lhs
        val language = fakeUnaryOp.language as? CLanguage

        // We need to check, if the expression in parentheses is really referring to a type or
        // not. A common example is code like `(long) &addr`. We could end up parsing this as a
        // binary operator with the left-hand side of `(long)`, an operator code `&` and a rhs
        // of `addr`.
        if (language == null || fakeUnaryOp !is UnaryOperator || fakeUnaryOp.operatorCode != "()") {
            return
        }

        // If the name (`long` in the example) is a type, then the unary operator (`(long)`)
        // is really a cast and our binary operator is really a unary operator `&addr`.
        var type = (fakeUnaryOp.input as? Reference)?.nameIsType()
        if (type != null) {
            // We need to perform the following steps:
            // * create a cast expression out of the ()-unary operator, with the type that is
            //   referred to in the op.
            val cast = newCastExpression().codeAndLocationFrom(fakeUnaryOp)
            cast.language = language
            cast.castType = type

            // * create a unary operator with the rhs of the binary operator (and the same
            //   operator code).
            // * in the unlikely case that the binary operator cannot actually be used as a
            //   unary operator, we abort this. This should not happen, but we might never know
            val opCode = binOp.operatorCode ?: ""
            if (opCode !in language.unaryOperators) {
                log.error(
                    "We tried to convert a binary operator into a unary operator, but the list of " +
                        "operator codes does not allow that. This is suspicious and the translation " +
                        "probably was incorrect"
                )
                return
            }

            val unaryOp =
                newUnaryOperator(opCode, postfix = false, prefix = true)
                    .codeAndLocationFrom(binOp.rhs)
            unaryOp.language = language
            unaryOp.input = binOp.rhs

            // * set the unary operator as the "expression" of the cast
            cast.expression = unaryOp

            // * replace the binary operator with the cast expression in the parent argument
            //   holder
            walker.replace(binOp.astParent, binOp, cast)
        }
    }

    protected fun fixInitializers(node: Node) {
        if (node is VariableDeclaration) {
            // check if we have the corresponding class for this type
            val record = node.type.root.recordDeclaration
            val typeString = node.type.root.name
            if (record != null) {
                val currInitializer = node.initializer
                if (currInitializer == null && node.isImplicitInitializerAllowed) {
                    val initializer =
                        newConstructExpression(typeString)
                            .codeAndLocationFrom(node)
                            .implicit(code = "$typeString()")
                    initializer.language = node.language
                    initializer.type = node.type
                    node.initializer = initializer
                    SymbolResolver.addImplicitTemplateParametersToCall(
                        node.templateParameters,
                        initializer,
                    )
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
    private fun connectDefinitions(declaration: Node) {
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
        if (scope != null) {
            // Update the definition
            val candidates =
                scope.symbols[declaration.symbol]?.filterIsInstance<FunctionDeclaration>()?.filter {
                    // We should only connect methods to methods, functions to functions and
                    // constructors to constructors.
                    it::class == declaration::class &&
                        !it.isDefinition &&
                        it.signature == declaration.signature
                } ?: emptyList()
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
