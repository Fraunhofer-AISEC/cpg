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
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import uniffi.cpgrust.RsAssocItem
import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsConst
import uniffi.cpgrust.RsEnum
import uniffi.cpgrust.RsFieldList
import uniffi.cpgrust.RsFn
import uniffi.cpgrust.RsImpl
import uniffi.cpgrust.RsItem
import uniffi.cpgrust.RsModule
import uniffi.cpgrust.RsParam
import uniffi.cpgrust.RsPat
import uniffi.cpgrust.RsPath
import uniffi.cpgrust.RsStruct
import uniffi.cpgrust.RsTrait
import uniffi.cpgrust.RsType
import uniffi.cpgrust.RsUse
import uniffi.cpgrust.RsUseTree

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
            is RsItem.Trait -> handleTrait(item.v1)
            is RsItem.Const -> handleConst(item.v1)
            is RsItem.Use -> handleUse(item.v1)
            is RsItem.Enum -> handleEnum(item.v1)
            else -> handleNotSupported(node, item::class.simpleName ?: "")
        }
    }

    private fun handleFunctionDeclaration(fn: RsFn): Function {
        val name = frontend.scopeManager.currentNamespace.fqn(fn.name ?: "")
        val raw = RsAst.RustItem(RsItem.Fn(fn))

        val function =
            fn.paramList?.selfParam?.let {
                newMethod(
                        name,
                        recordDeclaration = frontend.scopeManager.currentRecord,
                        rawNode = raw,
                    )
                    .apply {
                        val type = it.ty?.let { frontend.typeOf(it) }
                        this.parameters +=
                            newParameter(
                                // Todo We need to handle destructuring in a parameter properly
                                it.astNode.text,
                                type = type ?: unknownType(),
                                rawNode = RsAst.RustItem(RsItem.SelfParam(it)),
                            )
                    }
            } ?: newFunction(name, rawNode = raw)

        frontend.scopeManager.addDeclaration(function)

        fn.retType?.let { function.type = frontend.typeOf(it) }

        frontend.scopeManager.enterScope(function)

        // Adding implicitly created parameters to the scope
        function.parameters.forEach { frontend.scopeManager.addDeclaration(it) }

        for (param in fn.paramList?.params ?: listOf()) {
            function.parameters += handleParameterDeclaration(param) as Parameter
        }

        fn.body?.let { function.body = frontend.expressionHandler.handleBlockExpr(it) }

        frontend.scopeManager.leaveScope(function)
        return function
    }

    private fun handleParameterDeclaration(param: RsParam): Declaration {

        val type = param.ty?.let { frontend.typeOf(it) }

        val name = (param.pat as? RsPat.IdentPat)?.v1?.name ?: ""

        val parameter =
            newParameter(
                name,
                type = type ?: unknownType(),
                rawNode = RsAst.RustItem(RsItem.Param(param)),
            )

        frontend.scopeManager.addDeclaration(parameter)

        return parameter
    }

    private fun handleModule(module: RsModule): Declaration {
        val namespace =
            newNamespace(module.name ?: "", rawNode = RsAst.RustItem(RsItem.Module(module)))

        frontend.scopeManager.enterScope(namespace)

        for (item in module.items) {
            val declaration = handle(RsAst.RustItem(item))
            ((declaration as? DeclarationSequence)?.declarations ?: listOf(declaration)).forEach {
                declItem ->
                frontend.scopeManager.addDeclaration(declItem)
                namespace.declarations += declItem
            }
        }

        frontend.scopeManager.leaveScope(namespace)
        return namespace
    }

    private fun handleStruct(struct: RsStruct): Declaration {
        val raw = RsAst.RustItem(RsItem.Struct(struct))

        val record = newRecord(struct.name ?: "", "struct", raw)

        frontend.scopeManager.enterScope(record)

        struct.fieldList?.let { fields ->
            when (fields) {
                is RsFieldList.RecordFieldList -> {
                    val rfs = fields.v1
                    for (rField in rfs.fields) {
                        val type = rField.fieldType?.let { frontend.typeOf(it) }

                        val field = newField(rField.name ?: "", type ?: unknownType())

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

                        val field = newField(fieldCounter.toString(), type ?: unknownType())

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

    private fun handleTrait(trait: RsTrait): Declaration {
        val raw = RsAst.RustItem(RsItem.Trait(trait))

        val record = newRecord(trait.name ?: "", "trait", raw)

        frontend.scopeManager.enterScope(record)

        for (item in trait.items) {
            when (item) {
                is RsAssocItem.Fn -> {
                    val func = handleFunctionDeclaration(item.v1)
                    record.addDeclaration(func)
                    frontend.scopeManager.addDeclaration(func)
                }
                is RsAssocItem.Const -> {
                    val const =
                        frontend.declarationHandler.handleNode(
                            RsAst.RustItem(RsItem.Const(item.v1))
                        )
                    // Todo Add Consts
                }
                is RsAssocItem.TypeAlias -> {} // Todo handle Type Alias
                is RsAssocItem.MacroCall -> {} // Todo handle Macro Calls
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
        if (scopedNode is Record && impl.pathTypes.size > 1) {
            val implInterface = impl.pathTypes.first()
            name = implInterface.path?.segment?.nameRef?.let { parseName(it.text) } ?: Name("")
            scopedNode.implementedInterfaces +=
                objectType(name, rawNode = RsAst.RustType(RsType.PathType(implInterface)))
        }

        val extensionDeclaration = newExtension(name, RsAst.RustItem(RsItem.Impl(impl)))

        frontend.scopeManager.enterScope(extensionDeclaration)

        for (item in impl.items) {
            when (item) {
                is RsAssocItem.Fn -> {
                    val func = handleFunctionDeclaration(item.v1)

                    if (scopedNode is Record) {
                        frontend.scopeManager.addDeclaration(func)
                    }
                    extensionDeclaration.declarations += func
                }
                is RsAssocItem.Const -> {
                    val const =
                        frontend.declarationHandler.handleNode(
                            RsAst.RustItem(RsItem.Const(item.v1))
                        )
                    extensionDeclaration.addDeclaration(const)
                    frontend.scopeManager.addDeclaration(const)
                }
                is RsAssocItem.TypeAlias -> {} // Todo handle type alias
                is RsAssocItem.MacroCall -> {} // Todo handle macro call
            }
        }

        scope?.let {
            frontend.scopeManager.leaveScope(it)
            frontend.scopeManager.enterScope(currentScope)
        }

        frontend.scopeManager.leaveScope(extensionDeclaration)

        return extensionDeclaration
    }

    private fun handleUse(use: RsUse): Declaration {
        val raw = RsAst.RustItem(RsItem.Use(use))

        var imports = use.useTree?.let { flattenUseTree(rsUseTree = it) }

        imports?.forEach { import ->
            // After Flattening we must check if the first part of the import name starts with
            // crate, self or super
            // If they do we replace them with the concrete name they represent in the context
            import.import = frontend.handleKeywordsInNames(import.import) ?: Name("")
        }

        val declarations = DeclarationSequence()

        for (import in imports ?: listOf()) {
            declarations += import
            frontend.scopeManager.addDeclaration(import)
        }

        return if (declarations.isSingle) declarations.first() else declarations
    }

    private fun flattenUseTree(
        prefixName: Name? = null,
        rsUseTree: RsUseTree,
    ): MutableList<Import> {
        val imports = mutableListOf<Import>()

        var importName = rsUseTree.path?.let { handlePathForImport(it) } ?: newName("")

        importName = newName(importName, namespace = prefixName)

        rsUseTree.useTrees.forEach {
            flattenUseTree(prefixName = importName, it).let { imports += it }
        }

        if (rsUseTree.useTrees.isEmpty()) {
            // Todo Consider how we have to handle _
            val alias = rsUseTree.rename?.let { language.parseName(it) }
            imports +=
                when (importName.localName) {
                    "self" ->
                        newImport(
                            importName.parent ?: newName(""),
                            alias = alias,
                            style = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                            rawNode = RsAst.RustUseTree(rsUseTree),
                        )
                    else ->
                        newImport(
                            importName,
                            alias = alias,
                            style =
                                if (rsUseTree.star) ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE
                                else ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                            rawNode = RsAst.RustUseTree(rsUseTree),
                        )
                }
        }

        return imports
    }

    fun handleEnum(enum: RsEnum): Declaration {
        val raw = RsAst.RustItem(RsItem.Enum(enum))

        val enumRecord = newRecord(enum.name ?: "", "enum", raw)

        frontend.scopeManager.enterScope(enumRecord)

        for (variant in enum.variants) {
            val variantRecord = newRecord(variant.name ?: "", "variant", RsAst.RustVariant(variant))

            frontend.scopeManager.enterScope(variantRecord)

            // Handle discriminant expressions (integer discriminants)
            if (variant.expr.isNotEmpty()) {
                // Variant has an explicit discriminant value
                val discriminantField = newField("discriminant", primitiveType("isize"))
                discriminantField.initializer =
                    frontend.expressionHandler.handle(RsAst.RustExpr(variant.expr.first()))
                variantRecord.fields += discriminantField
                frontend.scopeManager.addDeclaration(discriminantField)
            }

            // Todo We have an issue with variants where these cannot always be used as a type.
            // While normally this would
            // not be a problem, the type of the invariant captures the type of struct references
            // and we get an incorrect
            // type if we use records of the same type name, in an Enum with a variant that has the
            // same type name

            // Handle field lists
            for (fieldList in variant.fields) {
                when (fieldList) {
                    is RsFieldList.RecordFieldList -> {
                        // Named fields - like struct with named fields
                        val rfs = fieldList.v1
                        for (rField in rfs.fields) {
                            val type = rField.fieldType?.let { frontend.typeOf(it) }
                            val field = newField(rField.name ?: "", type ?: unknownType())
                            field.initializer =
                                rField.expr?.let {
                                    frontend.expressionHandler.handle(RsAst.RustExpr(it))
                                }
                            variantRecord.fields += field
                            frontend.scopeManager.addDeclaration(field)
                        }
                    }
                    is RsFieldList.TupleFieldList -> {
                        // Unnamed fields - like struct with unnamed fields
                        var fieldCounter = 0
                        val tfs = fieldList.v1
                        for (tField in tfs.fields) {
                            val type = tField.fieldType?.let { frontend.typeOf(it) }
                            val field = newField(fieldCounter.toString(), type ?: unknownType())
                            variantRecord.fields += field
                            frontend.scopeManager.addDeclaration(field)
                            fieldCounter++
                        }
                    }
                }
            }

            variantRecord.implementedInterfaces += objectType(enumRecord.name, rawNode = raw)

            frontend.scopeManager.leaveScope(variantRecord)

            // Add the variant record as a sub-record of the enum
            enumRecord.addDeclaration(variantRecord)
            frontend.scopeManager.addDeclaration(variantRecord)
        }

        frontend.scopeManager.leaveScope(enumRecord)

        return enumRecord
    }

    private fun handlePathForImport(rsPath: RsPath): Name? {
        // In the case of imports we do not have to handle return type, type args, type anchor and
        // type args

        val qualifierName =
            rsPath.qualifier.firstOrNull()?.let { qualifier -> handlePathForImport(qualifier) }

        return rsPath.segment?.nameRef?.text?.let { text ->
            newName(text, namespace = qualifierName)
        }
    }

    private fun handleConst(const: RsConst): Declaration {
        val raw = RsAst.RustItem(RsItem.Const(const))

        val name = const.name ?: ""

        val type = const.ty?.let { frontend.typeOf(it) }

        return newVariable(name, type ?: unknownType(), rawNode = raw).apply {
            const.expr.firstOrNull()?.let {
                this.initializer = frontend.expressionHandler.handle(RsAst.RustExpr(it))
            }
            frontend.scopeManager.addDeclaration(this)
        }
    }
}
