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
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * An expression, which refers to something which is declared, e.g. a variable. For example, the
 * expression `a = b`, which itself is a [BinaryOperator], contains two [ ]s, one for the variable
 * `a` and one for variable `b ` * , which have been previously been declared.
 */
open class DeclaredReferenceExpression(
    override var resolutionDecider: ResolutionDecider<*, DeclaredReferenceExpression>? = null
) : Expression(), HasType.TypeListener, NeedsResolution<DeclaredReferenceExpression> {
    /** The [Declaration]s this expression might refer to. */
    @field:Relationship(value = "REFERS_TO")
    var refersTo: Declaration? = null
        set(value) {
            if (value == null) {
                return
            }
            val current = field

            // unregister type listeners for current declaration
            if (current != null) {
                when (access) {
                    AccessValues.WRITE -> removeNextDFG(current)
                    AccessValues.READ -> removePrevDFG(current)
                    else -> {
                        removeNextDFG(current)
                        removePrevDFG(current)
                    }
                }
                if (current is ValueDeclaration) {
                    current.unregisterTypeListener(this)
                }
                if (current is HasType.TypeListener) {
                    unregisterTypeListener(current as HasType.TypeListener?)
                }
            }

            // set it
            field = value

            // update type listeners
            when (access) {
                AccessValues.WRITE -> {
                    addNextDFG(field!!)
                }
                AccessValues.READ -> {
                    addPrevDFG(field!!)
                }
                else -> {
                    addNextDFG(field!!)
                    addPrevDFG(field!!)
                }
            }
            if (field is ValueDeclaration) {
                (field as ValueDeclaration?)!!.registerTypeListener(this)
            }
            if (field is HasType.TypeListener) {
                registerTypeListener(field as HasType.TypeListener?)
            }
        }

    /**
     * Is this reference used for writing data instead of just reading it? Determines dataflow
     * direction
     */
    var access = AccessValues.READ
        set(value) {
            val current = refersTo

            // remove DFG for the current reference
            if (current != null) {
                when (field) {
                    AccessValues.WRITE -> removeNextDFG(current)
                    AccessValues.READ -> removePrevDFG(current)
                    else -> {
                        removeNextDFG(current)
                        removePrevDFG(current)
                    }
                }
            }

            // set the access
            field = value

            // update the DFG again
            if (current != null) {
                when (field) {
                    AccessValues.WRITE -> addNextDFG(current)
                    AccessValues.READ -> addPrevDFG(current)
                    else -> {
                        addNextDFG(current)
                        addPrevDFG(current)
                    }
                }
            }
        }

    var isStaticAccess = false

    /**
     * Returns the contents of [.refersTo] as the specified class, if the class is assignable.
     * Otherwise, it will return null.
     *
     * @param clazz the expected class
     * @param <T> the type
     * @return the declaration cast to the expected class, or null if the class is not assignable
     * </T>
     */
    fun <T : VariableDeclaration?> getRefersToAs(clazz: Class<T>): T? {
        if (refersTo == null) {
            return null
        }
        return if (clazz.isAssignableFrom(refersTo!!.javaClass)) clazz.cast(refersTo) else null
    }

    override fun typeChanged(src: HasType, root: List<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val previous = type
        setType(src.propagationType, root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: List<HasType>) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)
        subTypes.addAll(src.possibleSubTypes)
        setPossibleSubTypes(subTypes, root)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append(super.toString())
            .append("refersTo", refersTo)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is DeclaredReferenceExpression) {
            return false
        }
        return super.equals(other) && refersTo == other.refersTo
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
