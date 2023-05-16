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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayCreationExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation

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
    code: String? = null,
    rawNode: Any? = null
): TranslationUnitDeclaration {
    val node = TranslationUnitDeclaration()
    node.applyMetadata(this, name, rawNode, code, true)

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
    code: String? = null,
    rawNode: Any? = null,
    localNameOnly: Boolean = false
): FunctionDeclaration {
    val node = FunctionDeclaration()
    node.applyMetadata(this, name, rawNode, code, localNameOnly)

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
    code: String? = null,
    isStatic: Boolean = false,
    recordDeclaration: RecordDeclaration? = null,
    rawNode: Any? = null
): MethodDeclaration {
    val node = MethodDeclaration()
    node.applyMetadata(this, name, rawNode, code, defaultNamespace = recordDeclaration?.name)

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
    code: String? = null,
    recordDeclaration: RecordDeclaration?,
    rawNode: Any? = null
): ConstructorDeclaration {
    val node = ConstructorDeclaration()

    node.applyMetadata(this, name, rawNode, code, defaultNamespace = recordDeclaration?.name)

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
fun MetadataProvider.newParamVariableDeclaration(
    name: CharSequence?,
    type: Type = newUnknownType(),
    variadic: Boolean = false,
    code: String? = null,
    rawNode: Any? = null
): ParamVariableDeclaration {
    val node = ParamVariableDeclaration()
    node.applyMetadata(this, name, rawNode, code, localNameOnly = true)

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
    type: Type = newUnknownType(),
    code: String? = null,
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null
): VariableDeclaration {
    val node = VariableDeclaration()
    node.applyMetadata(this, name, rawNode, code, true)

    node.type = type
    node.isImplicitInitializerAllowed = implicitInitializerAllowed

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
    code: String? = null,
    rawNode: Any? = null
): TypedefDeclaration {
    val node = TypedefDeclaration()
    node.applyMetadata(this, alias.typeName, rawNode, code, true)

    node.type = targetType
    node.alias = alias

    log(node)
    return node
}

/**
 * Creates a new [TypeParamDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeParamDeclaration(
    name: CharSequence?,
    code: String? = null,
    rawNode: Any? = null
): TypeParamDeclaration {
    val node = TypeParamDeclaration()
    node.applyMetadata(this, name, rawNode, code, true)

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
    code: String? = null,
    rawNode: Any? = null
): RecordDeclaration {
    val node = RecordDeclaration()
    node.applyMetadata(this, name, rawNode, code, false)

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
    code: String? = null,
    location: PhysicalLocation?,
    rawNode: Any? = null
): EnumDeclaration {
    val node = EnumDeclaration()
    node.applyMetadata(this, name, rawNode, code)

    node.location = location

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
    code: String? = null,
    rawNode: Any? = null
): FunctionTemplateDeclaration {
    val node = FunctionTemplateDeclaration()
    node.applyMetadata(this, name, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [ClassTemplateDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newClassTemplateDeclaration(
    name: CharSequence?,
    code: String? = null,
    rawNode: Any? = null
): ClassTemplateDeclaration {
    val node = ClassTemplateDeclaration()
    node.applyMetadata(this, name, rawNode, code, true)

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
    code: String? = null,
    location: PhysicalLocation?,
    rawNode: Any? = null
): EnumConstantDeclaration {
    val node = EnumConstantDeclaration()
    node.applyMetadata(this, name, rawNode, code)

    node.location = location

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
    type: Type = newUnknownType(),
    modifiers: List<String>? = listOf(),
    code: String? = null,
    location: PhysicalLocation? = null,
    initializer: Expression? = null,
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null
): FieldDeclaration {
    val node = FieldDeclaration()
    node.applyMetadata(this, name, rawNode, code)

    node.type = type
    node.modifiers = modifiers ?: listOf()
    node.location = location
    node.isImplicitInitializerAllowed = implicitInitializerAllowed
    if (initializer != null) {
        if (initializer is ArrayCreationExpression) {
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
    code: String? = null,
    rawNode: Any? = null
): ProblemDeclaration {
    val node = ProblemDeclaration()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

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
    code: String? = null,
    rawNode: Any? = null
): IncludeDeclaration {
    val node = IncludeDeclaration()
    node.applyMetadata(this, includeFilename, rawNode, code, true)
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
    code: String? = null,
    rawNode: Any? = null
): NamespaceDeclaration {
    val node = NamespaceDeclaration()
    node.applyMetadata(this, name, rawNode, code)

    log(node)
    return node
}

/**
 * Creates a new [UsingDirective]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newUsingDirective(
    code: String? = null,
    qualifiedName: CharSequence?,
    rawNode: Any? = null
): UsingDirective {
    val node = UsingDirective()
    node.applyMetadata(this, qualifiedName, rawNode, code)

    node.qualifiedName = qualifiedName.toString()

    log(node)
    return node
}
