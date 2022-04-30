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
import de.fraunhofer.aisec.cpg.graph.PopulatedByPass
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.NewResolver
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
    @PopulatedByPass(NewResolver::class)
    var constructor: ConstructorDeclaration? = null
        set(value) {
            field = value

            // Forward to CallExpression. This will also take care of DFG edges.
            if (value != null) {
                invokes = listOf(value as FunctionDeclaration)
            }
        }

    /** The [Declaration] of the type this expression instantiates. */
    @PopulatedByPass(NewResolver::class)
    var instantiates: Declaration? = null
        set(value) {
            field = value
            if (value != null && this.type is UnknownType) {
                setType(TypeParser.createFrom(value.name, language))
            }
        }

    /**
     * This function implements the [HasType.TypeListener] interface. We need to be really careful
     * about type changes in the [ConstructExpression]. The problem is, that usually, a
     * [VariableDeclaration] is registered as a type listener for its initializer, e.g, to infer the
     * type of the variable declaration based on its literal initializer. BUT, if the initializer
     * also implements [HasType.TypeListener], as does [ConstructExpression], the initializer is
     * also registered as a type listener for the declaration. The reason for that is primary
     * stemming from the way the C++ AST works where we need to get information about `Integer
     * i(4)`, in which the `Integer` type is only available to the declaration AST element and `(4)`
     * which is the [ConstructExpression] does not have the type information.
     *
     * Furthermore, there is a second source of type listening events coming from the [CallResolver]
     * , more specifically, if [CallExpression.invokes] is set. In this case, the call target, i.e.,
     * the [ConstructorDeclaration] invokes this function here. We have to differentiate between
     * those two, because in the second case we are not interested in the full
     * [FunctionDeclaration.type] that propagates this change (which is a [FunctionType], but only
     * its [FunctionDeclaration.returnTypes]. This is already handled by
     * [CallExpression.typeChanged], so we can just delegate to that.
     *
     * In fact, we could get rid of this particular implementation altogether, if we would somehow
     * work around the first case in a different way.
     */
    override fun typeChanged(src: HasType, root: List<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }

        // In the second case (see above), the src is always a function declaration, so we can
        // delegate this to our parent.
        if (src is FunctionDeclaration) {
            return super.typeChanged(src, root, oldType)
        }

        val previous: Type = this.type
        setType(src.propagationType, root)
        if (previous != this.type) {
            this.type.typeOrigin = Type.Origin.DATAFLOW
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
        if (this === other) {
            return true
        }
        if (other !is ConstructExpression) {
            return false
        }

        return super.equals(other) && constructor == other.constructor
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
