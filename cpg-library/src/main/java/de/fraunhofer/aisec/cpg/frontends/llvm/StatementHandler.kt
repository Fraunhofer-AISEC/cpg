/*
 * Copyright (c) 2019 - 2020, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.util.*
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.SizeTPointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

class StatementHandler(lang: LLVMIRLanguageFrontend) :
    Handler<Statement, Pointer, LLVMIRLanguageFrontend>(::Statement, lang) {
    init {
        map.put(LLVMValueRef::class.java) { handleInstruction(it as LLVMValueRef) }
        map.put(LLVMBasicBlockRef::class.java) { handleBasicBlock(it as LLVMBasicBlockRef) }
    }

    private fun handleInstruction(instr: LLVMValueRef): Statement {
        when (LLVMGetInstructionOpcode(instr)) {
            LLVMRet -> {
                val ret = NodeBuilder.newReturnStatement(lang.getCodeFromRawNode(instr))

                val numOps = LLVMGetNumOperands(instr)
                if (numOps == 0) {
                    println("ret void instruction")
                } else {
                    // TODO: loop through all operands and handle them as expressions
                    val paramType =
                        LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).string
                    val operandName = getOperandValueAtIndex(instr, 0, paramType)
                    println("ret $operandName")
                }

                return ret
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
                return parseBinaryOperator(instr, "+", false, false)
            }
            LLVMFAdd -> {
                return parseBinaryOperator(instr, "+", true, false)
            }
            LLVMSub -> {
                return parseBinaryOperator(instr, "-", false, false)
            }
            LLVMFSub -> {
                return parseBinaryOperator(instr, "-", true, false)
            }
            LLVMMul -> {
                return parseBinaryOperator(instr, "*", false, false)
            }
            LLVMFMul -> {
                return parseBinaryOperator(instr, "*", true, false)
            }
            LLVMUDiv -> {
                return parseBinaryOperator(instr, "+", false, true)
            }
            LLVMSDiv -> {
                return parseBinaryOperator(instr, "/", false, false)
            }
            LLVMFDiv -> {
                return parseBinaryOperator(instr, "/", true, false)
            }
            LLVMURem -> {
                return parseBinaryOperator(instr, "%", false, true)
            }
            LLVMSRem -> {
                println("srem instruction")
            }
            LLVMFRem -> {
                println("frem instruction")
            }
            LLVMShl -> {
                return parseBinaryOperator(instr, "<<", false, false)
            }
            LLVMLShr -> {
                return parseBinaryOperator(instr, ">>", false, false)
            }
            LLVMAShr -> {
                println("ashr instruction")
            }
            LLVMAnd -> {
                return parseBinaryOperator(instr, "&", false, false)
            }
            LLVMOr -> {
                return parseBinaryOperator(instr, "|", false, false)
            }
            LLVMXor -> {
                return parseBinaryOperator(instr, "^", false, false)
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
                val lhs = LLVMGetValueName(instr).string
                val numOps = LLVMGetNumOperands(instr)
                var args = ""
                for (idx: Int in 0 until numOps) {
                    val paramType =
                        LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, idx))).string
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
                    LLVMIntULE -> {
                        cmpPred = "<="
                        unsigned = true
                    }
                    LLVMIntSGT -> cmpPred = ">"
                    LLVMIntSGE -> cmpPred = ">="
                    LLVMIntSLT -> cmpPred = "<"
                    LLVMIntSLE -> cmpPred = "<="
                    else -> cmpPred = "unknown"
                }
                return parseBinaryOperator(instr, cmpPred, false, unsigned)
            }
            LLVMFCmp -> {
                var cmpPred: String
                var unordered = false
                when (LLVMGetICmpPredicate(instr)) {
                    LLVMRealPredicateFalse -> {
                        cmpPred = "false" // TODO
                        // continue // continue??!
                        return Statement()
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
                        return Statement()
                    }
                    else -> cmpPred = "unknown"
                }
                println("fcmp $cmpPred instruction")
                return parseBinaryOperator(instr, cmpPred, false, false, unordered)
            }
            LLVMPHI -> {
                println("phi instruction")
            }
            LLVMCall -> {
                return parseFunctionCall(instr)
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

        return Statement()
    }

    private fun parseFunctionCall(instr: LLVMValueRef): Statement {
        val lhs = LLVMGetValueName(instr).string
        val calledFunc = LLVMGetCalledValue(instr)
        val calledFuncName = LLVMGetValueName(calledFunc).string
        val funcType = LLVMGetCalledFunctionType(instr)
        val retVal = LLVMPrintTypeToString(LLVMGetReturnType(funcType)).string
        var param = LLVMGetFirstParam(calledFunc)
        var idx = 0
        var args = ""

        val callExpr =
            NodeBuilder.newCallExpression(
                calledFuncName,
                calledFuncName,
                LLVMPrintValueToString(instr).string,
                false
            )

        while (param != null) {
            val paramType = LLVMPrintTypeToString(LLVMTypeOf(param)).string // Type of the argument
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

    private fun handleBasicBlock(bb: LLVMBasicBlockRef?): CompoundStatement {
        val compound = NodeBuilder.newCompoundStatement("")

        var instr = LLVMGetFirstInstruction(bb)
        while (instr != null) {
            log.debug("Parsing {}", lang.getCodeFromRawNode(instr))

            val stmt = lang.statementHandler.handle(instr)

            compound.addStatement(stmt)

            instr = LLVMGetNextInstruction(instr)
        }

        return compound
    }

    private fun parseBinaryOperator(
        instr: LLVMValueRef,
        op: String,
        float: Boolean,
        unsigned: Boolean,
        unordered: Boolean = false
    ): Statement {
        val lhs = LLVMGetValueName(instr).string

        var op1Type = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).string
        val op1 = getOperandValueAtIndex(instr, 0, op1Type)
        if (unsigned) op1Type = "unsigned $op1Type"
        val t1 = TypeParser.createFrom(op1Type, true)

        var op2Type = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 1))).string
        val op2 = getOperandValueAtIndex(instr, 1, op2Type)
        if (unsigned) op2Type = "unsigned $op2Type"
        val t2 = TypeParser.createFrom(op2Type, true)

        val binaryOperator: Expression
        var binOpUnordered: BinaryOperator? = null

        if (op.equals("uno")) {
            // Unordered comparison operand => Replace with a call to isunordered(x, y)
            // Resulting statement: i1 lhs = isordered(op1, op2)
            binaryOperator =
                NodeBuilder.newCallExpression(
                    "isunordered",
                    "isunordered",
                    LLVMPrintValueToString(instr).string,
                    false
                )
            binaryOperator.addArgument(NodeBuilder.newDeclaredReferenceExpression(op1, t1, op1))
            binaryOperator.addArgument(NodeBuilder.newDeclaredReferenceExpression(op2, t2, op2))
        } else if (op.equals("ord")) {
            // Ordered comparison operand => Replace with !isunordered(x, y)
            // Resulting statement: i1 lhs = !isordered(op1, op2)
            val unorderedCall =
                NodeBuilder.newCallExpression(
                    "isunordered",
                    "isunordered",
                    LLVMPrintValueToString(instr).string,
                    false
                )
            unorderedCall.addArgument(NodeBuilder.newDeclaredReferenceExpression(op1, t1, op1))
            unorderedCall.addArgument(NodeBuilder.newDeclaredReferenceExpression(op2, t2, op2))
            binaryOperator =
                NodeBuilder.newUnaryOperator(
                    "!",
                    false,
                    false,
                    LLVMPrintValueToString(instr).string
                )
            binaryOperator.input = unorderedCall
        } else {
            // Resulting statement: lhs = op1 <op> op2.
            binaryOperator = NodeBuilder.newBinaryOperator(op, lang.getCodeFromRawNode(instr))

            if (op1Type.contains("unsigned "))
                binaryOperator.lhs = NodeBuilder.newCastExpression(lang.getCodeFromRawNode(instr))
            else binaryOperator.lhs = NodeBuilder.newDeclaredReferenceExpression(op1, t1, op1)

            if (op2Type.contains("unsigned "))
                binaryOperator.rhs = NodeBuilder.newCastExpression(lang.getCodeFromRawNode(instr))
            else binaryOperator.rhs = NodeBuilder.newDeclaredReferenceExpression(op2, t2, op2)

            if (unordered) {
                // Special case for floating point comparisons which check if a value is "unordered
                // or <op>".
                // Statement is then lhs = isunordered(op1, op2) || (op1 <op> op2)
                binOpUnordered = NodeBuilder.newBinaryOperator("||", lang.getCodeFromRawNode(instr))
                binOpUnordered.rhs = binaryOperator
                val unorderedCall =
                    NodeBuilder.newCallExpression(
                        "isunordered",
                        "isunordered",
                        LLVMPrintValueToString(instr).string,
                        false
                    )
                unorderedCall.addArgument(NodeBuilder.newDeclaredReferenceExpression(op1, t1, op1))
                unorderedCall.addArgument(NodeBuilder.newDeclaredReferenceExpression(op2, t2, op2))
                binOpUnordered.lhs = unorderedCall
            }
        }

        val decl = VariableDeclaration()
        if (Arrays.asList("==", "!=", "<", "<=", ">", ">=", "ord", "uno").contains(op)) {
            decl.type = TypeParser.createFrom("i1", true) // boolean type
        } else {
            decl.type = t1 // use the type of op1
        }
        decl.name = lhs
        decl.initializer = if (unordered) binOpUnordered else binaryOperator

        val declStatement = DeclarationStatement()
        declStatement.singleDeclaration = decl
        return declStatement
    }

    // TODO: this should return an expression instead and should be moved to an expressionhandler
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
                    (type == "double" ||
                        type == "bfloat" ||
                        type == "float" ||
                        type == "half" ||
                        type == "fp128" ||
                        type == "x86_fp80" ||
                        type == "ppc_fp128")
            ) {
                val losesInfo = IntArray(1)
                operandName = LLVMConstRealGetDouble(operand, losesInfo).toString()
            } else if (LLVMIsAGlobalAlias(operand) != null || LLVMIsGlobalConstant(operand) == 1) {
                val aliasee = LLVMAliasGetAliasee(operand)
                operandName =
                    LLVMPrintValueToString(aliasee)
                        .string // Already resolve the aliasee of the constant
            } else {
                operandName = "Some constant value" // TODO
            }
        } else if (LLVMIsUndef(operand) == 1) {
            operandName = "undef"
        } else if (LLVMIsPoison(operand) == 1) {
            operandName = "poison"
        } else {
            operandName = LLVMGetValueName(operand).string // The argument (without the %)
        }
        return operandName
    }
}
