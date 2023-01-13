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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.cpp.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.types.FloatingPointType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.Type.Qualifier
import de.fraunhofer.aisec.cpg.graph.types.Type.Storage
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
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

    /** The primitive types of this language. */
    open val primitiveTypes: Set<String>
        get() = setOf("byte", "short", "int", "long", "float", "double", "boolean", "char")

    /** The access modifiers of this programming language */
    open val accessModifiers: Set<String>
        get() = setOf("public", "protected", "private")

    /** Creates a new [LanguageFrontend] object to parse the language. */
    abstract fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager = ScopeManager(),
        typeManager: TypeManager = TypeManager()
    ): T

    open fun getTypeOf(typeString: String, modifier: ObjectType.Modifier): Type? {
        return when (typeString) {
            "byte" -> IntegerType(typeString, 8, this, modifier)
            "short" -> IntegerType(typeString, 16, this, modifier)
            "int" -> IntegerType(typeString, 32, this, modifier)
            "long" -> IntegerType(typeString, 64, this, modifier)
            "long long int" -> IntegerType(typeString, 64, this, modifier)
            "float" -> FloatingPointType(typeString, 32, this, modifier)
            "double" -> FloatingPointType(typeString, 64, this, modifier)
            "boolean" -> IntegerType(typeString, 1, this, modifier)
            "char" -> IntegerType(typeString, 8, this, ObjectType.Modifier.NOT_APPLICABLE)
            else -> null
        }
    }

    // TODO: These are language specific too.
    private val VOLATILE_QUALIFIER = "volatile"
    private val FINAL_QUALIFIER = "final"
    private val CONST_QUALIFIER = "const"
    private val RESTRICT_QUALIFIER = "restrict"
    private val ATOMIC_QUALIFIER = "atomic"

    open fun updateQualifier(qualifierString: String, old: Qualifier): Boolean {
        if (this !is HasQualifier || qualifierString !in qualifiers) {
            return false
        }
        val qualifier = old
        when (qualifierString) {
            FINAL_QUALIFIER,
            CONST_QUALIFIER -> {
                qualifier.isConst = true
                return true
            }
            VOLATILE_QUALIFIER -> {
                qualifier.isVolatile = true
                return true
            }
            RESTRICT_QUALIFIER -> {
                qualifier.isRestrict = true

                return true
            }
            ATOMIC_QUALIFIER -> {
                qualifier.isAtomic = true

                return true
            }
        }

        return false
    }

    open fun asStorageSpecifier(specifier: String): Storage? {
        // TODO: Which of these affect which language? Probably, we should separate it more clearly.
        // I'm also wondering why we actually need this information.
        if (specifier.uppercase() == "STATIC") {
            return Storage.STATIC
        } else if (specifier.uppercase() == "EXTERN") {
            return Storage.EXTERN
        } else if (specifier.uppercase() == "REGISTER") {
            return Storage.REGISTER
        } else if (specifier.uppercase() == "AUTO" && this is CPPLanguage) {
            return Storage.AUTO
        } else {
            return null
        }
    }

    /** Returns true if the [file] can be handled by the frontend of this language. */
    fun handlesFile(file: File): Boolean {
        return file.extension in fileExtensions
    }

    init {
        this.also { this.language = it }
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
