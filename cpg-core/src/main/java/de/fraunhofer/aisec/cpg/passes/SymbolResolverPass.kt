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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.EnumDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker

abstract class SymbolResolverPass : Pass() {
    protected lateinit var walker: SubgraphWalker.ScopedWalker
    protected lateinit var currentTU: TranslationUnitDeclaration

    protected val recordMap = mutableMapOf<String, RecordDeclaration>()
    protected val enumMap = mutableMapOf<Type, EnumDeclaration>()
    protected val templateList = mutableListOf<TemplateDeclaration>()
    protected val superTypesMap = mutableMapOf<String, List<Type>>()

    protected fun findRecords(node: Node) {
        if (node is RecordDeclaration) {
            // The type name is not the same as the node's name! So, we have to be careful when
            // using the map!
            val type = TypeParser.createFrom(node.name, true)
            recordMap.putIfAbsent(type.typeName, node)
        }
    }

    protected fun findEnums(node: Node) {
        if (node is EnumDeclaration) {
            val type = TypeParser.createFrom(node.name, true)
            enumMap.putIfAbsent(type, node)
        }
    }

    /** Caches all TemplateDeclarations in [CallResolver.templateList] */
    protected fun findTemplates(node: Node) {
        if (node is TemplateDeclaration) {
            templateList.add(node)
        }
    }

    protected fun collectSupertypes() {
        val currSuperTypes = recordMap.mapValues { (_, value) -> value.superTypes }
        superTypesMap.putAll(currSuperTypes)
    }

    override fun cleanup() {
        superTypesMap.clear()
        recordMap.clear()
        enumMap.clear()
        templateList.clear()
    }

    protected val Node.delimiter: String
        get() = lang!!.namespaceDelimiter

    protected val Node.language: LanguageFrontend
        get() = lang!!
}
