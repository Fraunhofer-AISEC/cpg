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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents the declaration of a variable. */
class VariableDeclaration : ValueDeclaration(), HasType.TypeListener, HasInitializer {

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
            field?.unregisterTypeListener(this)
            if (field is HasType.TypeListener) {
                unregisterTypeListener(field as HasType.TypeListener)
            }
            field = value
            value?.registerTypeListener(this)

            // if the initializer implements a type listener, inform it about our type changes
            // since the type is tied to the declaration, but it is convenient to have the type
            // information in the initializer, i.e. in a ConstructExpression.
            if (value is HasType.TypeListener) {
                registerTypeListener((value as HasType.TypeListener?)!!)
            }
        }

    fun <T> getInitializerAs(clazz: Class<T>): T? {
        return clazz.cast(initializer)
    }

    override fun typeChanged(src: HasType, root: MutableList<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        if (!TypeManager.getInstance().isUnknown(type) && src.propagationType == oldType) {
            return
        }
        val previous = type
        val newType =
            if (src === initializer && initializer is InitializerListExpression) {
                // Init list is seen as having an array type, but can be used ambiguously. It can be
                // either used to initialize an array, or to initialize some objects. If it is used
                // as an
                // array initializer, we need to remove the array/pointer layer from the type,
                // otherwise it can be ignored once we have a type
                if (isArray) {
                    src.type
                } else if (!TypeManager.getInstance().isUnknown(type)) {
                    return
                } else {
                    src.type.dereference()
                }
            } else {
                src.propagationType
            }
        setType(newType, root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)
        subTypes.addAll(src.possibleSubTypes)
        setPossibleSubTypes(subTypes, root)
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
