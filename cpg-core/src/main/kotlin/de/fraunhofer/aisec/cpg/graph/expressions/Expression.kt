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
package de.fraunhofer.aisec.cpg.graph.expressions

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.UnknownLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.MemoryAddressEdges
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdges
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflows
import de.fraunhofer.aisec.cpg.graph.edges.memoryAddressEdgesOf
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import de.fraunhofer.aisec.cpg.persistence.Relationship
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Represents one expression. It is used as a base class for multiple different types of
 * expressions. The only constraint is, that each expression has a type. The result of an expression
 * can also be ignored, e.g. terminated with ; to make it a statement.
 *
 * This [Node] is the most basic node type that represents source code elements which represents
 * executable code.
 */
abstract class Expression(usedAsExpression: Boolean = true) :
    AstNode(), DeclarationHolder, HasType, HasMemoryAddress, HasMemoryValue {

    /**
     * This property specifies that this node is used as an expression. Depending on the language,
     * an expression can be terminated by a ";" or a newline to be a statement. Meanwhile, some
     * languages allow using what normally is considered a statement, as an expression with a normal
     * or an empty value and type. Depending on the node, type the default will be true or false.
     */
    open var usedAsExpression = true

    @DoNotPersist override var observerEnabled: Boolean = true

    init {
        this.usedAsExpression = usedAsExpression
    }

    /**
     * Per default, expressions only read Data. The access value can be changed to modify this
     * modeling and determine the dataflow direction
     */
    open var access: AccessValues = AccessValues.READ

    /** Lazy backing field for [localEdges]. */
    private var _localEdges: AstEdges<ValueDeclaration, AstEdge<ValueDeclaration>>? = null

    /**
     * A list of local variables (or other values) associated to this statement, defined by their
     * [ValueDeclaration] extracted from Block because `for`, `while`, `if`, and `switch` can
     * declare locals in their condition or initializers.
     *
     * The backing container is allocated lazily on first access: most expressions declare no
     * locals, and [astEdgesOf] eagerly allocates a backing array. See [localsEqual] and [hashCode],
     * which avoid forcing this allocation during structural comparisons.
     *
     * TODO: This is actually an AST node just for a subset of nodes, i.e. initializers in for-loops
     */
    @Relationship(value = "LOCALS", direction = Relationship.Direction.OUTGOING)
    var localEdges: AstEdges<ValueDeclaration, AstEdge<ValueDeclaration>>
        get() = _localEdges ?: astEdgesOf<ValueDeclaration>().also { _localEdges = it }
        set(value) {
            _localEdges = value
        }

    /** Virtual property to access [localEdges] without property edges. */
    @DoNotPersist
    var locals: MutableList<ValueDeclaration>
        get() = localEdges.unwrap()
        set(value) {
            localEdges.resetTo(value)
        }

    @DoNotPersist override val typeObservers: MutableSet<HasType.TypeObserver> = identitySetOf()

    override var language: Language<*> = UnknownLanguage
        set(value) {
            // We need to adjust an eventual unknown type, once we know the language
            field = value
            if (type is UnknownType) {
                type = UnknownType.getUnknownType(value)
            }
        }

    override var type: Type = unknownType()
        set(value) {
            val old = field
            field = value

            // Only inform our observer if the type has changed. This should not trigger if we
            // "squash" types into one, because they should still be regarded as "equal", but not
            // the "same".
            if (old != value) {
                informObservers(HasType.TypeObserver.ChangeType.TYPE)
            }

            // We also want to add the definitive type (if known) to our assigned types
            if (value !is UnknownType && value !is AutoType) {
                addAssignedType(value)
            }
        }

    override var assignedTypes: Set<Type> = mutableSetOf()
        set(value) {
            if (field == value) {
                return
            }

            field = value
            informObservers(HasType.TypeObserver.ChangeType.ASSIGNED_TYPE)
        }

    /** Lazy backing field for [memoryValueEdges]. */
    private var _memoryValueEdges: Dataflows<Node>? = null

    /**
     * Each Expression also has a MemoryValue.
     *
     * This and the other two memory-model containers ([memoryValueUsageEdges],
     * [memoryAddressEdges]) are only populated by the DFG and points-to passes and stay empty on
     * the majority of expressions. Their backing containers are therefore allocated lazily on first
     * access. They are not part of [equals]/[hashCode], so lazy-on-access is safe.
     */
    @Relationship
    override var memoryValueEdges: Dataflows<Node>
        get() =
            _memoryValueEdges
                ?: Dataflows<Node>(
                        this,
                        mirrorProperty = HasMemoryValue::memoryValueUsageEdges,
                        outgoing = false,
                    )
                    .also { _memoryValueEdges = it }
        set(value) {
            _memoryValueEdges = value
        }

    /** Virtual property for accessing [memoryValueEdges] as plain nodes. */
    @DoNotPersist
    override var memoryValues: MutableSet<Node>
        get() = memoryValueEdges.unwrap()
        set(value) {
            memoryValueEdges.resetTo(value)
        }

    /** Lazy backing field for [memoryValueUsageEdges]. */
    private var _memoryValueUsageEdges: Dataflows<Node>? = null

    /**
     * Where the memory value of this Expression is used (allocated lazily, see [memoryValueEdges]).
     */
    @Relationship
    override var memoryValueUsageEdges: Dataflows<Node>
        get() =
            _memoryValueUsageEdges
                ?: Dataflows<Node>(
                        this,
                        mirrorProperty = HasMemoryValue::memoryValueEdges,
                        outgoing = true,
                    )
                    .also { _memoryValueUsageEdges = it }
        set(value) {
            _memoryValueUsageEdges = value
        }

    /** Virtual property for accessing [memoryValueUsageEdges] as plain nodes. */
    @DoNotPersist
    override var memoryValueUsages: MutableSet<Node>
        get() = memoryValueUsageEdges.unwrap()
        set(value) {
            memoryValueUsageEdges.resetTo(value)
        }

    /** Lazy backing field for [memoryAddressEdges]. */
    private var _memoryAddressEdges: MemoryAddressEdges? = null

    /** Each Expression also has a MemoryAddress (allocated lazily, see [memoryValueEdges]). */
    @Relationship
    override var memoryAddressEdges: MemoryAddressEdges
        get() =
            _memoryAddressEdges
                ?: memoryAddressEdgesOf(mirrorProperty = MemoryAddress::usageEdges, outgoing = true)
                    .also { _memoryAddressEdges = it }
        set(value) {
            _memoryAddressEdges = value
        }

    /** Virtual property for accessing [memoryAddressEdges] as plain nodes. */
    @DoNotPersist
    override var memoryAddresses: MutableSet<MemoryAddress>
        get() = memoryAddressEdges.unwrap()
        set(value) {
            memoryAddressEdges.resetTo(value)
        }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("type", type)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Expression) return false
        return super.equals(other) && localsEqual(other) && type == other.type
    }

    /**
     * Compares the locals of `this` and [other] without forcing allocation of the lazily-created
     * [localEdges] container when both sides are empty (the common case).
     */
    private fun localsEqual(other: Expression): Boolean {
        val thisEmpty = _localEdges?.isEmpty() != false
        val otherEmpty = other._localEdges?.isEmpty() != false
        if (thisEmpty && otherEmpty) return true
        return locals == other.locals && propertyEqualsList(localEdges, other.localEdges)
    }

    override fun hashCode(): Int {
        // Exactly matches the previous `Objects.hash(super.hashCode(), locals)`. An empty unwrapped
        // locals collection hashes to `Objects.hash(collection.size)` == `Objects.hash(0)` == 31
        // (see UnwrappedEdgeCollection.hashCode); using that constant for the empty case avoids
        // forcing the lazy [localEdges] container to be allocated during hashing.
        val localsHash = if (_localEdges?.isEmpty() != false) 31 else locals.hashCode()
        return 31 * (31 + super.hashCode()) + localsHash
    }

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is Variable) {
            addIfNotContains(localEdges, declaration)
        } else if (declaration is Function) {
            addIfNotContains(localEdges, declaration)
        }
    }

    override val declarations: List<Declaration>
        get() = locals
}
