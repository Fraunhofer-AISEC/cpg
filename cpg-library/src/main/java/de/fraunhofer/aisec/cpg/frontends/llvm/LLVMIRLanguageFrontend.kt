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
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.SizeTPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

class LLVMIRLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {

    companion object {
        @kotlin.jvm.JvmField var LLVM_EXTENSIONS: List<String> = listOf(".ll")
    }

    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)

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
            val errorMsg = String(errorMessage.array())
            // LLVMDisposeMessage(errorMessage)
            throw TranslationException("Could not create memory buffer: $errorMsg")
        }

        result = LLVMParseIRInContext(ctx, buf, mod, errorMessage)
        if (result != 0) {
            // something went wrong
            val errorMsg = String(errorMessage.array())
            // LLVMDisposeMessage(errorMessage)
            throw TranslationException("Could not parse IR: $errorMsg")
        }

        // println(result)
        // println(mod)

        val tu = TranslationUnitDeclaration()

        // we need to set our translation unit as the global scope
        scopeManager.resetToGlobal(tu)

        // TODO: no idea how to enumerate them
        val names = listOf("struct.ST", "struct.RT")

        for (name in names) {
            val typeRef = LLVMGetTypeByName2(ctx, name)

            if (typeRef != null) {
                val decl = parseStructType(typeRef)

                scopeManager.addDeclaration(decl)
            }
        }

        // loop through globals
        var global = LLVMGetFirstGlobal(mod)
        while (global != null) {
            val name = LLVMGetValueName(global)
            println(name.string)

            global = LLVMGetNextGlobal(global)
        }

        // loop through named meta
        var alias = LLVMGetFirstGlobalIFunc(mod)
        while (alias != null) {
            val name = LLVMGetValueName(global)
            println(name.string)

            alias = LLVMGetNextGlobal(alias)
        }

        // loop through functions
        var func = LLVMGetFirstFunction(mod)
        while (func != null) {
            // try to parse the function (declaration)
            val declaration = handleFunction(func)

            scopeManager.addDeclaration(declaration)

            func = LLVMGetNextFunction(func)
        }

        // TODO: actually clean them up, if we throw
        LLVMContextDispose(ctx)

        return tu
    }

    private fun parseStructType(typeRef: LLVMTypeRef): RecordDeclaration {
        val name = LLVMGetStructName(typeRef).string

        val record = NodeBuilder.newRecordDeclaration(name, "struct", "")

        scopeManager.enterScope(record)

        val size = LLVMCountStructElementTypes(typeRef)

        for (i in 0 until size) {
            val a = LLVMStructGetTypeAtIndex(typeRef, i)
            val fieldType = typeFrom(a)

            // there are no names, so we need to invent some dummy ones for easier reading
            val fieldName = "field$i"

            val field =
                NodeBuilder.newFieldDeclaration(
                    fieldName,
                    fieldType,
                    listOf(),
                    "",
                    null,
                    null,
                    false
                )

            scopeManager.addDeclaration(field)
        }

        scopeManager.leaveScope(record)

        return record
    }

    private fun typeOf(valueRef: LLVMValueRef): Type {
        val typeRef = LLVMTypeOf(valueRef)

        return typeFrom(typeRef)
    }

    private fun typeFrom(typeRef: LLVMTypeRef): Type {
        val typeBuf = LLVMPrintTypeToString(typeRef)

        // TODO: According to the doc LLVMDisposeMessage should be used, but it crashes

        var s = typeBuf.string

        // if the type is an identified type, i.e., it begins with a %, we get rid of the %
        // character
        // otherwise, the CPG will not connect it to the type. Note that the type name itself also
        // does
        // not include the % character.

        if (s.startsWith("%")) {
            s = s.substring(1)
        }

        return TypeParser.createFrom(s, false)
    }

    private fun handleFunction(func: LLVMValueRef): FunctionDeclaration {
        val name = LLVMGetValueName(func)

        val functionDeclaration =
            NodeBuilder.newFunctionDeclaration(name.string, this.getCodeFromRawNode(func))

        functionDeclaration.type = typeOf(func)

        scopeManager.enterScope(functionDeclaration)

        var bb = LLVMGetFirstBasicBlock(func)
        while (bb != null) {
            val stmt = handleBasicBlock(bb)

            // TODO: there are probably more than one basic block in a function, for now just take
            // one
            functionDeclaration.body = stmt

            bb = LLVMGetNextBasicBlock(bb)
        }

        scopeManager.leaveScope(functionDeclaration)

        return functionDeclaration
    }

    private fun handleBasicBlock(bb: LLVMBasicBlockRef?): CompoundStatement {
        val stmt = NodeBuilder.newCompoundStatement("")

        var instr = LLVMGetFirstInstruction(bb)
        while (instr != null) {
            log.debug("Parsing {}", getCodeFromRawNode(instr))

            when (LLVMGetInstructionOpcode(instr)) {
                LLVMRet -> {
                    val ret = NodeBuilder.newReturnStatement(getCodeFromRawNode(instr))

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

                    stmt.addStatement(ret)
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
                    stmt.addStatement(parseBinaryOperator(instr, "+", false, false))
                }
                LLVMFAdd -> {
                    stmt.addStatement(parseBinaryOperator(instr, "+", true, false))
                }
                LLVMSub -> {
                    stmt.addStatement(parseBinaryOperator(instr, "-", false, false))
                }
                LLVMFSub -> {
                    stmt.addStatement(parseBinaryOperator(instr, "-", true, false))
                }
                LLVMMul -> {
                    stmt.addStatement(parseBinaryOperator(instr, "*", false, false))
                }
                LLVMFMul -> {
                    stmt.addStatement(parseBinaryOperator(instr, "*", true, false))
                }
                LLVMUDiv -> {
                    stmt.addStatement(parseBinaryOperator(instr, "+", false, true))
                }
                LLVMSDiv -> {
                    stmt.addStatement(parseBinaryOperator(instr, "/", false, false))
                }
                LLVMFDiv -> {
                    stmt.addStatement(parseBinaryOperator(instr, "/", true, false))
                }
                LLVMURem -> {
                    stmt.addStatement(parseBinaryOperator(instr, "%", false, true))
                }
                LLVMSRem -> {
                    println("srem instruction")
                }
                LLVMFRem -> {
                    println("frem instruction")
                }
                LLVMShl -> {
                    stmt.addStatement(parseBinaryOperator(instr, "<<", false, false))
                }
                LLVMLShr -> {
                    stmt.addStatement(parseBinaryOperator(instr, ">>", false, false))
                }
                LLVMAShr -> {
                    println("ashr instruction")
                }
                LLVMAnd -> {
                    stmt.addStatement(parseBinaryOperator(instr, "&", false, false))
                }
                LLVMOr -> {
                    stmt.addStatement(parseBinaryOperator(instr, "|", false, false))
                }
                LLVMXor -> {
                    stmt.addStatement(parseBinaryOperator(instr, "^", false, false))
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
                    stmt.addStatement(parseBinaryOperator(instr, cmpPred, false, unsigned))
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
                    stmt.addStatement(parseBinaryOperator(instr, cmpPred, false, false, unordered))
                }
                LLVMPHI -> {
                    println("phi instruction")
                }
                LLVMCall -> {
                    stmt.addStatement(parseFunctionCall(instr))
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

        return stmt
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
            binaryOperator = NodeBuilder.newBinaryOperator(op, this.getCodeFromRawNode(instr))

            if (op1Type.contains("unsigned "))
                binaryOperator.lhs = NodeBuilder.newCastExpression(this.getCodeFromRawNode(instr))
            else binaryOperator.lhs = NodeBuilder.newDeclaredReferenceExpression(op1, t1, op1)

            if (op2Type.contains("unsigned "))
                binaryOperator.rhs = NodeBuilder.newCastExpression(this.getCodeFromRawNode(instr))
            else binaryOperator.rhs = NodeBuilder.newDeclaredReferenceExpression(op2, t2, op2)

            if (unordered) {
                // Special case for floating point comparisons which check if a value is "unordered
                // or <op>".
                // Statement is then lhs = isunordered(op1, op2) || (op1 <op> op2)
                binOpUnordered = NodeBuilder.newBinaryOperator("||", this.getCodeFromRawNode(instr))
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

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        if (astNode is LLVMValueRef) {
            val code = LLVMPrintValueToString(astNode)

            // TODO: dispose?

            return code.string
        }

        return null
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        TODO("Not yet implemented")
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {
        TODO("Not yet implemented")
    }
}
