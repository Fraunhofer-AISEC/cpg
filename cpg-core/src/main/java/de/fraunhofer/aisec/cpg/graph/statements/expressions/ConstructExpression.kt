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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.PopulatedByPass
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.CallResolver
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Represents a call to a constructor, usually as an initializer.
 *
 * * In C++ this can be part of a variable declaration plus initialization, such as `int a(5);` or
 * as part of a [NewExpression].
 * * In Java, it is the initializer of a [NewExpression].
 */
class ConstructExpression : CallExpression(), HasType.TypeListener {
    /**
     * The link to the [ConstructorDeclaration]. This is populated by the
     * [de.fraunhofer.aisec.cpg.passes.CallResolver] later.
     */
    @PopulatedByPass(CallResolver::class)
    var constructor: ConstructorDeclaration? = null
        set(value) {
            field = value

            // Forward to CallExpression. This will also take care of DFG edges.
            if (value != null) {
                setInvokes(listOf(value as FunctionDeclaration))
            }
        }

    /** The [Declaration] of the type this expression instantiates. */
    @PopulatedByPass(CallResolver::class)
    var instantiates: Declaration? = null
        set(value) {
            field = value
            if (value != null && this.type is UnknownType) {
                setType(TypeParser.createFrom(value.name, true))
            }
        }

    override fun typeChanged(src: HasType, root: Collection<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }

        val previous: Type = this.type
        setType(src.propagationType, root)
        if (previous != this.type) {
            this.type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun toString(): String {
        return ToStringBuilder(this, Node.TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("constructor", constructor)
            .append("instantiates", instantiates)
            .append("arguments", arguments)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ConstructExpression) {
            return false
        }

        return super.equals(other) &&
            constructor == other.constructor &&
            arguments == other.arguments &&
            PropertyEdge.propertyEqualsList(argumentsPropertyEdge, other.argumentsPropertyEdge)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
