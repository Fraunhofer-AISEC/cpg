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

import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.AssignmentTarget
import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * An expression, which refers to something which is declared, e.g. a variable. For example, the
 * expression `a = b`, which itself is a [BinaryOperator], contains two [ ]s, one for the variable `a` and one for variable `b
` * , which have been previously been declared.
 */
open class DeclaredReferenceExpression : Expression(), HasType.TypeListener, AssignmentTarget {
    /** The [Declaration]s this expression might refer to.  */
    @Relationship(value = "REFERS_TO")
    private var refersTo: Declaration? = null
    // set the access
    /**
     * Is this reference used for writing data instead of just reading it? Determines dataflow
     * direction
     */
    var access = AccessValues.READ
    var isStaticAccess = false
    fun getRefersTo(): Declaration? {
        return refersTo
    }

    /**
     * Returns the contents of [.refersTo] as the specified class, if the class is assignable.
     * Otherwise, it will return null.
     *
     * @param clazz the expected class
     * @param <T> the type
     * @return the declaration cast to the expected class, or null if the class is not assignable
    </T> */
    fun <T : VariableDeclaration?> getRefersToAs(clazz: Class<T>): T? {
        if (refersTo == null) {
            return null
        }
        return if (clazz.isAssignableFrom(refersTo!!.javaClass)) clazz.cast(refersTo) else null
    }

    fun setRefersTo(refersTo: Declaration?) {
        if (refersTo == null) {
            return
        }
        val current = this.refersTo

        // unregister type listeners for current declaration
        if (current != null) {
            if (current is ValueDeclaration) {
                current.unregisterTypeListener(this)
            }
            if (current is HasType.TypeListener) {
                unregisterTypeListener((current as HasType.TypeListener?)!!)
            }
        }

        // set it
        this.refersTo = refersTo
        if (refersTo is ValueDeclaration) {
            refersTo.addUsage(this)
        }

        // update type listeners
        if (this.refersTo is ValueDeclaration) {
            (this.refersTo as ValueDeclaration?)!!.registerTypeListener(this)
        }
        if (this.refersTo is HasType.TypeListener) {
            registerTypeListener((this.refersTo as HasType.TypeListener?)!!)
        }
    }

    override fun typeChanged(
        src: HasType, root: MutableList<HasType>, oldType: Type
    ) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val previous = type
        setType(src.propagationType, root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }

        // since we want to update the sub types, we need to exclude ourselves from the root, otherwise
        // it won't work. What a weird and broken system!
        root.remove(this)
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

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is DeclaredReferenceExpression) {
            return false
        }
        val that = o
        return super.equals(that) && refersTo == that.refersTo
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}