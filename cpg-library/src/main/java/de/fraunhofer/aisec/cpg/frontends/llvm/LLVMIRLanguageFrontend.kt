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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.ByteBuffer
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*

class LLVMIRLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::") {
    override fun parse(file: File): TranslationUnitDeclaration {
        val mod: LLVMModuleRef = LLVMModuleRef()
        val ctx: LLVMContextRef = LLVMContextCreate()
        val buf: LLVMMemoryBufferRef = LLVMMemoryBufferRef()

        // not sure what is going to be here then
        val buf2 = ByteBuffer.allocate(10000)

        // not sure what is going to be here then
        val buf3 = ByteBuffer.allocate(10000)

        LLVMCreateMemoryBufferWithContentsOfFile(BytePointer(file.toPath().toString()), buf, buf2)
        val result = LLVMParseIRInContext(ctx, buf, mod, buf3)

        println(result)
        println(mod)

        var func = LLVMGetFirstFunction(mod)
        while (func != null) {
            var bb = LLVMGetFirstBasicBlock(func)
            while (bb != null) {
                var instr = LLVMGetFirstInstruction(bb)
                while (instr != null) {
                    println(instr)
                    instr = LLVMGetNextInstruction(instr)
                }
                bb = LLVMGetNextBasicBlock(bb)
            }
            func = LLVMGetNextFunction(func)
        }

        LLVMContextDispose(ctx)

        return TranslationUnitDeclaration()
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
