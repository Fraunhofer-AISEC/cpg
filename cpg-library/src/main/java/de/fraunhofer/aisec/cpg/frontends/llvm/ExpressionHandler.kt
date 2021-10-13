/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

class ExpressionHandler(lang: LLVMIRLanguageFrontend) :
    Handler<Expression, LLVMValueRef, LLVMIRLanguageFrontend>(::Expression, lang) {
    init {
        map.put(LLVMValueRef::class.java) { handleValue(it) }
    }

    private fun handleValue(value: LLVMValueRef): Expression {
        when (LLVMGetValueKind(value)) {
            LLVMConstantStructValueKind -> {
                return handleConstantStructValue(value)
            }
            LLVMConstantIntValueKind -> {
                return handleConstantInt(value)
            }
        }

        return Expression()
    }

    private fun handleConstantInt(valueRef: LLVMValueRef): Literal<Long> {
        val value = LLVMConstIntGetSExtValue(valueRef)

        return newLiteral(value, lang.typeOf(valueRef), value.toString())
    }

    private fun handleConstantStructValue(value: LLVMValueRef): Expression {
        val size = LLVMGetNumOperands(value)

        val type = lang.typeOf(value)

        val expr = NodeBuilder.newConstructExpression("")
        expr.instantiates = (type as? ObjectType)?.recordDeclaration

        for (i in 0 until size) {
            val arg = handle(LLVMGetOperand(value, 0))
            expr.addArgument(arg)
        }

        return expr
    }
}
