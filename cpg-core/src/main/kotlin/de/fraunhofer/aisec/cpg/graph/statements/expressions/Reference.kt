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

import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * An expression, which refers to something which is declared, e.g. a variable. For example, the
 * expression `a = b`, which itself is an [AssignExpression], contains two [Reference]s, one for the
 * variable `a` and one for variable `b`, which have been previously been declared.
 */
open class Reference : Expression(), HasType.TypeObserver {
    /**
     * The [Declaration]s this expression might refer to. This will influence the [declaredType] of
     * this expression.
     */
    @PopulatedByPass(SymbolResolver::class)
    @Relationship(value = "REFERS_TO")
    var refersTo: Declaration? = null
        set(value) {
            val current = field

            // unregister type observers for current declaration
            if (current != null && current is HasType) {
                current.unregisterTypeObserver(this)
            }

            // set it
            field = value
            if (value is ValueDeclaration) {
                value.addUsage(this)
            }

            // Register ourselves to get type updates from the declaration
            if (value is HasType) {
                value.registerTypeObserver(this)
            }
        }
    // set the access
    /**
     * Is this reference used for writing data instead of just reading it? Determines dataflow
     * direction
     */
    var access = AccessValues.READ
    var isStaticAccess = false

    /**
     * This is a MAJOR workaround needed to resolve function pointers, until we properly re-design
     * the call resolver. When this [Reference] contains a function pointer reference that is
     * assigned to a variable (or to another reference), we need to set
     */
    var resolutionHelper: HasType? = null

    /**
     * Returns the contents of [refersTo] as the specified class, if the class is assignable.
     * Otherwise, it will return null.
     *
     * @param clazz the expected class
     * @param <T> the type
     * @return the declaration cast to the expected class, or null if the class is not assignable
     *   </T>
     */
    fun <T : VariableDeclaration?> getRefersToAs(clazz: Class<T>): T? {
        return if (refersTo?.javaClass?.let { clazz.isAssignableFrom(it) } == true)
            clazz.cast(refersTo)
        else null
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("refersTo", refersTo)
            .toString()
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // Make sure that the update comes from our declaration, if we change our declared type
        if (src == refersTo) {
            // Set our type
            this.type = newType
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Make sure that the update comes from our declaration, if we change our assigned types
        if (src == refersTo) {
            // Set our type
            this.addAssignedTypes(assignedTypes)
        }

        // We also allow updates from our previous DFG nodes
        if (prevDFG.contains(src as Node)) {
            this.addAssignedTypes(assignedTypes)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Reference) {
            return false
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun addPrevDFG(prev: Node, properties: MutableMap<Properties, Any?>) {
        super.addPrevDFG(prev, properties)

        // We want to propagate assigned types all through the previous DFG nodes. Therefore, we
        // override the DFG adding function here and add a type observer to the previous node (if it
        // is not ourselves)
        if (prev != this && prev is HasType) {
            prev.registerTypeObserver(this)
        }
    }

    /**
     * This function builds a unique tag for the particular reference, based on the [startScope].
     * Its purpose is to cache symbol resolutions, similar to LLVMs system of Unified Symbol
     * Resolution (USR).
     */
    val uniqueTag: ReferenceTag
        get() {
            return Objects.hash(this.name, this.resolutionHelper, this.scope)
        }
}

typealias ReferenceTag = Int
