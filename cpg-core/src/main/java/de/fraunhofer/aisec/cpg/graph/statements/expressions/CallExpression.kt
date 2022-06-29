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
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.*
import java.util.stream.Collectors
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * An expression, which calls another function. It has a list of arguments (list of [ ]s) and is
 * connected via the INVOKES edge to its [FunctionDeclaration].
 */
open class CallExpression : Expression(), HasType.TypeListener, HasBase, SecondaryTypeEdge {
    /** Connection to its [FunctionDeclaration]. This will be populated by the [ ]. */
    @Relationship(value = "INVOKES", direction = "OUTGOING")
    var invokesPropertyEdge: List<PropertyEdge<FunctionDeclaration>> = ArrayList()
        protected set

    var invokes: List<FunctionDeclaration>
        get(): List<FunctionDeclaration> {
            val targets: MutableList<FunctionDeclaration> = ArrayList()
            for (propertyEdge in invokesPropertyEdge) {
                targets.add(propertyEdge.end)
            }
            return Collections.unmodifiableList(targets)
        }
        set(value) {
            unwrap(invokesPropertyEdge).forEach {
                it.unregisterTypeListener(this)
                Util.detachCallParameters(it, arguments)
                removePrevDFG(it)
            }
            invokesPropertyEdge = transformIntoOutgoingPropertyEdgeList(value, this)
            value.forEach {
                it.registerTypeListener(this)
                Util.attachCallParameters(it, arguments)
                addPrevDFG(it)
            }
        }

    /** The list of arguments of this call expression, backed by a list of [PropertyEdge]s. */
    @Relationship(value = "ARGUMENTS", direction = "OUTGOING")
    @field:SubGraph("AST")
    var argumentEdges: MutableList<PropertyEdge<Expression>> = ArrayList()

    /**
     * The list of arguments as a simple list. This is a delegated property delegated to
     * [argumentEdges].
     */
    var arguments: List<Expression> by PropertyEdgeDelegate(CallExpression::argumentEdges)

    /**
     * The base object. This is marked as an AST child, because this is required for [ ]. Be aware
     * that for simple calls the implicit "this" base is not part of the original AST, but we treat
     * it as such for better consistency
     */
    @field:SubGraph("AST")
    override var base: Expression? = null
        set(value) {
            field?.unregisterTypeListener(this)
            field = value
            value?.registerTypeListener(this)
        }

    var fqn: String? = null

    fun setArgument(index: Int, argument: Expression) {
        argumentEdges[index].end = argument
    }

    @JvmOverloads
    fun addArgument(expression: Expression, name: String? = null) {
        val propertyEdge = PropertyEdge(this, expression)
        propertyEdge.addProperty(Properties.INDEX, argumentEdges.size)
        if (name != null) {
            propertyEdge.addProperty(Properties.NAME, name)
        }
        argumentEdges.add(propertyEdge)
    }

    val signature: List<Type>
        get() =
            arguments.stream().map { obj: Expression -> obj.getType() }.collect(Collectors.toList())
    var template = false

    /** If the CallExpression instantiates a Template, the call can provide template parameters */
    @Relationship(value = "TEMPLATE_PARAMETERS", direction = "OUTGOING")
    @field:SubGraph("AST")
    var templateParametersEdge: MutableList<PropertyEdge<Node>>? = null
        set(value) {
            field = value
            template = value != null
        }

    val templateParameters: List<Node>
        get(): List<Node> {
            return unwrap(templateParametersEdge ?: listOf())
        }

    /**
     * If the CallExpression instantiates a Template the CallExpression is connected to the template
     * which is instantiated. This is required by the expansion pass to access the Template
     * directly. The invokes edge will still point to the realization of the template.
     */
    @Relationship(value = "TEMPLATE_INSTANTIATION", direction = "OUTGOING")
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

    private fun addTemplateParameter(
        typeTemplateParam: Type,
        templateInitialization: TemplateInitialization?
    ) {
        if (templateParametersEdge == null) {
            templateParametersEdge = mutableListOf()
        }

        val propertyEdge = PropertyEdge<Node>(this, typeTemplateParam)
        propertyEdge.addProperty(Properties.INDEX, templateParameters.size)
        propertyEdge.addProperty(Properties.INSTANTIATION, templateInitialization)
        templateParametersEdge!!.add(propertyEdge)
        template = true
    }

