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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.LabelStatement
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.ByteBuffer
import kotlin.collections.HashMap
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

class LLVMIRLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {

    val labelMap = HashMap<String, LabelStatement>()
    val statementHandler = StatementHandler(this)
    val declarationHandler = DeclarationHandler(this)

    var ctx: LLVMContextRef? = null

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
            val declaration = declarationHandler.handle(func)

            scopeManager.addDeclaration(declaration)

            func = LLVMGetNextFunction(func)
        }

        // TODO: actually clean them up, if we throw
        LLVMContextDispose(ctx)

        return tu
    }

    fun typeOf(valueRef: LLVMValueRef): Type {
        val typeRef = LLVMTypeOf(valueRef)

        return typeFrom(typeRef)
    }

    internal fun typeFrom(typeRef: LLVMTypeRef): Type {
        when (LLVMGetTypeKind(typeRef)) {
            LLVMArrayTypeKind -> {
                // var length = LLVMGetArrayLength(typeRef)
                val elementType = typeFrom(LLVMGetElementType(typeRef))

                return elementType.reference(PointerType.PointerOrigin.ARRAY)
            }
            LLVMPointerTypeKind -> {
                val elementType = typeFrom(LLVMGetElementType(typeRef))

                return elementType.reference(PointerType.PointerOrigin.POINTER)
            }
            LLVMStructTypeKind -> {
                var record = declarationHandler.handle(typeRef)

                return TypeParser.createFrom(record.name, false)
            }
            else -> {
                val typeBuf = LLVMPrintTypeToString(typeRef)

                // TODO: According to the doc LLVMDisposeMessage should be used, but it crashes

                var s = typeBuf.string

                // if the type is an identified type, i.e., it begins with a %, we get rid of the %
                // character otherwise, the CPG will not connect it to the type. Note that the type
                // name
                // itself also does not include the % character.

                if (s.startsWith("%")) {
                    s = s.substring(1)
                }

                return TypeParser.createFrom(s, false)
            }
        }
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
        return null
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {}
}
