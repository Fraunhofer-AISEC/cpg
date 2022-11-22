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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayCreationExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
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
    name: String?,
    code: String? = null,
    rawNode: Any? = null
): TranslationUnitDeclaration {
    val node = TranslationUnitDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME

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
    name: String?,
    code: String? = null,
    rawNode: Any? = null
): FunctionDeclaration {
    val node = FunctionDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME

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
    name: String?,
    code: String? = null,
    isStatic: Boolean = false,
    recordDeclaration: RecordDeclaration? = null,
    rawNode: Any? = null
): MethodDeclaration {
    val node = MethodDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
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
    name: String?,
    code: String? = null,
    recordDeclaration: RecordDeclaration?,
    rawNode: Any? = null
): ConstructorDeclaration {
    val node = ConstructorDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
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
    name: String?,
    type: Type? = UnknownType.getUnknownType(),
    variadic: Boolean = false,
    code: String? = null,
    rawNode: Any? = null
): ParamVariableDeclaration {
    val node = ParamVariableDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
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
    name: String?,
    type: Type? = UnknownType.getUnknownType(),
    code: String? = null,
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null
): VariableDeclaration {
    val node = VariableDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
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
    targetType: Type?,
    alias: Type,
    code: String? = null,
    rawNode: Any? = null
): TypedefDeclaration {
    val node = TypedefDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = alias.typeName
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
    name: String?,
    code: String? = null,
    rawNode: Any? = null
): TypeParamDeclaration {
    val node = TypeParamDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME

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
    fqn: String,
    kind: String,
    code: String? = null,
    rawNode: Any? = null
): RecordDeclaration {
    val node = RecordDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = fqn
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
    name: String?,
    code: String? = null,
    location: PhysicalLocation?,
    rawNode: Any? = null
): EnumDeclaration {
    val node = EnumDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
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
    name: String?,
    code: String? = null,
    rawNode: Any? = null
): FunctionTemplateDeclaration {
    val node = FunctionTemplateDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME

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
    name: String?,
    code: String? = null,
    rawNode: Any? = null
): ClassTemplateDeclaration {
    val node = ClassTemplateDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME

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
    name: String?,
    code: String? = null,
    location: PhysicalLocation?,
    rawNode: Any? = null
): EnumConstantDeclaration {
    val node = EnumConstantDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
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
    name: String?,
    type: Type? = UnknownType.getUnknownType(),
    modifiers: List<String?>? = listOf(),
    code: String? = null,
    location: PhysicalLocation? = null,
    initializer: Expression? = null,
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null
): FieldDeclaration {
    val node = FieldDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
    node.type = type
    node.modifiers = modifiers
    node.location = location
    node.isImplicitInitializerAllowed = implicitInitializerAllowed
    if (initializer != null) {
        if (initializer is ArrayCreationExpression) {
            node.setIsArray(true)
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
    type: ProblemNode.ProblemType = ProblemNode.ProblemType.PARSING,
    code: String? = null,
    rawNode: Any? = null
): ProblemDeclaration {
    val node = ProblemDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.problem = problem
    node.type = type

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
    includeFilename: String,
    code: String? = null,
    rawNode: Any? = null
): IncludeDeclaration {
    val node = IncludeDeclaration()
    node.applyMetadata(this, rawNode, code)

    val name = includeFilename.substring(includeFilename.lastIndexOf('/') + 1)
    node.name = name
    node.filename = includeFilename

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
    fqn: String,
    code: String? = null,
    rawNode: Any? = null
): NamespaceDeclaration {
    val node = NamespaceDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = fqn

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
    qualifiedName: String?,
    rawNode: Any? = null
): UsingDirective {
    val node = UsingDirective()
    node.applyMetadata(this, rawNode, code)

    node.qualifiedName = qualifiedName

    log(node)
    return node
}
