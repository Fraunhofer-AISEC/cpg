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
package de.fraunhofer.aisec.cpg.backends.llvm

import de.fraunhofer.aisec.cpg.backends.LanguageBackend
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.IncompleteType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

class LLVMIRLanguageBackend : LanguageBackend<LLVMTypeRef>() {
    lateinit var ctx: LLVMContextRef
    lateinit var builder: LLVMBuilderRef

    private var typeMap: MutableMap<Type, LLVMTypeRef> = mutableMapOf()
    private var variableMap: MutableMap<Declaration, LLVMValueRef> = mutableMapOf()

    override fun generate(tu: TranslationUnitDeclaration) {
        ctx = LLVMContextCreate()
        builder = LLVMCreateBuilderInContext(ctx)

        var mod = LLVMModuleCreateWithName(tu.name)

        // TODO: this should have been inferred
        tu.addDeclaration(newRecordDeclaration("std.string", "struct", ""))

        for (record in tu.declarations.filterIsInstance<RecordDeclaration>()) {
            generateStruct(mod, record)
        }

        // LLVMPrintModuleToFile
        for (func in tu.declarations.filterIsInstance<FunctionDeclaration>()) {
            // check, if it is only a declaration for an existing definition and skip it
            if (!func.isDefinition && func.definition != null) {
                continue
            }

            generateFunction(mod, func)
        }

        println(LLVMPrintModuleToString(mod).string)
    }

    private fun generateStruct(mod: LLVMModuleRef, record: RecordDeclaration) {
        val ctx = LLVMGetModuleContext(mod)
        val structType = LLVMStructCreateNamed(ctx, record.name)
        LLVMStructSetBody(structType, LLVMTypeRef(), 0, 0)

        typeMap[record.toType()] = structType
    }

    private fun generateFunction(mod: LLVMModuleRef, func: FunctionDeclaration) {
        val returnType = this.typeOf(func)
        val functionType = LLVMFunctionType(returnType, LLVMTypeRef(), 0, 0)

        val valueRef = LLVMAddFunction(mod, func.name, functionType)

        func.body?.let {
            // handle the function body
            generateCompoundStatement(valueRef, it as CompoundStatement, "entry")
        }
    }

    private fun generateCompoundStatement(
        func: LLVMValueRef,
        comp: CompoundStatement,
        name: String
    ) {
        val bb = LLVMAppendBasicBlockInContext(ctx, func, name)

        LLVMPositionBuilderAtEnd(builder, bb)

        for (stmt in comp.statements) {
            generateStatement(stmt)
        }
    }

    private fun generateStatement(stmt: Statement) {
        when (stmt) {
            is ReturnStatement -> generateReturnStatement(stmt)
            is DeclarationStatement -> generateDeclarationStatement(stmt)
            is Expression -> generateExpression(stmt)
        }
    }

    private fun generateDeclarationStatement(stmt: DeclarationStatement) {
        // TODO: support multiple declarations
        var decl = stmt.singleDeclaration

        // only variable declarations for now
        (decl as? VariableDeclaration)?.let {
            val type = typeOf(decl)

            // just like LLVM, we are handling everything as an alloca. we might be able to optimize
            // this later, e.g., when variables can be directly created on the stack
            val valueRef = LLVMBuildAlloca(builder, type, decl.name)

            variableMap[decl] = valueRef

            // handle the initializer as a store, if any
            decl.initializer?.let { LLVMBuildStore(builder, generateExpression(it), valueRef) }
        }
    }

    private fun generateReturnStatement(returnStatement: ReturnStatement): LLVMValueRef {
        val valueRef = LLVMBuildRetVoid(builder)

        // LLVMInsertIntoBuilder(builder, valueRef)

        return valueRef
    }

    private fun generateExpression(expression: Expression): LLVMValueRef {
        return when (expression) {
            is BinaryOperator -> generateBinaryOperator(expression)
            is DeclaredReferenceExpression -> generateDeclRef(expression)
            is Literal<*> -> generateLiteral(expression)
            else -> LLVMValueRef()
        }
    }

    private fun generateLiteral(expression: Literal<*>): LLVMValueRef {
        val type = typeOf(expression)

        return if (expression.value == null) {
            LLVMConstNull(type)
        } else {
            LLVMConstInt(type, (expression.value as Int).toLong(), 0)
        }
    }

    private fun generateBinaryOperator(expression: BinaryOperator): LLVMValueRef {
        if (expression.operatorCode == "=") {
            val lhs = expression.lhs

            if (lhs is DeclaredReferenceExpression) {
                log.debug("Trying to store something in {}", lhs.name)

                val valueRef = variableMap[lhs.refersTo]

                valueRef?.let {
                    return LLVMBuildStore(builder, generateExpression(expression.rhs), valueRef)
                }
            } else {
                log.error(
                    "Left-hand side of assignment binary operation is not a reference. This assignment seems invalid."
                )
            }
        } else if (expression.operatorCode == "*") {
            log.debug("Handling mul")

            return LLVMBuildMul(
                builder,
                generateExpression(expression.lhs),
                generateExpression(expression.rhs),
                ""
            )
        } else if (expression.operatorCode == ">>") {
            return LLVMBuildLShr(
                builder,
                generateExpression(expression.lhs),
                generateExpression(expression.rhs),
                ""
            )
        }

        log.error(
            "Not handling operatorCode {}. This will probably crash now",
            expression.operatorCode
        )
        return LLVMValueRef()
    }

    private fun generateDeclRef(ref: DeclaredReferenceExpression): LLVMValueRef {
        val valueRef = variableMap[ref.refersTo]

        // in order to access this variable, we need to load it
        return LLVMBuildLoad(builder, valueRef, "")
    }

    override fun typeOf(node: HasType): LLVMTypeRef {
        return typeFrom(node.type)
    }

    override fun typeFrom(type: Type): LLVMTypeRef {
        return if (type is IncompleteType && type.name == "void") {
            LLVMVoidType()
        } else if (type is PointerType) {
            LLVMPointerType(typeFrom(type.elementType), 0)
        } else if (type is ObjectType && type.name == "int") {
            LLVMIntType(32)
        } else if (type is ObjectType) {
            // try to look it up in the typeMap
            typeMap[type] ?: LLVMIntType(64)
        } else {
            log.error("Not translating type {} yet. Assuming i64", type.typeName)
            LLVMIntType(64)
        }
    }
}
