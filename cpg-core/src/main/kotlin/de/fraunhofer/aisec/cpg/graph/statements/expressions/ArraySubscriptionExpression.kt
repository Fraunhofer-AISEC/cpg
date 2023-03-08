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
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import java.util.stream.Collectors

/**
 * Represents the subscription or access of an array of the form `array[index]`, where both `array`
 * ([arrayExpression]) and `index` ([subscriptExpression]) are of type [Expression]. CPP can
 * overload operators thus changing semantics of array access.
 */
class ArraySubscriptionExpression : Expression(), HasType.TypeListener, HasBase {
    /**
     * The array on which the access is happening. This is most likely a
     * [DeclaredReferenceExpression].
     */
    @AST
    var arrayExpression: Expression = ProblemExpression("could not parse array expression")
        set(value) {
            field = value
            type = getSubscriptType(value.type)
            value.registerTypeListener(this)
        }

    /**
     * The expression which represents the "subscription" or index on which the array is accessed.
     * This can for example be a reference to another variable ([DeclaredReferenceExpression]), a
     * [Literal] or a [SliceExpression].
     */
    @AST var subscriptExpression: Expression = ProblemExpression("could not parse index expression")

    override val base: Expression
        get() = arrayExpression

    override val operatorCode: String
        get() = "[]"

    /**
     * This helper function returns the subscript type of the [arrayType]. We have to differentiate
     * here between to types of subscripts:
     * * Slices (in the form of a [SliceExpression] return the same type as the array
     * * Everything else (for example a [Literal] or any other [Expression] that is being evaluated)
     *   returns the de-referenced type
     */
    private fun getSubscriptType(arrayType: Type): Type {
        return when (subscriptExpression) {
            is SliceExpression -> arrayType
            else -> arrayType.dereference()
        }
    }

    override fun typeChanged(src: HasType, root: MutableList<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val previous = type
        setType(getSubscriptType(src.propagationType), root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)
        subTypes.addAll(
            src.possibleSubTypes
                .stream()
                .map { arrayType: Type -> getSubscriptType(arrayType) }
                .collect(Collectors.toList())
        )
        setPossibleSubTypes(subTypes, root)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArraySubscriptionExpression) return false
        return super.equals(other) &&
            arrayExpression == other.arrayExpression &&
            subscriptExpression == other.subscriptExpression
    }

    override fun hashCode() = Objects.hash(super.hashCode(), arrayExpression, subscriptExpression)
}
