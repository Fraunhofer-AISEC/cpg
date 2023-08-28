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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation

/**
 * Creates a new [TranslationUnitDecl]. This is the top-most [Node] that a [LanguageFrontend] or
 * [Handler] should create. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTranslationUnitDecl(
    name: CharSequence?,
    code: String? = null,
    rawNode: Any? = null
): TranslationUnitDecl {
    val node = TranslationUnitDecl()
    node.applyMetadata(this, name, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [FunctionDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newFunctionDecl(
    name: CharSequence?,
    code: String? = null,
    rawNode: Any? = null,
    localNameOnly: Boolean = false
): FunctionDecl {
    val node = FunctionDecl()
    node.applyMetadata(this, name, rawNode, code, localNameOnly)

    log(node)
    return node
}

/**
 * Creates a new [MethodDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newMethodDecl(
    name: CharSequence?,
    code: String? = null,
    isStatic: Boolean = false,
    recordDecl: RecordDecl? = null,
    rawNode: Any? = null
): MethodDecl {
    val node = MethodDecl()
    node.applyMetadata(this, name, rawNode, code, defaultNamespace = recordDecl?.name)

    node.isStatic = isStatic
    node.recordDecl = recordDecl

    log(node)
    return node
}

/**
 * Creates a new [ConstructorDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newConstructorDecl(
    name: CharSequence?,
    code: String? = null,
    recordDecl: RecordDecl?,
    rawNode: Any? = null
): ConstructorDecl {
    val node = ConstructorDecl()

    node.applyMetadata(this, name, rawNode, code, defaultNamespace = recordDecl?.name)

    node.recordDecl = recordDecl

    log(node)
    return node
}

/**
 * Creates a new [MethodDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newParameterDecl(
    name: CharSequence?,
    type: Type = unknownType(),
    variadic: Boolean = false,
    code: String? = null,
    rawNode: Any? = null
): ParameterDecl {
    val node = ParameterDecl()
    node.applyMetadata(this, name, rawNode, code, localNameOnly = true)

    node.type = type
    node.isVariadic = variadic

    log(node)
    return node
}

/**
 * Creates a new [VariableDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newVariableDecl(
    name: CharSequence?,
    type: Type = unknownType(),
    code: String? = null,
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null
): VariableDecl {
    val node = VariableDecl()
    node.applyMetadata(this, name, rawNode, code, true)

    node.type = type
    node.isImplicitInitializerAllowed = implicitInitializerAllowed

    log(node)
    return node
}

/**
 * Creates a new [TypedefDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTypedefDecl(
    targetType: Type,
    alias: Type,
    code: String? = null,
    rawNode: Any? = null
): TypedefDecl {
    val node = TypedefDecl()
    node.applyMetadata(this, alias.typeName, rawNode, code, true)

    node.type = targetType
    node.alias = alias

    log(node)
    return node
}

/**
 * Creates a new [TypeParamDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeParamDecl(
    name: CharSequence?,
    code: String? = null,
    rawNode: Any? = null
): TypeParamDecl {
    val node = TypeParamDecl()
    node.applyMetadata(this, name, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [RecordDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newRecordDecl(
    name: CharSequence,
    kind: String,
    code: String? = null,
    rawNode: Any? = null
): RecordDecl {
    val node = RecordDecl()
    node.applyMetadata(this, name, rawNode, code, false)

    node.kind = kind

    log(node)
    return node
}

/**
 * Creates a new [EnumDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newEnumDecl(
    name: CharSequence?,
    code: String? = null,
    location: PhysicalLocation?,
    rawNode: Any? = null
): EnumDecl {
    val node = EnumDecl()
    node.applyMetadata(this, name, rawNode, code)

    node.location = location

    log(node)
    return node
}

/**
 * Creates a new [FunctionTemplateDecl]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newFunctionTemplateDecl(
    name: CharSequence?,
    code: String? = null,
    rawNode: Any? = null
): FunctionTemplateDecl {
    val node = FunctionTemplateDecl()
    node.applyMetadata(this, name, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [ClassTemplateDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newClassTemplateDecl(
    name: CharSequence?,
    code: String? = null,
    rawNode: Any? = null
): ClassTemplateDecl {
    val node = ClassTemplateDecl()
    node.applyMetadata(this, name, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [EnumConstantDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newEnumConstantDecl(
    name: CharSequence?,
    code: String? = null,
    location: PhysicalLocation?,
    rawNode: Any? = null
): EnumConstantDecl {
    val node = EnumConstantDecl()
    node.applyMetadata(this, name, rawNode, code)

    node.location = location

    log(node)
    return node
}

/**
 * Creates a new [FieldDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newFieldDecl(
    name: CharSequence?,
    type: Type = unknownType(),
    modifiers: List<String>? = listOf(),
    code: String? = null,
    location: PhysicalLocation? = null,
    initializer: Expression? = null,
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null
): FieldDecl {
    val node = FieldDecl()
    node.applyMetadata(this, name, rawNode, code)

    node.type = type
    node.modifiers = modifiers ?: listOf()
    node.location = location
    node.isImplicitInitializerAllowed = implicitInitializerAllowed
    if (initializer != null) {
        if (initializer is ArrayExpr) {
            node.isArray = true
        }
        node.initializer = initializer
    }

    log(node)
    return node
}

/**
 * Creates a new [ProblemDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newProblemDecl(
    problem: String = "",
    problemType: ProblemNode.ProblemType = ProblemNode.ProblemType.PARSING,
    code: String? = null,
    rawNode: Any? = null
): ProblemDecl {
    val node = ProblemDecl()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    node.problem = problem
    node.problemType = problemType

    log(node)
    return node
}

/**
 * Creates a new [IncludeDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newIncludeDecl(
    includeFilename: CharSequence,
    code: String? = null,
    rawNode: Any? = null
): IncludeDecl {
    val node = IncludeDecl()
    node.applyMetadata(this, includeFilename, rawNode, code, true)
    node.filename = includeFilename.toString()

    log(node)
    return node
}

/**
 * Creates a new [NamespaceDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newNamespaceDecl(
    name: CharSequence,
    code: String? = null,
    rawNode: Any? = null
): NamespaceDecl {
    val node = NamespaceDecl()
    node.applyMetadata(this, name, rawNode, code)

    log(node)
    return node
}

/**
 * Creates a new [UsingDecl]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newUsingDecl(
    code: String? = null,
    qualifiedName: CharSequence?,
    rawNode: Any? = null
): UsingDecl {
    val node = UsingDecl()
    node.applyMetadata(this, qualifiedName, rawNode, code)

    node.qualifiedName = qualifiedName.toString()

    log(node)
    return node
}
