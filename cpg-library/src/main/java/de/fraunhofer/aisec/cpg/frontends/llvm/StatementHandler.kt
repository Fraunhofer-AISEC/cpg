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
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.*
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

    /**
     * Handles the parsing of
     * [instructions](https://llvm.org/docs/LangRef.html#instruction-reference). Instructions are
     * usually mapped to statements.
     *
     * It is noteworthy, that LLVM IR is a single state assignment form, meaning, that all
     * instructions that perform an assignment will result in a [DeclarationStatement] and a
     * [VariableDeclaration], with the original instruction wrapped into the
     * [VariableDeclaration.initializer] property.
     *
     * Currently this wrapping is done in the individual instruction parsing functions, but should
     * be extracted from that, e.g. by routing it through the [DeclarationHandler].
     */
    private fun handleInstruction(instr: LLVMValueRef): Statement {
        when (LLVMGetInstructionOpcode(instr)) {
            LLVMRet -> {
                val ret = NodeBuilder.newReturnStatement(lang.getCodeFromRawNode(instr))

                val numOps = LLVMGetNumOperands(instr)
                if (numOps != 0) {
                    val retType = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).string
                    ret.returnValue = getOperandValueAtIndex(instr, 0, retType)
                }

                return ret
            }
            LLVMBr -> {
                return handleBrStatement(instr)
            }
            LLVMSwitch -> {
                return handleSwitchStatement(instr)
            }
            LLVMIndirectBr -> {
                println("indirect br instruction")
            }
            LLVMInvoke -> {
                // TODO: Function call and an edge potentially transferring control flow to a catch
                println("invoke instruction")
            }
            LLVMUnreachable -> {
                // Does nothing
                return Statement()
            }
            LLVMCallBr -> {
                // TODO: Maps to a call but also to a goto statement?
                println("call instruction")
            }
            LLVMFNeg -> {
                val fneg =
                    NodeBuilder.newUnaryOperator(
                        "-",
                        false,
                        false,
                        LLVMPrintValueToString(instr).string
                    )
                val opType = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).string
                fneg.input = getOperandValueAtIndex(instr, 0, opType)
                return fneg
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
                /* BinaryOperator. Arithmetic right shift operator:
                 * Set the most significant bit with the first bit of op1, not with 0.
                 *
                 * Returns a "poison value" if
                 * 1) op2 is bigger than or equal to the number of bits in op1, or
                 * 2) "exact" is present and a non-zero value is shifted out.
                 */
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
                return handleGetElementPtr(instr)
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
                return handleIntegerComparison(instr)
            }
            LLVMFCmp -> {
                return handleFloatComparison(instr)
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
                return handleAtomiccmpxchg(instr)
            }
            LLVMAtomicRMW -> {
                return handleAtomicrmw(instr)
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

    /** Handles comparison operators for integer values. */
    private fun handleIntegerComparison(instr: LLVMValueRef): Statement {
        val cmpPred: String
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

    /** Handles comparison operators for floating point values. */
    private fun handleFloatComparison(instr: LLVMValueRef): Statement {
        val cmpPred: String
        var unordered = false
        when (LLVMGetICmpPredicate(instr)) {
            LLVMRealPredicateFalse -> {
                return NodeBuilder.newLiteral(false, TypeParser.createFrom("i1", true), "false")
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
                return NodeBuilder.newLiteral(true, TypeParser.createFrom("i1", true), "true")
            }
            else -> cmpPred = "unknown"
        }
        return parseBinaryOperator(instr, cmpPred, true, false, unordered)
    }

    /**
     * Handles the [`getelementptr`](https://llvm.org/docs/LangRef.html#getelementptr-instruction)
     * instruction.
     *
     * We try to convert it either into an [ArraySubscriptionExpression] or an [MemberExpression],
     * depending whether the accessed variable is a struct or an array. Furthermore, since
     * `getelementptr` allows an (infinite) chain of sub-element access within a single instruction,
     * we need to unwrap those into individual expressions.
     */
    private fun handleGetElementPtr(instr: LLVMValueRef): Statement {
        val lhs = LLVMGetValueName(instr).string
        val numOps = LLVMGetNumOperands(instr)

        var expr: Expression? = null
        var base: Expression? = null

        // the first operand is the type that is the basis for the calculation
        var paramType = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).string
        var operand = getOperandValueAtIndex(instr, 0, paramType)

        var baseType: Type = UnknownType.getUnknownType()

        // try to check, if it is a struct
        if (paramType.startsWith("%")) {
            // get the base type
            baseType = TypeParser.createFrom(paramType.substring(1), false)

            // construct our base expression from the first operand
            base = operand
        } else {
            log.error(
                "First parameter is not a structure name. This should not happen. Cannot continue"
            )
            return Statement()
        }

        for (idx: Int in 1 until numOps) {
            // the second argument is the base address that we start our chain from
            paramType = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, idx))).string
            operand = getOperandValueAtIndex(instr, idx, paramType)

            // try to parse index as int for now only
            val index: Int
            if (operand is Literal<*>) {
                val literalValue = operand.value
                if (literalValue is Long) {
                    index = literalValue.toInt()
                } else throw Exception("No index specified")
            } else throw Exception("No index specified")

            // check, if it is a pointer -> then we need to handle this as an array access
            if (baseType is PointerType) {
                val arrayExpr = NodeBuilder.newArraySubscriptionExpression("")
                arrayExpr.arrayExpression = base
                arrayExpr.name = index.toString()
                arrayExpr.subscriptExpression = operand
                expr = arrayExpr

                log.info("{}", expr)

                // deference the type to get the new base type
                baseType = baseType.dereference()
                // expr is the new base
                base = expr
            } else {
                val record = usageOfStruct(baseType.typeName)

                if (record == null) {
                    log.error(
                        "Could not find structure type with name {}, cannot continue",
                        baseType.typeName
                    )
                    return Statement()
                }

                log.debug(
                    "Trying to access a field within the record declaration of {}",
                    record.name
                )

                // look for the field
                val field = record.getField("field_$index")

                // our new basetype is the type of the field
                baseType = field?.type ?: UnknownType.getUnknownType()

                // construct our member expression
                expr = NodeBuilder.newMemberExpression(base, field?.type, field?.name, ".", "")
                log.info("{}", expr)

                // expr is the new base
                base = expr
            }
        }

        // TODO: extract LHS wrapping into declaration handler
        val decl = NodeBuilder.newVariableDeclaration(lhs, UnknownType.getUnknownType(), "", false)

        lang.scopeManager.addDeclaration(decl)

        val ref = NodeBuilder.newUnaryOperator("&", false, true, "")
        ref.input = expr

        return declarationOrNot(ref, lhs)
    }

    private fun usageOfStruct(name: String): RecordDeclaration? {
        // try to see, if the struct already exists as a record declaration
        var record =
            lang.scopeManager
                .resolve<RecordDeclaration>(lang.scopeManager.globalScope, true) { it.name == name }
                .firstOrNull()

        // if not, maybe we can find it as a structure type
        if (record == null) {
            // try to parse it
            val typeRef = LLVMGetTypeByName2(lang.ctx, name)
            if (typeRef == null) {
                return null
            } else {
                record = lang.declarationHandler.handle(typeRef) as RecordDeclaration

                // add it to the global scope
                lang.scopeManager.globalScope?.addDeclaration(record)
            }
        }

        return record
    }

    /**
     * Parses the `atomicrmw` instruction. It returns either a single [Statement] or a
     * [CompoundStatement] if the value is assigned to another variable. Performs the following
     * operation atomically:
     * ```
     * lhs = {*pointer, *pointer == cmp} // A struct of {T, i1}
     * if(*pointer == cmp) { *pointer = new }
     * ```
     * Returns a [CompoundStatement] with those two instructions or, if lhs doesn't exist, only the
     * if-then statement.
     */
    private fun handleAtomiccmpxchg(instr: LLVMValueRef): Statement {
        val instrStr = lang.getCodeFromRawNode(instr)
        val compoundStatement = NodeBuilder.newCompoundStatement(instrStr)
        compoundStatement.name = "atomiccmpxchg"
        val tyPtr = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).string
        val ptr = getOperandValueAtIndex(instr, 0, tyPtr)
        val ty = tyPtr.substring(0, tyPtr.length - 1) // Remove the *
        val cmp = getOperandValueAtIndex(instr, 1, ty)
        val value = getOperandValueAtIndex(instr, 2, ty)

        val ptrDeref = NodeBuilder.newUnaryOperator("*", false, false, instrStr)
        ptrDeref.input = ptr

        val cmpExpr = NodeBuilder.newBinaryOperator("==", instrStr)
        cmpExpr.lhs = ptrDeref
        cmpExpr.rhs = cmp

        val lhs = LLVMGetValueName(instr).string
        if (lhs != "") {
            val structValue = Expression()
            // TODO: Create the anonymous struct if necessary and initialize the values.
            val decl = declarationOrNot(structValue, lhs)
            compoundStatement.addStatement(decl)
        }
        val assignment = NodeBuilder.newBinaryOperator("=", instrStr)
        assignment.lhs = ptrDeref
        assignment.rhs = value

        val ifStatement = NodeBuilder.newIfStatement(instrStr)
        ifStatement.condition = cmpExpr
        ifStatement.thenStatement = assignment

        compoundStatement.addStatement(ifStatement)

        return compoundStatement
    }

    /**
     * Parses the `atomicrmw` instruction. It returns either a single [Statement] or a
     * [CompoundStatement] if the value is assigned to another variable. >>>>>>> Start with cmpxchg
     * instruction
     */
    private fun handleAtomicrmw(instr: LLVMValueRef): Statement {
        val lhs = LLVMGetValueName(instr).string
        val instrStr = lang.getCodeFromRawNode(instr)
        val operation = LLVMGetAtomicRMWBinOp(instr)
        val tyPtr = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).string
        val ptr = getOperandValueAtIndex(instr, 0, tyPtr)
        val ty = tyPtr.substring(0, tyPtr.length - 1) // Remove the *
        val value = getOperandValueAtIndex(instr, 1, ty)
        val exchOp = NodeBuilder.newBinaryOperator("=", instrStr)
        exchOp.name = "atomicrmw"

        val ptrDeref = NodeBuilder.newUnaryOperator("*", false, false, instrStr)
        ptrDeref.input = ptr
        exchOp.lhs = ptrDeref

        when (operation) {
            LLVMAtomicRMWBinOpXchg -> {
                exchOp.rhs = value
            }
            LLVMAtomicRMWBinOpAdd -> {
                val binaryOperator = NodeBuilder.newBinaryOperator("+", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpSub -> {
                val binaryOperator = NodeBuilder.newBinaryOperator("-", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpAnd -> {
                val binaryOperator = NodeBuilder.newBinaryOperator("&", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpNand -> {
                val binaryOperator = NodeBuilder.newBinaryOperator("|", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                val unaryOperator = NodeBuilder.newUnaryOperator("~", false, false, instrStr)
                unaryOperator.input = binaryOperator
                exchOp.rhs = unaryOperator
            }
            LLVMAtomicRMWBinOpOr -> {
                val binaryOperator = NodeBuilder.newBinaryOperator("|", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpXor -> {
                val binaryOperator = NodeBuilder.newBinaryOperator("^", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpMax -> {
                val condition = NodeBuilder.newBinaryOperator(">", instrStr)
                condition.lhs = ptrDeref
                condition.rhs = value
                val conditional =
                    NodeBuilder.newConditionalExpression(
                        condition,
                        ptrDeref,
                        value,
                        TypeParser.createFrom(ty, true)
                    )
                exchOp.rhs = conditional
            }
            LLVMAtomicRMWBinOpMin -> {
                val condition = NodeBuilder.newBinaryOperator("<", instrStr)
                condition.lhs = ptrDeref
                condition.rhs = value
                val conditional =
                    NodeBuilder.newConditionalExpression(
                        condition,
                        ptrDeref,
                        value,
                        TypeParser.createFrom(ty, true)
                    )
                exchOp.rhs = conditional
            }
            LLVMAtomicRMWBinOpUMax -> {
                val condition = NodeBuilder.newBinaryOperator(">", instrStr)
                ptrDeref.input.type = TypeParser.createFrom("u$ty", true)
                condition.lhs = ptrDeref
                value.type = TypeParser.createFrom("u$ty", true)
                condition.rhs = value
                val conditional =
                    NodeBuilder.newConditionalExpression(
                        condition,
                        ptrDeref,
                        value,
                        TypeParser.createFrom(ty, true)
                    )
                exchOp.rhs = conditional
            }
            LLVMAtomicRMWBinOpUMin -> {
                val condition = NodeBuilder.newBinaryOperator("<", instrStr)
                ptrDeref.input.type = TypeParser.createFrom("u$ty", true)
                condition.lhs = ptrDeref
                value.type = TypeParser.createFrom("u$ty", true)
                condition.rhs = value
                val conditional =
                    NodeBuilder.newConditionalExpression(
                        condition,
                        ptrDeref,
                        value,
                        TypeParser.createFrom(ty, true)
                    )
                exchOp.rhs = conditional
            }
            LLVMAtomicRMWBinOpFAdd -> {
                val binaryOperator = NodeBuilder.newBinaryOperator("+", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpFSub -> {
                val binaryOperator = NodeBuilder.newBinaryOperator("-", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            else -> {
                throw Exception("LLVMAtomicRMWBinOp $operation not supported")
            }
        }

        if (lhs != "") {
            // set lhs = *ptr, then perform the replacement
            val compoundStatement = NodeBuilder.newCompoundStatement(instrStr)
            compoundStatement.statements = Arrays.asList(declarationOrNot(ptrDeref, lhs), exchOp)
            return compoundStatement
        } else {
            // only perform the replacement
            return exchOp
        }
    }

    /** Handles a [`br`](https://llvm.org/docs/LangRef.html#br-instruction) instruction. */
    private fun handleBrStatement(instr: LLVMValueRef): Statement {
        if (LLVMGetNumOperands(instr) == 3) {
            // if(op) then {label1} else {label2}
            val ifStatement =
                NodeBuilder.newConditionalBranchStatement(lang.getCodeFromRawNode(instr))
            val condition = getOperandValueAtIndex(instr, 0, "i1")
            val label1Name = LLVMGetValueName(LLVMGetOperand(instr, 1)).string

            val thenLabelStatement: LabelStatement
            if (lang.labelMap.contains(label1Name)) {
                thenLabelStatement = lang.labelMap.get(label1Name)!!
            } else {
                thenLabelStatement = NodeBuilder.newLabelStatement(label1Name)
                thenLabelStatement.name = label1Name
            }
            ifStatement.addConditionalTarget(condition, thenLabelStatement)

            // Set the default branch ("else")
            val label2Name =
                LLVMGetValueName(LLVMGetOperand(instr, 2)).string // The label of the else branch
            val elseLabelStatement: LabelStatement
            if (lang.labelMap.contains(label2Name)) {
                elseLabelStatement = lang.labelMap.get(label2Name)!!
            } else {
                elseLabelStatement = NodeBuilder.newLabelStatement(label2Name)
                elseLabelStatement.name = label2Name
            }
            ifStatement.setDefaultTargetLabel(elseLabelStatement)

            return ifStatement
        } else if (LLVMGetNumOperands(instr) == 1) {
            // goto defaultLocation
            val gotoStatement = NodeBuilder.newGotoStatement(lang.getCodeFromRawNode(instr))
            val defaultLocation = LLVMGetOperand(instr, 0) // The BB of the target
            val labelStatement: LabelStatement
            val labelName = LLVMGetValueName(defaultLocation).string
            if (lang.labelMap.contains(labelName)) labelStatement = lang.labelMap.get(labelName)!!
            else {
                labelStatement = NodeBuilder.newLabelStatement(labelName)
                labelStatement.label = labelName
            }
            gotoStatement.labelName = labelName
            gotoStatement.targetLabel = labelStatement

            return gotoStatement
        } else {
            throw Exception("Wrong number of operands in br statement")
        }
    }

    private fun handleSwitchStatement(instr: LLVMValueRef): Statement {
        val numOps = LLVMGetNumOperands(instr)
        if (numOps < 2) throw Exception("Switch statement without operand and default branch")

        val opType = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 0))).string
        val operand = getOperandValueAtIndex(instr, 0, opType)

        val switchStatement =
            NodeBuilder.newConditionalBranchStatement(lang.getCodeFromRawNode(instr))

        val defaultLocation =
            LLVMGetValueName(LLVMGetOperand(instr, 1)).string // The label of the "default" branch
        val defaultLabelStatement: LabelStatement
        if (lang.labelMap.contains(defaultLocation)) {
            defaultLabelStatement = lang.labelMap.get(defaultLocation)!!
        } else {
            defaultLabelStatement = NodeBuilder.newLabelStatement(defaultLocation)
            defaultLabelStatement.name = defaultLocation
        }
        switchStatement.setDefaultTargetLabel(defaultLabelStatement)

        var idx = 2
        while (idx < numOps) {
            val binaryOperator = NodeBuilder.newBinaryOperator("==", lang.getCodeFromRawNode(instr))
            binaryOperator.lhs = operand
            binaryOperator.rhs = getOperandValueAtIndex(instr, idx, opType)
            idx++
            val catchLabel = LLVMGetValueName(LLVMGetOperand(instr, idx)).string
            val catchLabelStatement: LabelStatement
            if (lang.labelMap.contains(catchLabel)) {
                catchLabelStatement = lang.labelMap.get(catchLabel)!!
            } else {
                catchLabelStatement = NodeBuilder.newLabelStatement(catchLabel)
                catchLabelStatement.name = catchLabel
            }
            switchStatement.addConditionalTarget(binaryOperator, catchLabelStatement)
            idx++
        }
        return switchStatement
    }

    private fun parseFunctionCall(instr: LLVMValueRef): Statement {
        val lhs = LLVMGetValueName(instr).string
        val calledFunc = LLVMGetCalledValue(instr)
        val calledFuncName = LLVMGetValueName(calledFunc).string
        val funcType = LLVMGetCalledFunctionType(instr)
        val retVal = LLVMPrintTypeToString(LLVMGetReturnType(funcType)).string
        var param = LLVMGetFirstParam(calledFunc)
        var idx = 0

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
            callExpr.addArgument(operandName)
            param = LLVMGetNextParam(param)
            idx++
        }

        return declarationOrNot(callExpr, lhs)
    }

    /**
     * Most instructions in LLVM have a variable assignment as part of their instruction. Since LLVM
     * IR is SSA, we need to declare a new variable in this case, which is named according to [lhs].
     * In case [lhs] is an empty string, the variable assignment is optional and we directly return
     * the [Expression] associated with the instruction.
     */
    private fun declarationOrNot(rhs: Expression, lhs: String): Statement {
        return if (lhs != "") {
            val decl = VariableDeclaration()
            decl.name = lhs
            decl.initializer = rhs

            val declStatement = DeclarationStatement()
            declStatement.singleDeclaration = decl
            declStatement
        } else {
            rhs
        }
    }

    /**
     * Handles a basic block and returns a [CompoundStatement] comprised of the statements of this
     * block.
     */
    private fun handleBasicBlock(bb: LLVMBasicBlockRef): CompoundStatement {
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
        if (unsigned) op1Type = "u$op1Type"
        val t1 = TypeParser.createFrom(op1Type, true)

        var op2Type = LLVMPrintTypeToString(LLVMTypeOf(LLVMGetOperand(instr, 1))).string
        val op2 = getOperandValueAtIndex(instr, 1, op2Type)
        if (unsigned) op2Type = "u$op2Type"
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
            binaryOperator.addArgument(op1)
            binaryOperator.addArgument(op2)
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
            unorderedCall.addArgument(op1)
            unorderedCall.addArgument(op2)
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

            if (unsigned)
                binaryOperator.lhs = NodeBuilder.newCastExpression(lang.getCodeFromRawNode(instr))
            else binaryOperator.lhs = op1

            if (unsigned)
                binaryOperator.rhs = NodeBuilder.newCastExpression(lang.getCodeFromRawNode(instr))
            else binaryOperator.rhs = op2

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
                unorderedCall.addArgument(op1)
                unorderedCall.addArgument(op2)
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

    private fun getOperandValueAtIndex(instr: LLVMValueRef, idx: Int, type: String): Expression {
        val cpgType = TypeParser.createFrom(type, true)
        val operand = LLVMGetOperand(instr, idx)
        val operandName: String
        if (LLVMIsConstant(operand) == 1) {
            if (LLVMIsConstantString(operand) == 1) {
                operandName = LLVMGetAsString(operand, SizeTPointer(100)).toString()
                return NodeBuilder.newLiteral(operandName, cpgType, operandName)
            } else if (type.startsWith("ui")) {
                val opValue = LLVMConstIntGetZExtValue(operand)
                return NodeBuilder.newLiteral(opValue, cpgType, opValue.toString())
            } else if (type.startsWith("i")) {
                val opValue = LLVMConstIntGetSExtValue(operand)
                return NodeBuilder.newLiteral(opValue, cpgType, opValue.toString())
            } else if (type == "double" ||
                    type == "bfloat" ||
                    type == "float" ||
                    type == "half" ||
                    type == "fp128" ||
                    type == "x86_fp80" ||
                    type == "ppc_fp128"
            ) {
                val losesInfo = IntArray(1)
                val opValue = LLVMConstRealGetDouble(operand, losesInfo)
                return NodeBuilder.newLiteral(opValue, cpgType, opValue.toString())
            } else if (LLVMIsAGlobalAlias(operand) != null || LLVMIsGlobalConstant(operand) == 1) {
                val aliasee = LLVMAliasGetAliasee(operand)
                operandName =
                    LLVMPrintValueToString(aliasee)
                        .string // Already resolve the aliasee of the constant
                return NodeBuilder.newLiteral(operandName, cpgType, operandName)
            } else {
                // TODO This does not return the actual constant but only a string representation
                return NodeBuilder.newLiteral(
                    LLVMPrintValueToString(operand).toString(),
                    cpgType,
                    LLVMPrintValueToString(operand).toString()
                )
            }
        } else if (LLVMIsUndef(operand) == 1) {
            return NodeBuilder.newDeclaredReferenceExpression("undef", cpgType, "undef")
        } else if (LLVMIsPoison(operand) == 1) {
            return NodeBuilder.newDeclaredReferenceExpression("poison", cpgType, "poison")
        } else {
            operandName = LLVMGetValueName(operand).string // The argument (without the %)
            return NodeBuilder.newDeclaredReferenceExpression(operandName, cpgType, operandName)
        }
    }
}
