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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.flows.Invokes
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.edges.unwrappingIncoming
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.DynamicType
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.buildSignature
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.graph.types.HasSecondaryTypeEdge
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.functional.EqualLinkedHashSet
import de.fraunhofer.aisec.cpg.helpers.functional.equalLinkedHashSetOf
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents the declaration or definition of a function. */
open class FunctionDeclaration :
    ValueDeclaration(),
    DeclarationHolder,
    EOGStarterHolder,
    HasType.TypeObserver,
    HasSecondaryTypeEdge {
    @Relationship("BODY") var bodyEdge = astOptionalEdgeOf<Statement>()
    /** The function body. Usually a [Block]. */
    var body by unwrapping(FunctionDeclaration::bodyEdge)

    /** The list of function parameters. */
    @Relationship(value = "PARAMETERS", direction = Relationship.Direction.OUTGOING)
    var parameterEdges = astEdgesOf<ParameterDeclaration>()
    /** Virtual property for accessing [parameterEdges] without property edges. */
    var parameters by unwrapping(FunctionDeclaration::parameterEdges)

    @Relationship(value = "THROWS_TYPES", direction = Relationship.Direction.OUTGOING)
    var throwsTypes = mutableListOf<Type>()

    @Relationship(value = "OVERRIDES", direction = Relationship.Direction.INCOMING)
    val overriddenBy = mutableListOf<FunctionDeclaration>()

    @Relationship(value = "OVERRIDES", direction = Relationship.Direction.OUTGOING)
    val overrides = mutableListOf<FunctionDeclaration>()

    /**
     * The mirror property for [CallExpression.invokeEdges]. This holds all incoming [Invokes] edges
     * from [CallExpression] nodes to this function.
     */
    @Relationship(value = "INVOKES", direction = Relationship.Direction.INCOMING)
    val calledByEdges: Invokes<FunctionDeclaration> =
        Invokes<FunctionDeclaration>(this, CallExpression::invokeEdges, outgoing = false)

    /** Virtual property for accessing [calledByEdges] without property edges. */
    val calledBy: MutableList<CallExpression> by
        unwrappingIncoming(FunctionDeclaration::calledByEdges)

    /** The list of return types. The default is an empty list. */
    var returnTypes = listOf<Type>()

    /**
     * Specifies, whether this function declaration is also a definition, i.e. has a function body
     * definition.
     */
    var isDefinition = false

    /** If this is only a declaration, this provides a link to the definition of the function. */
    @Relationship(value = "DEFINES")
    var definition: FunctionDeclaration? = null
        get() {
            return if (isDefinition) this else field
        }

    /**
     * Saves the information on which parameter(s) of the function are modified by the function.
     * This is interesting since we need to add DFG edges between the modified parameter and the
     * respective argument(s). For each [ParameterDeclaration] as well as the
     * [MethodDeclaration.receiver] that has some incoming DFG-edge within this
     * [FunctionDeclaration], we store all previous DFG nodes. The map stores a List of FSEntries
     * for each modified parameter. `derefDst` indicates if we write to the parameter's value or
     * it's dereferenced value, `srcNode` indicates the new source value and `derefSource` if it
     * should be dereferenced Additionally, `subAccessName` indicates sub-accesses, i.e. to parts of
     * a struct or to array-expressions
     */
    data class FSEntry(
        val destValueDepth: Int =
            1, // 0: Address, 1: Value, 2: DerefValue, 3: DerefderefValue, ....
        val srcNode: Node?,
        val srcValueDepth: Int = 1, // 0: Address, 1: Value, 2: DerefValue, 3:
        val subAccessName: String,
        // Node which a set of possible properties, such as a callingcontext
        val lastWrites: EqualLinkedHashSet<Pair<Node, EqualLinkedHashSet<Any>>> =
            equalLinkedHashSetOf(),
        // Additional properties such the granularity or the shortFS
        // We use shortFunctionSummaries to draw "short" DFG-Edges that allow us to follow DFG Paths
        // without going into functions. Not as detailed, but faster
        val properties: EqualLinkedHashSet<Any> = equalLinkedHashSetOf(),
        // Sometimes, we need a dummy of a functionSummary, for example to avoid recursion. We
        // indicate here if this is one
        val isDummy: Boolean = false,
    )

    var functionSummary = mutableMapOf<Node, MutableSet<FSEntry>>()

    /** Returns true, if this function has a [body] statement. */
    fun hasBody(): Boolean {
        return body != null
    }

    val signature: String
        get() = buildSignature(this, returnTypes)

    fun isOverrideCandidate(other: FunctionDeclaration): Boolean {
        return other.name.localName == name.localName &&
            other.type == type &&
            other.signature == signature
    }

    fun addOverriddenBy(c: Collection<FunctionDeclaration>) {
        for (functionDeclaration in c) {
            addOverriddenBy(functionDeclaration)
        }
    }

    fun addOverriddenBy(functionDeclaration: FunctionDeclaration) {
        addIfNotContains(overriddenBy, functionDeclaration)
    }

    fun addOverrides(functionDeclaration: FunctionDeclaration) {
        addIfNotContains(overrides, functionDeclaration)
    }

    fun addThrowTypes(type: Type) {
        throwsTypes.add(type)
    }

    fun addThrowTypes(collection: Collection<Type>) {
        for (type in collection) {
            addThrowTypes(type)
        }
    }

    /**
     * A list of default expressions for each item in [parameters]. If a [ParameterDeclaration] has
     * no default, the list will be null at this index. This list must have the same size as
     * [parameters].
     */
    val defaultParameters: List<Expression?>
        get() {
            return parameters.map { it.default }
        }

    val signatureTypes: List<Type>
        get() = parameters.map { it.type }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("parameters", parameters)
            .toString()
    }

    @DoNotPersist
    override val eogStarters: List<Node>
        get() = listOfNotNull(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is FunctionDeclaration) {
            return false
        }
        return (super.equals(other) &&
            body == other.body &&
            parameters == other.parameters &&
            propertyEqualsList(parameterEdges, other.parameterEdges) &&
            throwsTypes == other.throwsTypes)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), body, parameters, throwsTypes)

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is ParameterDeclaration) {
            addIfNotContains(parameterEdges, declaration)
        }
    }

    @DoNotPersist
    override val declarations: List<Declaration>
        get() {
            val list = ArrayList<Declaration>()
            list.addAll(parameters)
            return list
        }

    /** This returns a simple heuristic for the complexity of a function declaration. */
    val complexity: Int
        get() {
            return this.body?.cyclomaticComplexity ?: 0
        }

    override val secondaryTypes: List<Type>
        get() = returnTypes + throwsTypes + signatureTypes

    override fun typeChanged(newType: Type, src: HasType) {
        // We cannot really change the "type" of a function declaration, we want to stick to the
        // assigned type
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // We want to propagate the assigned types to the return type of the function and adjust the
        // function's type accordingly, but we only do this for dynamic types. And we only support
        // one return type for now.
        if (returnTypes.singleOrNull() !is DynamicType) {
            return
        }

        // Build new function types out of our function declaration and the assigned types
        var returnFuncTypes =
            assignedTypes.map { computeType(this, returnTypes = listOf(it)) }.toSet()

        // And assign it us
        addAssignedTypes(returnFuncTypes)
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return setOf()
    }

    override fun getExitNextEOG(): Collection<Node> {
        return setOf()
    }

    companion object {
        const val WHITESPACE = " "
        const val BRACKET_LEFT = "("
        const val COMMA = ","
        const val BRACKET_RIGHT = ")"
    }
}

