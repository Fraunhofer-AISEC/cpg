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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Declaration of a field within a [RecordDeclaration]. It contains the modifiers associated with
 * the field as well as an initializer [Expression] which provides an initial value for the field.
 */
class FieldDeclaration : ValueDeclaration(), HasInitializer, HasType.TypeObserver {
    /** Specifies, whether this field declaration is also a definition, i.e. has an initializer. */
    private var isDefinition = false

    /** If this is only a declaration, this provides a link to the definition of the field. */
    @Relationship(value = "DEFINES")
    var definition: FieldDeclaration = this
        get() {
            return if (isDefinition) {
                this
            } else {
                field
            }
        }

    var modifiers: List<String> = mutableListOf()

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

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("initializer", initializer)
            .append("modifiers", modifiers)
            .toString()
    }

    @AST
    override var initializer: Expression? = null
        set(value) {
            field?.unregisterTypeObserver(this)
            field = value
            if (value is DeclaredReferenceExpression) {
                value.resolutionHelper = this
            }
            value?.registerTypeObserver(this)
        }

    override fun typeChanged(newType: Type, src: HasType, chain: MutableList<HasType>) {
        // Only accept type changes from our initializer, if any
        if (src != initializer) {
            return
        }

        // In the auto-inference case, we want to set the type of our declaration to the
        // declared type of the initializer
        if (this.type is AutoType) {
            type = newType
        } else {
            // Otherwise, we are at least interested in what the initializer's type is, to see
            // whether we can fill our assigned types with that
            addAssignedType(newType)
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
        if (other !is FieldDeclaration) {
            return false
        }
        return (super.equals(other) &&
            initializer == other.initializer &&
            modifiers == other.modifiers)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), initializer, modifiers)
}
