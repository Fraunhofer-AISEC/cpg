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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents a call to a constructor, usually as an initializer.
 * * In C++ this can be part of a variable declaration plus initialization, such as `int a(5);` or
 *   as part of a [NewExpression].
 * * In Java, it is the initializer of a [NewExpression].
 */
// TODO Merge and/or refactor
class ConstructExpression : CallExpression() {
    /**
     * The link to the [ConstructorDeclaration]. This is populated by the
     * [de.fraunhofer.aisec.cpg.passes.SymbolResolver] later.
     */
    @PopulatedByPass(SymbolResolver::class)
    var constructor: ConstructorDeclaration? = null
        get() =
            if (anonymousClass != null) {
                anonymousClass?.constructors?.firstOrNull()
            } else {
                field
            }
        set(value) {
            field = value

            // Forward to CallExpression. This will also take care of DFG edges.
            if (value != null) {
                invokes = mutableListOf(value as FunctionDeclaration)
            }
        }

    @Relationship("ANONYMOUS_CLASS") var anonymousClassEdge = astOptionalEdgeOf<RecordDeclaration>()

    var anonymousClass by unwrapping(ConstructExpression::anonymousClassEdge)

    /** The [Declaration] of the type this expression instantiates. */
    @PopulatedByPass(SymbolResolver::class)
    var instantiates: Declaration? = null
        get() =
            if (anonymousClass != null) {
                anonymousClass
            } else {
                field
            }
        set(value) {
            field = value
            if (value != null && this.type is UnknownType) {
                type = objectType(value.name)
            }
        }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("constructor", constructor)
            .append("instantiates", instantiates)
            .append("arguments", arguments)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConstructExpression) return false
        return super.equals(other) &&
            constructor == other.constructor &&
            arguments == other.arguments
    }

    override fun hashCode() = Objects.hash(super.hashCode(), constructor, arguments)

    override fun startingPrevEOG(): Collection<Node> {
        return arguments.firstOrNull()?.startingPrevEOG() ?: this.prevEOG
    }
}
