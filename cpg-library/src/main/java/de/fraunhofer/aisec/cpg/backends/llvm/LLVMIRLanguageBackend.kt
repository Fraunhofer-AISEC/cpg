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
import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.TypeManager
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

        val mod = LLVMModuleCreateWithName(tu.name)

        inferMissingRecordTypes(tu)

        for (record in tu.declarations.filterIsInstance<RecordDeclaration>()) {
            generateStruct(mod, record)
        }

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

    /**
     * Handles individual declarations in a [DeclarationStatement]. For reach [Declaration] an
     * [`alloc`](https://llvm.org/docs/LangRef.html#alloca-instruction) instruction is generated,
     * which allocates an appropriate amount of memory and stores its into a named identifier.
     *
     * If the declaration has an initializer (see [HasInitializer]), its expression value (see
     * [generateExpression] will be stored into the memory address using the
     * [`store`](https://llvm.org/docs/LangRef.html#store-instruction) instruction.
     */
    private fun generateDeclarationStatement(stmt: DeclarationStatement) {
        // loop through all declarations
        for (declaration in stmt.declarations) {
            // we only support variable declarations for now
            (declaration as? VariableDeclaration)?.let {
                val type = typeOf(declaration)

                // just like LLVM, we are handling everything as an `alloca`. we might be able to
                // optimize this later, e.g., when variables can be directly created on the stack
                val valueRef = LLVMBuildAlloca(builder, type, declaration.name)

                variableMap[declaration] = valueRef

                // handle the initializer as a `store`, if any
                declaration.initializer?.let {
                    var expression = generateExpression(it)

                    if (expression.isNull) {
                        log.error(
                            "Initializer of store associated to a variable declaration resulted in an invalid LLVM value"
                        )
                    } else {
                        LLVMBuildStore(builder, expression, valueRef)
                    }
                }
            }
        }
    }

    /**
     * Generates a [`ret'](https://llvm.org/docs/LangRef.html#ret-instruction) instruction out of a
     * [ReturnStatement], with an optional return value.
     */
    private fun generateReturnStatement(returnStatement: ReturnStatement): LLVMValueRef {
        val valueRef =
            if (returnStatement.returnValue == null) {
                LLVMBuildRetVoid(builder)
            } else {
                LLVMBuildRet(builder, generateExpression(returnStatement.returnValue))
            }

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

        return when (expression.value) {
            null -> LLVMConstNull(type)
            is String -> {
                val nullTerminated = expression.value as String + '\u0000'
                LLVMConstString(nullTerminated, nullTerminated.length, 0)
            }
            else -> LLVMConstInt(type, (expression.value as Int).toLong(), 0)
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
        var valueRef = variableMap[ref.refersTo]

        if (valueRef == null) {
            log.error("Danger!")
            valueRef = LLVMConstNull(typeOf(ref))
        }

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

    /**
     * It seems that not all missing object types actually have an inferred record declaration. So
     * we create one for these types.
     *
     * TODO: What about types that are common across multiple translation units?
     */
    private fun inferMissingRecordTypes(tu: TranslationUnitDeclaration) {
        val typesToInfer =
            TypeManager.getInstance().firstOrderTypes.filter {
                it is ObjectType && it.recordDeclaration == null
                !it.isPrimitive
            }

        typesToInfer.forEach {
            val record = newRecordDeclaration(it.name, "struct", "")

            tu.addDeclaration(record)
        }
    }
}
