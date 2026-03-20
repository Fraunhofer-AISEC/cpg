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
import de.fraunhofer.aisec.cpg.graph.newConstructor
import de.fraunhofer.aisec.cpg.graph.newField
import de.fraunhofer.aisec.cpg.graph.newMethod
import de.fraunhofer.aisec.cpg.graph.newNamespace
import de.fraunhofer.aisec.cpg.graph.newParameter
import de.fraunhofer.aisec.cpg.graph.newRecord

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
     * Translates a C#
     * [`NamespaceDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.namespacedeclarationsyntax?view=roslyn-dotnet-5.0.0)
     * into a [Namespace].
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
     * Translates a C#
     * [`ClassDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.classdeclarationsyntax?view=roslyn-dotnet-5.0.0)
     * into a [Record].
     */
    private fun handleClassDeclaration(node: Csharp.AST.ClassDeclarationSyntax): Declaration {
        val record = newRecord(node.identifier, "class", rawNode = node)
        frontend.scopeManager.enterScope(record)

        for (member in node.members) {
            when (member) {
                is Csharp.AST.FieldDeclarationSyntax -> {
                    for (variable in member.variables) {
                        val field = newField(variable.identifier, rawNode = member)
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
     * Translates a C#
     * [`MethodDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.methoddeclarationsyntax?view=roslyn-dotnet-5.0.0)
     * into a [Method].
     */
    private fun handleMethodDeclaration(node: Csharp.AST.MethodDeclarationSyntax): Declaration {
        val method = newMethod(node.identifier, rawNode = node)
        frontend.scopeManager.enterScope(method)

        for (parameter in node.parameters) {
            val param =
                newParameter(
                    name = parameter.identifier,
                    type = frontend.typeOf(parameter.type),
                    rawNode = parameter,
                )
            frontend.scopeManager.addDeclaration(param)
            method.parameters += param
        }

        frontend.scopeManager.leaveScope(method)
        return method
    }

    /**
     * Translates a C#
     * [`ConstructorDeclarationSyntax`](https://learn.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.csharp.syntax.constructordeclarationsyntax?view=roslyn-dotnet-5.0.0)
     * into a [Constructor].
     */
    private fun handleConstructorDeclaration(
        node: Csharp.AST.ConstructorDeclarationSyntax
    ): Declaration {
        val record = frontend.scopeManager.currentRecord
        val constructor = newConstructor(node.identifier, record, rawNode = node)
        frontend.scopeManager.enterScope(constructor)

        for (parameter in node.parameters) {
            val param =
                newParameter(
                    name = parameter.identifier,
                    type = frontend.typeOf(parameter.type),
                    rawNode = parameter,
                )
            frontend.scopeManager.addDeclaration(param)
            constructor.parameters += param
        }

        frontend.scopeManager.leaveScope(constructor)
        return constructor
    }
}
