/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.graph.implicit
import de.fraunhofer.aisec.cpg.graph.newConstructor
import de.fraunhofer.aisec.cpg.graph.newField
import de.fraunhofer.aisec.cpg.graph.newMethod
import de.fraunhofer.aisec.cpg.graph.newNamespace
import de.fraunhofer.aisec.cpg.graph.newParameter
import de.fraunhofer.aisec.cpg.graph.newRecord
import de.fraunhofer.aisec.cpg.graph.newVariable
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.unknownType

class DeclarationHandler(frontend: CSharpLanguageFrontend) :
    CSharpHandler<Declaration, Csharp.AST.MemberDeclarationSyntax>(::ProblemDeclaration, frontend) {
    override fun handleNode(node: Csharp.AST.MemberDeclarationSyntax): Declaration {
        return when (node) {
            is Csharp.AST.NamespaceDeclarationSyntax -> handleNamespaceDeclaration(node)
            is Csharp.AST.ClassDeclarationSyntax -> handleClassDeclaration(node)
            is Csharp.AST.MethodDeclarationSyntax -> handleMethodDeclaration(node)
            is Csharp.AST.ConstructorDeclarationSyntax -> handleConstructorDeclaration(node)
            else -> ProblemDeclaration("Not supported: ${node.csharpType}")
        }
    }

    /**
     * Translates a [NamespaceDeclarationSyntax][Csharp.AST.NamespaceDeclarationSyntax] into a
     * [Namespace].
     *
     * C# spec:
     * [Namespace declarations](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/namespaces#143-namespace-declarations)
     */
    private fun handleNamespaceDeclaration(
        node: Csharp.AST.NamespaceDeclarationSyntax
    ): Declaration {
        val namespace = newNamespace(node.name, rawNode = node)
        frontend.scopeManager.enterScope(namespace)

        for (member in node.members) {
            val decl = handle(member)
            frontend.scopeManager.addDeclaration(decl)
            namespace.addDeclaration(decl)
        }

        frontend.scopeManager.leaveScope(namespace)
        return namespace
    }

    /**
     * Translates a [ClassDeclarationSyntax][Csharp.AST.ClassDeclarationSyntax] into a [Record].
     *
     * C# spec:
     * [Class declarations](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/classes#1522-class-declarations)
     */
    private fun handleClassDeclaration(node: Csharp.AST.ClassDeclarationSyntax): Declaration {
        val record = newRecord(node.identifier, "class", rawNode = node)
        frontend.scopeManager.enterScope(record)

        for (member in node.members) {
            when (member) {
                is Csharp.AST.FieldDeclarationSyntax -> {
                    val declaration = member.declaration
                    val fieldType = frontend.typeOf(declaration.type)
                    for (variable in declaration.variables) {
                        val field = newField(variable.identifier, rawNode = member)
                        field.type = fieldType
                        variable.initializer?.let {
                            field.initializer = frontend.expressionHandler.handle(it)
                        }
                        // TODO: Not sure if this is correct: For implicit constructor calls (e.g.
                        // new()), we need to
                        //  propagate the field type to the "New" and "Construction" nodes
                        val newExpr = field.initializer as? New
                        if (newExpr != null) {
                            newExpr.type = fieldType
                            val construction = newExpr.initializer as? Construction
                            if (construction != null) {
                                construction.type = fieldType
                                construction.name = parseName(fieldType.name.localName)
                            }
                        }
                        frontend.scopeManager.addDeclaration(field)
                        record.addDeclaration(field)
                    }
                }
                else -> {
                    val decl = handle(member)
                    frontend.scopeManager.addDeclaration(decl)
                    record.addDeclaration(decl)
                }
            }
        }

        frontend.scopeManager.leaveScope(record)
        return record
    }

    /**
     * Translates a [MethodDeclarationSyntax][Csharp.AST.MethodDeclarationSyntax] into a [Method].
     *
     * C# spec:
     * [Methods](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/classes#156-methods)
     */
    private fun handleMethodDeclaration(node: Csharp.AST.MethodDeclarationSyntax): Declaration {
        val method = newMethod(node.identifier, rawNode = node)
        frontend.scopeManager.enterScope(method)

        createMethodReceiver(method)

        for (parameter in node.parameterList.parameters) {
            val param =
                newParameter(
                    name = parameter.identifier,
                    type = frontend.typeOf(parameter.type),
                    rawNode = parameter,
                )
            frontend.scopeManager.addDeclaration(param)
            method.parameters += param
        }
        method.body = frontend.statementHandler.handle(node.body)
        frontend.scopeManager.leaveScope(method)
        return method
    }

    /**
     * Translates a [ConstructorDeclarationSyntax][Csharp.AST.ConstructorDeclarationSyntax] into a
     * [Constructor].
     *
     * C# spec:
     * [Instance constructors](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/classes#1511-instance-constructors)
     */
    private fun handleConstructorDeclaration(
        node: Csharp.AST.ConstructorDeclarationSyntax
    ): Declaration {
        val record = frontend.scopeManager.currentRecord
        val constructor = newConstructor(node.identifier, record, rawNode = node)
        frontend.scopeManager.enterScope(constructor)

        for (parameter in node.parameterList.parameters) {
            val param =
                newParameter(
                    name = parameter.identifier,
                    type = frontend.typeOf(parameter.type),
                    rawNode = parameter,
                )
            frontend.scopeManager.addDeclaration(param)
            constructor.parameters += param
        }
        //        constructor.body = frontend.statementHandler.handle(node.body)

        frontend.scopeManager.leaveScope(constructor)
        return constructor
    }

    /** Creates an implicit `this` receiver variable for the given [method]. */
    private fun createMethodReceiver(method: Method) {
        val record = frontend.scopeManager.currentRecord
        val receiver = newVariable("this", record?.toType() ?: unknownType()).implicit("this")
        frontend.scopeManager.addDeclaration(receiver)
        method.receiver = receiver
    }
}
