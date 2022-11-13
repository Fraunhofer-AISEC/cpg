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
import de.fraunhofer.aisec.cpg.graph.HasType.SecondaryTypeEdge
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration.TemplateInitialization
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.transformIntoOutgoingPropertyEdgeList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.CallResolver
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * An expression, which calls another function. It has a list of arguments (list of [Expression]s)
 * and is connected via the INVOKES edge to its [FunctionDeclaration].
 */
open class CallExpression : Expression(), HasType.TypeListener, HasBase, SecondaryTypeEdge {
    /** Connection to its [FunctionDeclaration]. This will be populated by the [CallResolver]. */
    @Relationship(value = "INVOKES", direction = Relationship.OUTGOING)
    @PopulatedByPass(CallResolver::class)
    var invokesRelationship = mutableListOf<PropertyEdge<FunctionDeclaration>>()
        protected set

    /**
     * A virtual property to quickly access the list of declarations that this call invokes without
     * property edges.
     */
    var invokes: List<FunctionDeclaration>
        get(): List<FunctionDeclaration> {
            val targets: MutableList<FunctionDeclaration> = ArrayList()
            for (propertyEdge in invokesRelationship) {
                targets.add(propertyEdge.end)
            }
            return Collections.unmodifiableList(targets)
        }
        set(value) {
            unwrap(invokesRelationship).forEach { it.unregisterTypeListener(this) }
            invokesRelationship = transformIntoOutgoingPropertyEdgeList(value, this)
            value.forEach { it.registerTypeListener(this) }
        }

    /**
     * The list of arguments of this call expression, backed by a list of [PropertyEdge] objects.
     */
    @Relationship(value = "ARGUMENTS", direction = Relationship.OUTGOING)
    @field:SubGraph("AST")
    var argumentsEdges = mutableListOf<PropertyEdge<Expression>>()

    /**
     * The list of arguments as a simple list. This is a delegated property delegated to
     * [argumentsEdges].
     */
    var arguments by PropertyEdgeDelegate(CallExpression::argumentsEdges)

    /**
     * The base object. This is marked as an AST child, because this is required for a
     * [MemberCallExpression]. Be aware that for simple calls the implicit "this" base is not part
     * of the original AST, but we treat it as such for better consistency
     */
    @field:SubGraph("AST")
    override var base: Expression? = null
        set(value) {
            field?.unregisterTypeListener(this)
            field = value
            value?.registerTypeListener(this)
        }

    /**
     * The expression that is being "called". This is currently not yet used in the [CallResolver]
     * but will be in the future. In most cases, this is a [DeclaredReferenceExpression] and its
     * [DeclaredReferenceExpression.refersTo] is intentionally left empty. It is not filled by the
     * [VariableUsageResolver].
     */
    @field:SubGraph("AST")
    var callee: Expression? = null
        set(value) {
            field?.unregisterTypeListener(this)

            field = value
            // We also want to update this node's name, based on the callee. This is purely for
            // readability reasons. We have a special handling for function pointers, where we want
            // to have the name of the variable. This might change in the future.
            this.fullName.localName =
                if (value is UnaryOperator && value.input.type is FunctionPointerType) {
                    value.input.fullName.localName
                } else {
                    value?.fullName?.localName ?: ""
                }

            // Register the callee as a type listener for this call expressions. Once we re-design
            // call resolution, we need to probably do this in the opposite way so that the call
            // expressions listens for the type of the callee.
            field?.registerTypeListener(this)
        }

    fun setArgument(index: Int, argument: Expression) {
        argumentsEdges[index].end = argument
    }

    /** Adds the specified [expression] with an optional [name] to this call. */
    @JvmOverloads
    fun addArgument(expression: Expression, name: String? = null) {
        val edge = PropertyEdge(this, expression)
        edge.addProperty(Properties.INDEX, argumentsEdges.size)

        if (name != null) {
            edge.addProperty(Properties.NAME, name)
        }

        argumentsEdges.add(edge)
    }

    /** Returns the function signature as list of types of the call arguments. */
    val signature: List<Type>
        get() = argumentsEdges.map { it.end.type }

    /** Specifies, whether this call has any template arguments. */
    var template = false

    /** If the CallExpression instantiates a template, the call can provide template parameters. */
    @Relationship(value = "TEMPLATE_PARAMETERS", direction = Relationship.OUTGOING)
    @field:SubGraph("AST")
    var templateParametersEdges: MutableList<PropertyEdge<Node>>? = null
        set(value) {
            field = value
            template = value != null
        }

    val templateParameters: List<Node>
        get(): List<Node> {
            return unwrap(templateParametersEdges ?: listOf())
        }

