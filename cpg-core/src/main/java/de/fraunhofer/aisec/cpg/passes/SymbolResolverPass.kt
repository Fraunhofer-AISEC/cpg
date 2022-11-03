/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.frontends.HasSuperclasses
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker

abstract class SymbolResolverPass : Pass() {
    protected lateinit var walker: SubgraphWalker.ScopedWalker
    lateinit var currentTU: TranslationUnitDeclaration

    val recordMap = mutableMapOf<String, RecordDeclaration>()
    protected val enumMap = mutableMapOf<Type, EnumDeclaration>()
    protected val templateList = mutableListOf<TemplateDeclaration>()
    protected val superTypesMap = mutableMapOf<String, List<Type>>()

    /** Maps the name of the type of record declarations to its declaration. */
    protected fun findRecords(node: Node) {
        if (node is RecordDeclaration) {
            // The type name is not the same as the node's name! So, we have to be careful when
            // using the map!
            val type = TypeParser.createFrom(node.name, true, node.language)
            recordMap.putIfAbsent(type.typeName, node)
        }
    }

    /** Maps the type of enums to its declaration. */
    protected fun findEnums(node: Node) {
        if (node is EnumDeclaration) {
            val type = TypeParser.createFrom(node.name, true, node.language)
            enumMap.putIfAbsent(type, node)
        }
    }

    /** Caches all TemplateDeclarations in [templateList] */
    protected fun findTemplates(node: Node) {
        if (node is TemplateDeclaration) {
            templateList.add(node)
        }
    }

    /**
     * Checks if the function has the given [name], and returnType and signature specified in
     * [fctPtrType].
     */
    protected fun FunctionDeclaration.matches(
        name: String,
        fctPtrType: FunctionPointerType
    ): Boolean {
        return this.matches(name, fctPtrType.returnType, fctPtrType.parameters)
    }

    /** Checks if the function has the given [name], [returnType] and [signature] */
    protected fun FunctionDeclaration.matches(
        name: String,
        returnType: Type,
        signature: List<Type?>
    ): Boolean {
        val thisReturnType =
            if (this.returnTypes.isEmpty()) {
                IncompleteType()
            } else {
                // TODO(oxisto): support multiple return types
                this.returnTypes[0]
            }
        return this.name == name && thisReturnType == returnType && this.hasSignature(signature)
    }

    protected fun collectSupertypes() {
        val currSuperTypes = recordMap.mapValues { (_, value) -> value.superTypes }
        superTypesMap.putAll(currSuperTypes)
    }

    /**
     * Determines if the [reference] is refers to the super class and we have to start searching
     * there.
     */
    protected fun isSuperclassReference(reference: DeclaredReferenceExpression): Boolean {
        val language = reference.language

        return language is HasSuperclasses &&
            reference.name.matches(
                Regex(
                    "(?<class>.+" +
                        Regex.escape(language.namespaceDelimiter) +
                        ")?" +
                        (reference.language as HasSuperclasses).superclassKeyword
                )
            )
    }

    override fun cleanup() {
        superTypesMap.clear()
        recordMap.clear()
        enumMap.clear()
        templateList.clear()
    }
}
