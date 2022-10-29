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
import de.fraunhofer.aisec.cpg.frontends.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Creates a new [TranslationUnitDeclaration]. This is the top-most [Node] that a [LanguageFrontend]
 * or [Handler] should create. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
fun MetadataProvider.newTranslationUnitDeclaration(
    name: String,
    code: String? = null,
    rawNode: Any? = null
): TranslationUnitDeclaration {
    val node = TranslationUnitDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name

    NodeBuilder.log(node)
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
    name: String,
    code: String? = null,
    rawNode: Any? = null
): FunctionDeclaration {
    val node = FunctionDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name

    NodeBuilder.log(node)
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
    name: String,
    code: String? = null,
    isStatic: Boolean,
    recordDeclaration: RecordDeclaration?,
    rawNode: Any? = null
): MethodDeclaration {
    val node = MethodDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name
    node.isStatic = isStatic
    node.recordDeclaration = recordDeclaration

    NodeBuilder.log(node)
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
    name: String,
    code: String? = null,
    recordDeclaration: RecordDeclaration?,
    rawNode: Any? = null
): ConstructorDeclaration {
    val node = ConstructorDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name
    node.recordDeclaration = recordDeclaration

    NodeBuilder.log(node)
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
    name: String,
    type: Type?,
    variadic: Boolean,
    code: String? = null,
    rawNode: Any? = null
): ParamVariableDeclaration {
    val node = ParamVariableDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name
    node.type = type
    node.isVariadic = variadic

    NodeBuilder.log(node)
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
    name: String,
    type: Type?,
    code: String? = null,
    implicitInitializerAllowed: Boolean,
    rawNode: Any? = null
): VariableDeclaration {
    val node = VariableDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name
    node.type = type
    node.isImplicitInitializerAllowed = implicitInitializerAllowed

    NodeBuilder.log(node)
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

    NodeBuilder.log(node)
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
    name: String,
    code: String? = null,
    rawNode: Any? = null
): TypeParamDeclaration {
    val node = TypeParamDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name

    NodeBuilder.log(node)
    return node
}
