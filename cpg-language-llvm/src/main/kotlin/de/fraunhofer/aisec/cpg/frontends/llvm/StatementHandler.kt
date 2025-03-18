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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.annotations.FunctionReplacement
import java.util.function.BiConsumer
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
            return declarationOrNot(frontend.expressionHandler.handleCastInstruction(instr), instr)
        }

        return when (val opcode = instr.opCode) {
            LLVMRet -> {
                val ret = newReturnStatement(rawNode = instr)

                val numOps = LLVMGetNumOperands(instr)
                if (numOps != 0) {
                    ret.returnValue = frontend.getOperandValueAtIndex(instr, 0)
                }

                ret
            }
            LLVMBr -> {
                handleBrStatement(instr)
            }
            LLVMSwitch -> {
                handleSwitchStatement(instr)
            }
            LLVMIndirectBr -> {
                handleIndirectbrStatement(instr)
            }
            LLVMCall,
            LLVMInvoke -> {
                handleFunctionCall(instr)
            }
            LLVMUnreachable -> {
                // Does nothing
                newEmptyStatement(rawNode = instr)
            }
            LLVMCallBr -> {
                // Maps to a call but also to a goto statement? Barely used => not relevant
                newProblemExpression(
                    "Cannot handle callbr instruction yet",
                    ProblemNode.ProblemType.TRANSLATION,
                    rawNode = instr,
                )
            }
            LLVMFNeg -> {
                val fneg = newUnaryOperator("-", postfix = false, prefix = true, rawNode = instr)
                fneg.input = frontend.getOperandValueAtIndex(instr, 0)

                val decl = declarationOrNot(fneg, instr)
                (decl as? DeclarationStatement)?.let {
                    // cache binding
                    frontend.bindingsCache[instr.symbolName] =
                        decl.singleDeclaration as VariableDeclaration
                }

                decl
            }
            LLVMAlloca -> {
                handleAlloca(instr)
            }
            LLVMLoad -> {
                handleLoad(instr)
            }
            LLVMStore -> {
                handleStore(instr)
            }
            LLVMExtractValue,
            LLVMGetElementPtr -> {
                declarationOrNot(frontend.expressionHandler.handleGetElementPtr(instr), instr)
            }
            LLVMICmp -> {
                declarationOrNot(handleIntegerComparison(instr), instr)
            }
            LLVMFCmp -> {
                handleFloatComparison(instr)
            }
            LLVMPHI -> {
                frontend.phiList.add(instr)
                newEmptyStatement(rawNode = instr)
            }
            LLVMSelect -> {
                declarationOrNot(frontend.expressionHandler.handleSelect(instr), instr)
            }
            LLVMUserOp1,
            LLVMUserOp2 -> {
                log.info(
                    "userop instruction is not a real instruction. Replacing it with empty statement"
                )
                newEmptyStatement(rawNode = instr)
            }
            LLVMVAArg -> {
                handleVaArg(instr)
            }
            LLVMExtractElement -> {
                handleExtractelement(instr)
            }
            LLVMInsertElement -> {
                handleInsertelement(instr)
            }
            LLVMShuffleVector -> {
                handleShufflevector(instr)
            }
            LLVMInsertValue -> {
                handleInsertValue(instr)
            }
            LLVMFreeze -> {
                handleFreeze(instr)
            }
            LLVMFence -> {
                handleFence(instr)
            }
            LLVMAtomicCmpXchg -> {
                handleAtomiccmpxchg(instr)
            }
            LLVMAtomicRMW -> {
                handleAtomicrmw(instr)
            }
            LLVMResume -> {
                // Resumes propagation of an existing (in-flight) exception whose unwinding was
                // interrupted with a landingpad instruction.
                newThrowExpression(rawNode = instr).apply {
                    exception =
                        newProblemExpression("We don't know the exception while parsing this node.")
                }
            }
            LLVMLandingPad -> {
                handleLandingpad(instr)
            }
            LLVMCleanupRet -> {
                // End of the cleanup basic block(s)
                // Jump to a label where handling the exception will unwind to next (e.g. a
                // catchswitch statement)
                handleCatchret(instr)
            }
            LLVMCatchRet -> {
                // Catch (caught by catchpad instruction) is over.
                // Jumps to a label where the "normal" function logic continues
                handleCatchret(instr)
            }
            LLVMCatchPad -> {
                // Actually handles the exception.
                handleCatchpad(instr)
            }
            LLVMCleanupPad -> {
                // Beginning of the cleanup basic block(s).
                // We should model this as the beginning of a catch block
                handleCleanuppad(instr)
            }
            LLVMCatchSwitch -> {
                // Marks the beginning of a "real" catch block
                // Jumps to one of the handlers specified or to the default handler (if specified)
                handleCatchswitch(instr)
            }
            else -> {
                log.error("Not handling instruction opcode {} yet", opcode)
                newProblemExpression(
                    "Not handling instruction opcode $opcode yet",
                    ProblemNode.ProblemType.TRANSLATION,
                    rawNode = instr,
                )
            }
        }
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
            } else {
                LLVMGetUnwindDest(instr)?.let { LLVMBasicBlockAsValue(it) }
            }
        val name =
            Name(
                if (instr.opCode == LLVMCatchRet) {
                    "catchret"
                } else {
                    "cleanuppad"
                }
            )
        return if (unwindDest != null) { // For "unwind to caller", the destination is null
            assembleGotoStatement(instr, unwindDest).apply { this.name = name }
        } else {
            newEmptyStatement(rawNode = instr).apply { this.name = name }
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

        val parent = frontend.getOperandValueAtIndex(instr, 0)

        val compoundStatement = newBlock(rawNode = instr)

        val dummyCall =
            newCallExpression(
                llvmInternalRef("llvm.catchswitch"),
                "llvm.catchswitch",
                false,
                rawNode = instr,
            )
        dummyCall.addArgument(parent, "parent")

        val tokenGeneration = declarationOrNot(dummyCall, instr) as DeclarationStatement
        compoundStatement.statements += tokenGeneration

        val ifStatement = newIfStatement(rawNode = instr)
        var currentIfStatement: IfStatement? = null
        var idx = 1
        while (idx < numOps) {
            if (currentIfStatement == null) {
                currentIfStatement = ifStatement
            } else {
                val newIf = newIfStatement(rawNode = instr)
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
                    llvmInternalRef("llvm.matchesCatchpad"),
                    "llvm.matchesCatchpad",
                    false,
                    rawNode = instr,
                )

            val parentCatchSwitch = LLVMGetParentCatchSwitch(catchpad)
            val catchswitch = frontend.expressionHandler.handle(parentCatchSwitch) as Expression
            matchesCatchpad.addArgument(catchswitch, "parentCatchswitch")

            for (i in 0 until catchOps) {
                val arg = frontend.getOperandValueAtIndex(catchpad, i)
                matchesCatchpad.addArgument(arg, "args_$i")
            }

            currentIfStatement.condition = matchesCatchpad

            // Get the label of the goto statement.
            val gotoStatement = assembleGotoStatement(instr, bbTarget)
            currentIfStatement.thenStatement = gotoStatement

            idx++
        }

        val unwindDest = LLVMGetUnwindDest(instr)
        if (unwindDest != null) { // For "unwind to caller", the destination is null
            val gotoStatement = assembleGotoStatement(instr, LLVMBasicBlockAsValue(unwindDest))
            if (currentIfStatement == null) {
                currentIfStatement = ifStatement
            }
            currentIfStatement.elseStatement = gotoStatement
        } else {
            // "unwind to caller". As we don't know where the control flow continues,
            // the best model would be to throw the exception again. Here, we only know
            // that we will throw something here, but we don't know what. We have to fix
            // that later once we know in which catch-block this statement is executed.
            val throwOperation =
                newThrowExpression(rawNode = instr).apply {
                    exception =
                        newProblemExpression("We don't know the exception while parsing this node.")
                }
            currentIfStatement?.elseStatement = throwOperation
        }

        compoundStatement.statements += ifStatement
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
        val catchswitch = frontend.getOperandValueAtIndex(instr, 0)

        val dummyCall =
            newCallExpression(
                llvmInternalRef("llvm.cleanuppad"),
                "llvm.cleanuppad",
                false,
                rawNode = instr,
            )
        dummyCall.addArgument(catchswitch, "parentCatchswitch")

        for (i in 1 until numOps) {
            val arg = frontend.getOperandValueAtIndex(instr, i)
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
        val catchswitch = frontend.expressionHandler.handle(parentCatchSwitch) as Expression

        val dummyCall =
            newCallExpression(
                llvmInternalRef("llvm.catchpad"),
                "llvm.catchpad",
                false,
                rawNode = instr,
            )
        dummyCall.addArgument(catchswitch, "parentCatchswitch")

        for (i in 0 until numOps) {
            val arg = frontend.getOperandValueAtIndex(instr, i)
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
            newCallExpression(llvmInternalRef("llvm.va_arg"), "llvm.va_arg", false, rawNode = instr)
        val operandName = frontend.getOperandValueAtIndex(instr, 0)
        callExpr.addArgument(operandName)
        val expectedType = frontend.typeOf(instr)
        val typeLiteral = newLiteral(expectedType, expectedType, rawNode = instr)
        callExpr.addArgument(typeLiteral) // TODO: Is this correct??
        return declarationOrNot(callExpr, instr)
    }

    /** Handles all kinds of instructions which are an arithmetic or logical binary instruction. */
    private fun handleBinaryInstruction(instr: LLVMValueRef): Statement {
        val binaryOperator =
            when (instr.opCode) {
                LLVMAdd,
                LLVMFAdd -> {
                    handleBinaryOperator(instr, "+", false)
                }
                LLVMSub,
                LLVMFSub -> {
                    handleBinaryOperator(instr, "-", false)
                }
                LLVMMul,
                LLVMFMul -> {
                    handleBinaryOperator(instr, "*", false)
                }
                LLVMUDiv -> {
                    handleBinaryOperator(instr, "/", true)
                }
                LLVMSDiv,
                LLVMFDiv -> {
                    handleBinaryOperator(instr, "/", false)
                }
                LLVMURem -> {
                    handleBinaryOperator(instr, "%", true)
                }
                LLVMSRem,
                LLVMFRem -> {
                    handleBinaryOperator(instr, "%", false)
                }
                LLVMShl -> {
                    handleBinaryOperator(instr, "<<", false)
                }
                LLVMLShr -> {
                    handleBinaryOperator(instr, ">>", true)
                }
                LLVMAShr -> {
                    handleBinaryOperator(instr, ">>", false)
                }
                LLVMAnd -> {
                    handleBinaryOperator(instr, "&", false)
                }
                LLVMOr -> {
                    handleBinaryOperator(instr, "|", false)
                }
                LLVMXor -> {
                    handleBinaryOperator(instr, "^", false)
                }
                else ->
                    newProblemExpression(
                        "No opcode found for binary operator",
                        ProblemNode.ProblemType.TRANSLATION,
                        rawNode = instr,
                    )
            }
        return declarationOrNot(binaryOperator, instr)
    }

    /**
     * Handles the ['alloca'](https://llvm.org/docs/LangRef.html#alloca-instruction) instruction,
     * which allocates a defined block of memory. The closest what we have in the graph is the
     * [NewArrayExpression], which creates a fixed sized array, i.e., a block of memory.
     */
    private fun handleAlloca(instr: LLVMValueRef): Statement {
        val array = newNewArrayExpression(rawNode = instr)

        array.type = frontend.typeOf(instr)

        // LLVM is quite forthcoming here. in case the optional length parameter is omitted in the
        // source code, it will automatically be set to 1
        val size = frontend.getOperandValueAtIndex(instr, 0)

        array.addDimension(size)

        return declarationOrNot(array, instr)
    }

    /**
     * Handles the [`store`](https://llvm.org/docs/LangRef.html#store-instruction) instruction. It
     * stores a particular value at a pointer address. This is the rough equivalent to an assignment
     * of a de-referenced pointer in C like `*a = 1`.
     */
    private fun handleStore(instr: LLVMValueRef): Statement {
        val dereference = newUnaryOperator("*", postfix = false, prefix = true, rawNode = instr)
        dereference.input = frontend.getOperandValueAtIndex(instr, 1)

        return newAssignExpression(
            "=",
            listOf(dereference),
            listOf(frontend.getOperandValueAtIndex(instr, 0)),
            rawNode = instr,
        )
    }

    /**
     * Handles the [`load`](https://llvm.org/docs/LangRef.html#load-instruction) instruction, which
     * is basically just a pointer de-reference.
     */
    private fun handleLoad(instr: LLVMValueRef): Statement {
        val ref = newUnaryOperator("*", postfix = false, prefix = true, rawNode = instr)
        ref.input = frontend.getOperandValueAtIndex(instr, 0)

        return declarationOrNot(ref, instr)
    }

    /**
     * Handles the [`icmp`](https://llvm.org/docs/LangRef.html#icmp-instruction) instruction for
     * comparing integer values.
     */
    fun handleIntegerComparison(instr: LLVMValueRef): Expression {
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
                    return newLiteral(false, primitiveType("i1"), rawNode = instr)
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
                    return newLiteral(true, primitiveType("i1"), rawNode = instr)
                }
                else -> "unknown"
            }

        return declarationOrNot(handleBinaryOperator(instr, cmpPred, false, unordered), instr)
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

        var baseType = frontend.typeOf(LLVMGetOperand(instr, 0))
        val operand = frontend.getOperandValueAtIndex(instr, 0)
        val valueToSet = frontend.getOperandValueAtIndex(instr, 1)

        var base = operand

        // Make a copy of the operand
        var copy: Statement =
            newProblemExpression(
                "Default statement for insertvalue",
                ProblemNode.ProblemType.TRANSLATION,
                rawNode = instr,
            )
        if (operand !is ConstructExpression) {
            copy = declarationOrNot(operand, instr)
            if (copy is DeclarationStatement) {
                base =
                    newReference(
                        copy.singleDeclaration?.name?.localName,
                        (copy.singleDeclaration as? VariableDeclaration)?.type ?: unknownType(),
                        rawNode = instr,
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
                val arrayExpr = newSubscriptExpression()
                arrayExpr.arrayExpression = base
                arrayExpr.name = Name(index.toString())
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
                        baseType.typeName,
                    )
                    break
                }

                log.debug(
                    "Trying to access a field within the record declaration of {}",
                    record.name,
                )

                // look for the field
                val field = record.fields["field_$index"]

                // our new base-type is the type of the field
                baseType = field?.type ?: unknownType()

                // construct our member expression
                expr = newMemberExpression(field?.name?.localName, base, baseType, ".")
                log.info("{}", expr)

                // the current expression is the new base
                base = expr
            }
        }

        val compoundStatement = newBlock(rawNode = instr)
        val assignment = newAssignExpression("=", listOf(base), listOf(valueToSet), rawNode = instr)
        compoundStatement.statements += copy
        compoundStatement.statements += assignment

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
        val operand = frontend.getOperandValueAtIndex(instr, 0)

        // condition: arg != undef && arg != poison
        val condition = newBinaryOperator("&&", rawNode = instr)
        val undefCheck = newBinaryOperator("!=", rawNode = instr)
        undefCheck.lhs = operand
        undefCheck.rhs = newLiteral(null, operand.type, rawNode = instr)
        condition.lhs = undefCheck
        val poisonCheck = newBinaryOperator("!=", rawNode = instr)
        poisonCheck.lhs = operand
        // This could be e.g. NAN. Not sure for complex types
        poisonCheck.rhs = newReference("poison", operand.type, rawNode = instr)
        condition.rhs = poisonCheck

        // Call to a dummy function "llvm.freeze" which would fill the undef or poison values
        // randomly.
        // The implementation of this function would depend on the data type (e.g. for integers, it
        // could be rand())
        val callExpression =
            newCallExpression(llvmInternalRef("llvm.freeze"), "llvm.freeze", false, rawNode = instr)
        callExpression.addArgument(operand)

        // res = (arg != undef && arg != poison) ? arg : llvm.freeze(arg)
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
        val instrString = frontend.codeOf(instr)
        val callExpression =
            newCallExpression(llvmInternalRef("llvm.fence"), "llvm.fence", false, rawNode = instr)
        val ordering = newLiteral(LLVMGetOrdering(instr), primitiveType("i32"), rawNode = instr)
        callExpression.addArgument(ordering, "ordering")
        if (instrString?.contains("syncscope") == true) {
            val syncscope = instrString.split("\"")[1]
            callExpression.addArgument(
                newLiteral(syncscope, objectType("String"), rawNode = instr),
                "syncscope",
            )
        }

        return callExpression
    }

    /**
     * Parses the [`cmpxchg`](https://llvm.org/docs/LangRef.html#cmpxchg-instruction) instruction.
     * It returns a single [Statement] or a [Block] if the value is assigned to another variable.
     * Performs the following operation atomically:
     * ```
     * lhs = {*pointer, *pointer == cmp} // A struct of {T, i1}
     * if(*pointer == cmp) { *pointer = new }
     * ```
     *
     * Returns a [Block] with those two instructions or, if `lhs` doesn't exist, only the if-then
     * statement.
     */
    private fun handleAtomiccmpxchg(instr: LLVMValueRef): Statement {
        val compoundStatement = newBlock(rawNode = instr)
        compoundStatement.name = Name("atomiccmpxchg")
        val ptr = frontend.getOperandValueAtIndex(instr, 0)
        val cmp = frontend.getOperandValueAtIndex(instr, 1)
        val value = frontend.getOperandValueAtIndex(instr, 2)

        val ptrDerefCmp = newUnaryOperator("*", postfix = false, prefix = true, rawNode = instr)
        ptrDerefCmp.input = ptr

        val cmpExpr = newBinaryOperator("==", rawNode = instr)
        cmpExpr.lhs = ptrDerefCmp
        cmpExpr.rhs = cmp

        val lhs = LLVMGetValueName(instr).string
        if (lhs != "") {
            // we need to create a crazy struct here. the target type can be found here
            val targetType = frontend.typeOf(instr)

            // construct it
            val construct = newConstructExpression("")
            construct.instantiates = (targetType as? ObjectType)?.recordDeclaration

            val ptrDerefConstruct =
                newUnaryOperator("*", postfix = false, prefix = true, rawNode = instr)
            ptrDerefConstruct.input = frontend.getOperandValueAtIndex(instr, 0)

            val ptrDerefCmpConstruct =
                newUnaryOperator("*", postfix = false, prefix = true, rawNode = instr)
            ptrDerefCmpConstruct.input = frontend.getOperandValueAtIndex(instr, 0)

            val cmpExprConstruct = newBinaryOperator("==", rawNode = instr)
            cmpExprConstruct.lhs = ptrDerefCmpConstruct
            cmpExprConstruct.rhs = frontend.getOperandValueAtIndex(instr, 1)

            construct.addArgument(ptrDerefConstruct)
            construct.addArgument(cmpExprConstruct)

            val decl = declarationOrNot(construct, instr)
            compoundStatement.statements += decl
        }

        val ptrDerefAssign = newUnaryOperator("*", false, true, rawNode = instr)
        ptrDerefAssign.input = frontend.getOperandValueAtIndex(instr, 0)

        val assignment =
            newAssignExpression("=", listOf(ptrDerefAssign), listOf(value), rawNode = instr)

        val ifStatement = newIfStatement(rawNode = instr)
        ifStatement.condition = cmpExpr
        ifStatement.thenStatement = assignment

        compoundStatement.statements += ifStatement

        return compoundStatement
    }

    /**
     * Parses the `atomicrmw` instruction. It returns either a single [Statement] or a [Block] if
     * the value is assigned to another variable.
     */
    private fun handleAtomicrmw(instr: LLVMValueRef): Statement {
        val lhs = LLVMGetValueName(instr).string
        val operation = LLVMGetAtomicRMWBinOp(instr)
        val ptr = frontend.getOperandValueAtIndex(instr, 0)
        val value = frontend.getOperandValueAtIndex(instr, 1)
        val ty = value.type
        val exchOp = newAssignExpression("=", rawNode = instr)
        exchOp.name = Name("atomicrmw")

        val ptrDeref = newUnaryOperator("*", postfix = false, prefix = true, rawNode = instr)
        ptrDeref.input = ptr

        val ptrDerefExch = newUnaryOperator("*", postfix = false, prefix = true, rawNode = instr)
        ptrDerefExch.input = frontend.getOperandValueAtIndex(instr, 0)
        exchOp.lhs = mutableListOf(ptrDerefExch)

        when (operation) {
            LLVMAtomicRMWBinOpXchg -> {
                exchOp.rhs = mutableListOf(value)
            }
            LLVMAtomicRMWBinOpFAdd,
            LLVMAtomicRMWBinOpAdd -> {
                val binaryOperator = newBinaryOperator("+", rawNode = instr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = mutableListOf(binaryOperator)
            }
            LLVMAtomicRMWBinOpFSub,
            LLVMAtomicRMWBinOpSub -> {
                val binaryOperator = newBinaryOperator("-", rawNode = instr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = mutableListOf(binaryOperator)
            }
            LLVMAtomicRMWBinOpAnd -> {
                val binaryOperator = newBinaryOperator("&", rawNode = instr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = mutableListOf(binaryOperator)
            }
            LLVMAtomicRMWBinOpNand -> {
                val binaryOperator = newBinaryOperator("|", rawNode = instr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                val unaryOperator = newUnaryOperator("~", false, true, rawNode = instr)
                unaryOperator.input = binaryOperator
                exchOp.rhs = mutableListOf(unaryOperator)
            }
            LLVMAtomicRMWBinOpOr -> {
                val binaryOperator = newBinaryOperator("|", rawNode = instr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = mutableListOf(binaryOperator)
            }
            LLVMAtomicRMWBinOpXor -> {
                val binaryOperator = newBinaryOperator("^", rawNode = instr)
                binaryOperator.lhs = ptrDeref
                binaryOperator.rhs = value
                exchOp.rhs = mutableListOf(binaryOperator)
            }
            LLVMAtomicRMWBinOpMax,
            LLVMAtomicRMWBinOpMin -> {
                val operatorCode =
                    if (operation == LLVMAtomicRMWBinOpMin) {
                        "<"
                    } else {
                        ">"
                    }
                val condition = newBinaryOperator(operatorCode, rawNode = instr)
                condition.lhs = ptrDeref
                condition.rhs = value

                val ptrDerefConditional = newUnaryOperator("*", false, true, rawNode = instr)
                ptrDerefConditional.input = frontend.getOperandValueAtIndex(instr, 0)
                val conditional =
                    newConditionalExpression(condition, ptrDerefConditional, value, ty)
                exchOp.rhs = mutableListOf(conditional)
            }
            LLVMAtomicRMWBinOpUMax,
            LLVMAtomicRMWBinOpUMin -> {
                val operatorCode =
                    if (operation == LLVMAtomicRMWBinOpUMin) {
                        "<"
                    } else {
                        ">"
                    }
                val condition = newBinaryOperator(operatorCode, rawNode = instr)
                val castExprLhs = newCastExpression(rawNode = instr)
                castExprLhs.castType = objectType("u${ty.name}")
                castExprLhs.expression = ptrDeref
                condition.lhs = castExprLhs

                val castExprRhs = newCastExpression(rawNode = instr)
                castExprRhs.castType = objectType("u${ty.name}")
                castExprRhs.expression = value
                condition.rhs = castExprRhs

                val ptrDerefConditional = newUnaryOperator("*", false, true, rawNode = instr)
                ptrDerefConditional.input = frontend.getOperandValueAtIndex(instr, 0)
                val conditional =
                    newConditionalExpression(condition, ptrDerefConditional, value, ty)
                exchOp.rhs = mutableListOf(conditional)
            }
            else -> {
                newProblemExpression(
                    "LLVMAtomicRMWBinOp $operation not supported",
                    ProblemNode.ProblemType.TRANSLATION,
                    rawNode = instr,
                )
            }
        }

        return if (lhs != "") {
            // set lhs = *ptr, then perform the replacement
            val compoundStatement = newBlock(rawNode = instr)

            val ptrDerefAssignment =
                newUnaryOperator("*", postfix = false, prefix = true, rawNode = instr)
            ptrDerefAssignment.input = frontend.getOperandValueAtIndex(instr, 0)

            compoundStatement.statements =
                mutableListOf(declarationOrNot(ptrDerefAssignment, instr), exchOp)
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
        if (numOps < 2)
            throw TranslationException(
                "Indirectbr statement without address and at least one target"
            )

        val address = frontend.getOperandValueAtIndex(instr, 0)

        val switchStatement = newSwitchStatement(rawNode = instr)
        switchStatement.selector = address

        val caseStatements = newBlock(rawNode = instr)

        var idx = 1
        while (idx < numOps) {
            // The case statement is derived from the address of the label which we can jump to
            val caseBBAddress = LLVMValueAsBasicBlock(LLVMGetOperand(instr, idx)).address()
            val caseStatement = newCaseStatement(rawNode = instr)
            caseStatement.caseExpression =
                newLiteral(caseBBAddress, primitiveType("i64"), rawNode = instr)
            caseStatements.statements += caseStatement

            // Get the label of the goto statement.
            val gotoStatement = assembleGotoStatement(instr, LLVMGetOperand(instr, idx))
            caseStatements.statements += gotoStatement
            caseStatements.statements += newBreakStatement().implicit()
            idx++
        }

        switchStatement.statement = caseStatements

        return switchStatement
    }

    /** Handles a [`br`](https://llvm.org/docs/LangRef.html#br-instruction) instruction. */
    private fun handleBrStatement(instr: LLVMValueRef): Statement {
        if (LLVMGetNumOperands(instr) == 3) {
            // if(op) then {goto label1} else {goto label2}
            val ifStatement = newIfStatement(rawNode = instr)
            val condition = frontend.getOperandValueAtIndex(instr, 0)
            ifStatement.condition = condition

            // Get the label of the "else" branch
            ifStatement.elseStatement = assembleGotoStatement(instr, LLVMGetOperand(instr, 1))

            // Get the label of the "if" branch
            ifStatement.thenStatement = assembleGotoStatement(instr, LLVMGetOperand(instr, 2))

            return ifStatement
        } else if (LLVMGetNumOperands(instr) == 1) {
            // goto defaultLocation
            return assembleGotoStatement(instr, LLVMGetOperand(instr, 0))
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
        if (numOps < 2 || numOps % 2 != 0)
            throw TranslationException("Switch statement without operand and default branch")

        val operand = frontend.getOperandValueAtIndex(instr, 0)

        val switchStatement = newSwitchStatement(rawNode = instr)
        switchStatement.selector = operand

        val caseStatements = newBlock(rawNode = instr)

        var idx = 2
        while (idx < numOps) {
            // Get the comparison value and add it to the CaseStatement
            val caseStatement = newCaseStatement(rawNode = instr)
            caseStatement.caseExpression = frontend.getOperandValueAtIndex(instr, idx)
            caseStatements.statements += caseStatement
            idx++
            // Get the "case" statements and add it to the CaseStatement
            val gotoStatement = assembleGotoStatement(instr, LLVMGetOperand(instr, idx))
            caseStatements.statements += gotoStatement
            idx++
        }

        // Get the label of the "default" branch
        caseStatements.statements += newDefaultStatement(rawNode = instr)
        val defaultGoto = assembleGotoStatement(instr, LLVMGetOperand(instr, 1))
        caseStatements.statements += defaultGoto

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
        val calledFunc = LLVMGetCalledValue(instr)
        var calledFuncName: CharSequence = LLVMGetValueName(calledFunc).string
        var max = LLVMGetNumOperands(instr) - 1
        var idx = 0

        if (calledFuncName == "") {
            // Function is probably called by a local variable. For some reason, this is the last
            // operand
            val opName = frontend.getOperandValueAtIndex(instr, max)
            calledFuncName = opName.name
        }

        var gotoCatch: GotoStatement = newGotoStatement(rawNode = instr)
        var tryContinue: GotoStatement = newGotoStatement(rawNode = instr)
        if (instr.opCode == LLVMInvoke) {
            max-- // Last one is the Decl.Expr of the function
            // Get the label of the catch clause.
            gotoCatch = assembleGotoStatement(instr, LLVMGetOperand(instr, max))
            max--
            // Get the label of the basic block where the control flow continues (e.g. if no error
            // occurs).
            tryContinue = assembleGotoStatement(instr, LLVMGetOperand(instr, max))
            max--
            log.info(
                "Invoke expression: Usually continues at ${tryContinue.labelName}, exception continues at ${gotoCatch.labelName}"
            )
        }

        val callee = newReference(calledFuncName, frontend.typeOf(calledFunc), rawNode = calledFunc)

        val callExpr = newCallExpression(callee, calledFuncName, false, rawNode = instr)

        while (idx < max) {
            val operandName = frontend.getOperandValueAtIndex(instr, idx)
            callExpr.addArgument(operandName)
            idx++
        }

        if (instr.opCode == LLVMInvoke) {
            // For the "invoke" instruction, the call is surrounded by a try statement which also
            // contains a goto statement after the call.
            val tryStatement = newTryStatement(rawNode = instr)
            frontend.scopeManager.enterScope(tryStatement)
            val tryBlock = newBlock(rawNode = instr)
            tryBlock.statements += declarationOrNot(callExpr, instr)
            tryBlock.statements += tryContinue
            tryStatement.tryBlock = tryBlock
            frontend.scopeManager.leaveScope(tryStatement)

            val catchClause = newCatchClause(rawNode = instr)
            catchClause.name = Name(gotoCatch.labelName)
            catchClause.parameter =
                newVariableDeclaration(
                    "e_${gotoCatch.labelName}",
                    unknownType(),
                    true,
                    rawNode = instr,
                )

            val catchBlockStatement = newBlock(rawNode = instr)
            catchBlockStatement.statements += gotoCatch
            catchClause.body = catchBlockStatement
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
        val catchInstr = newCatchClause()
        /* Get the number of clauses on the landingpad instruction and iterate through the clauses to get all types for the catch clauses */
        val numClauses = LLVMGetNumClauses(instr)
        var catchType = ""
        for (i in 0 until numClauses) {
            val clause = LLVMGetClause(instr, i)
            if (LLVMIsAConstantArray(clause) == null) {
                catchType +=
                    if (LLVMIsNull(clause) == 1) {
                        "..." + " | "
                    } else {
                        LLVMGetValueName(clause).string + " | "
                    }
            } else {
                // TODO: filter not handled yet
            }
        }
        if (catchType.endsWith(" | ")) catchType = catchType.substring(0, catchType.length - 3)

        val lhs = frontend.getNameOf(instr).first

        val exceptionName =
            if (lhs != "") {
                lhs
            } else {
                "e_${instr.address()}"
            }
        val except =
            newVariableDeclaration(
                exceptionName,
                objectType(catchType), // TODO: This doesn't work for multiple types to catch
                false,
                rawNode = instr,
            )
        frontend.bindingsCache["%${exceptionName}"] = except
        catchInstr.parameter = except
        catchInstr.name = Name(catchType)
        return catchInstr
    }

    /**
     * Handles the [`insertelement`](https://llvm.org/docs/LangRef.html#insertelement-instruction)
     * instruction which is modeled as access to an array at a given index. A new array with the
     * modified value is constructed.
     */
    private fun handleInsertelement(instr: LLVMValueRef): Statement {
        val compoundStatement = newBlock(rawNode = instr)

        // TODO: Probably we should make a proper copy of the array
        val newArrayDecl = declarationOrNot(frontend.getOperandValueAtIndex(instr, 0), instr)
        compoundStatement.statements += newArrayDecl

        val decl = newArrayDecl.declarations[0] as? VariableDeclaration
        val arrayExpr = newSubscriptExpression(rawNode = instr)
        arrayExpr.arrayExpression =
            newReference(
                decl?.name?.toString() ?: Node.EMPTY_NAME,
                decl?.type ?: unknownType(),
                rawNode = instr,
            )
        arrayExpr.subscriptExpression = frontend.getOperandValueAtIndex(instr, 2)

        val assignExpr =
            newAssignExpression(
                "=",
                listOf(arrayExpr),
                listOf(frontend.getOperandValueAtIndex(instr, 1)),
                rawNode = instr,
            )
        compoundStatement.statements += assignExpr

        return compoundStatement
    }

    /**
     * Handles the [`extractelement`](https://llvm.org/docs/LangRef.html#extractelement-instruction)
     * instruction which is modeled as access to an array at a given index.
     */
    private fun handleExtractelement(instr: LLVMValueRef): Statement {
        val arrayExpr = newSubscriptExpression(rawNode = instr)
        arrayExpr.arrayExpression = frontend.getOperandValueAtIndex(instr, 0)
        arrayExpr.subscriptExpression = frontend.getOperandValueAtIndex(instr, 1)

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
        val list = newInitializerListExpression(frontend.typeOf(instr), rawNode = instr)
        val elementType = frontend.typeOf(instr).dereference()

        val initializers = mutableListOf<Expression>()

        // Get the first vector and its length. The length is 0 if it's an undef value.
        val array1 = frontend.getOperandValueAtIndex(instr, 0)
        val array1Length =
            if (array1 is Literal<*> && array1.value == null) {
                0
            } else {
                LLVMGetVectorSize(LLVMTypeOf(LLVMGetOperand(instr, 0)))
            }

        // Get the second vector and its length. The length is 0 if it's an undef value.
        val array2 = frontend.getOperandValueAtIndex(instr, 1)
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
                    initializers += newLiteral(null, elementType, rawNode = instr)
                } else {
                    val arrayExpr = newSubscriptExpression(rawNode = instr)
                    arrayExpr.arrayExpression = frontend.getOperandValueAtIndex(instr, 0)
                    arrayExpr.subscriptExpression =
                        newLiteral(idxInt, primitiveType("i32"), rawNode = instr)
                    initializers += arrayExpr
                }
            } else if (idxInt < array1Length + array2Length) {
                if (array2 is InitializerListExpression) {
                    initializers += array2.initializers[idxInt - array1Length]
                } else if (array2 is Literal<*> && array2.value == null) {
                    initializers += newLiteral(null, elementType, rawNode = instr)
                } else {
                    val arrayExpr = newSubscriptExpression(rawNode = instr)
                    arrayExpr.arrayExpression = frontend.getOperandValueAtIndex(instr, 1)
                    arrayExpr.subscriptExpression =
                        newLiteral(idxInt - array1Length, primitiveType("i32"), rawNode = instr)
                    initializers += arrayExpr
                }
            } else {
                initializers += newLiteral(null, elementType, rawNode = instr)
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
    fun handlePhi(instr: LLVMValueRef, tu: TranslationUnitDeclaration, flatAST: MutableList<Node>) {
        val labelMap = mutableMapOf<LabelStatement, Expression>()
        val numOps = LLVMGetNumOperands(instr)
        var i = 0
        var bbsFunction: LLVMValueRef? = null
        while (i < numOps) {
            val valI = frontend.getOperandValueAtIndex(instr, i)
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

            val labelName = getBasicBlockName(incomingBB)
            val labelI = flatAST.firstOrNull { s -> s is LabelStatement && s.label == labelName }
            i++
            if (labelI == null) {
                log.error("Expecting to find a label with name $labelName for Phi statement.")
            }
            labelMap[labelI as LabelStatement] = valI
        }
        if (labelMap.keys.size == 1) {
            // We only have a single pair, so we insert a declaration in that one BB.
            val (key, value) = labelMap.entries.elementAt(0)
            val basicBlock = key.subStatement as? Block
            val decl = declarationOrNot(value, instr)
            flatAST.addAll(SubgraphWalker.flattenAST(decl))
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
        val functions = tu.functions(functionName)
        if (functions.size != 1) {
            log.error(
                "${functions.size} functions match the name of the one where the phi instruction is inserted. Can't handle this case."
            )
            throw TranslationException("Wrong number of functions for phi statement.")
        }
        // Create the dummy declaration at the beginning of the function body
        val firstBB = functions[0].body as Block
        val varName = instr.name
        val type = frontend.typeOf(instr)
        val declaration = newVariableDeclaration(varName, type, false, rawNode = instr)
        declaration.type = type

        flatAST.add(declaration)

        // add it to our bindings cache
        frontend.bindingsCache[instr.symbolName] = declaration

        val declStatement = newDeclarationStatement(rawNode = instr)
        // add the declaration to the current scope
        frontend.scopeManager.addDeclaration(declaration)
        declStatement.singleDeclaration = declaration

        val mutableFunctionStatements = firstBB.statements.toMutableList()
        mutableFunctionStatements.add(0, declStatement)
        firstBB.statements = mutableFunctionStatements

        for ((l, r) in labelMap) {
            // Now, we iterate over all the basic blocks and add an assign statement.
            val assignment =
                newAssignExpression(
                    "=",
                    listOf(newReference(varName, type, rawNode = instr)),
                    listOf(r),
                    rawNode = instr,
                )
            (assignment.lhs.first() as Reference).type = type
            (assignment.lhs.first() as Reference).refersTo = declaration
            flatAST.add(assignment)

            val basicBlock = l.subStatement as? Block
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
        val namePair = frontend.getNameOf(valueRef)
        val lhs = namePair.first
        val symbolName = namePair.second

        // if it is still empty, we probably do not have a left side
        return if (lhs != "") {
            val decl =
                newVariableDeclaration(lhs, frontend.typeOf(valueRef), false, rawNode = valueRef)
            decl.initializer = rhs

            // add the declaration to the current scope
            frontend.scopeManager.addDeclaration(decl)

            // add it to our bindings cache
            frontend.bindingsCache[symbolName] = decl

            // Since the declaration statement only contains the single declaration, we can use the
            // same raw node, so we end up with the same code and location
            val declStatement = newDeclarationStatement(rawNode = valueRef)
            declStatement.singleDeclaration = decl
            declStatement
        } else {
            rhs
        }
    }

    /**
     * Handles a basic block and returns a [Block] comprised of the statements of this block or a
     * [LabelStatement] if the basic block has a label.
     */
    private fun handleBasicBlock(bb: LLVMBasicBlockRef): Statement {
        val compound = newBlock(rawNode = bb)

        var instr = LLVMGetFirstInstruction(bb)
        while (instr != null) {
            log.debug("Parsing {}", frontend.codeOf(instr))

            val stmt = frontend.statementHandler.handle(instr)
            if (stmt != null) {
                compound.statements += stmt
            }

            instr = LLVMGetNextInstruction(instr)
        }

        val labelName = getBasicBlockName(bb)

        if (labelName != "") {
            val labelStatement = newLabelStatement()
            labelStatement.name = Name(labelName)
            labelStatement.label = labelName
            labelStatement.subStatement = compound

            return labelStatement
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
        unordered: Boolean = false,
    ): Expression {
        val op1 = frontend.getOperandValueAtIndex(instr, 0)
        val op2 = frontend.getOperandValueAtIndex(instr, 1)

        val binaryOperator: Expression
        var binOpUnordered: BinaryOperator? = null

        if (op == "uno") {
            // Unordered comparison operand => Replace with a call to isunordered(x, y)
            // Resulting statement: i1 lhs = isordered(op1, op2)
            binaryOperator =
                newCallExpression(
                    llvmInternalRef("isunordered"),
                    "isunordered",
                    false,
                    rawNode = instr,
                )
            binaryOperator.addArgument(op1)
            binaryOperator.addArgument(op2)
        } else if (op == "ord") {
            // Ordered comparison operand => Replace with !isunordered(x, y)
            // Resulting statement: i1 lhs = !isordered(op1, op2)
            val unorderedCall =
                newCallExpression(
                    llvmInternalRef("isunordered"),
                    "isunordered",
                    false,
                    rawNode = instr,
                )
            unorderedCall.addArgument(op1)
            unorderedCall.addArgument(op2)
            binaryOperator = newUnaryOperator("!", false, true, rawNode = instr)
            binaryOperator.input = unorderedCall
        } else {
            // Resulting statement: lhs = op1 <op> op2.
            binaryOperator = newBinaryOperator(op, rawNode = instr)

            if (unsigned) {
                val op1Type = "u${op1.type.name}"
                val castExprLhs = newCastExpression(rawNode = instr)
                castExprLhs.castType = objectType(op1Type)
                castExprLhs.expression = op1
                binaryOperator.lhs = castExprLhs

                val op2Type = "u${op2.type.name}"
                val castExprRhs = newCastExpression(rawNode = instr)
                castExprRhs.castType = objectType(op2Type)
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
                binOpUnordered = newBinaryOperator("||", rawNode = instr)
                binOpUnordered.rhs = binaryOperator
                val unorderedCall =
                    newCallExpression(
                        llvmInternalRef("isunordered"),
                        "isunordered",
                        false,
                        rawNode = instr,
                    )
                unorderedCall.addArgument(op1)
                unorderedCall.addArgument(op2)
                binOpUnordered.lhs = unorderedCall
            }
        }

        return binOpUnordered ?: binaryOperator
    }

    /**
     * Generates a [GotoStatement] and either links it to the [LabelStatement] if that statement has
     * already been processed or uses the listeners to generate the relation once the label
     * statement has been processed.
     */
    private fun assembleGotoStatement(instr: LLVMValueRef, bbTarget: LLVMValueRef): GotoStatement {
        val goto = newGotoStatement(rawNode = instr)
        val assigneeTargetLabel = BiConsumer { _: Any, to: Node ->
            if (to is LabelStatement) {
                goto.targetLabel = to
            } else if (goto.targetLabel != to) {
                log.error("$to is not a LabelStatement")
            }
        }
        val bb: LLVMBasicBlockRef = LLVMValueAsBasicBlock(bbTarget)
        val labelName = LLVMGetBasicBlockName(bb).string
        goto.labelName = labelName
        val label = newLabelStatement()
        label.name = Name(labelName)
        // If the bound AST node is/or was transformed into a CPG node the cpg node is bound
        // to the CPG goto statement
        frontend.registerObjectListener(label, assigneeTargetLabel)
        if (goto.targetLabel == null) {
            // If the Label AST node could not be resolved, the matching is done based on label
            // names of CPG nodes using the predicate listeners
            frontend.registerPredicateListener(
                { _: Any?, to: Any? -> (to is LabelStatement && to.label == goto.labelName) },
                assigneeTargetLabel,
            )
        }
        return goto
    }

    /** Returns the name of the given basic block. */
    private fun getBasicBlockName(bb: LLVMBasicBlockRef): String {
        var labelName = LLVMGetBasicBlockName(bb).string

        if (labelName.isNullOrEmpty()) {
            // Blocks are assigned an implicit counter-based label if it is not specified. We need
            // to parse it from the string representation of the basic block
            val bbStr = LLVMPrintValueToString(LLVMBasicBlockAsValue(bb)).string
            val firstLine = bbStr.trim().split("\n")[0]
            if (firstLine.contains(":")) {
                labelName = firstLine.substring(0, firstLine.indexOf(":"))
            }
        }
        return labelName
    }

    /**
     * This functions creates a new [Reference] to an internal LLVM function. This would allow us to
     * handle them all in the same way.
     */
    private fun llvmInternalRef(name: String): Reference {
        return newReference(name)
    }
}
