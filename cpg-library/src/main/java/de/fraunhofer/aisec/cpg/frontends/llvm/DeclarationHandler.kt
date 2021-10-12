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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

class DeclarationHandler(lang: LLVMIRLanguageFrontend) :
    Handler<Declaration, LLVMValueRef, LLVMIRLanguageFrontend>(::Declaration, lang) {
    init {
        map.put(LLVMValueRef::class.java, ::handleValue)
    }

    fun handleValue(value: LLVMValueRef): Declaration {
        return handleFunction(value)
    }

    private fun handleFunction(func: LLVMValueRef): FunctionDeclaration {
        val name = LLVMGetValueName(func)

        val functionDeclaration = newFunctionDeclaration(name.string, lang.getCodeFromRawNode(func))

        functionDeclaration.type = lang.typeOf(func)

        lang.scopeManager.enterScope(functionDeclaration)

        var bb = LLVMGetFirstBasicBlock(func)
        while (bb != null) {
            val stmt = lang.statementHandler.handle(bb)

            // TODO: there are probably more than one basic block in a function, for now just take
            // one
            functionDeclaration.body = stmt

            bb = LLVMGetNextBasicBlock(bb)
        }

        lang.scopeManager.leaveScope(functionDeclaration)

        return functionDeclaration
    }
}
