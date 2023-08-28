/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.helpers.Util

class SpecificationHandler(frontend: GoLanguageFrontend) :
    GoHandler<Declaration?, GoStandardLibrary.Ast.Spec>(::ProblemDeclaration, frontend) {

    override fun handleNode(node: GoStandardLibrary.Ast.Spec): Declaration? {
        return when (node) {
            is GoStandardLibrary.Ast.ImportSpec -> handleImportSpec(node)
            is GoStandardLibrary.Ast.TypeSpec -> handleTypeSpec(node)
            is GoStandardLibrary.Ast.ValueSpec -> handleValueSpec(node)
            else -> {
                return handleNotSupported(node, node.goType)
            }
        }
    }

    private fun handleImportSpec(importSpec: GoStandardLibrary.Ast.ImportSpec): IncludeDeclaration {
        // We set the name of the include declaration to the imported name, i.e., the package name
        val name = importSpec.importName

        // We set the filename of the include declaration to the package path, i.e., its full path
        // including any module identifiers. This way we can match the include declaration back to
        // the namespace's path and name
        val filename = importSpec.path.value.removeSurrounding("\"")
        val include = newIncludeDeclaration(filename, rawNode = importSpec)
        include.name = parseName(name)

        var alias = importSpec.name?.name
        if (alias != null && alias != name) {
            var location = frontend.locationOf(importSpec)

            // If the name differs from the import name, we have an alias
            // Add an alias for the package on the current file
            location?.artifactLocation?.let {
                frontend.scopeManager.addAlias(it, Name(name), Name(alias))
            }
        }

        return include
    }

    private fun handleTypeSpec(spec: GoStandardLibrary.Ast.TypeSpec): Declaration {
        val type = spec.type
        val decl =
            when (type) {
                is GoStandardLibrary.Ast.StructType -> handleStructTypeSpec(spec, type)
                is GoStandardLibrary.Ast.InterfaceType -> handleInterfaceTypeSpec(spec, type)
                is GoStandardLibrary.Ast.FuncType -> handleFuncTypeSpec(spec, type)
                is GoStandardLibrary.Ast.Ident,
                is GoStandardLibrary.Ast.SelectorExpr,
                is GoStandardLibrary.Ast.MapType,
                is GoStandardLibrary.Ast.ArrayType,
                is GoStandardLibrary.Ast.StarExpr,
                is GoStandardLibrary.Ast.ChanType -> handleTypeDef(spec, type)
                else -> return ProblemDeclaration("not parsing type of type ${type.goType} yet")
            }

        return decl
    }

    private fun handleStructTypeSpec(
        typeSpec: GoStandardLibrary.Ast.TypeSpec,
        structType: GoStandardLibrary.Ast.StructType
    ): RecordDeclaration {
        val record = buildRecordDeclaration(structType, typeSpec.name.name, typeSpec)

        // Make sure to register the type
        frontend.typeManager.registerType(record.toType())

        return record
    }

    fun buildRecordDeclaration(
        structType: GoStandardLibrary.Ast.StructType,
        name: CharSequence,
        typeSpec: GoStandardLibrary.Ast.TypeSpec? = null,
    ): RecordDeclaration {
        val record = newRecordDeclaration(name, "struct", rawNode = typeSpec)

        frontend.scopeManager.enterScope(record)

        if (!structType.incomplete) {
            for (field in structType.fields.list) {
                val type = frontend.typeOf(field.type)

                // A field can also have no name, which means that it is embedded. In this case, it
                // can be accessed by the local name of its type and therefore we name the field
                // accordingly
                val fieldName =
                    if (field.names.isEmpty()) {
                        // Retrieve the root type local name
                        type.root.name.localName
                    } else {
                        field.names[0].name
                    }

                val decl = newFieldDeclaration(fieldName, type, rawNode = field)
                frontend.scopeManager.addDeclaration(decl)
            }
        }

        frontend.scopeManager.leaveScope(record)

        return record
    }

    private fun handleInterfaceTypeSpec(
        typeSpec: GoStandardLibrary.Ast.TypeSpec,
        interfaceType: GoStandardLibrary.Ast.InterfaceType
    ): Declaration {
        val record = newRecordDeclaration(typeSpec.name.name, "interface", rawNode = typeSpec)

        // Make sure to register the type
        frontend.typeManager.registerType(record.toType())

        frontend.scopeManager.enterScope(record)

        if (!interfaceType.incomplete) {
            for (field in interfaceType.methods.list) {
                val type = frontend.typeOf(field.type)

                // Even though this list is called "Methods", it contains all kinds
                // of things, so we need to proceed with caution. Only if the
                // "method" actually has a name, we declare a new method
                // declaration.
                if (field.names.isNotEmpty()) {
                    val method = newMethodDeclaration(field.names[0].name, rawNode = field)
                    method.type = type

                    frontend.scopeManager.addDeclaration(method)
                } else {
                    log.debug("Adding {} as super class of interface {}", type.name, record.name)
                    // Otherwise, it contains either types or interfaces. For now, we
                    // hope that it only has interfaces. We consider embedded
                    // interfaces as sort of super types for this interface.
                    record.addSuperClass(type)
                }
            }
        }

        frontend.scopeManager.leaveScope(record)

        return record
    }

    /**
     * // handleValueSpec handles parsing of an ast.ValueSpec, which is a variable declaration.
     * Since this can potentially declare multiple variables with one "spec", this returns a
     * [DeclarationSequence].
     */
    private fun handleValueSpec(
        valueSpec: GoStandardLibrary.Ast.ValueSpec,
    ): Declaration {
        // Increment iota value
        frontend.declCtx.iotaValue++

        // If we only have one initializer on the right side and multiple ones on the left side,
        // we are deconstructing a tuple
        val lenValues = valueSpec.values.size
        if (lenValues == 1 && lenValues != valueSpec.names.size) {
            // We need to construct a "tuple" declaration on the left side that holds all the
            // variables
            val tuple = TupleDeclaration()
            tuple.type = autoType()

            for (ident in valueSpec.names) {
                // We want to make sure that top-level declarations, i.e, the ones that are directly
                // in a namespace are FQNs. Otherwise we cannot resolve them properly when we access
                // them outside of the package.
                val fqn =
                    if (frontend.scopeManager.currentScope is NameScope) {
                        fqn(ident.name)
                    } else {
                        ident.name
                    }
                val decl = newVariableDeclaration(fqn, rawNode = valueSpec)

                if (valueSpec.type != null) {
                    decl.type = frontend.typeOf(valueSpec.type!!)
                } else {
                    decl.type = autoType()
                }

                if (valueSpec.values.isNotEmpty()) {
                    tuple.initializer = frontend.expressionHandler.handle(valueSpec.values[0])
                }

                // We need to manually add the variables to the scope manager
                frontend.scopeManager.addDeclaration(decl)

                tuple += decl
            }
            return tuple
        } else {
            val sequence = DeclarationSequence()

            for ((nameIdx, ident) in valueSpec.names.withIndex()) {
                // We want to make sure that top-level declarations, i.e, the ones that are directly
                // in a namespace are FQNs. Otherwise we cannot resolve them properly when we access
                // them outside of the package.
                val fqn =
                    if (frontend.scopeManager.currentScope is NameScope) {
                        fqn(ident.name)
                    } else {
                        ident.name
                    }
                val decl = newVariableDeclaration(fqn, rawNode = valueSpec)

                if (valueSpec.type != null) {
                    decl.type = frontend.typeOf(valueSpec.type!!)
                } else {
                    decl.type = autoType()
                }

                if (valueSpec.values.size > nameIdx) {
                    // the initializer is in the "Values" slice with the respective index
                    decl.initializer = frontend.expressionHandler.handle(valueSpec.values[nameIdx])
                }

                // If we are in a const declaration, we need to do something rather unusual.
                // If we have an initializer, we need to set this as the current const initializer,
                // because following specs will "inherit" the one from the previous line.
                //
                // Note: we cannot just take the already parsed initializer, but instead we need to
                // reparse the raw AST expression, so that `iota` gets evaluated differently for
                // each spec
                if (frontend.declCtx.currentDecl?.tok == 64) {
                    var initializerExpr = valueSpec.values.getOrNull(nameIdx)
                    if (initializerExpr != null) {
                        // Set the current initializer
                        frontend.declCtx.constInitializers[nameIdx] = initializerExpr
                    } else {
                        // Fetch expr from existing initializers
                        initializerExpr = frontend.declCtx.constInitializers[nameIdx]
                        if (initializerExpr == null) {
                            Util.errorWithFileLocation(
                                decl,
                                log,
                                "Const declaration is missing its initializer"
                            )
                        } else {
                            decl.initializer = frontend.expressionHandler.handle(initializerExpr)
                        }
                    }
                }

                sequence += decl
            }

            return sequence
        }
    }

    private fun handleFuncTypeSpec(
        spec: GoStandardLibrary.Ast.TypeSpec,
        type: GoStandardLibrary.Ast.FuncType
    ): Declaration {
        // We model function types as typedef's, so that we can resolve it later
        val funcType = frontend.typeOf(type)
        val typedef = newTypedefDeclaration(funcType, frontend.typeOf(spec.name), rawNode = spec)

        frontend.scopeManager.addTypedef(typedef)

        return typedef
    }

    private fun handleTypeDef(
        spec: GoStandardLibrary.Ast.TypeSpec,
        type: GoStandardLibrary.Ast.Expr
    ): Declaration {
        val targetType = frontend.typeOf(type)

        // We need to return either a type alias or a new type. See
        // https://go.dev/ref/spec#Type_identity
        return when {
            // When we have an assignment, we have *identical* types. We handle them as a typedef /
            // alias.
            spec.assign != 0 -> {
                val aliasType = frontend.typeOf(spec.name)
                val typedef = newTypedefDeclaration(targetType, aliasType, rawNode = spec)

                frontend.scopeManager.addTypedef(typedef)
                typedef
            }
            // Otherwise, we are creating a new type, which is *different*. Since Go allows to add
            // methods to these kind of types, we need to create them as a record declaration. We
            // use the special kind "overlay" to identity such types and put the target type in the
            // list of superclasses.
            else -> {
                val record = newRecordDeclaration(spec.name.name, "overlay")

                // We add the underlying type as the single super class
                record.superClasses = mutableListOf(targetType)
                record
            }
        }
    }
}
