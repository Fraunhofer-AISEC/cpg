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
import de.fraunhofer.aisec.cpg.graph.declarations.Template.TemplateInitialization
import de.fraunhofer.aisec.cpg.graph.edges.*
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.ast.TemplateArguments
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.flows.Invokes
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * An expression, which calls another function. It has a list of arguments (list of [Expression]s)
 * and is connected via the INVOKES edge to its [FunctionDeclaration].
 */
open class CallExpression :
    Expression(),
    HasOverloadedOperation,
    HasType.TypeObserver,
    ArgumentHolder,
    HasSecondaryTypeEdge {
    /**
     * Connection to its [FunctionDeclaration]. This will be populated by the [SymbolResolver]. This
     * will have an effect on the [type]
     */
    @PopulatedByPass(SymbolResolver::class)
    @Relationship(value = "INVOKES", direction = Relationship.Direction.OUTGOING)
    var invokeEdges: Invokes<FunctionDeclaration> =
        Invokes<FunctionDeclaration>(
            this,
            mirrorProperty = FunctionDeclaration::calledByEdges,
            outgoing = true,
        )
        protected set

    /**
     * A virtual property to quickly access the list of declarations that this call invokes without
     * property edges.
     */
    @PopulatedByPass(SymbolResolver::class) var invokes by unwrapping(CallExpression::invokeEdges)

    /** The list of arguments of this call expression, backed by a list of [Edge] objects. */
    @Relationship(value = "ARGUMENTS", direction = Relationship.Direction.OUTGOING)
    var argumentEdges = astEdgesOf<Expression>()

    /**
     * The list of arguments as a simple list. This is a delegated property delegated to
     * [argumentEdges].
     */
    var arguments by unwrapping(CallExpression::argumentEdges)

    /** The list of argument types (aka the signature). */
    val signature: List<Type>
        get() {
            return argumentEdges.map { it.end.type }
        }

    /**
     * The expression that is being "called". This is currently not yet used in the [SymbolResolver]
     * but will be in the future. In most cases, this is a [Reference] and its [Reference.refersTo]
     * is intentionally left empty. It is not filled by the [SymbolResolver].
     */
    @Relationship(value = "CALLEE", direction = Relationship.Direction.OUTGOING)
    private var calleeEdge = astEdgeOf<Expression>(ProblemExpression("could not parse callee"))

    var callee by unwrapping(CallExpression::calleeEdge)

    /**
     * The [Name] of this call expression, based on its [callee].
     * * For simple calls, this is just the name of the [callee], e.g., a reference to a function
     * * For simple function pointers we want to prefix a *
     * * For class based function pointers we want to build a name like MyClass::*pointer
     */
    override var name: Name
        get() {
            val value = callee
            return if (value is UnaryOperator && value.input.type is FunctionPointerType) {
                value.input.name
            } else if (value is BinaryOperator && value.rhs.type is FunctionPointerType) {
                value.lhs.type.name.fqn("*" + value.rhs.name.localName)
            } else {
                value.name
            }
        }
        set(_) {
            // read-only
        }

    fun setArgument(index: Int, argument: Expression) {
        argumentEdges[index].end = argument
    }

    override fun addArgument(expression: Expression) {
        return addArgument(expression, null)
    }

    /** Adds the specified [expression] with an optional [name] to this call. */
    fun addArgument(expression: Expression, name: String? = null) {
        val edge = AstEdge(this, expression)
        edge.name = name

        argumentEdges.add(edge)
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        // First, we need to find the old index
        val idx = this.arguments.indexOf(old)
        if (idx == -1) {
            return false
        }

        setArgument(idx, new)
        return true
    }

    override fun hasArgument(expression: Expression): Boolean {
        return expression in this.arguments
    }

    override fun removeArgument(expression: Expression): Boolean {
        arguments -= expression
        return true
    }

    /** Specifies, whether this call has any template arguments. */
    var template = false

    /** If the CallExpression instantiates a template, the call can provide template arguments. */
    @Relationship(value = "TEMPLATE_ARGUMENTS", direction = Relationship.Direction.OUTGOING)
    var templateArgumentEdges: TemplateArguments<AstNode>? = null
        set(value) {
            field = value
            template = value != null
        }

    val templateArguments: List<AstNode>
        get(): List<AstNode> {
            return templateArgumentEdges?.toNodeCollection() ?: listOf()
        }

    /**
     * If the CallExpression instantiates a Template the CallExpression is connected to the template
     * which is instantiated. This is required by the expansion pass to access the Template
     * directly. The invokes edge will still point to the realization of the template.
     */
    @Relationship(value = "TEMPLATE_INSTANTIATION", direction = Relationship.Direction.OUTGOING)
    var templateInstantiation: Template? = null
        set(value) {
            field = value
            template = value != null
        }

    /**
     * Adds a template parameter to this call expression. A parameter can either be an [Expression]
     * (usually a [Literal]) or a [Type].
     */
    @JvmOverloads
    fun addTemplateParameter(
        templateParam: AstNode,
        templateInitialization: TemplateInitialization? = TemplateInitialization.EXPLICIT,
    ) {
        if (templateParam is Expression) {
            if (templateArgumentEdges == null) {
                templateArgumentEdges = TemplateArguments(this)
            }

            templateArgumentEdges?.add(templateParam) { instantiation = templateInitialization }
            template = true
        }
    }

    fun updateTemplateParameters(
        initializationType: Map<AstNode?, TemplateInitialization?>,
        orderedInitializationSignature: List<AstNode>,
    ) {
        if (templateArgumentEdges == null) {
            templateArgumentEdges = TemplateArguments(this)
        }

        for (edge in templateArgumentEdges ?: listOf()) {
            if (
                edge.instantiation != null &&
                    (edge.instantiation == TemplateInitialization.UNKNOWN) &&
                    initializationType.containsKey(edge.end)
            ) {
                edge.instantiation = initializationType[edge.end]
            }
        }

        for (i in (templateArgumentEdges?.size ?: 0) until orderedInitializationSignature.size) {
            templateArgumentEdges?.add(orderedInitializationSignature[i]) {
                instantiation =
                    initializationType.getOrDefault(
                        orderedInitializationSignature[i],
                        TemplateInitialization.UNKNOWN,
                    )
            }
        }
    }

    fun instantiatesTemplate(): Boolean {
        return templateInstantiation != null || templateArgumentEdges != null || template
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // If this is a template, we need to ignore incoming type changes, because our template
        // system will explicitly set the type
        if (this.template) {
            return
        }

        if (newType !is FunctionType) {
            return
        }

        if (newType.returnTypes.size == 1) {
            this.type = newType.returnTypes.single()
        } else if (newType.returnTypes.size > 1) {
            this.type = TupleType(newType.returnTypes)
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Propagate assigned func types from the function declaration to the call expression
        val assignedFuncTypes = assignedTypes.filterIsInstance<FunctionType>()
        assignedFuncTypes.forEach {
            if (it.returnTypes.size == 1) {
                addAssignedType(it.returnTypes.single())
            } else if (it.returnTypes.size > 1) {
                addAssignedType(TupleType(it.returnTypes))
            }
        }
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE).appendSuper(super.toString()).toString()
    }

    override val operatorCode: String?
        get() = "()"

    override val operatorArguments: List<Expression>
        get() = arguments

    /**
     * Some languages allow to even overload "()", meaning that basically a normal call to [callee]
     * is overloaded. In this case we want the [operatorBase] to point to [callee], so we can take
     * its type to lookup the necessary [Operator].
     */
    override val operatorBase: Expression
        get() = callee

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CallExpression) return false
        return super.equals(other) &&
            arguments == other.arguments &&
            propertyEqualsList(argumentEdges, other.argumentEdges) &&
            templateArguments == other.templateArguments &&
            propertyEqualsList(templateArgumentEdges, other.templateArgumentEdges) &&
            templateInstantiation == other.templateInstantiation &&
            template == other.template
    }

    // TODO: Not sure if we can add the template, templateParameters, templateInstantiation fields
    //  here
    override fun hashCode() = Objects.hash(super.hashCode(), arguments)

    override val secondaryTypes: List<Type>
        get() = signature

    override fun getStartingPrevEOG(): Collection<Node> {
        return if (this.callee is ProblemExpression)
            this.arguments.firstOrNull()?.getStartingPrevEOG() ?: this.prevEOG
        else this.callee.getStartingPrevEOG()
    }
}
