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
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newArrayCreationExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newArraySubscriptionExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newBinaryOperator
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCallExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCaseStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCastExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCatchClause
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCompoundStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConditionalExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDefaultStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newEmptyStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newGotoStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newIfStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newInitializerListExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLabelStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newReturnStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newSwitchStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newTryStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newUnaryOperator
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.annotations.FunctionReplacement
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

class StatementHandler(lang: LLVMIRLanguageFrontend) :
    Handler<Statement, Pointer, LLVMIRLanguageFrontend>(::ProblemExpression, lang) {
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
     * Currently, this wrapping is done in the individual instruction parsing functions, but should
     * be extracted from that, e.g. by routing it through the [DeclarationHandler].
     */
    private fun handleInstruction(instr: LLVMValueRef): Statement {
        if (LLVMIsABinaryOperator(instr) != null) {
            return handleBinaryInstruction(instr)
        } else if (LLVMIsACastInst(instr) != null) {
            return declarationOrNot(lang.expressionHandler.handleCastInstruction(instr), instr)
        }

        val opcode = instr.opCode

        when (opcode) {
            LLVMRet -> {
                val ret = newReturnStatement(lang.getCodeFromRawNode(instr))

                val numOps = LLVMGetNumOperands(instr)
                if (numOps != 0) {
                    ret.returnValue = lang.getOperandValueAtIndex(instr, 0)
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
                return handleIndirectbrStatement(instr)
            }
            LLVMCall, LLVMInvoke -> {
                return handleFunctionCall(instr)
            }
            LLVMUnreachable -> {
                // Does nothing
                return newEmptyStatement(lang.getCodeFromRawNode(instr))
            }
            LLVMCallBr -> {
                // Maps to a call but also to a goto statement? Barely used => not relevant
                log.error("Cannot parse callbr instruction yet")
            }
            LLVMFNeg -> {
                val fneg = newUnaryOperator("-", false, true, lang.getCodeFromRawNode(instr))
                fneg.input = lang.getOperandValueAtIndex(instr, 0)
                return fneg
            }
            LLVMAlloca -> {
                return handleAlloca(instr)
            }
            LLVMLoad -> {
                return handleLoad(instr)
            }
            LLVMStore -> {
                return handleStore(instr)
            }
            LLVMExtractValue, LLVMGetElementPtr -> {
                return declarationOrNot(lang.expressionHandler.handleGetElementPtr(instr), instr)
            }
            LLVMICmp -> {
                return handleIntegerComparison(instr)
            }
            LLVMFCmp -> {
                return handleFloatComparison(instr)
            }
            LLVMPHI -> {
                lang.phiList.add(instr)
                return newEmptyStatement(lang.getCodeFromRawNode(instr))
            }
            LLVMSelect -> {
                return declarationOrNot(lang.expressionHandler.handleSelect(instr), instr)
            }
            LLVMUserOp1, LLVMUserOp2 -> {
                log.info(
                    "userop instruction is not a real instruction. Replacing it with empty statement"
                )
                return newEmptyStatement(lang.getCodeFromRawNode(instr))
            }
            LLVMVAArg -> {
                return handleVaArg(instr)
            }
            LLVMExtractElement -> {
                return handleExtractelement(instr)
            }
            LLVMInsertElement -> {
                return handleInsertelement(instr)
            }
            LLVMShuffleVector -> {
                return handleShufflevector(instr)
            }
            LLVMInsertValue -> {
                return handleInsertValue(instr)
            }
            LLVMFreeze -> {
                return handleFreeze(instr)
            }
            LLVMFence -> {
                return handleFence(instr)
            }
            LLVMAtomicCmpXchg -> {
                return handleAtomiccmpxchg(instr)
            }
            LLVMAtomicRMW -> {
                return handleAtomicrmw(instr)
            }
            LLVMResume -> {
                // Resumes propagation of an existing (in-flight) exception whose unwinding was
                // interrupted with a landingpad instruction.
                return newUnaryOperator("throw", false, true, lang.getCodeFromRawNode(instr))
            }
            LLVMLandingPad -> {
                return handleLandingpad(instr)
            }
            LLVMCleanupRet -> {
                // End of the cleanup basic block(s)
                // Jump to a label where handling the exception will unwind to next (e.g. a
                // catchswitch statement)
                return handleCatchret(instr)
            }
            LLVMCatchRet -> {
                // Catch (caught by catchpad instruction) is over.
                // Jumps to a label where the "normal" function logic continues
                return handleCatchret(instr)
            }
            LLVMCatchPad -> {
                // Actually handles the exception.
                return handleCatchpad(instr)
            }
            LLVMCleanupPad -> {
                // Beginning of the cleanup basic block(s).
                // We should model this as the beginning of a catch block
                return handleCleanuppad(instr)
            }
            LLVMCatchSwitch -> {
                // Marks the beginning of a "real" catch block
                // Jumps to one of the handlers specified or to the default handler (if specified)
                return handleCatchswitch(instr)
            }
        }

        log.error("Not handling instruction opcode {} yet", opcode)
        return NodeBuilder.newProblemExpression(
            "Not handling instruction opcode ${opcode} yet",
            ProblemNode.ProblemType.TRANSLATION,
            lang.getCodeFromRawNode(instr)
        )
    }

    /**
     * Handles the [`catchret`](https://llvm.org/docs/LangRef.html#catchret-instruction) instruction
     * and the [`cleanupret`](https://llvm.org/docs/LangRef.html#cleanupret-instruction). These
     * instructions are used to end a catch block or cleanuppad and transfers control either to a
     * specified BB or to the caller as unwind location. We model them as an empty statement or as a
     * jump and set the name of the statement to "catchret" or "cleanupret".
     */
    private fun handleCatchret(instr: LLVMValueRef): Statement {
        val unwindDest =
            if (instr.opCode == LLVMCatchRet) {
                LLVMGetOperand(instr, 1)
            } else if (LLVMGetUnwindDest(instr) != null) {
                LLVMBasicBlockAsValue(LLVMGetUnwindDest(instr))
            } else {
                null
            }
        if (unwindDest != null) { // For "unwind to caller", the destination is null
            val unwindDestination = extractBasicBlockLabel(unwindDest)
            val gotoStatement = newGotoStatement(lang.getCodeFromRawNode(instr))
            gotoStatement.targetLabel = unwindDestination
            gotoStatement.labelName = unwindDestination.name
            gotoStatement.name =
                if (instr.opCode == LLVMCatchRet) {
                    "catchret"
                } else {
                    "cleanuppad"
                }
            return gotoStatement
        } else {
            val emptyStatement = newEmptyStatement(lang.getCodeFromRawNode(instr))
            emptyStatement.name =
                if (instr.opCode == LLVMCatchRet) {
                    "catchret"
                } else {
                    "cleanuppad"
                }
            return emptyStatement
        }
    }

    /**
     * We simulate a [`catchswitch`](https://llvm.org/docs/LangRef.html#catchswitch-instruction)
     * instruction with a call to a dummy function "llvm.catchswitch" which generates the parent
     * token and nested if statements. The function iterates over the possible handlers and parses
     * their first instruction (a `catchpad` instruction) to assemble another call to an implicit
     * function "llvm.matchesCatchpad". This function checks if the object thrown matches the
     * arguments of the catchpad and thus the respective handler and serves as the condition of the
     * if statements.
     */
    @FunctionReplacement(["llvm.catchswitch", "llvm.matchesCatchpad"], "catchswitch")
    private fun handleCatchswitch(instr: LLVMValueRef): Statement {
        val numOps = LLVMGetNumOperands(instr)
        val nodeCode = lang.getCodeFromRawNode(instr)

        val parent = lang.getOperandValueAtIndex(instr, 0)

        val compoundStatement = newCompoundStatement(nodeCode)

        val dummyCall =
            newCallExpression(
                "llvm.catchswitch",
                "llvm.catchswitch",
                lang.getCodeFromRawNode(instr),
                false
            )
        dummyCall.addArgument(parent, "parent")

        val tokenGeneration = declarationOrNot(dummyCall, instr) as DeclarationStatement
        compoundStatement.addStatement(tokenGeneration)

        val ifStatement = newIfStatement(nodeCode)
        var currentIfStatement: IfStatement? = null
        var idx = 1
        while (idx < numOps) {
            if (currentIfStatement == null) {
                currentIfStatement = ifStatement
            } else {
                val newIf = newIfStatement(nodeCode)
                currentIfStatement.elseStatement = newIf
                currentIfStatement = newIf
            }

            // For each of the handlers, we get the first instruction and insert a statement
            // case llvm.matchesCatchpad(parent, args), where args are used to determine if
            // this handler accepts the object thrown.
            val bbTarget = LLVMGetOperand(instr, idx)

            val catchpad = LLVMGetFirstInstruction(LLVMValueAsBasicBlock(bbTarget))
            val catchOps = LLVMGetNumArgOperands(catchpad)

            val matchesCatchpad =
                newCallExpression(
                    "llvm.matchesCatchpad",
                    "llvm.matchesCatchpad",
                    lang.getCodeFromRawNode(instr),
                    false
                )

            val parentCatchSwitch = LLVMGetParentCatchSwitch(catchpad)
            val catchswitch = lang.expressionHandler.handle(parentCatchSwitch) as Expression
            matchesCatchpad.addArgument(catchswitch, "parentCatchswitch")

            for (i in 0 until catchOps) {
                val arg = lang.getOperandValueAtIndex(catchpad, i)
                matchesCatchpad.addArgument(arg, "args_$i")
            }

            currentIfStatement.condition = matchesCatchpad

            // Get the label of the goto statement.
            val caseLabelStatement = extractBasicBlockLabel(bbTarget)
            val gotoStatement = newGotoStatement(nodeCode)
            gotoStatement.targetLabel = caseLabelStatement
            gotoStatement.labelName = caseLabelStatement.name
            currentIfStatement.thenStatement = gotoStatement

            idx++
        }

        val unwindDest = LLVMGetUnwindDest(instr)
        if (unwindDest != null) { // For "unwind to caller", the destination is null
            val unwindDestination = extractBasicBlockLabel(LLVMBasicBlockAsValue(unwindDest))
            val gotoStatement = newGotoStatement(nodeCode)
            gotoStatement.targetLabel = unwindDestination
            gotoStatement.labelName = unwindDestination.name
            if (currentIfStatement == null) {
                currentIfStatement = ifStatement
            }
            currentIfStatement.elseStatement = gotoStatement
        } else {
            // "unwind to caller". As we don't know where the control flow continues,
            // the best model would be to throw the exception again. Here, we only know
            // that we will throw something here but we don't know what. We have to fix
            // that later once we know in which catch-block this statement is executed.
            val throwOperation = newUnaryOperator("throw", false, true, nodeCode)
            currentIfStatement!!.elseStatement = throwOperation
        }

        compoundStatement.addStatement(ifStatement)
        return compoundStatement
    }

    /**
     * We simulate a [`cleanuppad`](https://llvm.org/docs/LangRef.html#cleanuppad-instruction)
     * instruction with a call to the dummy function "llvm.cleanuppad". The function receives the
     * parent and the args as arguments.
     */
    @FunctionReplacement(["llvm.cleanuppad"], "cleanuppad")
    private fun handleCleanuppad(instr: LLVMValueRef): Statement {
        val numOps = LLVMGetNumArgOperands(instr)
        val catchswitch = lang.getOperandValueAtIndex(instr, 0)

        val dummyCall =
            newCallExpression(
                "llvm.cleanuppad",
                "llvm.cleanuppad",
                lang.getCodeFromRawNode(instr),
                false
            )
        dummyCall.addArgument(catchswitch, "parentCatchswitch")

        for (i in 1 until numOps) {
            val arg = lang.getOperandValueAtIndex(instr, i)
            dummyCall.addArgument(arg, "args_${i-1}")
        }
        return declarationOrNot(dummyCall, instr)
    }

    /**
     * We simulate a [`catchpad`](https://llvm.org/docs/LangRef.html#catchpad-instruction)
     * instruction with a call to the dummy function "llvm.catchpad". The function receives the
     * catchswitch and the args as arguments.
     */
    @FunctionReplacement(["llvm.catchpad"], "catchpad")
    private fun handleCatchpad(instr: LLVMValueRef): Statement {
        val numOps = LLVMGetNumArgOperands(instr)
        val parentCatchSwitch = LLVMGetParentCatchSwitch(instr)
        val catchswitch = lang.expressionHandler.handle(parentCatchSwitch) as Expression

        val dummyCall =
            newCallExpression(
                "llvm.catchpad",
                "llvm.catchpad",
                lang.getCodeFromRawNode(instr),
                false
            )
        dummyCall.addArgument(catchswitch, "parentCatchswitch")

        for (i in 0 until numOps) {
            val arg = lang.getOperandValueAtIndex(instr, i)
            dummyCall.addArgument(arg, "args_$i")
        }
        return declarationOrNot(dummyCall, instr)
    }

    /**
     * Handles the [`va_arg`](https://llvm.org/docs/LangRef.html#va-arg-instruction) instruction. It
     * is simulated by a call to a function called `va_arg` simulating the respective C++-macro. The
     * function takes two arguments: the vararg-list and the type of the return value.
     */
    @FunctionReplacement(["llvm.va_arg"], "va_arg")
    private fun handleVaArg(instr: LLVMValueRef): Statement {
        val callExpr =
            newCallExpression("llvm.va_arg", "llvm.va_arg", lang.getCodeFromRawNode(instr), false)
        val operandName = lang.getOperandValueAtIndex(instr, 0)
        callExpr.addArgument(operandName)
        val expectedType = lang.typeOf(instr)
        val typeLiteral = newLiteral(expectedType, expectedType, lang.getCodeFromRawNode(instr))
        callExpr.addArgument(typeLiteral) // TODO: Is this correct??
        return declarationOrNot(callExpr, instr)
    }

    /** Handles all kinds of instructions which are an arithmetic or logical binary instruction. */
    private fun handleBinaryInstruction(instr: LLVMValueRef): Statement {
        when (instr.opCode) {
            LLVMAdd, LLVMFAdd -> {
                return handleBinaryOperator(instr, "+", false)
            }
            LLVMSub, LLVMFSub -> {
                return handleBinaryOperator(instr, "-", false)
            }
            LLVMMul, LLVMFMul -> {
                return handleBinaryOperator(instr, "*", false)
            }
            LLVMUDiv -> {
                return handleBinaryOperator(instr, "/", true)
            }
            LLVMSDiv, LLVMFDiv -> {
                return handleBinaryOperator(instr, "/", false)
            }
            LLVMURem -> {
                return handleBinaryOperator(instr, "%", true)
            }
            LLVMSRem, LLVMFRem -> {
                return handleBinaryOperator(instr, "%", false)
            }
            LLVMShl -> {
                return handleBinaryOperator(instr, "<<", false)
            }
            LLVMLShr -> {
                return handleBinaryOperator(instr, ">>", true)
            }
            LLVMAShr -> {
                return handleBinaryOperator(instr, ">>", false)
            }
            LLVMAnd -> {
                return handleBinaryOperator(instr, "&", false)
            }
            LLVMOr -> {
                return handleBinaryOperator(instr, "|", false)
            }
            LLVMXor -> {
                return handleBinaryOperator(instr, "^", false)
            }
        }
        return NodeBuilder.newProblemExpression(
            "Not opcode found for binary operator",
            ProblemNode.ProblemType.TRANSLATION,
            lang.getCodeFromRawNode(instr)
        )
    }

    /**
     * Handles the ['alloca'](https://llvm.org/docs/LangRef.html#alloca-instruction) instruction,
     * which allocates a defined block of memory. The closest what we have in the graph is the
     * [ArrayCreationExpression], which creates a fixed sized array, i.e., a block of memory.
     */
    private fun handleAlloca(instr: LLVMValueRef): Statement {
        val array = newArrayCreationExpression(lang.getCodeFromRawNode(instr))

        array.updateType(lang.typeOf(instr))

        // LLVM is quite forthcoming here. in case the optional length parameter is omitted in the
        // source code, it will automatically be set to 1
        val size = lang.getOperandValueAtIndex(instr, 0)

        array.addDimension(size)

        return declarationOrNot(array, instr)
    }

    /**
     * Handles the [`store`](https://llvm.org/docs/LangRef.html#store-instruction) instruction. It
     * stores a particular value at a pointer address. This is the rough equivalent to an assignment
     * of a de-referenced pointer in C like `*a = 1`.
     */
    private fun handleStore(instr: LLVMValueRef): Statement {
        val binOp = newBinaryOperator("=", lang.getCodeFromRawNode(instr))

        val dereference = newUnaryOperator("*", false, true, "")
        dereference.input = lang.getOperandValueAtIndex(instr, 1)

        binOp.lhs = dereference
        binOp.rhs = lang.getOperandValueAtIndex(instr, 0)

        return binOp
    }

    /**
     * Handles the [`load`](https://llvm.org/docs/LangRef.html#load-instruction) instruction, which
     * is basically just a pointer de-reference.
     */
    private fun handleLoad(instr: LLVMValueRef): Statement {
        val ref = newUnaryOperator("*", false, true, "")
        ref.input = lang.getOperandValueAtIndex(instr, 0)

        return declarationOrNot(ref, instr)
    }

    /**
     * Handles the [`icmp`](https://llvm.org/docs/LangRef.html#icmp-instruction) instruction for
     * comparing integer values.
     */
    fun handleIntegerComparison(instr: LLVMValueRef): Statement {
        var unsigned = false
        val cmpPred =
            when (LLVMGetICmpPredicate(instr)) {
                LLVMIntEQ -> "=="
                LLVMIntNE -> "!="
                LLVMIntUGT -> {
                    unsigned = true
                    ">"
                }
                LLVMIntUGE -> {
                    unsigned = true
                    ">="
                }
                LLVMIntULT -> {
                    unsigned = true
                    "<"
                }
                LLVMIntULE -> {
                    unsigned = true
                    "<="
                }
                LLVMIntSGT -> ">"
                LLVMIntSGE -> ">="
                LLVMIntSLT -> "<"
                LLVMIntSLE -> "<="
                else -> "unknown"
            }

        return handleBinaryOperator(instr, cmpPred, unsigned)
    }

    /**
     * Handles the [`fcmp`](https://llvm.org/docs/LangRef.html#fcmp-instruction) instruction for
     * comparing floating point values.
     */
    private fun handleFloatComparison(instr: LLVMValueRef): Statement {
        var unordered = false
        val cmpPred =
            when (LLVMGetFCmpPredicate(instr)) {
                LLVMRealPredicateFalse -> {
                    return newLiteral(false, TypeParser.createFrom("i1", true), "false")
                }
                LLVMRealOEQ -> "=="
                LLVMRealOGT -> ">"
                LLVMRealOGE -> ">="
                LLVMRealOLT -> "<"
                LLVMRealOLE -> "<="
                LLVMRealONE -> "!="
                LLVMRealORD -> "ord"
                LLVMRealUNO -> "uno"
                LLVMRealUEQ -> {
                    unordered = true
                    "=="
                }
                LLVMRealUGT -> {
                    unordered = true
                    ">"
                }
                LLVMRealUGE -> {
                    unordered = true
                    ">="
                }
                LLVMRealULT -> {
                    unordered = true
                    "<"
                }
                LLVMRealULE -> {
                    unordered = true
                    "<="
                }
                LLVMRealUNE -> {
                    unordered = true
                    "!="
                }
                LLVMRealPredicateTrue -> {
                    return newLiteral(true, TypeParser.createFrom("i1", true), "true")
                }
                else -> "unknown"
            }

        return handleBinaryOperator(instr, cmpPred, false, unordered)
    }

    /**
     * Handles the [`insertvalue`](https://llvm.org/docs/LangRef.html#insertvalue-instruction)
     * instruction.
     *
     * We use it similar to a constructor and assign the individual sub-elements.
     */
    private fun handleInsertValue(instr: LLVMValueRef): Statement {
        val numOps = LLVMGetNumIndices(instr)
        val indices = LLVMGetIndices(instr)

        var baseType = lang.typeOf(LLVMGetOperand(instr, 0))
        val operand = lang.getOperandValueAtIndex(instr, 0)
        val valueToSet = lang.getOperandValueAtIndex(instr, 1)

        var base = operand

        // Make a copy of the operand
        var copy: Statement =
            NodeBuilder.newProblemExpression(
                "Default statement for insertvalue",
                ProblemNode.ProblemType.TRANSLATION,
                lang.getCodeFromRawNode(instr)
            )
        if (operand !is ConstructExpression) {
            copy = declarationOrNot(operand, instr)
            if (copy is DeclarationStatement) {
                base =
                    newDeclaredReferenceExpression(
                        copy.singleDeclaration.name,
                        (copy.singleDeclaration as VariableDeclaration).type,
                        lang.getCodeFromRawNode(instr)
                    )
            }
        }
        var expr: Expression

        for (idx: Int in 0 until numOps) {
            val index = indices.get(idx.toLong())

            if (base is ConstructExpression) {
                if (idx == numOps - 1) {
                    base.setArgument(index, valueToSet)
                    return declarationOrNot(operand, instr)
                }
                base = base.arguments[index]
            } else if (baseType is PointerType) {
                val arrayExpr = newArraySubscriptionExpression("")
                arrayExpr.arrayExpression = base
                arrayExpr.name = index.toString()
                arrayExpr.subscriptExpression = operand
                expr = arrayExpr

                // deference the type to get the new base type
                baseType = baseType.dereference()

                // the current expression is the new base
                base = expr
            } else {
                // otherwise, this is a member field access, where the index denotes the n-th field
                // in the structure
                val record = (baseType as? ObjectType)?.recordDeclaration

                // this should not happen at this point, we cannot continue
                if (record == null) {
                    log.error(
                        "Could not find structure type with name {}, cannot continue",
                        baseType.typeName
                    )
                    break
                }

                log.debug(
                    "Trying to access a field within the record declaration of {}",
                    record.name
                )

                // look for the field
                val field = record.getField("field_$index")

                // our new base-type is the type of the field
                baseType = field?.type ?: UnknownType.getUnknownType()

                // construct our member expression
                expr = newMemberExpression(base, field?.type, field?.name, ".", "")
                log.info("{}", expr)

                // the current expression is the new base
                base = expr
            }
        }

        val compoundStatement = newCompoundStatement(lang.getCodeFromRawNode(instr))

        val assignment = newBinaryOperator("=", lang.getCodeFromRawNode(instr))
        assignment.lhs = base
        assignment.rhs = valueToSet
        compoundStatement.addStatement(copy)
        compoundStatement.addStatement(assignment)

        return compoundStatement
    }

    /**
     * Handles the [`freeze`](https://llvm.org/docs/LangRef.html#freeze-instruction) instruction.
     * This instruction checks if the operand is neither an undef nor a poison value (for aggregated
     * types such as vectors, individual elements are checked) and, if so, returns the operand.
     * Otherwise, it returns a random but non-undef and non-poison value. This initialization is
     * modeled in the graph by a call to an implicit function "llvm.freeze" which would be adapted
     * to each data type.
     */
    @FunctionReplacement(["llvm.freeze"], "freeze")
    private fun handleFreeze(instr: LLVMValueRef): Statement {
        val operand = lang.getOperandValueAtIndex(instr, 0)
        val instrCode = lang.getCodeFromRawNode(instr)

        // condition: arg != undef && arg != poison
        val condition = newBinaryOperator("&&", instrCode)
        val undefCheck = newBinaryOperator("!=", instrCode)
        undefCheck.lhs = operand
        undefCheck.rhs = newLiteral(null, operand.type, instrCode)
        condition.lhs = undefCheck
        val poisonCheck = newBinaryOperator("!=", instrCode)
        poisonCheck.lhs = operand
        poisonCheck.rhs =
            newLiteral(
                "POISON",
                operand.type,
                instrCode
            ) // This could be e.g. NAN. Not sure for complex types
        condition.rhs = poisonCheck

        // Call to a dummy function "llvm.freeze" which would fill the undef or poison values
        // randomly.
        // The implementation of this function would depend on the data type (e.g. for integers, it
        // could be rand())
        val callExpression = newCallExpression("llvm.freeze", "llvm.freeze", instrCode, false)
        callExpression.addArgument(operand)

        // res = (arg != undef && arg != poison) ? arg : llvm.freeze(in)
        val conditional = newConditionalExpression(condition, operand, callExpression, operand.type)
        return declarationOrNot(conditional, instr)
    }

    /**
     * Handles the [`freeze`](https://llvm.org/docs/LangRef.html#fence-instruction) instruction.
     * This instruction is used to guarantee the atomicity of load and store operations. The
     * subsequent load or store are affected by the instruction.
     *
     * In the graph, this is modeled with a call to an implicit "llvm.fence" method which accepts
     * the ordering and an optional syncscope as argument.
     */
    @FunctionReplacement(["llvm.fence"], "fence")
    private fun handleFence(instr: LLVMValueRef): Statement {
        val instrString = lang.getCodeFromRawNode(instr)
        val callExpression = newCallExpression("llvm.fence", "llvm.fence", instrString, false)
        val ordering =
            newLiteral(
                LLVMGetOrdering(instr),
                TypeParser.createFrom("i32", true),
                lang.getCodeFromRawNode(instr)
            )
        callExpression.addArgument(ordering, "ordering")
        if (instrString?.contains("syncscope") == true) {
            val syncscope = instrString.split("\"")[1]
            callExpression.addArgument(
                newLiteral(syncscope, TypeParser.createFrom("String", true), instrString),
                "syncscope"
            )
        }

        return callExpression
    }

    /**
     * Parses the [`cmpxchg`](https://llvm.org/docs/LangRef.html#cmpxchg-instruction) instruction.
     * It returns a single [Statement] or a [CompoundStatement] if the value is assigned to another
     * variable. Performs the following operation atomically:
     * ```
     * lhs = {*pointer, *pointer == cmp} // A struct of {T, i1}
     * if(*pointer == cmp) { *pointer = new }
     * ```
     * Returns a [CompoundStatement] with those two instructions or, if `lhs` doesn't exist, only
     * the if-then statement.
     */
    private fun handleAtomiccmpxchg(instr: LLVMValueRef): Statement {
        val instrStr = lang.getCodeFromRawNode(instr)
        val compoundStatement = newCompoundStatement(instrStr)
        compoundStatement.name = "atomiccmpxchg"
        val ptr = lang.getOperandValueAtIndex(instr, 0)
        val cmp = lang.getOperandValueAtIndex(instr, 1)
        val value = lang.getOperandValueAtIndex(instr, 2)

        val ptrDeref = newUnaryOperator("*", false, true, instrStr)
        ptrDeref.input = ptr

        val cmpExpr = newBinaryOperator("==", instrStr)
        cmpExpr.lhs = ptrDeref
        cmpExpr.rhs = cmp

        val lhs = LLVMGetValueName(instr).string
        if (lhs != "") {
            // we need to create a crazy struct here. the target type can be found here
            val targetType = lang.typeOf(instr)

            // construct it
            val construct = newConstructExpression("")
            construct.instantiates = (targetType as? ObjectType)?.recordDeclaration

            construct.addArgument(ptrDeref)
            construct.addArgument(cmpExpr)

            val decl = declarationOrNot(construct, instr)
            compoundStatement.addStatement(decl)
        }
        val assignment = newBinaryOperator("=", instrStr)
        assignment.lhs = ptrDeref
        assignment.rhs = value

        val ifStatement = newIfStatement(instrStr)
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
        val ptr = lang.getOperandValueAtIndex(instr, 0)
        val value = lang.getOperandValueAtIndex(instr, 1)
        val ty = value.type
        val exchOp = newBinaryOperator("=", instrStr)
        exchOp.name = "atomicrmw"

        val ptrDeref = newUnaryOperator("*", false, true, instrStr)
        ptrDeref.input = ptr
        exchOp.lhs = ptrDeref

        when (operation) {
            LLVMAtomicRMWBinOpXchg -> {
                exchOp.rhs = value
            }
            LLVMAtomicRMWBinOpFAdd, LLVMAtomicRMWBinOpAdd -> {
                val binaryOperator = newBinaryOperator("+", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpFSub, LLVMAtomicRMWBinOpSub -> {
                val binaryOperator = newBinaryOperator("-", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpAnd -> {
                val binaryOperator = newBinaryOperator("&", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpNand -> {
                val binaryOperator = newBinaryOperator("|", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                val unaryOperator = newUnaryOperator("~", false, true, instrStr)
                unaryOperator.input = binaryOperator
                exchOp.rhs = unaryOperator
            }
            LLVMAtomicRMWBinOpOr -> {
                val binaryOperator = newBinaryOperator("|", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpXor -> {
                val binaryOperator = newBinaryOperator("^", instrStr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = binaryOperator
            }
            LLVMAtomicRMWBinOpMax, LLVMAtomicRMWBinOpMin -> {
                val operatorCode =
                    if (operation == LLVMAtomicRMWBinOpMin) {
                        "<"
                    } else {
                        ">"
                    }
                val condition = newBinaryOperator(operatorCode, instrStr)
                condition.lhs = ptrDeref
                condition.rhs = value
                val conditional = newConditionalExpression(condition, ptrDeref, value, ty)
                exchOp.rhs = conditional
            }
            LLVMAtomicRMWBinOpUMax, LLVMAtomicRMWBinOpUMin -> {
                val operatorCode =
                    if (operation == LLVMAtomicRMWBinOpUMin) {
                        "<"
                    } else {
                        ">"
                    }
                val condition = newBinaryOperator(operatorCode, instrStr)
                val castExprLhs = newCastExpression(lang.getCodeFromRawNode(instr))
                castExprLhs.castType = TypeParser.createFrom("u${ty.name}", true)
                castExprLhs.expression = ptrDeref
                condition.lhs = castExprLhs

                val castExprRhs = newCastExpression(lang.getCodeFromRawNode(instr))
                castExprRhs.castType = TypeParser.createFrom("u${ty.name}", true)
                castExprRhs.expression = value
                condition.rhs = castExprRhs
                val conditional = newConditionalExpression(condition, ptrDeref, value, ty)
                exchOp.rhs = conditional
            }
            else -> {
                throw TranslationException("LLVMAtomicRMWBinOp $operation not supported")
            }
        }

        return if (lhs != "") {
            // set lhs = *ptr, then perform the replacement
            val compoundStatement = newCompoundStatement(instrStr)
            compoundStatement.statements = listOf(declarationOrNot(ptrDeref, instr), exchOp)
            compoundStatement
        } else {
            // only perform the replacement
            exchOp
        }
    }

    /**
     * Handles the [`indirectbr`](https://llvm.org/docs/LangRef.html#indirectbr-instruction)
     * instruction.
     */
    private fun handleIndirectbrStatement(instr: LLVMValueRef): Statement {
        val numOps = LLVMGetNumOperands(instr)
        val nodeCode = lang.getCodeFromRawNode(instr)
        if (numOps < 2)
            throw TranslationException(
                "Indirectbr statement without address and at least one target"
            )

        val address = lang.getOperandValueAtIndex(instr, 0)

        val switchStatement = newSwitchStatement(nodeCode)
        switchStatement.selector = address

        val caseStatements = newCompoundStatement(nodeCode)

        var idx = 1
        while (idx < numOps) {
            // The case statement is derived from the address of the label which we can jump to
            val caseBBAddress = LLVMValueAsBasicBlock(LLVMGetOperand(instr, idx)).address()
            val caseStatement = newCaseStatement(nodeCode)
            caseStatement.caseExpression =
                newLiteral(caseBBAddress, TypeParser.createFrom("i64", true), nodeCode)
            caseStatements.addStatement(caseStatement)

            // Get the label of the goto statement.
            val caseLabelStatement = extractBasicBlockLabel(LLVMGetOperand(instr, idx))
            val gotoStatement = newGotoStatement(nodeCode)
            gotoStatement.targetLabel = caseLabelStatement
            gotoStatement.labelName = caseLabelStatement.name
            caseStatements.addStatement(gotoStatement)
            idx++
        }

        switchStatement.statement = caseStatements

        return switchStatement
    }

    /** Handles a [`br`](https://llvm.org/docs/LangRef.html#br-instruction) instruction. */
    private fun handleBrStatement(instr: LLVMValueRef): Statement {
        if (LLVMGetNumOperands(instr) == 3) {
            // if(op) then {goto label1} else {goto label2}
            val ifStatement = newIfStatement(lang.getCodeFromRawNode(instr))
            val condition = lang.getOperandValueAtIndex(instr, 0)
            ifStatement.condition = condition

            // Get the label of the "else" branch
            val elseGoto = newGotoStatement(lang.getCodeFromRawNode(instr))
            val elseLabel = extractBasicBlockLabel(LLVMGetOperand(instr, 1))
            elseGoto.targetLabel = elseLabel
            elseGoto.labelName = elseLabel.name
            ifStatement.elseStatement = elseGoto

            // Get the label of the "if" branch
            val ifGoto = newGotoStatement(lang.getCodeFromRawNode(instr))
            val thenLabelStatement = extractBasicBlockLabel(LLVMGetOperand(instr, 2))
            ifGoto.targetLabel = thenLabelStatement
            ifGoto.labelName = thenLabelStatement.name
            ifStatement.thenStatement = ifGoto

            return ifStatement
        } else if (LLVMGetNumOperands(instr) == 1) {
            // goto defaultLocation
            val gotoStatement = newGotoStatement(lang.getCodeFromRawNode(instr))
            val labelStatement = extractBasicBlockLabel(LLVMGetOperand(instr, 0))
            gotoStatement.labelName = labelStatement.name
            gotoStatement.targetLabel = labelStatement

            return gotoStatement
        } else {
            throw TranslationException("Wrong number of operands in br statement")
        }
    }

    /**
     * Handles a [`switch`](https://llvm.org/docs/LangRef.html#switch-instruction) instruction.
     * Throws a [TranslationException] if there are less than 2 operands specified (the first one is
     * used for the comparison of the "case" statements, the second one is the default location) or
     * if the number of operands is not even.
     *
     * Returns a [SwitchStatement].
     */
    private fun handleSwitchStatement(instr: LLVMValueRef): Statement {
        val numOps = LLVMGetNumOperands(instr)
        val nodeCode = lang.getCodeFromRawNode(instr)
        if (numOps < 2 || numOps % 2 != 0)
            throw TranslationException("Switch statement without operand and default branch")

        val operand = lang.getOperandValueAtIndex(instr, 0)

        val switchStatement = newSwitchStatement(nodeCode)
        switchStatement.selector = operand

        val caseStatements = newCompoundStatement(nodeCode)

        var idx = 2
        while (idx < numOps) {
            // Get the comparison value and add it to the CaseStatement
            val caseStatement = newCaseStatement(nodeCode)
            caseStatement.caseExpression = lang.getOperandValueAtIndex(instr, idx)
            caseStatements.addStatement(caseStatement)
            idx++
            // Get the "case" statements and add it to the CaseStatement
            val caseLabelStatement = extractBasicBlockLabel(LLVMGetOperand(instr, idx))
            val gotoStatement = newGotoStatement(nodeCode)
            gotoStatement.targetLabel = caseLabelStatement
            gotoStatement.labelName = caseLabelStatement.name
            caseStatements.addStatement(gotoStatement)
            idx++
        }

        // Get the label of the "default" branch
        caseStatements.addStatement(newDefaultStatement(nodeCode))
        val defaultLabel = extractBasicBlockLabel(LLVMGetOperand(instr, 1))
        val defaultGoto = newGotoStatement(nodeCode)
        defaultGoto.targetLabel = defaultLabel
        defaultGoto.labelName = defaultLabel.name
        caseStatements.addStatement(defaultGoto)

        switchStatement.statement = caseStatements

        return switchStatement
    }

    /**
     * Handles different types of function calls, including the
     * [`call`](https://llvm.org/docs/LangRef.html#call-instruction) and the
     * [`invoke`](https://llvm.org/docs/LangRef.html#invoke-instruction) instruction.
     *
     * Returns either a [DeclarationStatement] or a [CallExpression].
     */
    private fun handleFunctionCall(instr: LLVMValueRef): Statement {
        val instrStr = lang.getCodeFromRawNode(instr)
        val calledFunc = LLVMGetCalledValue(instr)
        var calledFuncName = LLVMGetValueName(calledFunc).string
        var max = LLVMGetNumOperands(instr) - 1
        var idx = 0

        if (calledFuncName.equals("")) {
            // Function is probably called by a local variable. For some reason, this is the last
            // operand
            val opName = lang.getOperandValueAtIndex(instr, max)
            calledFuncName = opName.name
        }

        var catchLabel = LabelStatement()
        var continueLabel = LabelStatement()
        if (instr.opCode == LLVMInvoke) {
            max-- // Last one is the Decl.Expr of the function
            // Get the label of the catch clause.
            catchLabel = extractBasicBlockLabel(LLVMGetOperand(instr, max))
            max--
            // Get the label of the basic block where the control flow continues (e.g. if no error
            // occurs).
            continueLabel = extractBasicBlockLabel(LLVMGetOperand(instr, max))
            max--
            log.info(
                "Invoke expression: Usually continues at ${continueLabel.name}, exception continues at ${catchLabel.name}"
            )
        }

        val callExpr = newCallExpression(calledFuncName, calledFuncName, instrStr, false)

        while (idx < max) {
            val operandName = lang.getOperandValueAtIndex(instr, idx)
            callExpr.addArgument(operandName)
            idx++
        }

        if (instr.opCode == LLVMInvoke) {
            // For the "invoke" instruction, the call is surrounded by a try statement which also
            // contains a goto statement after the call.
            val tryStatement = newTryStatement(instrStr!!)
            lang.scopeManager.enterScope(tryStatement)
            val tryBlock = newCompoundStatement(instrStr)
            tryBlock.addStatement(declarationOrNot(callExpr, instr))
            val tryContinue = newGotoStatement(instrStr)
            tryContinue.targetLabel = continueLabel
            tryBlock.addStatement(tryContinue)
            tryStatement.tryBlock = tryBlock
            lang.scopeManager.leaveScope(tryStatement)

            val catchClause = newCatchClause(instrStr)
            catchClause.name = catchLabel.name
            val gotoCatch = newGotoStatement(instrStr)
            gotoCatch.targetLabel = catchLabel
            catchClause.setParameter(
                newVariableDeclaration(
                    "e_${catchLabel.name}",
                    UnknownType.getUnknownType(),
                    instrStr,
                    true
                )
            )
            val catchCompoundStatement = newCompoundStatement(instrStr)
            catchCompoundStatement.addStatement(gotoCatch)
            catchClause.body = catchCompoundStatement
            tryStatement.catchClauses = mutableListOf(catchClause)

            return tryStatement
        }

        return declarationOrNot(callExpr, instr)
    }

    /**
     * Handles a [`landingpad`](https://llvm.org/docs/LangRef.html#landingpad-instruction) by
     * replacing it with a catch instruction containing all possible catchable types. Later, the
     * [CompressLLVMPass] will move this instruction to the correct location
     */
    private fun handleLandingpad(instr: LLVMValueRef): Statement {
        val catchInstr = newCatchClause(lang.getCodeFromRawNode(instr)!!)
        /* Get the number of clauses on the landingpad instruction and iterate through the clauses to get all types for the catch clauses */
        val numClauses = LLVMGetNumClauses(instr)
        var catchType = ""
        for (i in 0 until numClauses) {
            val clause = LLVMGetClause(instr, i)
            if (LLVMIsAConstantArray(clause) == null) {
                if (LLVMIsNull(clause) == 1) {
                    catchType += "..." + " | "
                } else {
                    catchType += LLVMGetValueName(clause).string + " | "
                }
            } else {
                // TODO: filter not handled yet
            }
        }
        if (catchType.endsWith(" | ")) catchType = catchType.substring(0, catchType.length - 3)

        val lhs = lang.getNameOf(instr).first

        val exceptionName =
            if (lhs != "") {
                lhs
            } else {
                "e_${instr.address()}"
            }
        val except =
            newVariableDeclaration(
                exceptionName,
                TypeParser.createFrom(
                    catchType,
                    false
                ), // TODO: This doesn't work for multiple types to catch
                lang.getCodeFromRawNode(instr),
                false
            )
        lang.bindingsCache["%${exceptionName}"] = except
        catchInstr.setParameter(except)
        catchInstr.name = catchType
        return catchInstr
    }

    /**
     * Handles the [`insertelement`](https://llvm.org/docs/LangRef.html#insertelement-instruction)
     * instruction which is modeled as access to an array at a given index. A new array with the
     * modified value is constructed.
     */
    private fun handleInsertelement(instr: LLVMValueRef): Statement {
        val instrStr = lang.getCodeFromRawNode(instr)
        val compoundStatement = newCompoundStatement(instrStr)

        // TODO: Probably we should make a proper copy of the array
        val newArrayDecl = declarationOrNot(lang.getOperandValueAtIndex(instr, 0), instr)
        compoundStatement.addStatement(newArrayDecl)

        val decl = newArrayDecl.declarations[0] as? VariableDeclaration
        val arrayExpr = newArraySubscriptionExpression(instrStr)
        arrayExpr.arrayExpression = newDeclaredReferenceExpression(decl?.name, decl?.type, instrStr)
        arrayExpr.subscriptExpression = lang.getOperandValueAtIndex(instr, 2)

        val binaryExpr = newBinaryOperator("=", instrStr)
        binaryExpr.lhs = arrayExpr
        binaryExpr.rhs = lang.getOperandValueAtIndex(instr, 1)
        compoundStatement.addStatement(binaryExpr)

        return compoundStatement
    }

    /**
     * Handles the [`extractelement`](https://llvm.org/docs/LangRef.html#extractelement-instruction)
     * instruction which is modeled as access to an array at a given index.
     */
    private fun handleExtractelement(instr: LLVMValueRef): Statement {
        val arrayExpr = newArraySubscriptionExpression(lang.getCodeFromRawNode(instr))
        arrayExpr.arrayExpression = lang.getOperandValueAtIndex(instr, 0)
        arrayExpr.subscriptExpression = lang.getOperandValueAtIndex(instr, 1)

        return declarationOrNot(arrayExpr, instr)
    }

    /**
     * Handles the [`shufflevector`](https://llvm.org/docs/LangRef.html#shufflevector-instruction)
     * which is used to merge two vectors (which are arrays) into one and change the order of the
     * elements.
     *
     * It does not handle scalable vectors yet (where the size is unknown) but that feature is
     * barely used and also the features of LLVM are very limited in that scenario.
     */
    private fun handleShufflevector(instr: LLVMValueRef): Statement {
        val instrStr = lang.getCodeFromRawNode(instr)

        val list = newInitializerListExpression(instrStr)
        val elementType = lang.typeOf(instr).dereference()

        val initializers = mutableListOf<Expression>()

        // Get the first vector and its length. The length is 0 if it's an undef value.
        val array1 = lang.getOperandValueAtIndex(instr, 0)
        val array1Length =
            if (array1 is Literal<*> && array1.value == null) {
                0
            } else {
                LLVMGetVectorSize(LLVMTypeOf(LLVMGetOperand(instr, 0)))
            }

        // Get the second vector and its length. The length is 0 if it's an undef value.
        val array2 = lang.getOperandValueAtIndex(instr, 1)
        val array2Length =
            if (array2 is Literal<*> && array2.value == null) {
                0
            } else {
                LLVMGetVectorSize(LLVMTypeOf(LLVMGetOperand(instr, 1)))
            }

        // Get the number of mask elements. They determine the ordering of the elements.
        val indices = LLVMGetNumMaskElements(instr)

        // Get the respective elements depending on the mask and put them into an initializer for
        // the resulting vector.
        // If a vector is an initializer itself (i.e., a constant array), we directly put the values
        // in the new initializer.
        // Otherwise, we use the array as a variable.
        for (idx in 0 until indices) {
            val idxInt = LLVMGetMaskValue(instr, idx)
            if (idxInt < array1Length) {
                if (array1 is InitializerListExpression) {
                    initializers += array1.initializers[idxInt]
                } else if (array1 is Literal<*> && array1.value == null) {
                    initializers += newLiteral(null, elementType, instrStr)
                } else {
                    val arrayExpr = newArraySubscriptionExpression(instrStr)
                    arrayExpr.arrayExpression = array1
                    arrayExpr.subscriptExpression =
                        newLiteral(idxInt, TypeParser.createFrom("i32", true), instrStr)
                    initializers += arrayExpr
                }
            } else if (idxInt < array1Length + array2Length) {
                if (array2 is InitializerListExpression) {
                    initializers += array2.initializers[idxInt - array1Length]
                } else if (array2 is Literal<*> && array2.value == null) {
                    initializers += newLiteral(null, elementType, instrStr)
                } else {
                    val arrayExpr = newArraySubscriptionExpression(instrStr)
                    arrayExpr.arrayExpression = array2
                    arrayExpr.subscriptExpression =
                        newLiteral(
                            idxInt - array1Length,
                            TypeParser.createFrom("i32", true),
                            instrStr
                        )
                    initializers += arrayExpr
                }
            } else {
                initializers += newLiteral(null, elementType, instrStr)
            }
        }

        list.initializers = initializers

        return declarationOrNot(list, instr)
    }

    /**
     * Handles the [`phi`](https://llvm.org/docs/LangRef.html#phi-instruction) instruction. It
     * therefore adds dummy statements to the end of basic blocks where a certain variable is
     * declared and initialized. The original phi instruction is not added to the CPG.
     */
    fun handlePhi(instr: LLVMValueRef, tu: TranslationUnitDeclaration) {
        val labelMap = mutableMapOf<LabelStatement, Expression>()
        val numOps = LLVMGetNumOperands(instr)
        var i = 0
        var bbsFunction: LLVMValueRef? = null
        while (i < numOps) {
            val valI = lang.getOperandValueAtIndex(instr, i)
            val incomingBB = LLVMGetIncomingBlock(instr, i)
            if (bbsFunction == null) {
                bbsFunction = LLVMGetBasicBlockParent(incomingBB)
            } else if (bbsFunction.address() != LLVMGetBasicBlockParent(incomingBB).address()) {
                log.error(
                    "The basic blocks of the phi instructions are in different functions. Can't handle this!"
                )
                throw TranslationException(
                    "The basic blocks of the phi instructions are in different functions."
                )
            }

            val labelI = extractBasicBlockLabel(LLVMBasicBlockAsValue(incomingBB))
            i++
            labelMap[labelI] = valI
        }
        if (labelMap.keys.size == 1) {
            // We only have a single pair, so we insert a declaration in that one BB.
            val key = labelMap.keys.elementAt(0)
            val basicBlock = key.subStatement as? CompoundStatement
            val decl = declarationOrNot(labelMap[key]!!, instr)
            val mutableStatements = basicBlock?.statements?.toMutableList()
            mutableStatements?.add(basicBlock.statements.size - 1, decl)
            if (mutableStatements != null) {
                basicBlock.statements = mutableStatements
            }
            return
        }
        // We have multiple pairs, so we insert a declaration at the beginning of the function and
        // make an assignment in each BB.
        val functionName = LLVMGetValueName(bbsFunction).string
        val functions =
            tu.declarations.filter { d ->
                (d as? FunctionDeclaration)?.name != null &&
                    (d as? FunctionDeclaration)?.name.equals(functionName)
            }
        if (functions.size != 1) {
            log.error(
                "${functions.size} functions match the name of the one where the phi instruction is inserted. Can't handle this case."
            )
            throw TranslationException("Wrong number of functions for phi statement.")
        }
        // Create the dummy declaration at the beginning of the function body
        val firstBB = (functions[0] as FunctionDeclaration).body as CompoundStatement
        val varName = instr.name
        val type = lang.typeOf(instr)
        val code = lang.getCodeFromRawNode(instr)
        val declaration = newVariableDeclaration(varName, type, code, false)
        declaration.updateType(type)
        // add the declaration to the current scope
        lang.scopeManager.addDeclaration(declaration)
        // add it to our bindings cache
        lang.bindingsCache[instr.symbolName] = declaration

        val declStatement = newDeclarationStatement(code)
        declStatement.singleDeclaration = declaration
        val mutableFunctionStatements = firstBB.statements.toMutableList()
        mutableFunctionStatements.add(0, declStatement)
        firstBB.statements = mutableFunctionStatements

        for (l in labelMap.keys) {
            // Now, we iterate over all the basic blocks and add an assign statement.
            val assignment = newBinaryOperator("=", code)
            assignment.rhs = labelMap[l]!!
            assignment.lhs = newDeclaredReferenceExpression(varName, type, code)
            assignment.lhs.type = type
            assignment.lhs.unregisterTypeListener(assignment)
            assignment.unregisterTypeListener(assignment.lhs as DeclaredReferenceExpression)
            (assignment.lhs as DeclaredReferenceExpression).refersTo = declaration

            val basicBlock = l.subStatement as? CompoundStatement
            val mutableStatements = basicBlock?.statements?.toMutableList()
            mutableStatements?.add(basicBlock.statements.size - 1, assignment)
            if (mutableStatements != null) {
                basicBlock.statements = mutableStatements
            }
        }
    }

    /**
     * Most instructions in LLVM have a variable assignment as part of their instruction. Since LLVM
     * IR is SSA, we need to declare a new variable in this case, which is named according to
     * [valueRef]. In case the variable assignment is optional, and we directly return the
     * [Expression] associated with the instruction.
     */
    private fun declarationOrNot(rhs: Expression, valueRef: LLVMValueRef): Statement {
        val namePair = lang.getNameOf(valueRef)
        val lhs = namePair.first
        val symbolName = namePair.second

        // if it is still empty, we probably do not have a left side
        return if (lhs != "") {
            val decl =
                newVariableDeclaration(
                    lhs,
                    lang.typeOf(valueRef),
                    lang.getCodeFromRawNode(valueRef),
                    false
                )
            decl.initializer = rhs

            // add the declaration to the current scope
            lang.scopeManager.addDeclaration(decl)

            // add it to our bindings cache
            lang.bindingsCache[symbolName] = decl

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
        val compound = newCompoundStatement("")

        var instr = LLVMGetFirstInstruction(bb)
        while (instr != null) {
            log.debug("Parsing {}", lang.getCodeFromRawNode(instr))

            val stmt = lang.statementHandler.handle(instr)

            compound.addStatement(stmt)

            instr = LLVMGetNextInstruction(instr)
        }

        return compound
    }

    /**
     * Handles a binary operation and returns either a [BinaryOperator], [UnaryOperator],
     * [CallExpression] or a [DeclarationStatement].
     *
     * It expects the llvm-instruction in [instr] and the operator in [op]. The argument [unsigned]
     * indicates if the operands have to be treated as unsigned integer values. In this case, a cast
     * expression is used to ensure that the information is represented in the graph. The argument
     * [unordered] indicates if a floating-point comparison needs to be `or`ed with a check to
     * whether the value is unordered (i.e., NAN).
     */
    @FunctionReplacement(["isunordered"])
    fun handleBinaryOperator(
        instr: LLVMValueRef,
        op: String,
        unsigned: Boolean,
        unordered: Boolean = false
    ): Statement {
        val op1 = lang.getOperandValueAtIndex(instr, 0)
        val op2 = lang.getOperandValueAtIndex(instr, 1)

        val binaryOperator: Expression
        var binOpUnordered: BinaryOperator? = null

        if (op == "uno") {
            // Unordered comparison operand => Replace with a call to isunordered(x, y)
            // Resulting statement: i1 lhs = isordered(op1, op2)
            binaryOperator =
                newCallExpression(
                    "isunordered",
                    "isunordered",
                    LLVMPrintValueToString(instr).string,
                    false
                )
            binaryOperator.addArgument(op1)
            binaryOperator.addArgument(op2)
        } else if (op == "ord") {
            // Ordered comparison operand => Replace with !isunordered(x, y)
            // Resulting statement: i1 lhs = !isordered(op1, op2)
            val unorderedCall =
                newCallExpression(
                    "isunordered",
                    "isunordered",
                    LLVMPrintValueToString(instr).string,
                    false
                )
            unorderedCall.addArgument(op1)
            unorderedCall.addArgument(op2)
            binaryOperator =
                newUnaryOperator("!", false, true, LLVMPrintValueToString(instr).string)
            binaryOperator.input = unorderedCall
        } else {
            // Resulting statement: lhs = op1 <op> op2.
            binaryOperator = newBinaryOperator(op, lang.getCodeFromRawNode(instr))

            if (unsigned) {
                val op1Type = "u${op1.type.name}"
                val castExprLhs = newCastExpression(lang.getCodeFromRawNode(instr))
                castExprLhs.castType = TypeParser.createFrom(op1Type, true)
                castExprLhs.expression = op1
                binaryOperator.lhs = castExprLhs

                val op2Type = "u${op2.type.name}"
                val castExprRhs = newCastExpression(lang.getCodeFromRawNode(instr))
                castExprRhs.castType = TypeParser.createFrom(op2Type, true)
                castExprRhs.expression = op2
                binaryOperator.rhs = castExprRhs
            } else {
                binaryOperator.lhs = op1
                binaryOperator.rhs = op2
            }

            if (unordered) {
                // Special case for floating point comparisons which check if a value is "unordered
                // or <op>".
                // Statement is then lhs = isunordered(op1, op2) || (op1 <op> op2)
                binOpUnordered = newBinaryOperator("||", lang.getCodeFromRawNode(instr))
                binOpUnordered.rhs = binaryOperator
                val unorderedCall =
                    newCallExpression(
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

        val declOp = if (unordered) binOpUnordered else binaryOperator
        val decl = declarationOrNot(declOp!!, instr)

        (decl as? DeclarationStatement)?.let {
            // cache binding
            lang.bindingsCache[instr.symbolName] = decl.singleDeclaration as VariableDeclaration
        }

        return decl
    }

    /** Returns a [LabelStatement] for the basic block represented by [valueRef]. */
    private fun extractBasicBlockLabel(valueRef: LLVMValueRef): LabelStatement {
        val bb = LLVMValueAsBasicBlock(valueRef)
        var labelName = LLVMGetBasicBlockName(bb).string

        if (labelName.isNullOrEmpty()) {
            val bbStr = LLVMPrintValueToString(valueRef).string
            val firstLine = bbStr.trim().split("\n")[0]
            labelName = firstLine.substring(0, firstLine.indexOf(":"))
        }

        val labelStatement =
            lang.labelMap.computeIfAbsent(labelName) {
                val label = newLabelStatement(labelName)
                label.name = labelName
                label
            }
        return labelStatement
    }
}
