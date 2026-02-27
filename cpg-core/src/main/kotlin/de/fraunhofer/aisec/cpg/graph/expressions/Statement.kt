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
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import org.apache.commons.lang3.builder.ToStringBuilder
import java.util.*
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Transient

/**
 * This [Node] is the most basic node type that represents source code elements which represents
 * executable code.
 */
@NodeEntity
abstract class Statement : AstNode(), DeclarationHolder, HasType {

    /**
     * Per default, expressions only read Data. The access value can be changed to modify
     * this modeling and determine the dataflow direction
     */
    open var access: AccessValues = AccessValues.READ

    /**
     * A list of local variables (or other values) associated to this statement, defined by their
     * [ValueDeclaration] extracted from Block because `for`, `while`, `if`, and `switch` can
     * declare locals in their condition or initializers.
     *
     * TODO: This is actually an AST node just for a subset of nodes, i.e. initializers in for-loops
     */
    @Relationship(value = "LOCALS", direction = Relationship.Direction.OUTGOING)
    var localEdges = astEdgesOf<ValueDeclaration>()

    /** Virtual property to access [localEdges] without property edges. */
    var locals by unwrapping(Statement::localEdges)

    /**
     * This property specifies that this node is used as an expression. Depending on the language, an expression can
     * be terminated by a ";" or a newline to be a statement. Meanwhile, some languages allow using what normally is
     * considered a statement, as an expression with a normal or an empty value and type. Depending on the node, type
     * the default will be true or false.
     */
    open var usedAsExpression = true


    @DoNotPersist override var observerEnabled: Boolean = true


    @Transient override val typeObservers: MutableSet<HasType.TypeObserver> = identitySetOf()


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

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("type", type)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Statement) return false
        return super.equals(other) &&
            locals == other.locals &&
            propertyEqualsList(localEdges, other.localEdges)
            && type == other.type
    }

    override fun hashCode() = Objects.hash(super.hashCode(), locals)

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
