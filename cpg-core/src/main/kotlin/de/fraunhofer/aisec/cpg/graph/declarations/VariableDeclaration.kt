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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents the declaration of a variable. */
open class VariableDeclaration : ValueDeclaration(), HasInitializer, HasType.TypeObserver {

    /**
     * We need a way to store the templateParameters that a VariableDeclaration might have before
     * the ConstructExpression is created.
     *
     * Because templates are only used by a small subset of languages and variable declarations are
     * used often, we intentionally make this a nullable list instead of an empty list.
     */
    @Relationship(value = "TEMPLATE_PARAMETERS", direction = Relationship.Direction.OUTGOING)
    @AST
    var templateParameters: List<Node>? = null

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

    /** The (optional) initializer of the declaration. */
    @AST
    override var initializer: Expression? = null
        set(value) {
            // Make sure to unregister an eventual type observer
            field?.unregisterTypeObserver(this)
            field = value
            // If we have an auto type, we register a type observer in order to retrieve the type
            // from our initializer
            if (type is AutoType) {
                value?.registerTypeObserver(this)
            }
        }

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

    override val assignments: List<Assignment>
        get() {
            return initializer?.let { listOf(Assignment(it, this, this)) } ?: listOf()
        }

    override var type: Type
        get() = super.type
        set(value) {
            super.type = value

            // There is an additional special case for variable declarations: The handling of the
            // "auto" type. If this type is assigned, it means that we need to auto-infer the type
            // from a suitable source (most likely the initializer). We do this here as well as in
            // the setter of the initializer, because the order in which either the type or the
            // initializer is set differs from frontend to frontend.
            if (value is AutoType) {
                initializer?.registerTypeObserver(this)
            }
        }

    override fun typeChanged(newType: Type, src: HasType, chain: MutableList<HasType>) {
        // There is only one use case to listen for type changes, and this is when we need type
        // inference from an initializer. There is also the possibility that we want to infer types
        // for inferred fields, but this handled by the inference system itself
        if (this.type is AutoType && src == initializer) {
            // In the auto-inference case, we want to set the type of our declaration to the
            // declared type of the
            // initializer
            type = newType
        }
    }

    override fun assignedTypeChanged(
        assignedTypes: Set<Type>,
        src: HasType,
        chain: MutableList<HasType>
    ) {
        // Nothing to do
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
}
