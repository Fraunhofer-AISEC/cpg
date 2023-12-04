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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArrayExpression
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Creates a new [TranslationUnitDeclaration]. This is the top-most [Node] that a [LanguageFrontend]
 * or [Handler] should create. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTranslationUnitDeclaration(
    name: CharSequence?,
    rawNode: Any? = null
): TranslationUnitDeclaration {
    val node = TranslationUnitDeclaration()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [FunctionDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newFunctionDeclaration(
    name: CharSequence?,
    localNameOnly: Boolean = false,
    rawNode: Any? = null,
): FunctionDeclaration {
    val node = FunctionDeclaration()
    node.applyMetadata(this@MetadataProvider, name, rawNode, localNameOnly)

    log(node)
    return node
}

/**
 * Creates a new [MethodDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newMethodDeclaration(
    name: CharSequence?,
    isStatic: Boolean = false,
    recordDeclaration: RecordDeclaration? = null,
    rawNode: Any? = null
): MethodDeclaration {
    val node = MethodDeclaration()
    node.applyMetadata(this, name, rawNode, defaultNamespace = recordDeclaration?.name)

    node.isStatic = isStatic
    node.recordDeclaration = recordDeclaration

    log(node)
    return node
}

/**
 * Creates a new [ConstructorDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newConstructorDeclaration(
    name: CharSequence?,
    recordDeclaration: RecordDeclaration?,
    rawNode: Any? = null
): ConstructorDeclaration {
    val node = ConstructorDeclaration()

    node.applyMetadata(this, name, rawNode, defaultNamespace = recordDeclaration?.name)

    node.recordDeclaration = recordDeclaration

    log(node)
    return node
}

/**
 * Creates a new [MethodDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newParameterDeclaration(
    name: CharSequence?,
    type: Type = unknownType(),
    variadic: Boolean = false,
    rawNode: Any? = null
): ParameterDeclaration {
    val node = ParameterDeclaration()
    node.applyMetadata(this, name, rawNode, localNameOnly = true)

    node.type = type
    node.isVariadic = variadic

    log(node)
    return node
}

/**
 * Creates a new [VariableDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newVariableDeclaration(
    name: CharSequence?,
    type: Type = unknownType(),
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null
): VariableDeclaration {
    val node = VariableDeclaration()
    node.applyMetadata(this, name, rawNode, true)

    node.type = type
    node.isImplicitInitializerAllowed = implicitInitializerAllowed

    log(node)
    return node
}

/**
 * Creates a new [TupleDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun LanguageProvider.newTupleDeclaration(
    elements: List<VariableDeclaration>,
    initializer: Expression?,
    rawNode: Any? = null
): TupleDeclaration {
    val node = TupleDeclaration()
    node.applyMetadata(this, null, rawNode, true)

    // Tuples always have an auto-type
    node.type = autoType()

    // Also all our elements need to have an auto-type
    elements.forEach { it.type = autoType() }
    node.elements = elements

    node.initializer = initializer

    log(node)
    return node
}

/**
 * Creates a new [TypedefDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTypedefDeclaration(
    targetType: Type,
    alias: Type,
    rawNode: Any? = null
): TypedefDeclaration {
    val node = TypedefDeclaration()
    node.applyMetadata(this, alias.typeName, rawNode, true)

    node.type = targetType
    node.alias = alias

    log(node)
    return node
}

/**
 * Creates a new [TypeParameterDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeParameterDeclaration(
    name: CharSequence?,
    rawNode: Any? = null
): TypeParameterDeclaration {
    val node = TypeParameterDeclaration()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [RecordDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newRecordDeclaration(
    name: CharSequence,
    kind: String,
    rawNode: Any? = null
): RecordDeclaration {
    val node = RecordDeclaration()
    node.applyMetadata(this, name, rawNode, false)

    node.kind = kind

    log(node)
    return node
}

/**
 * Creates a new [EnumDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newEnumDeclaration(
    name: CharSequence?,
    rawNode: Any? = null
): EnumDeclaration {
    val node = EnumDeclaration()
    node.applyMetadata(this, name, rawNode)

    log(node)
    return node
}

/**
 * Creates a new [FunctionTemplateDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newFunctionTemplateDeclaration(
    name: CharSequence?,
    rawNode: Any? = null
): FunctionTemplateDeclaration {
    val node = FunctionTemplateDeclaration()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [RecordTemplateDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newRecordTemplateDeclaration(
    name: CharSequence?,
    rawNode: Any? = null
): RecordTemplateDeclaration {
    val node = RecordTemplateDeclaration()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [EnumConstantDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newEnumConstantDeclaration(
    name: CharSequence?,
    rawNode: Any? = null
): EnumConstantDeclaration {
    val node = EnumConstantDeclaration()
    node.applyMetadata(this, name, rawNode)

    log(node)
    return node
}

/**
 * Creates a new [FieldDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newFieldDeclaration(
    name: CharSequence?,
    type: Type = unknownType(),
    modifiers: List<String>? = listOf(),
    initializer: Expression? = null,
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null
): FieldDeclaration {
    val node = FieldDeclaration()
    node.applyMetadata(this, name, rawNode)

    node.type = type
    node.modifiers = modifiers ?: listOf()
    node.isImplicitInitializerAllowed = implicitInitializerAllowed
    if (initializer != null) {
        if (initializer is NewArrayExpression) {
            node.isArray = true
        }
        node.initializer = initializer
    }

    log(node)
    return node
}

/**
 * Creates a new [ProblemDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newProblemDeclaration(
    problem: String = "",
    problemType: ProblemNode.ProblemType = ProblemNode.ProblemType.PARSING,
    rawNode: Any? = null
): ProblemDeclaration {
    val node = ProblemDeclaration()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.problem = problem
    node.problemType = problemType

    log(node)
    return node
}

/**
 * Creates a new [IncludeDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newIncludeDeclaration(
    includeFilename: CharSequence,
    rawNode: Any? = null
): IncludeDeclaration {
    val node = IncludeDeclaration()
    node.applyMetadata(this, includeFilename, rawNode, true)
    node.filename = includeFilename.toString()

    log(node)
    return node
}

/**
 * Creates a new [NamespaceDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newNamespaceDeclaration(
    name: CharSequence,
    rawNode: Any? = null
): NamespaceDeclaration {
    val node = NamespaceDeclaration()
    node.applyMetadata(this, name, rawNode)

    log(node)
    return node
}

/**
 * Creates a new [UsingDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newUsingDeclaration(
    qualifiedName: CharSequence?,
    rawNode: Any? = null
): UsingDeclaration {
    val node = UsingDeclaration()
    node.applyMetadata(this, qualifiedName, rawNode)

    node.qualifiedName = qualifiedName.toString()

    log(node)
    return node
}
