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

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.graph.implicit
import de.fraunhofer.aisec.cpg.graph.incompleteType
import de.fraunhofer.aisec.cpg.graph.newAssign
import de.fraunhofer.aisec.cpg.graph.newBlock
import de.fraunhofer.aisec.cpg.graph.newConstructor
import de.fraunhofer.aisec.cpg.graph.newEnumConstant
import de.fraunhofer.aisec.cpg.graph.newEnumeration
import de.fraunhofer.aisec.cpg.graph.newField
import de.fraunhofer.aisec.cpg.graph.newMemberAccess
import de.fraunhofer.aisec.cpg.graph.newMethod
import de.fraunhofer.aisec.cpg.graph.newNamespace
import de.fraunhofer.aisec.cpg.graph.newParameter
import de.fraunhofer.aisec.cpg.graph.newRecord
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.newReturn
import de.fraunhofer.aisec.cpg.graph.newVariable
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.unknownType

class DeclarationHandler(frontend: CSharpLanguageFrontend) :
    CSharpHandler<Declaration, Csharp.AST.MemberDeclarationSyntax>(::ProblemDeclaration, frontend) {
    override fun handleNode(node: Csharp.AST.MemberDeclarationSyntax): Declaration {
        return when (node) {
            is Csharp.AST.BaseNamespaceDeclarationSyntax -> handleNamespaceDeclaration(node)
            is Csharp.AST.ClassDeclarationSyntax -> handleClassDeclaration(node)
            is Csharp.AST.EnumDeclarationSyntax -> handleEnumDeclaration(node)
            is Csharp.AST.MethodDeclarationSyntax -> handleMethodDeclaration(node)
            is Csharp.AST.PropertyDeclarationSyntax -> handlePropertyDeclaration(node)
            is Csharp.AST.ConstructorDeclarationSyntax -> handleConstructorDeclaration(node)
            is Csharp.AST.InterfaceDeclarationSyntax -> handleInterfaceDeclaration(node)
            else -> ProblemDeclaration("Not supported: ${node.csharpType}")
        }
    }

    /**
     * Translates a [BaseNamespaceDeclarationSyntax][Csharp.AST.BaseNamespaceDeclarationSyntax] into
     * a [Namespace]. Handles both block-scoped and file-scoped namespaces.
     *
     * C# spec:
     * [Namespace declarations](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/namespaces#143-namespace-declarations)
     */
    private fun handleNamespaceDeclaration(
        node: Csharp.AST.BaseNamespaceDeclarationSyntax
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
        record.modifiers = node.modifiers.toSet()
        frontend.scopeManager.enterScope(record)

        node.baseList?.let {
            for (baseType in it.types) {
                record.superClasses += frontend.typeOf(baseType)
            }
        }

        node.typeParameterList?.let {
            frontend.typeManager.addTypeParameter(
                record,
                it.parameters.map { name -> ParameterizedType(name, language) },
            )
        }

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
     * Translates an [InterfaceDeclarationSyntax][Csharp.AST.InterfaceDeclarationSyntax] into a
     * [Record].
     *
     * C# spec:
     * [Interface declarations](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/interfaces#182-interface-declarations)
     */
    private fun handleInterfaceDeclaration(
        node: Csharp.AST.InterfaceDeclarationSyntax
    ): Declaration {
        val record = newRecord(node.identifier, "interface", rawNode = node)
        record.modifiers = node.modifiers.toSet()
        frontend.scopeManager.enterScope(record)

        node.baseList?.let {
            for (baseType in it.types) {
                record.superClasses += frontend.typeOf(baseType)
            }
        }

        node.typeParameterList?.let {
            frontend.typeManager.addTypeParameter(
                record,
                it.parameters.map { name -> ParameterizedType(name, language) },
            )
        }

        for (member in node.members) {
            val decl = handle(member)
            frontend.scopeManager.addDeclaration(decl)
            record.addDeclaration(decl)
        }

        frontend.scopeManager.leaveScope(record)
        return record
    }

    /**
     * Translates an [EnumDeclarationSyntax][Csharp.AST.EnumDeclarationSyntax] into an
     * [Enumeration].
     *
     * C# spec:
     * [Enum declarations](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/enums)
     */
    private fun handleEnumDeclaration(node: Csharp.AST.EnumDeclarationSyntax): Declaration {
        val enumeration = newEnumeration(node.identifier, rawNode = node)
        enumeration.kind = "enum"
        enumeration.modifiers = node.modifiers.toSet()
        frontend.scopeManager.enterScope(enumeration)

        val enumType = enumeration.toType()
        enumeration.entries =
            node.members
                .map { member ->
                    val enumConstant = newEnumConstant(member.identifier, rawNode = member)
                    enumConstant.type = enumType
                    member.equalsValue?.let {
                        enumConstant.initializer = frontend.expressionHandler.handle(it)
                    }
                    frontend.scopeManager.addDeclaration(enumConstant)
                    enumConstant
                }
                .toMutableList()

        frontend.scopeManager.leaveScope(enumeration)
        return enumeration
    }

    /**
     * Translates a [MethodDeclarationSyntax][Csharp.AST.MethodDeclarationSyntax] into a [Method].
     *
     * C# spec:
     * [Methods](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/classes#156-methods)
     */
    private fun handleMethodDeclaration(node: Csharp.AST.MethodDeclarationSyntax): Declaration {
        val method = newMethod(node.identifier, rawNode = node)
        method.modifiers = node.modifiers.toSet()
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
        method.returnTypes = listOf(frontend.typeOf(node.returnType))
        method.body = frontend.statementHandler.handle(node.body)
        frontend.scopeManager.leaveScope(method)
        return method
    }

    /**
     * Translates a [PropertyDeclarationSyntax][Csharp.AST.PropertyDeclarationSyntax] into a [Field]
     * and a [Method] per accessor (`get`, `set`).
     *
     * C# spec:
     * [Properties](https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/classes#157-properties)
     */
    private fun handlePropertyDeclaration(node: Csharp.AST.PropertyDeclarationSyntax): Declaration {
        val record = frontend.scopeManager.currentRecord
        val propertyType = frontend.typeOf(node.type)

        val field = newField(node.identifier, rawNode = node).implicit()
        field.type = propertyType

        node.initializer?.let { field.initializer = frontend.expressionHandler.handle(it) }

        node.accessorList?.accessors?.forEach { accessor ->
            val accessorName =
                Name.temporary(
                    prefix = "${accessor.keyword}_${node.identifier}",
                    separatorChar = '_',
                )
            val method =
                newMethod(accessorName, recordDeclaration = record, rawNode = accessor)
                    .implicit(
                        code = frontend.codeOf(accessor),
                        location = frontend.locationOf(accessor),
                    )
            method.modifiers =
                if (accessor.modifiers.isNotEmpty()) accessor.modifiers.toSet()
                else node.modifiers.toSet()
            frontend.scopeManager.enterScope(method)
            createMethodReceiver(method)

            val isSetter = accessor.keyword == "set"
            if (isSetter) {
                val param = newParameter("value", propertyType, rawNode = accessor)
                frontend.scopeManager.addDeclaration(param)
                method.parameters += param
                method.returnTypes = listOf(incompleteType())
            } else {
                method.returnTypes = listOf(propertyType)
            }

            val body = accessor.body
            val expressionBody = accessor.expressionBody
            if (body != null) {
                method.body = frontend.statementHandler.handle(body)
            } else if (expressionBody != null) {
                val block =
                    newBlock()
                        .implicit(
                            code = frontend.codeOf(accessor),
                            location = frontend.locationOf(accessor),
                        )
                val expr = frontend.expressionHandler.handle(expressionBody)
                if (isSetter) {
                    val assign =
                        newAssign(
                                operatorCode = "=",
                                lhs = listOf(implicitThisFieldAccess(field, accessor)),
                                rhs = listOf(expr),
                            )
                            .implicit(
                                code = frontend.codeOf(accessor),
                                location = frontend.locationOf(accessor),
                            )
                    block.statements += assign
                } else {
                    val ret =
                        newReturn()
                            .implicit(
                                code = frontend.codeOf(accessor),
                                location = frontend.locationOf(accessor),
                            )
                    ret.returnValue = expr
                    block.statements += ret
                }
                method.body = block
            } else {
                // Auto-accessor (`get;`, `set;`)
                val block =
                    newBlock()
                        .implicit(
                            code = frontend.codeOf(accessor),
                            location = frontend.locationOf(accessor),
                        )
                if (isSetter) {
                    val valueRef =
                        newReference("value", propertyType)
                            .implicit(code = "value", location = frontend.locationOf(accessor))
                    val assign =
                        newAssign(
                                operatorCode = "=",
                                lhs = listOf(implicitThisFieldAccess(field, accessor)),
                                rhs = listOf(valueRef),
                            )
                            .implicit(
                                code = frontend.codeOf(accessor),
                                location = frontend.locationOf(accessor),
                            )
                    block.statements += assign
                } else {
                    val ret =
                        newReturn()
                            .implicit(
                                code = frontend.codeOf(accessor),
                                location = frontend.locationOf(accessor),
                            )
                    ret.returnValue = implicitThisFieldAccess(field, accessor)
                    block.statements += ret
                }
                method.body = block
            }

            frontend.scopeManager.leaveScope(method)
            frontend.scopeManager.addDeclaration(method)
            record?.addDeclaration(method)
        }

        node.expressionBody?.let {
            val accessorName =
                Name.temporary(prefix = "get_${node.identifier}", separatorChar = '_')
            val method =
                newMethod(accessorName, recordDeclaration = record, rawNode = node)
                    .implicit(code = frontend.codeOf(node), location = frontend.locationOf(node))
            method.modifiers = node.modifiers.toSet()
            frontend.scopeManager.enterScope(method)
            createMethodReceiver(method)
            method.returnTypes = listOf(propertyType)
            val block =
                newBlock()
                    .implicit(code = frontend.codeOf(node), location = frontend.locationOf(node))
            val ret =
                newReturn()
                    .implicit(code = frontend.codeOf(node), location = frontend.locationOf(node))
            ret.returnValue = frontend.expressionHandler.handle(it)
            block.statements += ret
            method.body = block
            frontend.scopeManager.leaveScope(method)
            frontend.scopeManager.addDeclaration(method)
            record?.addDeclaration(method)
        }

        return field
    }

    private fun implicitThisFieldAccess(field: Field, from: Csharp.AST.Node): MemberAccess {
        val record = frontend.scopeManager.currentRecord
        val thisRef =
            newReference(name = "this", type = record?.toType() ?: unknownType())
                .implicit(code = "this", location = frontend.locationOf(from))
        return newMemberAccess(name = field.name, base = thisRef, operatorCode = ".")
            .implicit(code = "this.${field.name.localName}", location = frontend.locationOf(from))
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
        constructor.modifiers = node.modifiers.toSet()
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
        constructor.body = frontend.statementHandler.handle(node.body)

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
