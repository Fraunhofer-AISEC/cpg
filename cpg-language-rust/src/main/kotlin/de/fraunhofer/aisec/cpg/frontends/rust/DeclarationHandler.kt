/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import uniffi.cpgrust.RsAssocItem
import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsFieldList
import uniffi.cpgrust.RsFn
import uniffi.cpgrust.RsImpl
import uniffi.cpgrust.RsItem
import uniffi.cpgrust.RsModule
import uniffi.cpgrust.RsParam
import uniffi.cpgrust.RsPat
import uniffi.cpgrust.RsStruct
import uniffi.cpgrust.RsType

class DeclarationHandler(frontend: RustLanguageFrontend) :
    RustHandler<Declaration, RsAst.RustItem>(::ProblemDeclaration, frontend) {
    override fun handleNode(node: RsAst.RustItem): Declaration {
        val item = node.v1
        return when (item) {
            is RsItem.Fn -> handleFunctionDeclaration(item.v1)
            is RsItem.Param -> handleParameterDeclaration(item.v1)
            is RsItem.Module -> handleModule(item.v1)
            is RsItem.Struct -> handleStruct(item.v1)
            is RsItem.Impl -> handleImpl(item.v1)
            else -> handleNotSupported(node, item::class.simpleName ?: "")
        }
    }

    private fun handleFunctionDeclaration(fn: RsFn): FunctionDeclaration {
        val name = frontend.scopeManager.currentNamespace.fqn(fn.name ?: "")
        val raw = RsAst.RustItem(RsItem.Fn(fn))

        val function =
            fn.paramList?.selfParam?.let {
                newMethodDeclaration(
                        name,
                        recordDeclaration = frontend.scopeManager.currentRecord,
                        rawNode = raw,
                    )
                    .apply {
                        val type = it.ty?.let { frontend.typeOf(it) }
                        this.parameters +=
                            newParameterDeclaration(
                                it.astNode.text,
                                type = type ?: unknownType(),
                                rawNode = RsAst.RustItem(RsItem.SelfParam(it)),
                            )
                        frontend.scopeManager.addDeclaration(this.parameters.last())
                    }
            } ?: newFunctionDeclaration(name, rawNode = raw)

        fn.retType?.let { function.type = frontend.typeOf(it) }

        frontend.scopeManager.enterScope(function)
        for (param in fn.paramList?.params ?: listOf()) {
            function.parameters += handleParameterDeclaration(param) as ParameterDeclaration
        }

        fn.body?.let { function.body = frontend.expressionHandler.handleBlockExpr(it) }

        frontend.scopeManager.leaveScope(function)
        return function
    }

    private fun handleParameterDeclaration(param: RsParam): Declaration {

        val type = param.ty?.let { frontend.typeOf(it) }

        val name = (param.pat as? RsPat.IdentPat)?.v1?.name ?: ""

        val parameter =
            newParameterDeclaration(
                name,
                type = type ?: unknownType(),
                rawNode = RsAst.RustItem(RsItem.Param(param)),
            )

        frontend.scopeManager.addDeclaration(parameter)

        return parameter
    }

    private fun handleModule(module: RsModule): Declaration {
        val namespace =
            newNamespaceDeclaration(
                module.name ?: "",
                rawNode = RsAst.RustItem(RsItem.Module(module)),
            )
        frontend.scopeManager.enterScope(namespace)

        for (item in module.items) {
            val declaration = handle(RsAst.RustItem(item))
            frontend.scopeManager.addDeclaration(declaration)
            namespace.declarations += declaration
        }

        frontend.scopeManager.leaveScope(namespace)
        return namespace
    }

    private fun handleStruct(struct: RsStruct): Declaration {
        val raw = RsAst.RustItem(RsItem.Struct(struct))

        val record = newRecordDeclaration(struct.name ?: "", "struct", raw)

        frontend.scopeManager.enterScope(record)

        struct.fieldList?.let { fields ->
            when (fields) {
                is RsFieldList.RecordFieldList -> {
                    val rfs = fields.v1
                    for (rField in rfs.fields) {
                        val type = rField.fieldType?.let { frontend.typeOf(it) }

                        val field = newFieldDeclaration(rField.name ?: "", type ?: unknownType())

                        field.initializer =
                            rField.expr?.let {
                                frontend.expressionHandler.handle(RsAst.RustExpr(it))
                            }
                        record.fields += field
                        frontend.scopeManager.addDeclaration(field)
                    }
                }
                is RsFieldList.TupleFieldList -> {
                    var fieldCounter = 0
                    val tfs = fields.v1
                    for (tField in tfs.fields) {
                        val type = tField.fieldType?.let { frontend.typeOf(it) }

                        val field =
                            newFieldDeclaration(fieldCounter.toString(), type ?: unknownType())

                        record.fields += field
                        frontend.scopeManager.addDeclaration(field)
                        fieldCounter++
                    }
                }
                else -> {}
            }
        }

        frontend.scopeManager.leaveScope(record)

        return record
    }

    private fun handleImpl(impl: RsImpl): Declaration {
        val implTarget = impl.pathTypes.last()
        // The last part of the implementation block path is the target of the implementation, the
        // record to add definitions to
        var name = implTarget.path?.segment?.nameRef?.let { parseName(it.text) } ?: Name("")

        val scope =
            frontend.scopeManager.lookupScope(
                parseName(
                    frontend.scopeManager.currentNamespace
                        .fqn(name.toString(), delimiter = name.delimiter)
                        .toString()
                )
            )
        val currentScope = frontend.scopeManager.currentScope

        scope?.let { frontend.scopeManager.enterScope(it) }

        val scopedNode = scope?.astNode

        // If the implementation blocks pathTypes is longer than one element, a trait was
        // implemented for the record.
        if (scopedNode is RecordDeclaration && impl.pathTypes.size > 1) {
            val implInterface = impl.pathTypes.first()
            name = implInterface.path?.segment?.nameRef?.let { parseName(it.text) } ?: Name("")
            scopedNode.superClasses +=
                objectType(name, rawNode = RsAst.RustType(RsType.PathType(implInterface)))
        }

        val extensionDeclaration = newExtensionDeclaration(name, RsAst.RustItem(RsItem.Impl(impl)))

        frontend.scopeManager.enterScope(extensionDeclaration)

        for (item in impl.items) {
            when (item) {
                is RsAssocItem.Fn -> {
                    val func = handleFunctionDeclaration(item.v1)

                    if (scopedNode is RecordDeclaration) {
                        frontend.scopeManager.addDeclaration(func)
                    }
                    extensionDeclaration.declarations += func
                }
                is RsAssocItem.Const -> {}
                is RsAssocItem.TypeAlias -> {}
                is RsAssocItem.MacroCall -> {}
            }
        }

        scope?.let {
            frontend.scopeManager.leaveScope(it)
            frontend.scopeManager.enterScope(currentScope)
        }

        frontend.scopeManager.leaveScope(extensionDeclaration)

        return extensionDeclaration
    }
}
