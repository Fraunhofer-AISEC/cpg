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

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.LegacyTypeManager
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.*
import java.util.stream.Collectors
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents the declaration or definition of a function. */
open class FunctionDeclaration : ValueDeclaration(), DeclarationHolder {
    /** The function body. Usually a [CompoundStatement]. */
    @field:SubGraph("AST") var body: Statement? = null

    /**
     * Classes and Structs can be declared inside a function and are only valid within the function.
     */
    @Relationship(value = "RECORDS", direction = Relationship.Direction.OUTGOING)
    var recordEdges = mutableListOf<PropertyEdge<RecordDeclaration>>()

    /** The list of function parameters. */
    @Relationship(value = "PARAMETERS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var parameterEdges = mutableListOf<PropertyEdge<ParamVariableDeclaration>>()

    /** Virtual property for accessing [parameterEdges] without property edges. */
    var parameters by PropertyEdgeDelegate(FunctionDeclaration::parameterEdges)

    /** Virtual property for accessing [parameterEdges] without property edges. */
    var records by PropertyEdgeDelegate(FunctionDeclaration::recordEdges)

    @Relationship(value = "THROWS_TYPES", direction = Relationship.Direction.OUTGOING)
    var throwsTypes = mutableListOf<Type>()

    @Relationship(value = "OVERRIDES", direction = Relationship.Direction.INCOMING)
    val overriddenBy = mutableListOf<FunctionDeclaration>()

    @Relationship(value = "OVERRIDES", direction = Relationship.Direction.OUTGOING)
    val overrides = mutableListOf<FunctionDeclaration>()

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

    /** Returns true, if this function has a [body] statement. */
    fun hasBody(): Boolean {
        return body != null
    }

    val signature: String
        get() =
            name.localName +
                parameters.joinToString(COMMA + WHITESPACE, BRACKET_LEFT, BRACKET_RIGHT) {
                    it.type.typeName
                } +
                (if (returnTypes.size == 1) {
                    returnTypes.first().typeName
                } else {
                    returnTypes.joinToString(COMMA + WHITESPACE, BRACKET_LEFT, BRACKET_RIGHT) {
                        it.typeName
                    }
                })

    fun hasSameSignature(targetFunctionDeclaration: FunctionDeclaration): Boolean {
        return targetFunctionDeclaration.name.localName == name.localName &&
            targetFunctionDeclaration.signatureTypes == signatureTypes
    }

    fun hasSignature(targetSignature: List<Type?>): Boolean {
        val signature =
            parameters
                .stream()
                .sorted(Comparator.comparingInt(ParamVariableDeclaration::argumentIndex))
                .collect(Collectors.toList())
        return if (targetSignature.size < signature.size) {
            false
        } else {
            // signature is a collection of positional arguments, so the order must be preserved
            for (i in signature.indices) {
                val declared = signature[i]
                if (declared.isVariadic && targetSignature.size >= signature.size) {
                    // Everything that follows is collected by this param, so the signature is
                    // fulfilled no matter what comes now (potential FIXME: in Java, we could have
                    // overloading with different vararg types, in C++ we can't, as vararg types are
                    // not defined here anyways)
                    return true
                }
                val provided = targetSignature[i]
                if (!LegacyTypeManager.getInstance().isSupertypeOf(declared.type, provided, this)) {
                    return false
                }
            }
            // Longer target signatures are only allowed with varargs. If we reach this point, no
            // vararg has been encountered
            targetSignature.size == signature.size
        }
    }

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

    fun <T> getBodyStatementAs(i: Int, clazz: Class<T>): T? {
        if (body is CompoundStatement) {
            val statement = (body as CompoundStatement?)!!.statements[i]
            return if (clazz.isAssignableFrom(statement.javaClass)) clazz.cast(statement) else null
        }
        return null
    }

    /**
     * A list of default expressions for each item in [parameters]. If a [ParamVariableDeclaration]
     * has no default, the list will be null at this index. This list must have the same size as
     * [parameters].
     */
    val defaultParameters: List<Expression?>
        get() {
            return parameters.map { it.default }
        }

    val defaultParameterSignature: List<Type>
        get() {
            val signature: MutableList<Type> = ArrayList()
            for (paramVariableDeclaration in parameters) {
                if (paramVariableDeclaration.default != null) {
                    signature.add(paramVariableDeclaration.type)
                } else {
                    signature.add(UnknownType.getUnknownType(language))
                }
            }
            return signature
        }

    val signatureTypes: List<Type>
        get() = parameters.map { it.type }

    fun addParameter(paramVariableDeclaration: ParamVariableDeclaration) {
        val propertyEdge = PropertyEdge(this, paramVariableDeclaration)
        propertyEdge.addProperty(Properties.INDEX, parameters.size)
        parameterEdges.add(propertyEdge)
    }

    fun removeParameter(paramVariableDeclaration: ParamVariableDeclaration) {
        parameterEdges.removeIf { it.end == paramVariableDeclaration }
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("parameters", parameters)
            .toString()
    }

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
            throwsTypes == other.throwsTypes &&
            overriddenBy == other.overriddenBy &&
            overrides == other.overrides)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), parameters, throwsTypes, overrides)

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is ParamVariableDeclaration) {
            addIfNotContains(parameterEdges, declaration)
        }

        if (declaration is RecordDeclaration) {
            addIfNotContains(recordEdges, declaration)
        }
    }

    override val declarations: List<Declaration>
        get() {
            val list = ArrayList<Declaration>()
            list.addAll(parameters)
            list.addAll(records)
            return list
        }

    companion object {
        const val WHITESPACE = " "
        const val BRACKET_LEFT = "("
        const val COMMA = ","
        const val BRACKET_RIGHT = ")"
    }
}
