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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import kotlin.reflect.typeOf
import org.bytedeco.javacpp.SizeTPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

class ExpressionHandler(lang: LLVMIRLanguageFrontend) :
    Handler<Expression, LLVMValueRef, LLVMIRLanguageFrontend>(::Expression, lang) {
    init {
        map.put(LLVMValueRef::class.java) { handleValue(it) }
    }

    private fun handleValue(value: LLVMValueRef): Expression {
        val kind = LLVMGetValueKind(value)

        when (kind) {
            LLVMConstantStructValueKind -> {
                return handleConstantStructValue(value)
            }
            LLVMConstantIntValueKind -> {
                return handleConstantInt(value)
            }
            else -> {
                log.info(
                    "Not handling value kind {} in handleValue yet. Falling back to the legacy way. Please change",
                    kind
                )
                val cpgType = lang.typeOf(value)
                val operandName: String
                val type = cpgType.typeName

                // old stuff from getOperandValue, needs to be refactored to the when above
                // TODO also move the other stuff to the expression handler
                if (LLVMIsConstant(value) == 1) {
                    if (LLVMIsConstantString(value) == 1) {
                        operandName = LLVMGetAsString(value, SizeTPointer(100)).toString()
                        return NodeBuilder.newLiteral(operandName, cpgType, operandName)
                    } else if (type != null && type.startsWith("ui")) {
                        val opValue = LLVMConstIntGetZExtValue(value)
                        return NodeBuilder.newLiteral(opValue, cpgType, opValue.toString())
                    } else if (type != null && type.startsWith("i")) {
                        val opValue = LLVMConstIntGetSExtValue(value)
                        return NodeBuilder.newLiteral(opValue, cpgType, opValue.toString())
                    } else if (type != null &&
                            (type == "double" ||
                                type == "bfloat" ||
                                type == "float" ||
                                type == "half" ||
                                type == "fp128" ||
                                type == "x86_fp80" ||
                                type == "ppc_fp128")
                    ) {
                        val losesInfo = IntArray(1)
                        val opValue = LLVMConstRealGetDouble(value, losesInfo)
                        return NodeBuilder.newLiteral(opValue, cpgType, opValue.toString())
                    } else if (LLVMIsAGlobalAlias(value) != null || LLVMIsGlobalConstant(value) == 1
                    ) {
                        val aliasee = LLVMAliasGetAliasee(value)
                        operandName =
                            LLVMPrintValueToString(aliasee)
                                .string // Already resolve the aliasee of the constant
                        return NodeBuilder.newLiteral(operandName, cpgType, operandName)
                    } else {
                        // TODO This does not return the actual constant but only a string
                        // representation
                        return NodeBuilder.newLiteral(
                            LLVMPrintValueToString(value).toString(),
                            cpgType,
                            LLVMPrintValueToString(value).toString()
                        )
                    }
                } else if (LLVMIsUndef(value) == 1) {
                    return NodeBuilder.newDeclaredReferenceExpression("undef", cpgType, "undef")
                } else if (LLVMIsPoison(value) == 1) {
                    return NodeBuilder.newDeclaredReferenceExpression("poison", cpgType, "poison")
                } else {
                    operandName = LLVMGetValueName(value).string // The argument (without the %)
                    return NodeBuilder.newDeclaredReferenceExpression(
                        operandName,
                        cpgType,
                        operandName
                    )
                }
            }
        }
    }

    /**
     * Handles a constant int value, which belongs to the
     * [simple constants](https://llvm.org/docs/LangRef.html#simple-constants).
     */
    private fun handleConstantInt(valueRef: LLVMValueRef): Literal<Long> {
        val value = LLVMConstIntGetSExtValue(valueRef)

        return newLiteral(value, lang.typeOf(valueRef), value.toString())
    }

    /**
     * Handles a constant struct value, which belongs to the
     * [complex constants](https://llvm.org/docs/LangRef.html#complex-constants). Its type needs to
     * be a structure type (either identified or literal) and we currently map this to a
     * [ConstructExpression], with the individual struct members being added as arguments.
     */
    private fun handleConstantStructValue(value: LLVMValueRef): Expression {
        // retrieve the type
        val type = lang.typeOf(value)

        val expr: ConstructExpression =
            NodeBuilder.newConstructExpression(lang.getCodeFromRawNode(value))
        // map the construct expression to the record declaration of the type
        expr.instantiates = (type as? ObjectType)?.recordDeclaration

        // loop through the operands
        for (i in 0 until LLVMGetNumOperands(value)) {
            // and handle them as expressions themselves
            val arg = this.handle(LLVMGetOperand(value, i))
            expr.addArgument(arg)
        }

        return expr
    }
}
