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
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

/**
 * This handler primarily handles operands, as returned by [LLVMGetOperand] and turns them into an
 * [Expression]. Operands are basically arguments to an instruction.
 */
class ExpressionHandler(lang: LLVMIRLanguageFrontend) :
    Handler<Expression, LLVMValueRef, LLVMIRLanguageFrontend>(::Expression, lang) {
    init {
        map.put(LLVMValueRef::class.java) { handleValue(it) }
    }

    private fun handleValue(value: LLVMValueRef): Expression {
        when (val kind = LLVMGetValueKind(value)) {
            LLVMConstantStructValueKind -> {
                return handleConstantStructValue(value)
            }
            LLVMConstantIntValueKind -> {
                return handleConstantInt(value)
            }
            LLVMConstantFPValueKind -> {
                return handleConstantFP(value)
            }
            LLVMUndefValueValueKind -> {
                return handleUndefValue(value)
            }
            LLVMArgumentValueKind,
            LLVMGlobalVariableValueKind,
            // this is a little tricky. It seems weird, that an instruction value kind turns
            // up here. What is happening is, that this is a variable reference in the form of
            // %var. In this case LLVMGetValueKind will return LLVMInstructionValueKind because
            // it actually points to the instruction where this variable was defined. However,
            // we are only interested in its name and type.
            LLVMInstructionValueKind -> {
                return handleReference(value)
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
                    if (LLVMIsAGlobalAlias(value) != null || LLVMIsGlobalConstant(value) == 1) {
                        val aliasee = LLVMAliasGetAliasee(value)
                        operandName =
                            LLVMPrintValueToString(aliasee)
                                .string // Already resolve the aliasee of the constant
                        return newLiteral(operandName, cpgType, operandName)
                    } else {
                        // TODO This does not return the actual constant but only a string
                        // representation
                        return newLiteral(
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
                    log.error("Unknown expression")
                    return Expression()
                }
            }
        }
    }

    /**
     * Handles a reference to an [identifier](https://llvm.org/docs/LangRef.html#identifiers). It
     * can either be a reference to a global or local one, depending on the prefix.
     *
     * This function will also take care of actually resolving the reference. This is a) faster and
     * b) needed because the [VariableUsageResolver] is not familiar with the prefix system,
     * determining the scope of the variable.
     */
    private fun handleReference(valueRef: LLVMValueRef): Expression {
        val name = LLVMGetValueName(valueRef).string

        val type = lang.typeOf(valueRef)

        val ref = NodeBuilder.newDeclaredReferenceExpression(name, type, "${type.typeName} $name")

        // try to resolve the reference. actually the valueRef is already referring to the resolved
        // variable because we obtain it using LLVMGetOperand, so we just need to look it up in the
        // cache bindings
        val decl = lang.bindingsCache[valueRef.symbolName]

        if (decl == null) {

            // there is something seriously wrong here, if this happens, because all variables need
            // to be declared before use and we _should_ have seen the variable
            log.warn("Could not resolve reference ${valueRef.symbolName}. This should not happen.")
        } else {
            ref.refersTo = decl
        }

        return ref
    }

    /**
     * Handles a constant int value, which belongs to the
     * [simple constants](https://llvm.org/docs/LangRef.html#simple-constants).
     */
    private fun handleConstantInt(valueRef: LLVMValueRef): Literal<Long> {
        val type = lang.typeOf(valueRef)

        val value =
            if (type.typeName.startsWith("ui")) {
                LLVMConstIntGetZExtValue(valueRef)
            } else {
                LLVMConstIntGetSExtValue(valueRef)
            }

        return newLiteral(value, type, value.toString())
    }

    /**
     * Handles a constant floating point value, which belongs to the
     * [simple constants](https://llvm.org/docs/LangRef.html#simple-constants) and needs to be a
     * [floating-point type](https://llvm.org/docs/LangRef.html#t-floating).
     */
    private fun handleConstantFP(valueRef: LLVMValueRef): Literal<Double> {
        val losesInfo = IntArray(1)
        val value = LLVMConstRealGetDouble(valueRef, losesInfo)

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

    private fun initializeAsUndef(type: Type, code: String): Expression {
        if (!type.name.contains("literal")) { // TODO: We need a comparison for primitive types.
            return newLiteral(null, type, code)
        } else {
            val expr: ConstructExpression = NodeBuilder.newConstructExpression(code)
            // map the construct expression to the record declaration of the type
            expr.instantiates = (type as? ObjectType)?.recordDeclaration

            // loop through the operands
            for (field in (expr.instantiates as RecordDeclaration).fields) {
                // and handle them as expressions themselves
                val arg = initializeAsUndef(field.type, code)
                expr.addArgument(arg)
            }

            return expr
        }
    }

    private fun handleUndefValue(value: LLVMValueRef): Expression {
        // retrieve the type
        val type = lang.typeOf(value)

        return initializeAsUndef(type, lang.getCodeFromRawNode(value)!!)
    }
}
