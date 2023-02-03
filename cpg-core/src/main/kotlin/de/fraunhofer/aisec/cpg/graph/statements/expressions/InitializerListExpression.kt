/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.LegacyTypeManager
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** A list of initializer expressions. */
class InitializerListExpression : Expression(), HasType.TypeListener {
    /** The list of initializers. */
    @Relationship(value = "INITIALIZERS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var initializerEdges = mutableListOf<PropertyEdge<Expression>>()
        set(value) {
            field.forEach {
                it.end.unregisterTypeListener(this)
                removePrevDFG(it.end)
            }
            field = value
            value.forEach {
                it.end.registerTypeListener(this)
                addPrevDFG(it.end)
            }
        }

    /** Virtual property to access [initializerEdges] without property edges. */
    var initializers by PropertyEdgeDelegate(InitializerListExpression::initializerEdges)

    fun addInitializer(initializer: Expression) {
        val edge = PropertyEdge(this, initializer)
        edge.addProperty(Properties.INDEX, initializerEdges.size)
        initializer.registerTypeListener(this)
        addPrevDFG(initializer)
        initializerEdges.add(edge)
    }

    override fun typeChanged(src: HasType, root: MutableList<HasType>, oldType: Type) {
        if (!LegacyTypeManager.isTypeSystemActive()) {
            return
        }
        if (!LegacyTypeManager.getInstance().isUnknown(type) && src.propagationType == oldType) {
            return
        }
        val previous = type
        val newType: Type
        val subTypes: MutableList<Type>
        if (initializers.contains(src)) {
            val types =
                initializers
                    .map {
                        LegacyTypeManager.getInstance()
                            .registerType(it.type.reference(PointerOrigin.ARRAY))
                    }
                    .toSet()
            val alternative =
                if (types.isNotEmpty()) types.iterator().next() else UnknownType.getUnknownType()
            newType = LegacyTypeManager.getInstance().getCommonType(types, this).orElse(alternative)
            subTypes = ArrayList(possibleSubTypes)
            subTypes.remove(oldType)
            subTypes.addAll(types)
        } else {
            newType = src.type
            subTypes = ArrayList(possibleSubTypes)
            subTypes.remove(oldType)
            subTypes.add(newType)
        }
        setType(newType, root)
        setPossibleSubTypes(subTypes, root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>) {
        if (!LegacyTypeManager.isTypeSystemActive()) {
            return
        }
        val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)
        subTypes.addAll(src.possibleSubTypes)
        setPossibleSubTypes(subTypes, root)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("initializers", initializers)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InitializerListExpression) return false
        return super.equals(other) &&
            initializers == other.initializers &&
            propertyEqualsList(initializerEdges, other.initializerEdges)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), initializers)
}
