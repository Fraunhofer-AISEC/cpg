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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Declaration of a field within a [RecordDeclaration]. It contains the modifiers associated with
 * the field as well as an initializer [Expression] which provides an initial value for the field.
 */
class FieldDeclaration : ValueDeclaration(), HasType.TypeListener, HasInitializer {
    @AST
    override var initializer: Expression? = null
        set(value) {
            if (field != null) {
                isDefinition = true
                field?.unregisterTypeListener(this)
                if (field is HasType.TypeListener) {
                    unregisterTypeListener(field as HasType.TypeListener)
                }
            }
            field = value
            if (value != null) {
                value.registerTypeListener(this)
                if (value is HasType.TypeListener) {
                    registerTypeListener(value as HasType.TypeListener)
                }
            }
        }

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

    /** @see VariableDeclaration.implicitInitializerAllowed */
    var isImplicitInitializerAllowed = false

    var isArray = false
    var modifiers: List<String> = mutableListOf()

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
                // otherwise it
                // can be ignored once we have a type
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
        newType?.let { setType(it, root) }
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
            .appendSuper(super.toString())
            .append("initializer", initializer)
            .append("modifiers", modifiers)
            .toString()
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
