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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import org.bytedeco.javacpp.Pointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*

/**
 * This handler is in charge of parsing all LLVM IR language constructs that are related to
 * declarations, mainly functions and types.
 */
class DeclarationHandler(lang: LLVMIRLanguageFrontend) :
    Handler<Declaration, Pointer, LLVMIRLanguageFrontend>(::Declaration, lang) {
    init {
        map.put(LLVMValueRef::class.java) { handleValue(it as LLVMValueRef) }
        map.put(LLVMTypeRef::class.java) { lang.handleStructureType(it as LLVMTypeRef) }
    }

    private fun handleValue(value: LLVMValueRef): Declaration {
        return when (val kind = LLVMGetValueKind((value))) {
            LLVMFunctionValueKind -> handleFunction(value)
            LLVMGlobalVariableValueKind -> handleGlobal(value)
            else -> {
                log.error("Not handling declaration kind {} yet", kind)
                Declaration()
            }
        }
    }

    /**
     * Handles parsing of [global variables](https://llvm.org/docs/LangRef.html#global-variables).
     */
    private fun handleGlobal(valueRef: LLVMValueRef): Declaration {
        val name = LLVMGetValueName(valueRef).string

        // beware, that globals are always pointers to the type they specify. This already returns
        // the pointer type
        val type = lang.typeOf(valueRef)

        val variableDeclaration =
            newVariableDeclaration(name, type, lang.getCodeFromRawNode(valueRef), false)

        // cache binding
        lang.bindingsCache[valueRef.symbolName] = variableDeclaration

        val size = LLVMGetNumOperands(valueRef)
        // the first operand (if it exists) is an initializer
        if (size > 0) {
            val expr = lang.expressionHandler.handle(LLVMGetOperand(valueRef, 0))
            variableDeclaration.initializer = expr
        }

        return variableDeclaration
    }

    /**
     * Handles the parsing of [functions](https://llvm.org/docs/LangRef.html#functions). They can
     * either be pure declarations of (external) functions, which do not have a
     * [FunctionDeclaration.body] or complete definitions of functions including a body of at least
     * one basic block.
     */
    private fun handleFunction(func: LLVMValueRef): FunctionDeclaration {
        val name = LLVMGetValueName(func)

        val functionDeclaration = newFunctionDeclaration(name.string, lang.getCodeFromRawNode(func))

        // return types are a bit tricky, because the type of the function is a pointer to the
        // function type, which then has the return type in it
        val funcPtrType = LLVMTypeOf(func)
        val funcType = LLVMGetElementType(funcPtrType)
        val returnType = LLVMGetReturnType(funcType)

        functionDeclaration.type = lang.typeFrom(returnType)

        lang.scopeManager.enterScope(functionDeclaration)

        var param = LLVMGetFirstParam(func)
        while (param != null) {
            val type = lang.typeOf(param)

            // TODO: support variardic
            val decl =
                newMethodParameterIn(
                    LLVMGetValueName(param).string,
                    type,
                    false,
                    lang.getCodeFromRawNode(param)
                )

            lang.scopeManager.addDeclaration(decl)

            param = LLVMGetNextParam(param)
        }

        var bb = LLVMGetFirstBasicBlock(func)
        while (bb != null) {
            val stmt = lang.statementHandler.handle(bb)

            // Notice: we have one fundamental challenge here. Basic blocks in LLVM have a flat
            // hierarchy, meaning that a function has a list of basic blocks, of which one can
            // be unlabeled and is considered to be the entry. All other blocks need to have
            // labels and can be reached by branching or jump instructions. If all blocks are
            // labeled, then the first one is considered to be the entry.
            //
            // For our translation into the CPG we translate a basic block into a compound
            // statement, i.e. a list of statements. However, in the CPG structure, a function
            // definition does not have an entry, which specifies the first block, but it has a
            // *body*, which comprises *all* statements within the abstract syntax tree of
            // that function, hierarchically organized by compound statements. To emulate that, we
            // take the first basic block as our body and add subsequent blocks as statements to
            // the body. More specifically, we use the CPG node LabelStatement, which denotes the
            // use of a label, Its property substatement contains the original basic block, parsed
            // as a compound statement

            // Take the entry block as our body
            if (LLVMGetEntryBasicBlock(func) == bb) {
                functionDeclaration.body = stmt
            } else {
                // All further basic blocks are then added to the body wrapped in a label
                // statement
                // TODO: it seems that blocks are assigned an implicit counter-based label if it is
                // not specified
                val labelName = LLVMGetBasicBlockName(bb).string

                val labelStatement =
                    lang.labelMap.computeIfAbsent(labelName) {
                        val label = newLabelStatement(labelName)
                        label.name = labelName
                        label
                    }

                labelStatement.subStatement = stmt

                // add the label statement, containing this basic block as a compound statement to
                // our body (if we have none, which we should)
                (functionDeclaration.body as? CompoundStatement)?.addStatement(labelStatement)
            }

            bb = LLVMGetNextBasicBlock(bb)
        }

        lang.scopeManager.leaveScope(functionDeclaration)

        return functionDeclaration
    }
}
