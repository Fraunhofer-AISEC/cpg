/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.ancestors
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Represents a programming language. When creating new languages in the CPG, one must derive custom
 * class from this and override the necessary fields and methods.
 *
 * Furthermore, since this also implements [Node], one node for each programming language used is
 * persisted in the final graph (database) and each node links to its corresponding language using
 * the [Node.language] property.
 */
abstract class Language<T : LanguageFrontend<*, *>> : Node() {
    /** The file extensions without the dot */
    abstract val fileExtensions: List<String>

    /** The namespace delimiter used by the language. Often, this is "." */
    abstract val namespaceDelimiter: String

    @get:JsonSerialize(using = KClassSerializer::class)
    /** The class of the frontend which is used to parse files of this language. */
    abstract val frontend: KClass<out T>

    /** The primitive type names of this language. */
    @get:JsonIgnore
    val primitiveTypeNames: Set<String>
        get() = builtInTypes.keys

    /** The built-in types of this language. */
    @get:JsonIgnore abstract val builtInTypes: Map<String, Type>

    /** The access modifiers of this programming language */
    open val accessModifiers: Set<String>
        get() = setOf("public", "protected", "private")

    /** The arithmetic operations of this language */
    open val arithmeticOperations: Set<String>
        get() = setOf("+", "-", "*", "/", "%", "<<", ">>")

    /** All operators which perform and assignment and an operation using lhs and rhs. */
    abstract val compoundAssignmentOperators: Set<String>

    /**
     * Creates a new [LanguageFrontend] object to parse the language. It requires the
     * [TranslationContext], which holds the necessary managers.
     */
    open fun newFrontend(ctx: TranslationContext): T {
        return this.frontend.primaryConstructor?.call(this, ctx)
            ?: throw TranslationException("could not instantiate language frontend")
    }

    /**
     * Returns the type conforming to the given [typeString]. If no matching type is found in the
     * [builtInTypes] map, it returns null. The [typeString] must precisely match the key in the
     * map.
     */
    fun getSimpleTypeOf(typeString: String) = builtInTypes[typeString]

    /** Returns true if the [file] can be handled by the frontend of this language. */
    fun handlesFile(file: File): Boolean {
        return file.extension in fileExtensions
    }

    override fun equals(other: Any?): Boolean {
        return other?.javaClass == this.javaClass
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fileExtensions.hashCode()
        result = 31 * result + namespaceDelimiter.hashCode()
        result = 31 * result + frontend.hashCode()
        result = 31 * result + primitiveTypeNames.hashCode()
        result = 31 * result + accessModifiers.hashCode()
        return result
    }

    init {
        this.also { language ->
            this.language = language
            language::class.simpleName?.let { this.name = Name(it) }
        }
    }

    private fun arithmeticOpTypePropagation(lhs: Type, rhs: Type): Type {
        return when {
            lhs is FloatingPointType && rhs !is FloatingPointType && rhs is NumericType -> lhs
            lhs !is FloatingPointType && lhs is NumericType && rhs is FloatingPointType -> rhs
            lhs is FloatingPointType && rhs is FloatingPointType ||
                lhs is IntegerType && rhs is IntegerType ->
                // We take the one with the bigger bitwidth
                if (((lhs as NumericType).bitWidth ?: 0) >= ((rhs as NumericType).bitWidth ?: 0)) {
                    lhs
                } else {
                    rhs
                }
            else -> unknownType()
        }
    }

    /**
     * Determines how to propagate types across binary operations since these may differ among the
     * programming languages.
     */
    open fun propagateTypeOfBinaryOperation(operation: BinaryOperator): Type {
        if (operation.operatorCode == "==" || operation.operatorCode == "===") {
            // A comparison, so we return the type "boolean"
            return this.builtInTypes.values.firstOrNull { it is BooleanType }
                ?: this.builtInTypes.values.firstOrNull { it.name.localName.startsWith("bool") }
                    ?: unknownType()
        }

        return when (operation.operatorCode) {
            "+" ->
                if (operation.lhs.type is StringType) {
                    // string + anything => string
                    operation.lhs.type
                } else if (operation.rhs.type is StringType) {
                    // anything + string => string
                    operation.rhs.type
                } else {
                    arithmeticOpTypePropagation(operation.lhs.type, operation.rhs.type)
                }
            "-",
            "*",
            "/" -> arithmeticOpTypePropagation(operation.lhs.type, operation.rhs.type)
            "<<",
            ">>" ->
                if (operation.lhs.type.isPrimitive && operation.rhs.type.isPrimitive) {
                    // primitive type 1 OP primitive type 2 => primitive type 1
                    operation.lhs.type
                } else {
                    unknownType()
                }
            else -> unknownType() // We don't know what is this thing
        }
    }

    /**
     * When propagating [HasType.assignedTypes] from one node to another, we might want to propagate
     * only certain types. A common example is to truncate [NumericType]s, when they are not "big"
     * enough.
     */
    open fun shouldPropagateType(hasType: HasType, srcType: Type): Boolean {
        val node = hasType as Node
        var nodeType = hasType.type

        // We only want to add certain types, in case we have a numeric type
        if (nodeType is NumericType) {
            // We do not allow to propagate non-numeric types into numeric types
            return if (srcType !is NumericType) {
                false
            } else {
                val srcWidth = srcType.bitWidth
                val lhsWidth = nodeType.bitWidth
                // Do not propagate anything if the new type is too big for the current type.
                return !(lhsWidth != null && srcWidth != null && lhsWidth < srcWidth)
            }
        }

        return true
    }

    open fun isDerivedFrom(type: Type, superType: Type): Boolean {
        // Retrieve all ancestor types of our type (more concretely of the root type)
        val root = type.root
        val superTypes = root.ancestors.map { it.type }

        // Check, if super type (or its root) is in the list
        return superType.root in superTypes
    }
}

/**
 * We need to bring our own serializer for [KClass] until
 * https://github.com/FasterXML/jackson-module-kotlin/issues/361 is resolved.
 */
internal class KClassSerializer : StdSerializer<KClass<*>>(KClass::class.java) {
    override fun serialize(value: KClass<*>, gen: JsonGenerator, provider: SerializerProvider) {
        // Write the fully qualified name as a string
        gen.writeString(value.qualifiedName)
    }
}
