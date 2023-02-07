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
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.ArrayList
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Represents an expression containing a ternary operator: `var x = condition ? valueIfTrue :
 * valueIfFalse`;
 */
class ConditionalExpression : Expression(), HasType.TypeListener {
    @field:SubGraph("AST")
    var condition: Expression = ProblemExpression("could not parse condition expression")

    @field:SubGraph("AST")
    var thenExpr: Expression? = null
        set(value) {
            field?.unregisterTypeListener(this)
            field = value
            value?.registerTypeListener(this)
        }

    @field:SubGraph("AST")
    var elseExpr: Expression? = null
        set(value) {
            field?.unregisterTypeListener(this)
            field = value
            value?.registerTypeListener(this)
        }

    override fun typeChanged(src: HasType, root: MutableList<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val previous = type
        val types: MutableList<Type> = ArrayList()

        thenExpr?.propagationType?.let { types.add(it) }
        elseExpr?.propagationType?.let { types.add(it) }

        val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)
        subTypes.remove(oldType)
        subTypes.addAll(types)
        val alternative = if (types.isNotEmpty()) types[0] else UnknownType.getUnknownType()
        setType(TypeManager.getInstance().getCommonType(types, this).orElse(alternative), root)
        setPossibleSubTypes(subTypes, root)
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
        possibleSubTypes = subTypes
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .append("thenExpr", thenExpr)
            .append("elseExpr", elseExpr)
            .build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConditionalExpression) return false
        return super.equals(other) &&
            condition == other.condition &&
            thenExpr == other.thenExpr &&
            elseExpr == other.elseExpr
    }

    override fun hashCode() = Objects.hash(super.hashCode(), condition, thenExpr, elseExpr)
}
