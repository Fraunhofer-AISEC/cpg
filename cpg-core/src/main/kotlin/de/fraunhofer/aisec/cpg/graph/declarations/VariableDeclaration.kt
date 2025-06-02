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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents the declaration of a local variable. */
open class VariableDeclaration : ValueDeclaration(), HasInitializer, HasType.TypeObserver {

    /**
     * We need a way to store the templateParameters that a [VariableDeclaration] might have before
     * the [ConstructExpression] is created.
     */
    @Relationship(value = "TEMPLATE_PARAMETERS", direction = Relationship.Direction.OUTGOING)
    var templateParameterEdges = astEdgesOf<Node>()
    var templateParameters by unwrapping(VariableDeclaration::templateParameterEdges)

    /** Determines if this is a global variable. */
    val isGlobal: Boolean
        get() = this.scope is GlobalScope

    /**
     * C++ uses implicit constructor calls for statements like `A a;` but this only applies to types
     * that are actually classes and not just primitive types or typedef aliases of primitives.
     * Thus, during AST construction, we can only suggest that an implicit constructor call might be
     * allowed by the language (so this is set to true for C++ but false for Java, as such a
     * statement in Java leads to an uninitialized variable). The final decision can then be made
     * after we have analyzed all classes present in the current scope.
     */
    var isImplicitInitializerAllowed = false
    var isArray = false

    @Relationship("INITIALIZER")
    var initializerEdge =
        astOptionalEdgeOf<Expression>(
            onChanged = { old, new ->
                val value = new?.end
                exchangeTypeObserverWithAccessPropagation(old, new)
                if (value is Reference) {
                    value.resolutionHelper = this
                }
            }
        )
    /** The (optional) initializer of the declaration. */
    override var initializer by unwrapping(VariableDeclaration::initializerEdge)

    fun <T> getInitializerAs(clazz: Class<T>): T? {
        return clazz.cast(initializer)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append("name", name)
            .append("location", location)
            .append("initializer", initializer)
            .toString()
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // Only accept type changes from our initializer; or if the source is a tuple
        if (src != initializer && src !is TupleDeclaration) {
            return
        }

        // If our type is set to "auto", we want to derive our type from the initializer (or the
        // tuple source)
        if (this.type is AutoType) {
            // If the source is a tuple, we need to check, if we are really part of the source tuple
            // and if yes, on which position
            if (src is TupleDeclaration && newType is TupleType) {
                // We can then derive our appropriate type out of the tuple type based on the
                // position in the tuple
                val idx = src.elements.indexOf(this)
                if (idx != -1) {
                    type = newType.types.getOrElse(idx) { unknownType() }
                }
            } else {
                // Otherwise, we can just set the type directly.
                type = newType
            }
        } else {
            if (src !is TupleDeclaration) {
                // If we are not in "auto" mode, we are at least interested in what the
                // initializer's type is, to see
                // whether we can fill our assigned types with that
                addAssignedType(newType)
            }
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Propagate the assigned types from our initializer into the declaration
        if (src == initializer) {
            addAssignedTypes(assignedTypes)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other !is VariableDeclaration) {
            false
        } else super.equals(other) && initializer == other.initializer
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.prevEOG
    }

    override fun getExitNextEOG(): Collection<Node> {
        return this.initializer?.getExitNextEOG() ?: this.nextEOG
    }
}
