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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.ByteBuffer
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.SizeTPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*
import java.util.*

class LLVMIRLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {
    override fun parse(file: File): TranslationUnitDeclaration {
        // these will be filled by our create and parse functions later and will be passed as
        // pointer
        val mod = LLVMModuleRef()
        val buf = LLVMMemoryBufferRef()

        // create a new LLVM context
        val ctx: LLVMContextRef = LLVMContextCreate()

        // allocate a buffer for a possible error message
        val errorMessage = ByteBuffer.allocate(10000)

        var result =
            LLVMCreateMemoryBufferWithContentsOfFile(
                BytePointer(file.toPath().toString()),
                buf,
                errorMessage
            )
        if (result != 0) {
            // something went wrong
            val errorMsg = errorMessage.toString()
            LLVMDisposeMessage(errorMessage)
            throw TranslationException("Could not create memory buffer: $errorMsg")
        }

        result = LLVMParseIRInContext(ctx, buf, mod, errorMessage)
        if (result != 0) {
            // something went wrong
            val errorMsg = errorMessage.toString()
            // LLVMDisposeMessage(errorMessage)
            throw TranslationException("Could not parse IR: $errorMsg")
        }

        // println(result)
        // println(mod)

        var func = LLVMGetFirstFunction(mod)

        while (func != null) {
            var bb = LLVMGetFirstBasicBlock(func)
            while (bb != null) {
                var instr = LLVMGetFirstInstruction(bb)
                while (instr != null) {
                    when (LLVMGetInstructionOpcode(instr)) {
                        LLVMRet -> {
                            val numOps = LLVMGetNumOperands(instr)
                            if (numOps == 0) {
                                println("ret void instruction")
                            } else {
                                val paramType =
                                    LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0)))
                                        .getString()
                                val operandName = getOperandValueAtIndex(instr, 0, paramType)
                                println("ret $operandName")
                            }
                        }
                        LLVMBr -> {
                            println("br instruction")
                        }
                        LLVMSwitch -> {
                            println("switch instruction")
                        }
                        LLVMIndirectBr -> {
                            println("indirect br instruction")
                        }
                        LLVMInvoke -> {
                            println("invoke instruction")
                        }
                        LLVMUnreachable -> {
                            println("unreachable instruction")
                        }
                        LLVMCallBr -> {
                            println("call instruction")
                        }
                        LLVMFNeg -> {
                            println("fneg instruction")
                        }
                        LLVMAdd -> {
                            parseBinaryOperator(instr, "+", false, false)
                        }
                        LLVMFAdd -> {
                            parseBinaryOperator(instr, "+", true, false)
                        }
                        LLVMSub -> {
                            parseBinaryOperator(instr, "-", false, false)
                        }
                        LLVMFSub -> {
                            parseBinaryOperator(instr, "-", true, false)
                        }
                        LLVMMul -> {
                            parseBinaryOperator(instr, "*", false, false)
                        }
                        LLVMFMul -> {
                            parseBinaryOperator(instr, "*", true, false)
                        }
                        LLVMUDiv -> {
                            parseBinaryOperator(instr, "+", false, true)
                        }
                        LLVMSDiv -> {
                            parseBinaryOperator(instr, "/", false, false)
                        }
                        LLVMFDiv -> {
                            parseBinaryOperator(instr, "/", true, false)
                        }
                        LLVMURem -> {
                            parseBinaryOperator(instr, "%", false, true)
                        }
                        LLVMSRem -> {
                            println("srem instruction")
                        }
                        LLVMFRem -> {
                            println("frem instruction")
                        }
                        LLVMShl -> {
                            parseBinaryOperator(instr, "<<", false, false)
                        }
                        LLVMLShr -> {
                            parseBinaryOperator(instr, ">>", false, false)
                        }
                        LLVMAShr -> {
                            println("ashr instruction")
                        }
                        LLVMAnd -> {
                            parseBinaryOperator(instr, "&", false, false)
                        }
                        LLVMOr -> {
                            parseBinaryOperator(instr, "|", false, false)
                        }
                        LLVMXor -> {
                            parseBinaryOperator(instr, "^", false, false)
                        }
                        LLVMAlloca -> {
                            println("alloca instruction")
                        }
                        LLVMLoad -> {
                            println("load instruction")
                        }
                        LLVMStore -> {
                            println("store instruction")
                        }
                        LLVMGetElementPtr -> {
                            val lhs = LLVMGetValueName(instr).getString()
                            val numOps = LLVMGetNumOperands(instr)
                            var args = ""
                            for (idx: Int in 0..numOps - 1) {
                                val paramType =
                                    LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, idx)))
                                        .getString()
                                val operandName = getOperandValueAtIndex(instr, idx, paramType)
                                args += "$paramType $operandName"
                            }
                            println("$lhs = getelementptr with operands ($args)")
                        }
                        LLVMTrunc -> {
                            println("trunc instruction")
                        }
                        LLVMZExt -> {
                            println("zext instruction")
                        }
                        LLVMSExt -> {
                            println("sext instruction")
                        }
                        LLVMFPToUI -> {
                            println("fptoui instruction")
                        }
                        LLVMFPToSI -> {
                            println("fptosi instruction")
                        }
                        LLVMUIToFP -> {
                            println("uitofp instruction")
                        }
                        LLVMSIToFP -> {
                            println("sitofp instruction")
                        }
                        LLVMFPTrunc -> {
                            println("fptrunc instruction")
                        }
                        LLVMFPExt -> {
                            println("fpext instruction")
                        }
                        LLVMPtrToInt -> {
                            println("ptrtoint instruction")
                        }
                        LLVMIntToPtr -> {
                            println("inttoptr instruction")
                        }
                        LLVMBitCast -> {
                            println("bitcast instruction")
                        }
                        LLVMAddrSpaceCast -> {
                            println("addrspacecast instruction")
                        }
                        LLVMICmp -> {
                            var cmpPred: String
                            var unsigned = false
                            when (LLVMGetICmpPredicate(instr)) {
                                LLVMIntEQ -> cmpPred = "=="
                                LLVMIntNE -> cmpPred = "!="
                                LLVMIntUGT -> {
                                    cmpPred = ">"
                                    unsigned = true
                                }
                                LLVMIntUGE -> {
                                    cmpPred = ">="
                                    unsigned = true
                                }
                                LLVMIntULT -> {
                                    cmpPred = "<"
                                    unsigned = true
                                }
                                LLVMIntULE ->  {
                                    cmpPred = "<="
                                    unsigned = true
                                }
                                LLVMIntSGT -> cmpPred = ">"
                                LLVMIntSGE -> cmpPred = ">="
                                LLVMIntSLT -> cmpPred = "<"
                                LLVMIntSLE -> cmpPred = "<="
                                else -> cmpPred = "unknown"
                            }
                            parseBinaryOperator(instr, cmpPred, false, unsigned)
                        }
                        LLVMFCmp -> {
                            var cmpPred: String
                            var unordered = false
                            when (LLVMGetICmpPredicate(instr)) {
                                LLVMRealPredicateFalse -> {
                                    cmpPred = "false" // TODO
                                    continue
                                }
                                LLVMRealOEQ -> cmpPred = "=="
                                LLVMRealOGT -> cmpPred = ">"
                                LLVMRealOGE -> cmpPred = ">="
                                LLVMRealOLT -> cmpPred = "<"
                                LLVMRealOLE -> cmpPred = "<="
                                LLVMRealONE -> cmpPred = "!="
                                LLVMRealORD -> cmpPred = "ord"
                                LLVMRealUNO -> cmpPred = "uno"
                                LLVMRealUEQ -> {
                                    cmpPred = "=="
                                    unordered = true
                                }
                                LLVMRealUGT -> {
                                    cmpPred = ">"
                                    unordered = true
                                }
                                LLVMRealUGE -> {
                                    cmpPred = ">="
                                    unordered = true
                                }
                                LLVMRealULT -> {
                                    cmpPred = "<"
                                    unordered = true
                                }
                                LLVMRealULE -> {
                                    cmpPred = "<="
                                    unordered = true
                                }
                                LLVMRealUNE -> {
                                    cmpPred = "!="
                                    unordered = true
                                }
                                LLVMRealPredicateTrue -> {
                                    cmpPred = "true" // TODO
                                    continue
                                }
                                else -> cmpPred = "unknown"
                            }
                            println("fcmp $cmpPred instruction")
                            parseBinaryOperator(instr, cmpPred, false, false, unordered)
                        }
                        LLVMPHI -> {
                            println("phi instruction")
                        }
                        LLVMCall -> {
                            println(parseFunctionCall(instr).toString())
                        }
                        LLVMSelect -> {
                            println("select instruction")
                        }
                        LLVMUserOp1 -> {
                            println("userop1 instruction")
                        }
                        LLVMUserOp2 -> {
                            println("userop2 instruction")
                        }
                        LLVMVAArg -> {
                            println("va_arg instruction")
                        }
                        LLVMExtractElement -> {
                            println("extractelement instruction")
                        }
                        LLVMInsertElement -> {
                            println("insertelement instruction")
                        }
                        LLVMShuffleVector -> {
                            println("shufflevector instruction")
                        }
                        LLVMExtractValue -> {
                            println("extractvalue instruction")
                        }
                        LLVMInsertValue -> {
                            println("insertvalue instruction")
                        }
                        LLVMFreeze -> {
                            println("freeze instruction")
                        }
                        LLVMFence -> {
                            println("fence instruction")
                        }
                        LLVMAtomicCmpXchg -> {
                            println("atomiccmpxchg instruction")
                        }
                        LLVMAtomicRMW -> {
                            println("atomicrmw instruction")
                        }
                        LLVMResume -> {
                            println("resume instruction")
                        }
                        LLVMLandingPad -> {
                            println("landingpad instruction")
                        }
                        LLVMCleanupRet -> {
                            println("cleanupret instruction")
                        }
                        LLVMCatchRet -> {
                            println("catchret instruction")
                        }
                        LLVMCatchPad -> {
                            println("catchpad instruction")
                        }
                        LLVMCleanupPad -> {
                            println("cleanuppad instruction")
                        }
                        LLVMCatchSwitch -> {
                            println("catchswitch instruction")
                        }
                        else -> {
                            println("Something else")
                        }
                    }

                    instr = LLVMGetNextInstruction(instr)
                }
                bb = LLVMGetNextBasicBlock(bb)
            }
            func = LLVMGetNextFunction(func)
        }

        // TODO: actually clean them up, if we throw
        LLVMContextDispose(ctx)

        return TranslationUnitDeclaration()
    }

    private fun parseFunctionCall(instr: LLVMValueRef): Statement {
        val lhs = LLVMGetValueName(instr).getString()
        val calledFunc = LLVMGetCalledValue(instr)
        val calledFuncName = LLVMGetValueName(calledFunc).getString()
        val funcType = LLVMGetCalledFunctionType(instr)
        val retVal = LLVMPrintTypeToString(LLVMGetReturnType(funcType)).getString()
        var param = LLVMGetFirstParam(calledFunc)
        var idx = 0
        var args = ""

        val callExpr =
            NodeBuilder.newCallExpression(
                calledFuncName,
                calledFuncName,
                LLVMPrintValueToString(instr).getString(),
                false
            )

        while (param != null) {
            val paramType =
                LLVMPrintTypeToString(LLVMTypeOf(param)).getString() // Type of the argument
            val operandName = getOperandValueAtIndex(instr, idx, paramType)
            callExpr.addArgument(
                NodeBuilder.newDeclaredReferenceExpression(
                    operandName,
                    TypeParser.createFrom(paramType, true),
                    operandName
                )
            )
            args += "$operandName, "
            param = LLVMGetNextParam(param)
            idx++
        }

        if (args.endsWith(", ")) args = args.substring(0, args.length - 2)

        if (lhs != "") {
            val decl = VariableDeclaration()
            decl.type = TypeParser.createFrom(retVal, true)
            decl.name = lhs
            decl.initializer = callExpr

            val declStatement = DeclarationStatement()
            declStatement.singleDeclaration = decl
            return declStatement
        } else {
            println("call $calledFuncName($args): $retVal")
            return callExpr
        }
    }

    private fun parseBinaryOperator(
        instr: LLVMValueRef,
        op: String,
        float: Boolean,
        unsigned: Boolean,
        unordered: Boolean = false
    ): Statement {
        val lhs = LLVMGetValueName(instr).getString()

        var op1Type = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).getString()
        val op1 = getOperandValueAtIndex(instr, 0, op1Type)
        if (unsigned) op1Type = "unsigned $op1Type"

        var op2Type = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 1))).getString()
        val op2 = getOperandValueAtIndex(instr, 1, op2Type)
        if (unsigned) op2Type = "unsigned $op2Type"

        val binaryOperator = NodeBuilder.newBinaryOperator(op, "($op1Type) $op1 $op ($op2Type) $op2")

        val t = TypeParser.createFrom(op1Type, true)
        if (op1Type.contains("unsigned "))
            binaryOperator.lhs = NodeBuilder.newCastExpression("($op1Type) $op1")
        else binaryOperator.lhs = NodeBuilder.newDeclaredReferenceExpression(op1, t, op1)

        if (op2Type.contains("unsigned "))
            binaryOperator.rhs = NodeBuilder.newCastExpression("($op2Type) $op2")
        else binaryOperator.rhs = NodeBuilder.newDeclaredReferenceExpression(op2, t, op2)

        var binOpUnordered: BinaryOperator? = null
        if(unordered) {
            binOpUnordered = NodeBuilder.newBinaryOperator("||", "isunordered($op1, $op2) || ($op1Type) $op1 $op ($op2Type) $op2")
            binOpUnordered.rhs = binaryOperator
            val unorderedCall = NodeBuilder.newCallExpression("isunordered","isunordered", LLVMPrintValueToString(instr).getString(),false)
            unorderedCall.addArgument(NodeBuilder.newDeclaredReferenceExpression(op1, t, op1))
            unorderedCall.addArgument(NodeBuilder.newDeclaredReferenceExpression(op2, t, op2))
            binOpUnordered.lhs = unorderedCall
        }

        val decl = VariableDeclaration()
        if(Arrays.asList("==", "!=", "<", "<=", ">", ">=").contains(op)) {
            decl.type = TypeParser.createFrom("i1", true) // boolean type
        } else {
            decl.type = t
        }
        decl.name = lhs
        decl.initializer = if(unordered) binOpUnordered else binaryOperator

        val declStatement = DeclarationStatement()
        declStatement.singleDeclaration = decl
        return declStatement
    }

    private fun getOperandValueAtIndex(instr: LLVMValueRef, idx: Int, type: String?): String {
        val operand = LLVMGetOperand(instr, idx)
        val operandName: String
        if (LLVMIsConstant(operand) == 1) {
            if (LLVMIsConstantString(operand) == 1) {
                operandName = LLVMGetAsString(operand, SizeTPointer(100)).toString()
            } else if (type != null && type.startsWith("ui")) {
                operandName = LLVMConstIntGetZExtValue(operand).toString()
            } else if (type != null && type.startsWith("i")) {
                operandName = LLVMConstIntGetSExtValue(operand).toString()
            } else if (type != null &&
                    (type.equals("double") ||
                        type.equals("bfloat") ||
                        type.equals("float") ||
                        type.equals("half") ||
                        type.equals("fp128") ||
                        type.equals("x86_fp80") ||
                        type.equals("ppc_fp128"))
            ) {
                val losesInfo = IntArray(1)
                operandName = LLVMConstRealGetDouble(operand, losesInfo).toString()
            } else if (LLVMIsAGlobalAlias(operand) != null || LLVMIsGlobalConstant(operand) == 1) {
                val aliasee = LLVMAliasGetAliasee(operand)
                operandName =
                    LLVMPrintValueToString(aliasee)
                        .getString() // Already resolve the aliasee of the constant
            } else {
                operandName = "Some constant value" // TODO
            }
        } else if (LLVMIsUndef(operand) == 1) {
            operandName = "undef"
        } else if (LLVMIsPoison(operand) == 1) {
            operandName = "poison"
        } else {
            operandName = LLVMGetValueName(operand).getString() // The argument (without the %)
        }
        return operandName
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        TODO("Not yet implemented")
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {
        TODO("Not yet implemented")
    }
}
