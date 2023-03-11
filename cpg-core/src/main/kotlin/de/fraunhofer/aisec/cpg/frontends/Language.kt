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
import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.newUnknownType
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.io.File
import kotlin.reflect.KClass

/**
 * Represents a programming language. When creating new languages in the CPG, one must derive custom
 * class from this and override the necessary fields and methods.
 *
 * Furthermore, since this also implements [Node], one node for each programming language used is
 * persisted in the final graph (database) and each node links to its corresponding language using
 * the [Node.language] property.
 */
abstract class Language<T : LanguageFrontend> : Node() {
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

    /** Creates a new [LanguageFrontend] object to parse the language. */
    abstract fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager = ScopeManager(),
    ): T

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
        this.also { this.language = it }
    }

    private fun arithmeticOpTypePropagation(lhs: Type, rhs: Type): Type {
        return if (lhs is FloatingPointType && rhs !is FloatingPointType) {
            lhs
        } else if (lhs !is FloatingPointType && rhs is FloatingPointType) {
            rhs
        } else if (lhs is FloatingPointType && rhs is FloatingPointType) {
            // We take the one with the bigger bitwidth
            if ((lhs.bitWidth ?: 0) >= (rhs.bitWidth ?: 0)) {
                lhs
            } else {
                rhs
            }
        } else if (lhs is IntegerType && rhs is IntegerType) {
            // We take the one with the bigger bitwidth
            if ((lhs.bitWidth ?: 0) >= (rhs.bitWidth ?: 0)) {
                lhs
            } else {
                rhs
            }
        } else {
            newUnknownType()
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
                    ?: newUnknownType()
        }

        return when (operation.operatorCode) {
            "+" ->
                if (operation.lhs.propagationType is StringType) {
                    // string + anything => string
                    operation.lhs.propagationType
                } else if (operation.rhs.propagationType is StringType) {
                    // anything + string => string
                    operation.rhs.propagationType
                } else {
                    arithmeticOpTypePropagation(
                        operation.lhs.propagationType,
                        operation.rhs.propagationType
                    )
                }
            "-",
            "*",
            "/" ->
                arithmeticOpTypePropagation(
                    operation.lhs.propagationType,
                    operation.rhs.propagationType
                )
            "<<",
            ">>" ->
                if (
                    operation.lhs.propagationType.isPrimitive &&
                        operation.rhs.propagationType.isPrimitive
                ) {
                    // primitive type 1 OP primitive type 2 => primitive type 1
                    operation.lhs.propagationType
                } else {
                    newUnknownType()
                }
            else -> newUnknownType() // We don't know what is this thing
        }
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