/** This is a very basic implementation of Cyclomatic Complexity. */
val Statement.cyclomaticComplexity: Int
    get() {
        var i = 0
        for (stmt in (this as? StatementHolder)?.statements ?: listOf(this)) {
            when (stmt) {
                is ForEachStatement -> {
                    // add one and include the children
                    i += (stmt.statement?.cyclomaticComplexity ?: 0) + 1
                }
                is ForStatement -> {
                    // add one and include the children
                    i += (stmt.statement?.cyclomaticComplexity ?: 0) + 1
                }
                is IfStatement -> {
                    // add one for each branch (and include the children)
                    stmt.thenStatement?.let { i += it.cyclomaticComplexity + 1 }
                    stmt.elseStatement?.let { i += it.cyclomaticComplexity + 1 }
                }
                is SwitchStatement -> {
                    // forward it to the block containing the case statements
                    stmt.statement?.let { i += it.cyclomaticComplexity }
                }
                is CaseStatement -> {
                    // add one for each branch (and include the children)
                    stmt.caseExpression?.let { i += it.cyclomaticComplexity }
                }
                is DoStatement -> {
                    // add one for the do statement (and include the children)
                    i += (stmt.statement?.cyclomaticComplexity ?: 0) + 1
                }
                is WhileStatement -> {
                    // add one for the while statement (and include the children)
                    i += (stmt.statement?.cyclomaticComplexity ?: 0) + 1
                }
                is GotoStatement -> {
                    // add one
                    i++
                }
                is StatementHolder -> {
                    i += stmt.cyclomaticComplexity
                }
            }
        }

        return i
    }
