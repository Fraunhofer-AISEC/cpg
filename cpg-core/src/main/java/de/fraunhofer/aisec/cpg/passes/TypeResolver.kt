/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.HasType.SecondaryTypeEdge
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

@DependsOn(CallResolver::class)
open class TypeResolver : Pass() {
    protected val firstOrderTypes = mutableSetOf<Type>()
    protected val typeState = mutableMapOf<Type, MutableList<Type>>()

    /**
     * Reduce the SecondOrderTypes to store only the unique SecondOrderTypes
     *
     * @param type SecondOrderType that is to be eliminated if an equal is already in typeState or
     * is added if not
     */
    protected fun processSecondOrderTypes(type: Type) {
        val state = typeState.computeIfAbsent(type.root, ::mutableListOf)
        if (state.contains(type)) return

        state.add(type)

        val element = ((type as? SecondOrderType)?.elementType as? SecondOrderType) ?: return

        val newElement = state.find { it == element }
        if (newElement != null) {
            (type as SecondOrderType).elementType = newElement
        } else {
            processSecondOrderTypes(element as Type)
        }
    }

    /**
     * Ensures that two different Types that are created at different Points are still the same
     * object in order to only store one node into the database
     *
     * @param type newly created Type
     * @return If the same type was already stored in the typeState Map the stored one is returned.
     * In the other case the parameter type is stored into the map and the parameter type is
     * returned
     */
    private fun obtainType(type: Type): Type {
        return if (type.root == type && type in typeState) {
            typeState.keys.first { it == type }
        } else {
            addType(type)
            type
        }
    }

    /**
     * Responsible for storing new types into typeState
     *
     * @param type new type
     */
    protected fun addType(type: Type) {
        if (type.root == type && type !in typeState) {
            // This is a rootType and is included in the map as key with empty references
            typeState[type] = mutableListOf()
            return
        }

        // ReferencesTypes
        if (type.root in typeState) {
            if (type !in typeState[type.root]!!) {
                typeState[type.root]?.add(type)
                addType((type as SecondOrderType).elementType)
            }
        } else {
            addType(type.root)
            addType(type)
        }
    }

    protected fun removeDuplicateTypes() {
        val typeManager = TypeManager.getInstance()
        // Remove duplicate firstOrderTypes
        firstOrderTypes.addAll(typeManager.firstOrderTypes)

        // Propagate new firstOrderTypes into secondOrderTypes
        val secondOrderTypes = typeManager.secondOrderTypes
        for (t in secondOrderTypes) {
            t.root = firstOrderTypes.firstOrNull { it == t.root } ?: t.root
        }

        // Build Map from firstOrderTypes to list of secondOderTypes
        for (t in firstOrderTypes) {
            typeState[t] = mutableListOf()
        }

        // Remove duplicate secondOrderTypes
        secondOrderTypes.forEach { processSecondOrderTypes(it) }

        // Remove duplicates from fields
        secondOrderTypes.forEach { removeDuplicatesInFields(it) }
    }

    /**
     * Visits all FirstOrderTypes and replace all the fields like returnVal or parameters for
     * FunctionPointertype or Generics for ObjectType
     *
     * @param t FirstOrderType
     */
    protected fun removeDuplicatesInFields(t: Type) {
        // Remove duplicates from fields
        if (t is FunctionPointerType) {
            t.returnType = obtainType(t.returnType)
            t.parameters = t.parameters.map(::obtainType)
        } else if (t is ObjectType) {
            t.generics = t.generics.map(::obtainType)
        }
    }

    /**
     * Pass on the TypeSystem: Sets RecordDeclaration Relationship from ObjectType to
     * RecordDeclaration
     *
     * @param translationResult
     */
    override fun accept(translationResult: TranslationResult) {
        removeDuplicateTypes()
        val walker = IterativeGraphWalker()
        walker.registerOnNodeVisit(::ensureUniqueType)
        walker.registerOnNodeVisit(::handle)
        walker.registerOnNodeVisit(::ensureUniqueSecondaryTypeEdge)

        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
    }

    protected fun ensureUniqueSubTypes(subTypes: Collection<Type>): List<Type> {
        val uniqueTypes = mutableListOf<Type>()
        for (subType in subTypes) {
            val trackedTypes =
                if (subType.isFirstOrderType) {
                    typeState.keys
                } else {
                    typeState[subType.root]!!
                }
            val unique = trackedTypes.firstOrNull { it == subType }
            // TODO Why do we only take the first one even if we don't add it?
            if (unique != null && unique !in uniqueTypes) uniqueTypes.add(unique)
        }
        return uniqueTypes
    }

    protected fun ensureUniqueType(node: Node) {
        // Avoid handling of ParameterizedType as they should be unique to each class and not
        // globally unique
        if (node is HasType && node.type !is ParameterizedType) {
            val type = node.type
            val types =
                if (type.isFirstOrderType) {
                    typeState.keys
                } else {
                    typeState.computeIfAbsent(type.root, ::mutableListOf)
                }
            updateType(node, types)
            node.updatePossibleSubtypes(ensureUniqueSubTypes(node.possibleSubTypes))
        }
    }

    /**
     * ensures that the if a nodes contains secondary type edges, those types are also merged and no
     * duplicate is left
     *
     * @param node implementing [HasType.SecondaryTypeEdge]
     */
    protected fun ensureUniqueSecondaryTypeEdge(node: Node) {
        if (node is SecondaryTypeEdge) {
            node.updateType(typeState.keys)
        } else if (node is HasType && node.type is SecondaryTypeEdge) {
            (node.type as SecondaryTypeEdge).updateType(typeState.keys)
            for (possibleSubType in node.possibleSubTypes) {
                if (possibleSubType is SecondaryTypeEdge) {
                    possibleSubType.updateType(typeState.keys)
                }
            }
        }
    }

    protected fun updateType(node: HasType, types: Collection<Type>) {
        // TODO: Why do we perform the update only for the first type?
        val typeToUpdate = types.firstOrNull { it == node.type } ?: return
        node.updateType(typeToUpdate)
    }

    /**
     * Creates the recordDeclaration relationship between ObjectTypes and RecordDeclaration (from
     * the Type to the Class)
     *
     * @param node
     */
    fun handle(node: Node) {
        if (node is RecordDeclaration) {
            for (t in typeState.keys) {
                if (t.typeName == node.name && t is ObjectType) {
                    // The node is the class of the type t
                    t.recordDeclaration = node
                }
            }
        }
    }

    override fun cleanup() {
        firstOrderTypes.clear()
        typeState.clear()
        TypeManager.reset()
    }
}
