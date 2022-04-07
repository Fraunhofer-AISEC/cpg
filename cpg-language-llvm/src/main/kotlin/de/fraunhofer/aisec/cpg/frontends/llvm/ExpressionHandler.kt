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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newArraySubscriptionExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCastExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConditionalExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newInitializerListExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newUnaryOperator
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import org.bytedeco.javacpp.IntPointer
import org.bytedeco.javacpp.SizeTPointer
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
        return when (val kind = LLVMGetValueKind(value)) {
            LLVMConstantExprValueKind -> handleConstantExprValueKind(value)
            LLVMConstantArrayValueKind, LLVMConstantStructValueKind ->
                handleConstantStructValue(value)
            LLVMConstantDataArrayValueKind,
            LLVMConstantVectorValueKind,
            LLVMConstantDataVectorValueKind -> handleConstantDataArrayValue(value)
            LLVMConstantIntValueKind -> handleConstantInt(value)
            LLVMConstantFPValueKind -> handleConstantFP(value)
            LLVMConstantPointerNullValueKind -> handleNullPointer(value)
            LLVMPoisonValueValueKind -> {
                newDeclaredReferenceExpression("poison", lang.typeOf(value), "poison")
            }
            LLVMConstantTokenNoneValueKind ->
                newLiteral(null, UnknownType.getUnknownType(), lang.getCodeFromRawNode(value))
            LLVMUndefValueValueKind ->
                initializeAsUndef(lang.typeOf(value), lang.getCodeFromRawNode(value)!!)
            LLVMConstantAggregateZeroValueKind ->
                initializeAsZero(lang.typeOf(value), lang.getCodeFromRawNode(value)!!)
            LLVMArgumentValueKind,
            LLVMGlobalVariableValueKind,
            // this is a little tricky. It seems weird, that an instruction value kind turns
            // up here. What is happening is, that this is a variable reference in the form of
            // %var. In this case LLVMGetValueKind will return LLVMInstructionValueKind because
            // it actually points to the instruction where this variable was defined. However,
            // we are only interested in its name and type.
            LLVMInstructionValueKind -> handleReference(value)
            LLVMFunctionValueKind -> handleFunction(value)
            LLVMGlobalAliasValueKind -> {
                val name = lang.getNameOf(value).first
                newDeclaredReferenceExpression(
                    name,
                    lang.typeOf(value),
                    lang.getCodeFromRawNode(value)
                )
            }
            LLVMMetadataAsValueValueKind, LLVMInlineAsmValueKind -> {
                // TODO
                return Expression()
            }
            else -> {
                log.info(
                    "Not handling value kind {} in handleValue yet. Falling back to the legacy way. Please change",
                    kind
                )
                val cpgType = lang.typeOf(value)

                // old stuff from getOperandValue, needs to be refactored to the when above
                // TODO also move the other stuff to the expression handler
                if (LLVMIsConstant(value) != 1) {
                    val operandName: String =
                        if (LLVMIsAGlobalAlias(value) != null || LLVMIsGlobalConstant(value) == 1) {
                            val aliasee = LLVMAliasGetAliasee(value)
                            LLVMPrintValueToString(aliasee)
                                .string // Already resolve the aliasee of the constant
                        } else {
                            // TODO This does not return the actual constant but only a string
                            // representation
                            LLVMPrintValueToString(value).string
                        }
                    return newLiteral(operandName, cpgType, operandName)
                } else if (LLVMIsUndef(value) == 1) {
                    return newDeclaredReferenceExpression("undef", cpgType, "undef")
                } else if (LLVMIsPoison(value) == 1) {
                    return newDeclaredReferenceExpression("poison", cpgType, "poison")
                } else {
                    log.error("Unknown expression {}", kind)
                    return Expression()
                }
            }
        }
    }

    /** Returns a [DeclaredReferenceExpression] for a function (pointer). */
    private fun handleFunction(valueRef: LLVMValueRef): Expression {
        return newDeclaredReferenceExpression(
            valueRef.name,
            lang.typeOf(valueRef),
            lang.getCodeFromRawNode(valueRef)
        )
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
        val namePair = lang.getNameOf(valueRef)
        val name = namePair.first
        val symbolName = namePair.second

        val type = lang.typeOf(valueRef)

        val ref = newDeclaredReferenceExpression(name, type, "${type.typeName} $name")

        // try to resolve the reference. actually the valueRef is already referring to the resolved
        // variable because we obtain it using LLVMGetOperand, so we just need to look it up in the
        // cache bindings
        val decl = lang.bindingsCache[symbolName]

        if (decl == null) {
            // there is something seriously wrong here, if this happens, because all variables need
            // to be declared before use and we _should_ have seen the variable
            log.warn("Could not resolve reference ${symbolName}. This should not happen.")
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
     * Handles [constant expressions](https://llvm.org/docs/LangRef.html#constant-expressions). They
     * are basically constants involving certain operations on other constants. In the future we
     * might treat them differently in the graph, but for now we basically just parse them as a
     * regular expression.
     */
    private fun handleConstantExprValueKind(value: LLVMValueRef): Expression {
        val expr =
            when (val kind = LLVMGetConstOpcode(value)) {
                LLVMGetElementPtr -> handleGetElementPtr(value)
                LLVMSelect -> handleSelect(value)
                LLVMTrunc,
                LLVMZExt,
                LLVMSExt,
                LLVMFPToUI,
                LLVMFPToSI,
                LLVMUIToFP,
                LLVMSIToFP,
                LLVMFPTrunc,
                LLVMFPExt,
                LLVMPtrToInt,
                LLVMIntToPtr,
                LLVMBitCast,
                LLVMAddrSpaceCast -> handleCastInstruction(value)
                LLVMAdd, LLVMFAdd ->
                    lang.statementHandler.handleBinaryOperator(value, "+", false) as? Expression
                        ?: Expression()
                LLVMSub, LLVMFSub ->
                    lang.statementHandler.handleBinaryOperator(value, "-", false) as? Expression
                        ?: Expression()
                LLVMAShr ->
                    lang.statementHandler.handleBinaryOperator(value, ">>", false) as? Expression
                        ?: Expression()
                LLVMICmp -> lang.statementHandler.handleIntegerComparison(value) as? Expression
                        ?: Expression()
                else -> {
                    log.error("Not handling constant expression of opcode {} yet", kind)
                    Expression()
                }
            }

        return expr
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

        val expr: ConstructExpression = newConstructExpression(lang.getCodeFromRawNode(value))
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

    /**
     * Handles a constant array value, which belongs to the
     * [complex constants](https://llvm.org/docs/LangRef.html#complex-constants). Their element
     * types and number of elements needs to match the specified array type. We parse the array
     * contents as an [InitializerListExpression], similar to the C syntax of `int a[] = { 1, 2 }`.
     *
     * There is a special case, in which LLVM allows to represent the array as a double-quoted
     * string, prefixed with `c`. In this case we
     */
    private fun handleConstantDataArrayValue(valueRef: LLVMValueRef): Expression {
        if (LLVMIsConstantString(valueRef) == 1) {
            val string = LLVMGetAsString(valueRef, SizeTPointer(0)).string

            return newLiteral(string, lang.typeOf(valueRef), lang.getCodeFromRawNode(valueRef))
        }

        val list = newInitializerListExpression(lang.getCodeFromRawNode(valueRef))
        val arrayType = LLVMTypeOf(valueRef)
        val length =
            if (LLVMIsAConstantDataArray(valueRef) != null) {
                LLVMGetArrayLength(arrayType)
            } else {
                LLVMGetVectorSize(arrayType)
            }

        val initializers = mutableListOf<Expression>()

        for (i in 0 until length) {
            val expr =
                if (LLVMGetValueKind(valueRef) == LLVMConstantVectorValueKind) {
                    // This type of vectors needs to access the elements via LLVMGetOperand(). Not
                    // sure why but the other method crashes.
                    handle(LLVMGetOperand(valueRef, i)) as Expression
                } else {
                    handle(LLVMGetElementAsConstant(valueRef, i)) as Expression
                }

            initializers += expr
        }

        list.initializers = initializers

        return list
    }

    /**
     * Recursively creates a structure of [type] and initializes all its fields with a `null`-
     * [Literal] as this is closest to `undef`.
     *
     * Returns a [ConstructExpression].
     */
    private fun initializeAsUndef(type: Type, code: String): Expression {
        if (!lang.isKnownStructTypeName(type.name) && !type.name.contains("{")) {
            return newLiteral(null, type, code)
        } else {
            val expr: ConstructExpression = newConstructExpression(code)
            // map the construct expression to the record declaration of the type
            expr.instantiates = (type as? ObjectType)?.recordDeclaration
            if (expr.instantiates == null) return expr

            // loop through the operands
            for (field in (expr.instantiates as RecordDeclaration).fields) {
                // and handle them as expressions themselves
                val arg = initializeAsUndef(field.type, code)
                expr.addArgument(arg)
            }

            return expr
        }
    }

    /**
     * Recursively creates a structure of [type] and initializes all its fields with 0-[Literal].
     *
     * Returns a [ConstructExpression].
     */
    private fun initializeAsZero(type: Type, code: String): Expression {
        if (!lang.isKnownStructTypeName(type.name) && !type.name.contains("{")) {
            return newLiteral(0, type, code)
        } else {
            val expr: ConstructExpression = newConstructExpression(code)
            // map the construct expression to the record declaration of the type
            expr.instantiates = (type as? ObjectType)?.recordDeclaration
            if (expr.instantiates == null) return expr

            // loop through the operands
            for (field in (expr.instantiates as RecordDeclaration).fields) {
                // and handle them as expressions themselves
                val arg = initializeAsZero(field.type, code)
                expr.addArgument(arg)
            }

            return expr
        }
    }

    /** Returns a literal with the type of [value] and value `null`. */
    private fun handleNullPointer(value: LLVMValueRef): Expression {
        val type = lang.typeOf(value)
        return newLiteral(null, type, lang.getCodeFromRawNode(value))
    }

    /**
     * Handles the [`getelementptr`](https://llvm.org/docs/LangRef.html#getelementptr-instruction)
     * instruction and the
     * [`extractvalue`](https://llvm.org/docs/LangRef.html#extractvalue-instruction) instruction
     * which works in a similar way.
     *
     * We try to convert it either into an [ArraySubscriptionExpression] or an [MemberExpression],
     * depending on whether the accessed variable is a struct or an array. Furthermore, since
     * `getelementptr` allows an (infinite) chain of sub-element access within a single instruction,
     * we need to unwrap those into individual expressions.
     */
    internal fun handleGetElementPtr(instr: LLVMValueRef): Expression {
        val isGetElementPtr =
            instr.opCode == LLVMGetElementPtr ||
                (LLVMIsAConstantExpr(instr) != null &&
                    LLVMGetConstOpcode(instr) == LLVMGetElementPtr)

        val numOps: Int
        val loopStart: Int
        var indices = IntPointer()

        if (isGetElementPtr) {
            numOps = LLVMGetNumOperands(instr)
            loopStart = 1
        } else {
            numOps = LLVMGetNumIndices(instr)
            loopStart = 0
            indices = LLVMGetIndices(instr)
        }

        // the first operand is always type that is the basis for the calculation
        var baseType = lang.typeOf(LLVMGetOperand(instr, 0))
        var operand = lang.getOperandValueAtIndex(instr, 0)

        // the start
        var base = operand

        var expr = Expression()

        // loop through all operands / indices
        for (idx: Int in loopStart until numOps) {
            val index: Any =
                if (isGetElementPtr) {
                    // the second argument is the base address that we start our chain from
                    operand = lang.getOperandValueAtIndex(instr, idx)

                    if (operand is Literal<*>) {
                        // Parse index as int
                        (operand.value as Long).toInt()
                    } else {
                        // The index is some variable and thus unknown.
                        operand as DeclaredReferenceExpression
                    }
                } else {
                    indices.get(idx.toLong())
                }

            // check, if the current base type is a pointer -> then we need to handle this as an
            // array access
            if (baseType is PointerType) {
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
                var record = (baseType as? ObjectType)?.recordDeclaration

                if (record == null) {
                    record =
                        lang.scopeManager
                            .resolve<RecordDeclaration>(lang.scopeManager.globalScope, true) {
                                it.name == baseType.typeName
                            }
                            .firstOrNull()
                    if (record != null) {
                        (baseType as? ObjectType)?.recordDeclaration = record
                    }
                }

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
                val field: FieldDeclaration?
                val fieldName: String?
                if (index is Int) {
                    field = record.getField("field_$index")
                    fieldName = field?.name
                } else {
                    // We won't find a field because it's accessed by a variable index.
                    // We indicate this with this array-like notation for now.
                    field = null
                    fieldName = "[${(operand as DeclaredReferenceExpression).name}]"
                }

                // our new base-type is the type of the field
                baseType = field?.type ?: UnknownType.getUnknownType()

                // construct our member expression
                expr = newMemberExpression(base, field?.type, fieldName, ".", "")
                log.info("{}", expr)

                // the current expression is the new base
                base = expr
            }
        }

        // since getelementpr returns the *address*, whereas extractvalue returns a *value*, we need
        // to do a final unary & operation
        if (isGetElementPtr) {
            val ref = newUnaryOperator("&", false, true, "")
            ref.input = expr
            expr = ref
        }

        return expr
    }

    /**
     * Handles the [`select`](https://llvm.org/docs/LangRef.html#i-select) instruction, which
     * behaves like a [ConditionalExpression].
     */
    fun handleSelect(instr: LLVMValueRef): Expression {
        val cond = lang.getOperandValueAtIndex(instr, 0)
        val value1 = lang.getOperandValueAtIndex(instr, 1)
        val value2 = lang.getOperandValueAtIndex(instr, 2)

        return newConditionalExpression(cond, value1, value2, value1.type)
    }

    /**
     * Handles all kinds of instructions which are a
     * [cast instruction](https://llvm.org/docs/LangRef.html#conversion-operations).
     */
    fun handleCastInstruction(instr: LLVMValueRef): Expression {
        val castExpr = newCastExpression(lang.getCodeFromRawNode(instr))
        castExpr.castType = lang.typeOf(instr)
        castExpr.expression = lang.getOperandValueAtIndex(instr, 0)
        return castExpr
    }
}
