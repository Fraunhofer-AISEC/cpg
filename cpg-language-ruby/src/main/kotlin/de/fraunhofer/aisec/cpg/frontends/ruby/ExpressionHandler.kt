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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import org.jruby.ast.*
import org.jruby.ast.Node
import org.jruby.ast.types.INameNode
import org.jruby.ast.visitor.OperatorCallNode

class ExpressionHandler(lang: RubyLanguageFrontend) :
    RubyHandler<Expression, Node>({ ProblemExpression() }, lang) {

    override fun handleNode(node: Node): Expression {
        return when (node) {
            is OperatorCallNode -> handleOperatorCallNode(node)
            is CallNode -> handleCallNode(node)
            is FCallNode -> handleFCallNode(node)
            is IterNode -> handleIterNode(node)
            is StrNode -> handleStrNode(node)
            is FixnumNode -> handleFixnumNode(node)
            is FloatNode -> handleFloatNode(node)
            is DVarNode -> handleINameNode(node)
            is LocalVarNode -> handleINameNode(node)
            is DAsgnNode -> handleDAsgnNode(node)
            is LocalAsgnNode -> handleLocalAsgnNode(node)
            else -> handleNotSupported(node, node::class.simpleName ?: "")
        }
    }

    private fun handleOperatorCallNode(node: OperatorCallNode): BinaryOperator {
        val binOp = newBinaryOperator(node.name.idString())

        (this.handle(node.receiverNode) as? Expression)?.let { binOp.lhs = it }

        // Always seems to be an array?
        val list = node.argsNode as ArrayNode
        (this.handle(list.get(0)) as? Expression)?.let { binOp.rhs = it }

        return binOp
    }

    private fun handleINameNode(node: INameNode): Reference {
        return newReference(node.name.idString())
    }

    private fun handleIterNode(node: IterNode): LambdaExpression {
        // a complete hack, to handle iter nodes, which is sort of a lambda expression
        // so we create an anonymous function declaration out of the bodyNode and varNode
        val func = newFunctionDeclaration("", rawNode = node)

        frontend.scopeManager.enterScope(func)

        for (arg in node.argsNode.args) {
            val param = frontend.declarationHandler.handle(arg) as? Parameter
            if (param == null) {
                continue
            }

            frontend.scopeManager.addDeclaration(param)
            func.parameters += param
        }

        func.body = frontend.statementHandler.handle(node.bodyNode)

        frontend.scopeManager.leaveScope(func)

        val lambda = newLambdaExpression()
        lambda.function = func

        return lambda
    }

    private fun handleDAsgnNode(node: DAsgnNode): AssignExpression {
        val assign = newAssignExpression("=")

        // we need to build a reference out of the assignment node itself for our LHS
        assign.lhs = mutableListOf(handleINameNode(node))
        assign.rhs = mutableListOf(this.handle(node.valueNode))

        return assign
    }

    private fun handleLocalAsgnNode(node: LocalAsgnNode): AssignExpression {
        val assign = newAssignExpression("=")

        // we need to build a reference out of the assignment node itself for our LHS
        assign.lhs = mutableListOf(handleINameNode(node))
        assign.rhs = mutableListOf(this.handle(node.valueNode))

        return assign
    }

    private fun handleCallNode(node: CallNode): Expression {
        val base =
            handle(node.receiverNode) as? Expression
                ?: return ProblemExpression("could not parse base")
        val callee = newMemberExpression(node.name.asJavaString(), base)

        val mce = newMemberCallExpression(callee, false)

        for (arg in node.argsNode?.childNodes() ?: emptyList()) {
            mce.addArgument(handle(arg))
        }

        // add the iterNode as last argument
        node.iterNode?.let { mce.addArgument(handle(it)) }

        return mce
    }

    private fun handleFCallNode(node: FCallNode): Expression {
        val callee = handleINameNode(node)

        val call = newCallExpression(callee)

        for (arg in node.argsNode?.childNodes() ?: emptyList()) {
            call.addArgument(handle(arg))
        }

        // add the iterNode as last argument
        node.iterNode?.let { call.addArgument(handle(it)) }

        return call
    }

    private fun handleStrNode(node: StrNode): Literal<String> {
        return newLiteral(String(node.value.bytes()), primitiveType("String"))
    }

    private fun handleFixnumNode(node: FixnumNode): Literal<Long> {
        return newLiteral(node.value, primitiveType("Integer"))
    }

    private fun handleFloatNode(node: FloatNode): Literal<Double> {
        return newLiteral(node.value, primitiveType("Float"))
    }
}
