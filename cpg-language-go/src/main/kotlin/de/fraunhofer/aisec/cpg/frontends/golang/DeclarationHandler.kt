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
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

class DeclarationHandler(frontend: GoLanguageFrontend) :
    GoHandler<Declaration?, GoStandardLibrary.Ast.Decl>(::Problem, frontend) {

    override fun handleNode(node: GoStandardLibrary.Ast.Decl): Declaration? {
        return when (node) {
            is GoStandardLibrary.Ast.FuncDecl -> handleFuncDecl(node)
            is GoStandardLibrary.Ast.GenDecl -> handleGenDecl(node)
            else -> {
                handleNotSupported(node, node.goType)
            }
        }
    }

    private fun handleFuncDecl(funcDecl: GoStandardLibrary.Ast.FuncDecl): Function {
        val recv = funcDecl.recv
        val func =
            if (recv != null) {
                val recvField = recv.list.firstOrNull()
                val recordType = recvField?.type?.let { frontend.typeOf(it) } ?: unknownType()
                // The record type is an unqualified type, so we need to use the current namespace
                // to make a FQN out of it
                val fqnRecord =
                    frontend.scopeManager.currentNamespace.fqn(recordType.root.name.localName)

                val method =
                    newMethod(Name(funcDecl.name.name, fqnRecord), rawNode = funcDecl)

                // The name of the Go receiver is optional. In fact, if the name is not
                // specified we probably do not need any receiver variable at all,
                // because the syntax is only there to ensure that this method is part
                // of the struct, but it is not modifying the receiver.
                if (recvField?.names?.isNotEmpty() == true) {
                    method.receiver =
                        newVariable(
                            recvField.names[0].name,
                            recordType,
                            rawNode = recvField,
                        )
                }

                if (recordType !is UnknownType) {
                    // TODO: this will only find methods within the current translation unit.
                    //  this is a limitation that we have for C++ as well
                    val record =
                        frontend.scopeManager.lookupScope(fqnRecord)?.astNode as? Record

                    // Enter scope of record, so we can later resolve this correctly. We do NOT add
                    // the method to the AST methods property because it is declared outside of the
                    // record, but we add it to the symbols by declaring it.
                    if (record != null) {
                        method.recordDeclaration = record

                        frontend.scopeManager.enterScope(record)
                    }
                }
                method
            } else {
                var localNameOnly = false

                if (funcDecl.name.name == "") {
                    localNameOnly = true
                }

                newFunction(funcDecl.name.name, localNameOnly, rawNode = funcDecl)
            }

        frontend.scopeManager.enterScope(func)

        val receiver = (func as? Method)?.receiver
        if (receiver != null) {
            // Add the receiver do the scope manager, so we can resolve the receiver value
            frontend.scopeManager.addDeclaration(receiver)
        }

        val returnTypes = mutableListOf<Type>()

        // Build return types (and variables)
        val results = funcDecl.type.results
        if (results != null) {
            for (returnVar in results.list) {
                returnTypes += frontend.typeOf(returnVar.type)

                // If the function has named return variables, be sure to declare them as well
                if (returnVar.names.isNotEmpty()) {
                    val returnParam =
                        newVariable(
                            returnVar.names[0].name,
                            frontend.typeOf(returnVar.type),
                            rawNode = returnVar,
                        )

                    // Add parameter to scope
                    frontend.scopeManager.addDeclaration(returnParam)

                    // TODO(oxisto): Add the return parameter to the function declaration's AST. See
                    //  https://github.com/Fraunhofer-AISEC/cpg/issues/430
                }
            }
        }

        func.type = frontend.typeOf(funcDecl.type)
        func.returnTypes = returnTypes

        // Parse parameters
        handleFuncParams(func, funcDecl.type.params)

        // Only parse function body in non-dependencies
        if (!frontend.frontendConfiguration.doNotParseBody(func)) {
            // Check, if the last statement is a return statement, otherwise we insert an implicit
            // one
            val body = funcDecl.body?.let { frontend.statementHandler.handle(it) }
            if (body is Block) {
                val last = body.statements.lastOrNull()
                if (last !is ReturnStatement) {
                    val ret = newReturnStatement()
                    ret.isImplicit = true
                    body += ret
                }
            }
            func.body = body
        }

        frontend.scopeManager.leaveScope(func)

        // Leave scope of record, if applicable
        (func as? Method)?.recordDeclaration?.let {
            frontend.scopeManager.leaveScope(it)
        }

        return func
    }

    internal fun handleFuncParams(
        func: Function,
        list: GoStandardLibrary.Ast.FieldList,
    ) {
        for (param in list.list) {
            // We need to differentiate between three cases:
            // - an empty list of names, which means that the parameter is unnamed; and we also give
            //   it an empty name
            // - a single entry in the list of names, which is one regular parameter
            // - multiple entries in the list of names, which specifies multiple parameters with the
            //   same type
            //
            // We can treat the last two cases together, by just gathering all the names and
            // creating one param per name with the same type
            val names = mutableListOf<String>()
            if (param.names.isEmpty()) {
                names += ""
            } else {
                names +=
                    param.names.map {
                        // If the name is an underscore, it means that the parameter is
                        // unused (but not unnamed).
                        //
                        // But, but order to avoid confusion in resolving and to add
                        // some compatibility with other languages, we are just setting
                        // the name to an empty string in this case as well.
                        val name = it.name
                        if (name == "_") {
                            return@map ""
                        }
                        name
                    }
            }

            // Create one param variable per name
            for (name in names) {
                // Check for varargs. In this case we want to parse the element type
                // (and make it an array afterward)
                val (type, variadic) = frontend.fieldTypeOf(param.type)

                val p = newParameter(name, type, variadic, rawNode = param)

                frontend.scopeManager.addDeclaration(p)
                func.parameters += p

                frontend.setComment(p, param)
            }
        }
    }

    private fun handleGenDecl(genDecl: GoStandardLibrary.Ast.GenDecl): DeclarationSequence {
        // Reset the iota value. We need to start with -1 because we immediately increment in
        // handleValueSpec
        frontend.declCtx.iotaValue = -1
        // Set ourselves as the current gendecl
        frontend.declCtx.currentDecl = genDecl
        // Reset the initializers
        frontend.declCtx.constInitializers.clear()

        val sequence = DeclarationSequence()

        for (spec in genDecl.specs) {
            // We parse imports specifications directly in the frontend
            if (spec is GoStandardLibrary.Ast.ImportSpec) {
                continue
            }

            val declaration = frontend.specificationHandler.handle(spec)
            if (declaration != null) {
                sequence += declaration

                // Go associates the comment to the genDecl, so we need to explicitly launch
                // setComment here.
                frontend.setComment(declaration, genDecl)
            }
        }

        return sequence
    }
}
