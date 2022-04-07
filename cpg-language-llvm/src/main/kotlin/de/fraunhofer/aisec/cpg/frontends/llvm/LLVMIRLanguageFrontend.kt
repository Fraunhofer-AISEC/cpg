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
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.ByteBuffer
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

class LLVMIRLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {

    val labelMap = mutableMapOf<String, LabelStatement>()
    val statementHandler = StatementHandler(this)
    val declarationHandler = DeclarationHandler(this)
    val expressionHandler = ExpressionHandler(this)
    val typeCache = mutableMapOf<String, Type>()

    val phiList = mutableListOf<LLVMValueRef>()

    var ctx: LLVMContextRef? = null

    /**
     * This contains a cache binding between an LLVMValueRef (representing a variable) and its
     * [Declaration] in the graph. We need this, because this way we can look up and connect a
     * [DeclaredReferenceExpression] to its [Declaration] already in the language frontend. This in
     * turn is needed because of the local/global system we cannot rely on the
     * [VariableUsageResolver].
     */
    var bindingsCache = mutableMapOf<String, Declaration>()

    companion object {
        @kotlin.jvm.JvmField var LLVM_EXTENSIONS: List<String> = listOf(".ll")
    }

    override fun parse(file: File): TranslationUnitDeclaration {
        // clear the bindings cache, because it is just valid within one module
        bindingsCache.clear()

        TypeManager.getInstance().setLanguageFrontend(this)

        // these will be filled by our create and parse functions later and will be passed as
        // pointer
        val mod = LLVMModuleRef()
        val buf = LLVMMemoryBufferRef()

        // create a new LLVM context
        ctx = LLVMContextCreate()

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
            LLVMContextDispose(ctx)
            throw TranslationException("Could not create memory buffer: $errorMsg")
        }

        result = LLVMParseIRInContext(ctx, buf, mod, errorMessage)
        if (result != 0) {
            // something went wrong
            val errorMsg = String(errorMessage.array())
            LLVMContextDispose(ctx)
            throw TranslationException("Could not parse IR: $errorMsg")
        }

        val tu = TranslationUnitDeclaration()

        // we need to set our translation unit as the global scope
        scopeManager.resetToGlobal(tu)

        // loop through globals
        var global = LLVMGetFirstGlobal(mod)
        while (global != null) {
            // try to parse the variable (declaration)
            val declaration = declarationHandler.handle(global)

            scopeManager.addDeclaration(declaration)

            global = LLVMGetNextGlobal(global)
        }

        // loop through functions
        var func = LLVMGetFirstFunction(mod)
        while (func != null) {
            // try to parse the function (declaration)
            val declaration = declarationHandler.handle(func)

            scopeManager.addDeclaration(declaration)

            func = LLVMGetNextFunction(func)
        }

        for (phiInstr in phiList) {
            statementHandler.handlePhi(phiInstr, tu)
        }

        LLVMContextDispose(ctx)

        return tu
    }

    /** Returns a pair of the name and symbol name of [valueRef]. */
    fun getNameOf(valueRef: LLVMValueRef): Pair<String, String> {
        var name = valueRef.name
        var symbolName = valueRef.symbolName

        // The name could be empty because of an unnamed variable. In this we need to apply some
        // dirty tricks to get its "name", unless we find a function that returns the slot number
        if (name == "") {
            name = guessSlotNumber(valueRef)
            symbolName = "%$name"
        }
        return Pair(name, symbolName)
    }

    fun typeOf(valueRef: LLVMValueRef): Type {
        val typeRef = LLVMTypeOf(valueRef)

        return typeFrom(typeRef)
    }

    internal fun typeFrom(
        typeRef: LLVMTypeRef,
        alreadyVisited: MutableMap<LLVMTypeRef, Type?> = mutableMapOf()
    ): Type {
        val typeStr = LLVMPrintTypeToString(typeRef).string
        if (typeStr in typeCache && typeCache[typeStr] != null) {
            return typeCache[typeStr]!!
        }
        if (typeRef in alreadyVisited && alreadyVisited[typeRef] != null) {
            return alreadyVisited[typeRef]!!
        } else if (typeRef in alreadyVisited) {
            // Recursive call but we can't resolve it.
            return UnknownType.getUnknownType()
        }
        alreadyVisited[typeRef] = null
        val res: Type =
            when (LLVMGetTypeKind(typeRef)) {
                LLVMVectorTypeKind, LLVMArrayTypeKind -> {
                    // var length = LLVMGetArrayLength(typeRef)
                    val elementType = typeFrom(LLVMGetElementType(typeRef), alreadyVisited)
                    elementType.reference(PointerType.PointerOrigin.ARRAY)
                }
                LLVMPointerTypeKind -> {
                    val elementType = typeFrom(LLVMGetElementType(typeRef), alreadyVisited)
                    elementType.reference(PointerType.PointerOrigin.POINTER)
                }
                LLVMStructTypeKind -> {
                    val record = declarationHandler.handleStructureType(typeRef, alreadyVisited)
                    record.toType() ?: UnknownType.getUnknownType()
                }
                else -> {
                    TypeParser.createFrom(typeStr, false)
                }
            }
        alreadyVisited[typeRef] = res
        typeCache[typeStr] = res
        return res
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        if (astNode is LLVMValueRef) {
            val code = LLVMPrintValueToString(astNode)

            return code.string
        } else if (astNode is LLVMBasicBlockRef) {
            return this.getCodeFromRawNode(LLVMBasicBlockAsValue(astNode))
        }

        return null
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        return null
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {}

    /** Determines if a struct with [name] exists in the scope. */
    fun isKnownStructTypeName(name: String): Boolean {
        return this.scopeManager
            .resolve<RecordDeclaration>(this.scopeManager.globalScope, true) { it.name == name }
            .isNotEmpty()
    }

    fun getOperandValueAtIndex(instr: LLVMValueRef, idx: Int): Expression {
        val operand = LLVMGetOperand(instr, idx)

        // there is also LLVMGetOperandUse, which might be of use to us

        return this.expressionHandler.handle(operand) as Expression
    }

    fun guessSlotNumber(valueRef: LLVMValueRef): String {
        val code = getCodeFromRawNode(valueRef)
        if (code?.contains("=") == true) {
            return code.split("=").firstOrNull()?.trim()?.trim('%') ?: ""
        } else {
            return ""
        }
    }
}

/**
 * Returns the name / identified of a value, if it is a variable, including the "scope" symbol,
 * i.e., % for local and @ for global variables.
 */
val LLVMValueRef.symbolName: String
    get() {
        val symbol =
            if (LLVMGetValueKind(this) == LLVMGlobalVariableValueKind) {
                "@"
            } else {
                "%"
            }

        return "$symbol${this.name}"
    }

/** Returns the name of a value using [LLVMGetValueName]. */
inline val LLVMValueRef.name: String
    get() {
        return LLVMGetValueName(this).string
    }

/**
 * Returns the opcode for an instruction using [LLVMGetInstructionOpcode].
 *
 * See also:
 * [llvm::Instruction::getOpCode()](https://llvm.org/doxygen/classllvm_1_1Instruction.html)
 */
inline val LLVMValueRef.opCode: Int
    get() {
        return LLVMGetInstructionOpcode(this)
    }
