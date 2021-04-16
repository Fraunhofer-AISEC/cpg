/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.*
import java.util.function.Consumer
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * An expression, which calls another function. It has a list of arguments (list of [ ]s) and is
 * connected via the INVOKES edge to its [FunctionDeclaration].
 */
open class CallExpression : Expression(), TypeListener {

    var fqn: String? = null

    /**
     * Connection to its [FunctionDeclaration]. This will be populated by the
     * [de.fraunhofer.aisec.cpg.passes.CallResolver].
     */
    @field:Relationship(value = "INVOKES", direction = "OUTGOING")
    protected var invokesEdges: List<PropertyEdge<FunctionDeclaration>> = ArrayList()

    /** The list of arguments. */
    @field:Relationship(value = "ARGUMENTS", direction = "OUTGOING")
    @field:SubGraph("AST")
    protected var argumentsEdges: MutableList<PropertyEdge<Expression>> = ArrayList()

    /** A virtual property to access [argumentsEdges] without property edges. */
    var arguments: List<Expression>
        get() {
            return PropertyEdge.unwrap(this.argumentsEdges)
        }
        set(value) {
            this.argumentsEdges = PropertyEdge.transformIntoOutgoingPropertyEdgeList(value, this)
        }

    /** A virtual property to access [invokesEdges] without property edges. */
    var invokes: List<FunctionDeclaration>
        get() {
            return PropertyEdge.unwrap(this.invokesEdges)
        }
        set(value) {
            PropertyEdge.getTarget(invokesEdges)
                .forEach(
                    Consumer { i: FunctionDeclaration ->
                        i.unregisterTypeListener(this)
                        Util.detachCallParameters(i, arguments)
                        removePrevDFG(i)
                    }
                )
            invokesEdges = PropertyEdge.transformIntoOutgoingPropertyEdgeList(value, this)
            invokes.forEach(
                Consumer { i: FunctionDeclaration ->
                    i.registerTypeListener(this)
                    Util.attachCallParameters(i, arguments)
                    addPrevDFG(i)
                }
            )
        }

    val signature: List<Type>
        get() = arguments.map(Expression::getType)

    /**
     * The base object. This is marked as an AST child, because this is required for [ ]. Be aware
     * that for simple calls the implicit "this" base is not part of the original AST, but we treat
     * it as such for better consistency
     */
    @field:SubGraph("AST")
    var base: Node? = null
        set(value: Node?) {
            if (field is HasType) {
                (field as HasType).unregisterTypeListener(this)
            }
            field = value
            if (value is HasType) {
                (value as HasType).registerTypeListener(this)
            }
        }

    /**
     * Adds an argument to this call.
     *
     * @param expression the expression representing the argument
     * @param isDefault whether, this is a default argument, defaults to false
     * @param name an optional name for the arguments, used in languages that have keywords
     * arguments that follow indexed arguments
     */
    @JvmOverloads
    fun addArgument(expression: Expression, isDefault: Boolean = false, name: String? = null) {
        val propertyEdge = PropertyEdge(this, expression)
        propertyEdge.addProperty(Properties.INDEX, arguments.size)
        propertyEdge.addProperty(Properties.DEFAULT, isDefault)

        name?.let { propertyEdge.addProperty(Properties.NAME, it) }

        this.argumentsEdges.add(propertyEdge)
    }

    fun setArgument(index: Int, argument: Expression) {
        this.argumentsEdges[index].end = argument
    }

    override fun typeChanged(src: HasType, root: HasType, oldType: Type) {
        if (src === base) {
            fqn = src.type.root.typeName + "." + getName()
        } else {
            val previous = type
            val types = invokes.mapNotNull { it.type }
            val alternative = types.firstOrNull()
            val commonType = TypeManager.getInstance().getCommonType(types).orElse(alternative)
            val subTypes: MutableSet<Type> = HashSet(possibleSubTypes)

            subTypes.remove(oldType)
            subTypes.addAll(types)
            setType(commonType, root)
            setPossibleSubTypes(subTypes, root)

            if (previous != type) {
                type.typeOrigin = Type.Origin.DATAFLOW
            }
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: HasType, oldSubTypes: Set<Type>) {
        if (src !== base) {
            val subTypes: MutableSet<Type> = HashSet(possibleSubTypes)
            subTypes.addAll(src.possibleSubTypes)
            setPossibleSubTypes(subTypes, root)
        }
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("base", base)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CallExpression) {
            return false
        }

        return (super.equals(other) &&
            arguments == other.arguments &&
            arguments == other.arguments &&
            invokesEdges == other.invokesEdges &&
            invokes == other.invokes &&
            base == other.base)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