    /**
     * If the CallExpression instantiates a Template the CallExpression is connected to the template
     * which is instantiated. This is required by the expansion pass to access the Template
     * directly. The invokes edge will still point to the realization of the template.
     */
    @Relationship(value = "TEMPLATE_INSTANTIATION", direction = Relationship.OUTGOING)
    var templateInstantiation: TemplateDeclaration? = null
        set(value) {
            field = value
            template = value != null
        }

    private val typeTemplateParameters: List<Type>
        get() {
            val types: MutableList<Type> = ArrayList()
            for (n in templateParameters) {
                if (n is Type) {
                    types.add(n)
                }
            }
            return types
        }

    private fun replaceTypeTemplateParameter(oldType: Type?, newType: Type) {
        for (i in templateParametersEdges?.indices ?: listOf()) {
            val propertyEdge = templateParametersEdges!![i]
            if (propertyEdge.end == oldType) {
                propertyEdge.end = newType
            }
        }
    }

    /**
     * Adds a template parameter to this call expression. A parameter can either be an [Expression]
     * (usually a [Literal]) or a [Type].
     */
    @JvmOverloads
    fun addTemplateParameter(
        templateParam: Node,
        templateInitialization: TemplateInitialization? = TemplateInitialization.EXPLICIT
    ) {
        if (templateParam is Expression || templateParam is Type) {
            if (templateParametersEdges == null) {
                templateParametersEdges = mutableListOf()
            }

            val propertyEdge = PropertyEdge(this, templateParam)
            propertyEdge.addProperty(Properties.INDEX, templateParameters.size)
            propertyEdge.addProperty(Properties.INSTANTIATION, templateInitialization)
            templateParametersEdges!!.add(propertyEdge)
            template = true
        }
    }

    fun updateTemplateParameters(
        initializationType: Map<Node?, TemplateInitialization?>,
        orderedInitializationSignature: List<Node>
    ) {
        if (templateParametersEdges == null) {
            templateParametersEdges = mutableListOf()
        }

        for (edge in templateParametersEdges!!) {
            if (
                edge.getProperty(Properties.INSTANTIATION) != null &&
                    (edge.getProperty(Properties.INSTANTIATION) ==
                        TemplateInitialization.UNKNOWN) &&
                    initializationType.containsKey(edge.end)
            ) {
                edge.addProperty(Properties.INSTANTIATION, initializationType[edge.end])
            }
        }

        for (i in templateParametersEdges!!.size until orderedInitializationSignature.size) {
            val propertyEdge = PropertyEdge(this, orderedInitializationSignature[i])
            propertyEdge.addProperty(Properties.INDEX, templateParametersEdges!!.size)
            propertyEdge.addProperty(
                Properties.INSTANTIATION,
                initializationType.getOrDefault(
                    orderedInitializationSignature[i],
                    TemplateInitialization.UNKNOWN
                )
            )
            templateParametersEdges!!.add(propertyEdge)
        }
    }

    fun instantiatesTemplate(): Boolean {
        return templateInstantiation != null || templateParametersEdges != null || template
    }

    override fun typeChanged(src: HasType, root: List<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        if (src === base) {
            fullName =
                Name(
                    fullName.localName,
                    src.getType().root.fullName,
                    language?.namespaceDelimiter ?: "."
                )
        } else {
            val previous = type
            val types =
                invokesRelationship.map(PropertyEdge<FunctionDeclaration>::end).mapNotNull {
                    // TODO(oxisto): Support multiple return values
                    it.returnTypes.firstOrNull()
                }
            val alternative = if (types.isNotEmpty()) types[0] else null
            val commonType =
                TypeManager.getInstance().getCommonType(types, this).orElse(alternative)
            val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)

            subTypes.remove(oldType)
            subTypes.addAll(types)
            setType(commonType, root)
            setPossibleSubTypes(subTypes, root)
            if (previous != type) {
                type.typeOrigin = Type.Origin.DATAFLOW
            }
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: List<HasType>) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        if (src !== base) {
            val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)
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

        return (((super.equals(other) &&
            arguments == other.arguments &&
            propertyEqualsList(argumentsEdges, other.argumentsEdges)) &&
            invokes == other.invokes &&
            propertyEqualsList(invokesRelationship, other.invokesRelationship)) &&
            base == other.base &&
            templateParameters == other.templateParameters &&
            propertyEqualsList(templateParametersEdges, other.templateParametersEdges)) &&
            templateInstantiation == other.templateInstantiation &&
            template == other.template
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun updateType(typeState: Collection<Type>) {
        for (t in typeTemplateParameters) {
            for (t2 in typeState) {
                if (t2 == t) {
                    replaceTypeTemplateParameter(t, t2)
                }
            }
        }
    }
}
