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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*

class SpecificationHandler(frontend: GoLanguageFrontend) :
    Handler<Declaration, GoStandardLibrary.Ast.Spec, GoLanguageFrontend>(
        ::ProblemDeclaration,
        frontend
    ) {

    init {
        map[GoStandardLibrary.Ast.ImportSpec::class.java] = HandlerInterface {
            handleImportSpec(it as GoStandardLibrary.Ast.ImportSpec)
        }
        map[GoStandardLibrary.Ast.TypeSpec::class.java] = HandlerInterface {
            handleTypeSpec(it as GoStandardLibrary.Ast.TypeSpec)
        }
        map[GoStandardLibrary.Ast.ValueSpec::class.java] = HandlerInterface {
            handleValueSpec(it as GoStandardLibrary.Ast.ValueSpec)
        }
        map.put(GoStandardLibrary.Ast.Spec::class.java, ::handleNode)
    }

    private fun handleNode(spec: GoStandardLibrary.Ast.Spec): Declaration {
        val message = "Not parsing specification of type ${spec.goType} yet"
        log.error(message)

        return newProblemDeclaration(message)
    }

    private fun handleImportSpec(importSpec: GoStandardLibrary.Ast.ImportSpec): IncludeDeclaration {
        // We set the name of the include declaration to the imported name, i.e., the package name
        val name = frontend.getImportName(importSpec)
        // We set the filename of the include declaration to the package path, i.e., its full path
        // including any module identifiers. This way we can match the include declaration back to
        // the namespace's path and name
        val filename = importSpec.path.value.removeSurrounding("\"")
        val include = newIncludeDeclaration(filename, rawNode = importSpec)
        include.name = parseName(name)

        return include
    }

    private fun handleTypeSpec(spec: GoStandardLibrary.Ast.TypeSpec): Declaration {
        val type = spec.type
        val decl =
            when (type) {
                is GoStandardLibrary.Ast.StructType -> handleStructTypeSpec(spec, type)
                is GoStandardLibrary.Ast.InterfaceType -> handleInterfaceTypeSpec(spec, type)
                else -> return ProblemDeclaration("not parsing type of type ${spec.goType} yet")
            }

        return decl
    }

    private fun handleStructTypeSpec(
        typeSpec: GoStandardLibrary.Ast.TypeSpec,
        structType: GoStandardLibrary.Ast.StructType
    ): RecordDeclaration {
        val record = newRecordDeclaration(typeSpec.name.name, "struct", rawNode = typeSpec)

        frontend.scopeManager.enterScope(record)

        if (!structType.incomplete) {
            for (field in structType.fields.list) {
                // a field can also have no name, which means that it is embedded, not quite
                // sure yet how to handle this, but since the embedded field can be accessed
                // by its type, it could make sense to name the field according to the type
                val type = frontend.typeOf(field.type)

                val name =
                    if (field.names.isEmpty()) {
                        // Retrieve the root type name
                        type.root.name.toString()
                    } else {
                        field.names[0].name
                    }

                val decl = newFieldDeclaration(name, type, rawNode = field)
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
     * // handleValueSpec handles parsing of an ast.ValueSpec, which is a variable // declaration.
     * Since this can potentially declare multiple variables with one // "spec", this returns a
     * [DeclarationSequence].
     */
    private fun handleValueSpec(valueSpec: GoStandardLibrary.Ast.ValueSpec): DeclarationSequence {
        val sequence = DeclarationSequence()

        for ((idx, ident) in valueSpec.names.withIndex()) {
            val decl = newVariableDeclaration(ident.name, rawNode = valueSpec)

            if (valueSpec.type != null) {
                decl.type = frontend.typeOf(valueSpec.type!!)
            } else {
                decl.type = autoType()
            }

            // There could either be no initializers, otherwise the amount of values
            // must match the names
            val lenValues = valueSpec.values.size
            if (lenValues != 0 && lenValues != valueSpec.names.size) {
                log.error(
                    "Number of initializers does not match number of names. Initializers might be incomplete"
                )
            }

            // The initializer is in the "Values" slice with the respective index
            if (valueSpec.values.size > idx) {
                decl.initializer = frontend.expressionHandler.handle(valueSpec.values[idx])
            }

            sequence += decl
        }

        return sequence
    }
}
