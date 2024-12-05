/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.backends.cxx

import de.fraunhofer.aisec.cpg.LanguageBackend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.*

class CXXLanguageBackend : LanguageBackend() {

    override fun translate(
        decl: Declaration,
        indent: Int,
        predicate: ((Node) -> Boolean)?
    ): String? {
        if (decl is FunctionDeclaration) {
            return translateFunctionDeclaration(decl, indent)
        } else if (decl is RecordDeclaration) {
            return translateRecordDeclaration(decl, indent, predicate)
        } else if (decl is NamespaceDeclaration) {
            return translateNamespaceDeclaration(decl, indent, predicate)
        } else if (decl is FieldDeclaration) {
            return translateFieldDeclaration(decl, indent)
        }

        return null
    }

    override fun translate(type: Type?): String? {
        return type?.typeName
    }

    fun translateFunctionDeclaration(declaration: FunctionDeclaration, indent: Int): String {
        return "${translate(declaration.returnTypes.singleOrNull())} ${declaration.name.localName}(${translateParameters(declaration.parameters)}) {};\n"
    }

    fun translateRecordDeclaration(
        record: RecordDeclaration,
        indent: Int,
        predicate: ((Node) -> Boolean)?
    ): String {
        var children =
            if (predicate != null) {
                record.declarations.filter(predicate)
            } else {
                record.declarations
            }

        val desiredClassOrder =
            listOf(RecordDeclaration::class, MethodDeclaration::class, FieldDeclaration::class)
        val childrenByClass = children.groupBy { it::class }
        val sortedChildren = desiredClassOrder.mapNotNull { childrenByClass[it] }.flatMap { it }

        return "class ${record.name.localName} {\npublic:\n${translateList(sortedChildren, indent)}};\n"
    }

    fun translateList(list: List<Declaration>, indent: Int): String {
        return list
            .map { translate(it, indent + 2) }
            .joinToString(
                "\n  ",
                prefix = " ".repeat(indent + 2),
                postfix = "\n" + " ".repeat(indent)
            )
    }

    fun translateNamespaceDeclaration(
        namespace: NamespaceDeclaration,
        indent: Int,
        predicate: ((Node) -> Boolean)?
    ): String {
        var children =
            if (predicate != null) {
                namespace.declarations.filter(predicate)
            } else {
                namespace.declarations
            }

        val inner =
            children
                .map { translate(it, indent, predicate) }
                .joinToString(
                    "\n  ",
                    prefix = " ".repeat(indent),
                    postfix = "\n" + " ".repeat(indent)
                )

        return "namespace ${namespace.name.localName} {\n\n${inner}}\n"
    }

    fun translateFieldDeclaration(field: FieldDeclaration, indent: Int): String {
        return "${translate(field.type)} ${field.name.localName};\n"
    }

    fun translateParameters(parameters: List<ParameterDeclaration>): String {
        return parameters.map { translateNameAndType(it) }.joinToString(", ")
    }

    fun translateNameAndType(param: HasType): String {
        var name = (param as Node).name
        // Depending on the type, we need to put some parts of it after the name
        // TODO: any other cases?
        if (param.type is PointerType && (param.type as PointerType).isArray) {
            return "${translate((param.type as PointerType).elementType)} ${name.localName}[]"
        } else {
            return "${translate(param.type)} ${name.localName}"
        }
    }
}
