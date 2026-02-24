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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * This node represents the initialization of an "aggregate" object, such as an array or a struct or
 * object. The actual use can greatly differ by the individual language frontends. In order to be as
 * accurate as possible when propagating types, the [InitializerList.type] property MUST be set
 * before adding any values to [InitializerList.initializers].
 */
// TODO Merge and/or refactor
class InitializerList : Expression(), ArgumentHolder, HasType.TypeObserver {

    /** The list of initializers. */
    @Relationship(value = "INITIALIZERS", direction = Relationship.Direction.OUTGOING)
    var initializerEdges =
        astEdgesOf<Expression>(
            onAdd = {
                it.end.registerTypeObserver(this)
                it.end.access = this.access
            }
        ) {
            it.end.unregisterTypeObserver(this)
        }

    /** Virtual property to access [initializerEdges] without property edges. */
    var initializers by unwrapping(InitializerList::initializerEdges)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("initializers", initializers)
            .toString()
    }

    override fun addArgument(expression: Expression) {
        this.initializers += expression
        expression.access = this.access
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        val idx = initializerEdges.indexOfFirst { it.end == old }
        if (idx != -1) {
            old.unregisterTypeObserver(this)
            initializerEdges[idx].end = new
            new.registerTypeObserver(this)
            new.access = this.access
            return true
        }

        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return expression in this.initializers
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // Normally, we would check, if the source comes from our initializers, but we want to limit
        // the iteration of the initializer list (which can potentially contain tens of thousands of
        // entries in generated code), we skip it here.
        //
        // So we just have to look what kind of object we are initializing (its type is stored in
        // our "type"), to see whether we need to propagate something at all. If it has an array
        // type, we need to propagate an array version of the incoming type. If our "target" is a
        // regular object type, we do NOT propagate anything at all, because in this case we get the
        // types of individual fields, and we are not interested in those (yet).
        val type = type
        if (type is PointerType && type.pointerOrigin == PointerType.PointerOrigin.ARRAY) {
            addAssignedType(newType.array())
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Same as above, we can just propagate the incoming assigned types to us (in array form),
        // if we are initializing an array
        val type = type
        if (type is PointerType && type.pointerOrigin == PointerType.PointerOrigin.ARRAY) {
            addAssignedTypes(assignedTypes.map { it.array() }.toSet())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InitializerList) return false
        return super.equals(other) && propertyEqualsList(initializerEdges, other.initializerEdges)
    }

    override fun hashCode(): Int {
        // Including initializerEdges directly is a HUGE performance loss in the calculation of each
        // hash code. Therefore, we only include the array's size, which should hopefully be sort of
        // unique to avoid too many hash collisions.
        return Objects.hash(super.hashCode(), initializerEdges.size)
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return initializers.firstOrNull()?.getStartingPrevEOG() ?: this.prevEOG
    }

    override var access = AccessValues.READ
        set(value) {
            field = value
            initializers.forEach { it.access = value }
        }
}
