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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.CompressLLVMPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.configuration.RegisterExtraPass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.ByteBuffer
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

/**
 * Because we are using the C LLVM API, there are two possibly AST nodes that we need to consider:
 * [LLVMValueRef] and [LLVMBasicBlockRef]. Because they do not share any class hierarchy, we need to
 * resort to use [Pointer] as the AST node type here.
 */
@RegisterExtraPass(CompressLLVMPass::class)
class LLVMIRLanguageFrontend(ctx: TranslationContext, language: Language<LLVMIRLanguageFrontend>) :
    LanguageFrontend<Pointer, LLVMTypeRef>(ctx, language) {

    val statementHandler = StatementHandler(this)
    val declarationHandler = DeclarationHandler(this)
    val expressionHandler = ExpressionHandler(this)
    val typeCache = mutableMapOf<String, Type>()

    val phiList = mutableListOf<LLVMValueRef>()

    var ctxRef: LLVMContextRef? = null

    /**
     * This contains a cache binding between an LLVMValueRef (representing a variable) and its
     * [Declaration] in the graph. We need this, because this way we can look up and connect a
     * [Reference] to its [Declaration] already in the language frontend. This in turn is needed
     * because of the local/global system we cannot rely on the [SymbolResolver].
     */
    var bindingsCache = mutableMapOf<String, Declaration>()

    override fun parse(file: File): TranslationUnitDeclaration {
        var bench = Benchmark(this.javaClass, "Parsing sourcefile")
        // clear the bindings cache, because it is just valid within one module
        bindingsCache.clear()

        // these will be filled by our create and parse functions later and will be passed as
        // pointer
        val mod = LLVMModuleRef()
        val buf = LLVMMemoryBufferRef()

        // create a new LLVM context
        ctxRef = LLVMContextCreate()

        // disable opaque pointers, until all necessary new functions are available in the C API.
        // See https://llvm.org/docs/OpaquePointers.html
        LLVMContextSetOpaquePointers(ctxRef, 0)

        // allocate a buffer for a possible error message
        val errorMessage = ByteBuffer.allocate(10000)

        var result =
            LLVMCreateMemoryBufferWithContentsOfFile(
                BytePointer(file.toPath().toString()),
                buf,
                errorMessage,
            )
        if (result != 0) {
            // something went wrong
            val errorMsg = String(errorMessage.array())
            LLVMContextDispose(ctxRef)
            throw TranslationException("Could not create memory buffer: $errorMsg")
        }

        result = LLVMParseIRInContext(ctxRef, buf, mod, errorMessage)
        if (result != 0) {
            // something went wrong
            val errorMsg = String(errorMessage.array())
            LLVMContextDispose(ctxRef)
            throw TranslationException("Could not parse IR: $errorMsg")
        }
        bench.addMeasurement()
        bench = Benchmark(this.javaClass, "Transform to CPG")

        val tu = newTranslationUnitDeclaration(file.name)
        currentTU = tu

        // we need to set our translation unit as the global scope
        resetToGlobal(tu)

        // loop through globals
        var global = LLVMGetFirstGlobal(mod)
        while (global != null) {
            // try to parse the variable (declaration)
            val declaration = declarationHandler.handle(global)
            if (declaration != null) {
                declareSymbol(declaration)
                tu.declarations += declaration
            }

            global = LLVMGetNextGlobal(global)
        }

        // loop through functions
        var func = LLVMGetFirstFunction(mod)
        while (func != null) {
            // try to parse the function (declaration)
            val declaration = declarationHandler.handle(func)
            if (declaration != null) {
                declareSymbol(declaration)
                tu.declarations += declaration
            }

            func = LLVMGetNextFunction(func)
        }

        var counter = 0
        val flatAST = SubgraphWalker.flattenAST(tu).toMutableList()
        for (phiInstr in phiList) {
            statementHandler.handlePhi(phiInstr, tu, flatAST)
            counter++
        }

        LLVMContextDispose(ctxRef)
        bench.addMeasurement()

        return tu
    }

    override fun typeOf(type: LLVMTypeRef): Type {
        return typeOf(type, mutableMapOf())
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

        return typeOf(typeRef)
    }

    internal fun typeOf(
        typeRef: LLVMTypeRef,
        alreadyVisited: MutableMap<LLVMTypeRef, Type?> = mutableMapOf(),
    ): Type {
        val typeStr = LLVMPrintTypeToString(typeRef).string
        if (typeStr in typeCache) {
            val result = typeCache[typeStr]
            if (result != null) return result
        }
        if (typeRef in alreadyVisited) {
            return alreadyVisited[typeRef] ?: unknownType()
        }
        alreadyVisited[typeRef] = null
        val res: Type =
            when (LLVMGetTypeKind(typeRef)) {
                LLVMVectorTypeKind,
                LLVMArrayTypeKind -> {
                    // var length = LLVMGetArrayLength(typeRef)
                    val elementType = typeOf(LLVMGetElementType(typeRef), alreadyVisited)
                    elementType.array()
                }
                LLVMPointerTypeKind -> {
                    val elementType = typeOf(LLVMGetElementType(typeRef), alreadyVisited)
                    elementType.pointer()
                }
                LLVMStructTypeKind -> {
                    val record = declarationHandler.handleStructureType(typeRef, alreadyVisited)
                    record.toType()
                }
                LLVMFunctionTypeKind -> {
                    // we are not really interested in function types in this frontend
                    unknownType()
                }
                else -> {
                    objectType(typeStr)
                }
            }
        alreadyVisited[typeRef] = res
        typeCache[typeStr] = res
        return res
    }

    override fun codeOf(astNode: Pointer): String? {
        if (astNode is LLVMValueRef) {
            val code = LLVMPrintValueToString(astNode)

            return code.string
        } else if (astNode is LLVMBasicBlockRef) {
            return this.codeOf(LLVMBasicBlockAsValue(astNode))
        }

        return null
    }

    override fun locationOf(astNode: Pointer): PhysicalLocation? {
        return null
    }

    override fun setComment(node: Node, astNode: Pointer) {
        // There are no comments in LLVM
    }

    /** Determines if a struct with [name] exists in the scope. */
    fun isKnownStructTypeName(name: String): Boolean {
        return getRecordForName(Name(name), language) != null
    }

    fun getOperandValueAtIndex(instr: LLVMValueRef, idx: Int): Expression {
        val operand = LLVMGetOperand(instr, idx)

        // there is also LLVMGetOperandUse, which might be of use to us

        return this.expressionHandler.handle(operand) as Expression
    }

    fun guessSlotNumber(valueRef: LLVMValueRef): String {
        val code = codeOf(valueRef)
        return if (code?.contains("=") == true) {
            code.split("=").firstOrNull()?.trim()?.trim('%') ?: ""
        } else {
            ""
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