    private fun replaceTypeTemplateParameter(oldType: Type?, newType: Type) {
        for (i in templateParametersEdge?.indices ?: listOf()) {
            val propertyEdge = templateParametersEdge!![i]
            if (propertyEdge.end == oldType) {
                propertyEdge.end = newType
            }
        }
    }

    private fun addTemplateParameter(
        expressionTemplateParam: Expression,
        templateInitialization: TemplateInitialization?
    ) {
        if (templateParametersEdge == null) {
            templateParametersEdge = mutableListOf()
        }

        val propertyEdge = PropertyEdge<Node>(this, expressionTemplateParam)
        propertyEdge.addProperty(Properties.INDEX, templateParametersEdge!!.size)
        propertyEdge.addProperty(Properties.INSTANTIATION, templateInitialization)
        templateParametersEdge!!.add(propertyEdge)
        template = true
    }

    fun addTemplateParameter(
        templateParam: Node?,
        templateInitialization: TemplateInitialization?
    ) {
        if (templateParam is Expression) {
            addTemplateParameter(templateParam, templateInitialization)
        } else if (templateParam is Type) {
            addTemplateParameter(templateParam, templateInitialization)
        }
    }

    fun addExplicitTemplateParameter(templateParameter: Node?) {
        addTemplateParameter(templateParameter, TemplateInitialization.EXPLICIT)
    }

    fun addExplicitTemplateParameters(templateParameters: List<Node?>) {
        for (node in templateParameters) {
            addTemplateParameter(node, TemplateInitialization.EXPLICIT)
        }
    }

    fun removeRealization(templateParam: Node?) {
        templateParametersEdge?.removeIf { propertyEdge: PropertyEdge<Node> ->
            propertyEdge.end == templateParam
        }
    }

    fun updateTemplateParameters(
        initializationType: Map<Node?, TemplateInitialization?>,
        orderedInitializationSignature: List<Node>
    ) {
        if (templateParametersEdge == null) {
            templateParametersEdge = mutableListOf()
        }

        for (edge in templateParametersEdge!!) {
            if (edge.getProperty(Properties.INSTANTIATION) != null &&
                    (edge.getProperty(Properties.INSTANTIATION) ==
                        TemplateInitialization.UNKNOWN) &&
                    initializationType.containsKey(edge.end)
            ) {
                edge.addProperty(Properties.INSTANTIATION, initializationType[edge.end])
            }
        }

        for (i in templateParametersEdge!!.size until orderedInitializationSignature.size) {
            val signature = orderedInitializationSignature[i] ?: continue
            val propertyEdge = PropertyEdge(this, orderedInitializationSignature[i])
            propertyEdge.addProperty(Properties.INDEX, templateParametersEdge!!.size)
            propertyEdge.addProperty(
                Properties.INSTANTIATION,
                initializationType.getOrDefault(
                    orderedInitializationSignature[i],
                    TemplateInitialization.UNKNOWN
                )
            )
            templateParametersEdge!!.add(propertyEdge)
        }
    }

    fun instantiatesTemplate(): Boolean {
        return templateInstantiation != null || templateParametersEdge != null || template
    }

    override fun typeChanged(src: HasType, root: List<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        if (src === base) {
            fqn = src.getType().root.typeName + "." + name
        } else {
            val previous = type
            val types =
                invokesPropertyEdge
                    .map(PropertyEdge<FunctionDeclaration>::end)
                    .map { it.type }
                    .filter(Objects::nonNull)
            val alternative = if (types.isNotEmpty()) types[0] else null
            val commonType = TypeManager.getInstance().getCommonType(types).orElse(alternative)
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
            propertyEqualsList(argumentEdges, other.argumentEdges)) &&
            invokes == other.invokes &&
            propertyEqualsList(invokesPropertyEdge, other.invokesPropertyEdge)) &&
            base == other.base &&
            templateParameters == other.templateParameters &&
            propertyEqualsList(templateParametersEdge, other.templateParametersEdge)) &&
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
