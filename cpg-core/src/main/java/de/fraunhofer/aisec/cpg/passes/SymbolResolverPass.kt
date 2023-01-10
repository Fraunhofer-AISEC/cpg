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

import de.fraunhofer.aisec.cpg.frontends.HasSuperClasses
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker

abstract class SymbolResolverPass : Pass() {
    protected lateinit var walker: SubgraphWalker.ScopedWalker
    lateinit var currentTU: TranslationUnitDeclaration

    val recordMap = mutableMapOf<Name, RecordDeclaration>()
    protected val enumMap = mutableMapOf<Type, EnumDeclaration>()
    protected val templateList = mutableListOf<TemplateDeclaration>()
    protected val superTypesMap = mutableMapOf<Name, List<Type>>()

    /** Maps the name of the type of record declarations to its declaration. */
    protected fun findRecords(node: Node) {
        if (node is RecordDeclaration) {
            recordMap.putIfAbsent(node.name, node)
        }
    }

    /** Maps the type of enums to its declaration. */
    protected fun findEnums(node: Node) {
        if (node is EnumDeclaration) {
            // TODO: Use the name instead of the type.
            val type = TypeParser.createFrom(node.name, node.language)
            enumMap.putIfAbsent(type, node)
        }
    }

    /** Caches all TemplateDeclarations in [templateList] */
    protected fun findTemplates(node: Node) {
        if (node is TemplateDeclaration) {
            templateList.add(node)
        }
    }

    /** Checks if the function has the given [name], [returnType] and [signature] */
    protected fun FunctionDeclaration.matches(
        name: Name,
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
        return this.name.lastPartsMatch(name) &&
            thisReturnType == returnType &&
            this.hasSignature(signature)
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

        return language is HasSuperClasses &&
            reference.name
                .toString()
                .matches(
                    Regex(
                        "(?<class>.+" +
                            Regex.escape(language.namespaceDelimiter) +
                            ")?" +
                            (reference.language as HasSuperClasses).superClassKeyword
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
