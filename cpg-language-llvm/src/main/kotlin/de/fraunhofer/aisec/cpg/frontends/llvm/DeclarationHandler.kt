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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLabelStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodParameterIn
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.types.Type
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
        map.put(LLVMTypeRef::class.java) { handleStructureType(it as LLVMTypeRef) }
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
            val namePair = lang.getNameOf(param)
            val paramName = namePair.first
            val paramSymbolName = namePair.second

            val type = lang.typeOf(param)

            // TODO: support variardic
            val decl = newMethodParameterIn(paramName, type, false, lang.getCodeFromRawNode(param))

            lang.scopeManager.addDeclaration(decl)
            lang.bindingsCache[paramSymbolName] = decl

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
            // use of a label. Its property substatement contains the original basic block, parsed
            // as a compound statement

            // Take the entry block as our body
            if (LLVMGetEntryBasicBlock(func) == bb) {
                functionDeclaration.body = stmt
            } else {
                // All further basic blocks are then added to the body wrapped in a label
                // statement
                var labelName = LLVMGetBasicBlockName(bb).string

                if (labelName.equals("")) {
                    // It seems that blocks are assigned an implicit counter-based label if it is
                    // not specified. We need to parse it from the string representation of the
                    // basic block
                    val bbStr = LLVMPrintValueToString(LLVMBasicBlockAsValue(bb)).string
                    val firstLine = bbStr.trim().split("\n")[0]
                    labelName = firstLine.substring(0, firstLine.indexOf(":"))
                }

                val labelStatement =
                    lang.labelMap.computeIfAbsent(labelName) {
                        val label = newLabelStatement(labelName)
                        label.name = labelName
                        label.label = labelName
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

    /**
     * Handles the parsing of [structure types](https://llvm.org/docs/LangRef.html#structure-types).
     * Member fields of structs in LLVM IR do not have names, so we need to assign dummy names for
     * easier reading, such s `field_0`.
     *
     * there are two different types of structs:
     * - identified structs, which have a name are explicitly declared
     * - literal structs, which do not have a name, but are structurally unique To emulate this
     * uniqueness, we create a [RecordDeclaration] for each literal struct and name it according to
     * its element types (see [getLiteralStructName]).
     */
    fun handleStructureType(
        typeRef: LLVMTypeRef,
        alreadyVisited: MutableMap<LLVMTypeRef, Type?> = mutableMapOf()
    ): RecordDeclaration {
        // if this is a literal struct, we will give it a pseudo name
        val name =
            if (LLVMIsLiteralStruct(typeRef) == 1) {
                getLiteralStructName(typeRef, alreadyVisited)
            } else {
                replaceCharsInName(LLVMGetStructName(typeRef).string)
            }

        // try to see, if the struct already exists as a record declaration
        var record =
            lang.scopeManager
                .resolve<RecordDeclaration>(lang.scopeManager.globalScope, true) { it.name == name }
                .firstOrNull()

        // if yes, return it
        if (record != null) {
            return record
        }

        record = newRecordDeclaration(name, "struct", "")

        lang.scopeManager.enterScope(record)

        val size = LLVMCountStructElementTypes(typeRef)

        for (i in 0 until size) {
            val a = LLVMStructGetTypeAtIndex(typeRef, i)
            val fieldType = lang.typeFrom(a, alreadyVisited)

            // there are no names, so we need to invent some dummy ones for easier reading
            val fieldName = "field_$i"

            val field = newFieldDeclaration(fieldName, fieldType, listOf(), "", null, null, false)

            lang.scopeManager.addDeclaration(field)
        }

        lang.scopeManager.leaveScope(record)

        // add it to the global scope
        lang.scopeManager.globalScope?.addDeclaration(record)

        return record
    }

    /**
     * A small internal helper function to retrieve a unique name for literal structs. The idea is
     * that, because they are unique according to their structure layout, we can commonly refer to
     * two literal structures with the same layout by the same name.
     */
    private fun getLiteralStructName(
        typeRef: LLVMTypeRef,
        alreadyVisited: MutableMap<LLVMTypeRef, Type?>
    ): String {
        val typeStr = LLVMPrintTypeToString(typeRef).string
        if (typeStr in lang.typeCache && lang.typeCache[typeStr] != null) {
            return lang.typeCache[typeStr]!!.name
        }

        var name = "literal"

        val size = LLVMCountStructElementTypes(typeRef)

        for (i in 0 until size) {
            val field = LLVMStructGetTypeAtIndex(typeRef, i)
            val fieldType = lang.typeFrom(field, alreadyVisited)

            name += "_${fieldType.typeName}"
        }

        return replaceCharsInName(name)
    }

    /**
     * Replaces some "dangerous" characters in the name of structures as they can be misinterpreted
     * by the [TypeParser].
     */
    private fun replaceCharsInName(name: String): String {
        return name.replace("[]", "Array")
            .replace("*", "Ptr")
            .replace("+", "%2B")
            .replace("&", "%26")
            .replace("#", "%23")
            .replace("<", "%3c")
            .replace(">", "%3e")
            .replace("@", "%40")
            .replace("[", "%5b")
            .replace("]", "%5d")
            .replace("{", "%7b")
            .replace("}", "%7d")
    }
}
