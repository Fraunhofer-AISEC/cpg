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
import de.fraunhofer.aisec.cpg.graph.statements.BlockStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

class DeclarationHandler(frontend: GoLanguageFrontend) :
    Handler<Declaration, GoStandardLibrary.Ast.Decl, GoLanguageFrontend>(
        ::ProblemDeclaration,
        frontend
    ) {

    init {
        map[GoStandardLibrary.Ast.FuncDecl::class.java] = HandlerInterface {
            handleFuncDecl(it as GoStandardLibrary.Ast.FuncDecl)
        }
        map[GoStandardLibrary.Ast.GenDecl::class.java] = HandlerInterface {
            handleGenDecl(it as GoStandardLibrary.Ast.GenDecl)
        }
        map.put(GoStandardLibrary.Ast.Decl::class.java, ::handleNode)
    }

    private fun handleNode(decl: GoStandardLibrary.Ast.Decl): Declaration {
        val message = "Not parsing declaration of type ${decl.goType} yet"
        log.error(message)

        return newProblemDeclaration(message)
    }

    private fun handleFuncDecl(funcDecl: GoStandardLibrary.Ast.FuncDecl): FunctionDeclaration {
        val recv = funcDecl.recv
        val func =
            if (recv != null) {
                val method = newMethodDeclaration(funcDecl.name.name, rawNode = funcDecl)
                val recvField = recv.list.firstOrNull()
                val recordType = recvField?.type?.let { frontend.typeOf(it) } ?: unknownType()

                // The name of the Go receiver is optional. In fact, if the name is not
                // specified we probably do not need any receiver variable at all,
                // because the syntax is only there to ensure that this method is part
                // of the struct, but it is not modifying the receiver.
                if (recvField?.names?.isNotEmpty() == true) {
                    method.receiver =
                        newVariableDeclaration(
                            recvField.names[0].name,
                            recordType,
                            rawNode = recvField
                        )
                }

                if (recordType !is UnknownType) {
                    val recordName = recordType.name

                    // TODO: this will only find methods within the current translation unit.
                    //  this is a limitation that we have for C++ as well
                    val record =
                        frontend.scopeManager.currentScope?.let {
                            frontend.scopeManager.getRecordForName(it, recordName)
                        }

                    // now this gets a little bit hacky, we will add it to the record declaration
                    // this is strictly speaking not 100 % true, since the method property edge is
                    // marked as AST and in Go a method is not part of the struct's AST but is
                    // declared outside. In the future, we need to differentiate between just the
                    // associated members  of the class and the pure AST nodes declared in the
                    // struct
                    // itself
                    record?.addMethod(method)
                }
                method
            } else {
                var localNameOnly = false

                if (funcDecl.name.name == "") {
                    localNameOnly = true
                }

                newFunctionDeclaration(funcDecl.name.name, null, funcDecl, localNameOnly)
            }

        frontend.scopeManager.enterScope(func)

        if (func is MethodDeclaration && func.receiver != null) {
            // Add the receiver do the scope manager, so we can resolve the receiver value
            frontend.scopeManager.addDeclaration(func.receiver)
        }

        val returnTypes = mutableListOf<Type>()

        // Build return types (and variables)
        val results = funcDecl.type.results
        if (results != null) {
            for (returnVar in results.list) {
                returnTypes += frontend.typeOf(returnVar.type)

                // If the function has named return variables, be sure to declare them as well
                if (returnVar.names.isNotEmpty()) {
                    val param =
                        newVariableDeclaration(
                            returnVar.names[0].name,
                            frontend.typeOf(returnVar.type),
                            rawNode = returnVar
                        )

                    // Add parameter to scope
                    frontend.scopeManager.addDeclaration(param)
                }
            }
        }

        func.type = frontend.typeOf(funcDecl.type)
        func.returnTypes = returnTypes

        // Parse parameters
        for (param in funcDecl.type.params.list) {
            var name = ""

            // Somehow parameters end up having no name sometimes, have not fully understood why.
            if (param.names.isNotEmpty()) {
                name = param.names[0].name

                // If the name is an underscore, it means that the parameter is
                // unnamed. In order to avoid confusing and some compatibility with
                // other languages, we are just setting the name to an empty string
                // in this case.
                if (name == "_") {
                    name = ""
                }
            } else {
                log.warn("Some param has no name, which is a bit weird: $param")
            }

            // Check for varargs. In this case we want to parse the element type
            // (and make it an array afterwards)
            var variadic = false
            val type =
                if (param.type is GoStandardLibrary.Ast.Ellipsis) {
                    variadic = true
                    frontend.typeOf((param.type as GoStandardLibrary.Ast.Ellipsis).elt).array()
                } else {
                    frontend.typeOf(param.type)
                }

            val p = newParameterDeclaration(name, type, variadic, rawNode = param)

            frontend.scopeManager.addDeclaration(p)

            frontend.setComment(p, param)
        }

        // Check, if the last statement is a return statement, otherwise we insert an implicit one
        val body = frontend.statementHandler.handle(funcDecl.body)
        if (body is BlockStatement) {
            val last = body.statements.lastOrNull()
            if (last !is ReturnStatement) {
                val ret = newReturnStatement()
                ret.isImplicit = true
                body += ret
            }
        }

        func.body = body

        frontend.scopeManager.leaveScope(func)

        return func
    }

    private fun handleGenDecl(genDecl: GoStandardLibrary.Ast.GenDecl): DeclarationSequence {
        val sequence = DeclarationSequence()

        for (spec in genDecl.specs) {
            frontend.specificationHandler.handle(spec)?.let {
                sequence += it

                // Go associates the comment to the genDecl, so we need to explicitly launch
                // setComment here.
                frontend.setComment(it, genDecl)
            }
        }

        return sequence
    }
}
