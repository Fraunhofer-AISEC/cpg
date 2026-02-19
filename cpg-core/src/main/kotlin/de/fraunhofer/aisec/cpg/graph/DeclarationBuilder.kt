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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArrayExpression
import de.fraunhofer.aisec.cpg.graph.types.Type
import kotlin.io.path.Path

/**
 * Creates a new [TranslationUnit]. This is the top-most [Node] that a [LanguageFrontend] or
 * [Handler] should create. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
context(provider: ContextProvider)
fun MetadataProvider.newTranslationUnit(name: CharSequence, rawNode: Any? = null): TranslationUnit {
    val node = TranslationUnit()
    val path = Path(name.toString())

    // We must avoid absolute path names as name for the translation unit, as this
    // specific to the analysis machine and therefore make node IDs not comparable across
    // machines.
    val relativeName =
        if (path.isAbsolute) {
            val topLevel = path.topLevel
            path.toFile().relativeToOrNull(topLevel)?.toString() ?: path.fileName?.toString()
        } else {
            name
        }

    node.applyMetadata(this, relativeName, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Function]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newFunction(
    name: CharSequence?,
    localNameOnly: Boolean = false,
    rawNode: Any? = null,
): Function {
    val node = Function()
    node.applyMetadata(this, name, rawNode, localNameOnly)

    log(node)
    return node
}

/**
 * Creates a new [Method]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newMethod(
    name: CharSequence?,
    isStatic: Boolean = false,
    recordDeclaration: Record? = null,
    rawNode: Any? = null,
): Method {
    val node = Method()
    node.applyMetadata(this, name, rawNode, defaultNamespace = recordDeclaration?.name)

    node.isStatic = isStatic
    node.recordDeclaration = recordDeclaration

    log(node)
    return node
}

/**
 * Creates a new [Operator]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newOperator(
    name: CharSequence,
    operatorCode: String,
    recordDeclaration: Record? = null,
    rawNode: Any? = null,
): Operator {
    val node = Operator()
    node.applyMetadata(this, name, rawNode, defaultNamespace = recordDeclaration?.name)

    node.operatorCode = operatorCode
    node.recordDeclaration = recordDeclaration

    log(node)
    return node
}

/**
 * Creates a new [Constructor]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
context(provider: ContextProvider)
@JvmOverloads
fun MetadataProvider.newConstructor(
    name: CharSequence?,
    recordDeclaration: Record?,
    rawNode: Any? = null,
): Constructor {
    val node = Constructor()

    node.applyMetadata(this, name, rawNode, defaultNamespace = recordDeclaration?.name)

    node.recordDeclaration = recordDeclaration
    node.type = recordDeclaration?.toType() ?: provider.unknownType()

    log(node)
    return node
}

/**
 * Creates a new [Parameter]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newParameter(
    name: CharSequence?,
    type: Type = unknownType(),
    variadic: Boolean = false,
    rawNode: Any? = null,
): Parameter {
    val node = Parameter()
    node.applyMetadata(this, name, rawNode, doNotPrependNamespace = true)

    node.type = type
    node.isVariadic = variadic

    log(node)
    return node
}

/**
 * Creates a new [Variable]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newVariable(
    name: CharSequence?,
    type: Type = unknownType(),
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null,
): Variable {
    val node = Variable()
    node.applyMetadata(this, name, rawNode, true)

    node.type = type
    node.isImplicitInitializerAllowed = implicitInitializerAllowed

    log(node)
    return node
}

/**
 * Creates a new [Tuple]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
context(provider: ContextProvider)
@JvmOverloads
fun LanguageProvider.newTuple(
    elements: List<Variable>,
    initializer: Expression?,
    rawNode: Any? = null,
): Tuple {
    val node = Tuple()
    node.applyMetadata(this, null, rawNode, true)

    // Tuples always have an auto-type
    node.type = autoType()

    // Also all our elements need to have an auto-type
    elements.forEach { it.type = autoType() }
    node.elements = elements.toMutableList()

    node.initializer = initializer

    log(node)
    return node
}

/**
 * Creates a new [Typedef]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTypedef(targetType: Type, alias: Type, rawNode: Any? = null): Typedef {
    val node = Typedef()
    node.applyMetadata(this, alias.typeName, rawNode)

    node.type = targetType
    node.alias = alias
    // litle bit of a hack to make the type FQN
    node.alias.name = node.name

    log(node)
    return node
}

/**
 * Creates a new [TypeParameter]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeParameter(name: CharSequence?, rawNode: Any? = null): TypeParameter {
    val node = TypeParameter()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Record]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newRecord(name: CharSequence, kind: String, rawNode: Any? = null): Record {
    val node = Record()
    node.applyMetadata(this, name, rawNode, false)

    node.kind = kind

    log(node)
    return node
}

/**
 * Creates a new [Enumeration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newEnumeration(name: CharSequence?, rawNode: Any? = null): Enumeration {
    val node = Enumeration()
    node.applyMetadata(this, name, rawNode)

    log(node)
    return node
}

/**
 * Creates a new [FunctionTemplate]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newFunctionTemplate(
    name: CharSequence?,
    rawNode: Any? = null,
): FunctionTemplate {
    val node = FunctionTemplate()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [RecordTemplate]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newRecordTemplate(name: CharSequence?, rawNode: Any? = null): RecordTemplate {
    val node = RecordTemplate()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [EnumConstant]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newEnumConstant(name: CharSequence?, rawNode: Any? = null): EnumConstant {
    val node = EnumConstant()
    node.applyMetadata(this, name, rawNode)

    log(node)
    return node
}

/**
 * Creates a new [Field]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newField(
    name: CharSequence?,
    type: Type = unknownType(),
    modifiers: Set<String> = setOf(),
    initializer: Expression? = null,
    implicitInitializerAllowed: Boolean = false,
    rawNode: Any? = null,
): Field {
    val node = Field()
    node.applyMetadata(this, name, rawNode)

    node.type = type
    node.modifiers = modifiers ?: setOf()
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
    rawNode: Any? = null,
): ProblemDeclaration {
    val node = ProblemDeclaration()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.problem = problem
    node.problemType = problemType

    log(node)
    return node
}

/**
 * Creates a new [Include]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newInclude(includeFilename: CharSequence, rawNode: Any? = null): Include {
    val node = Include()
    node.applyMetadata(this, includeFilename, rawNode, true)
    node.filename = includeFilename.toString()

    log(node)
    return node
}

/**
 * Creates a new [Namespace]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newNamespace(name: CharSequence, rawNode: Any? = null): Namespace {
    val node = Namespace()
    node.applyMetadata(this, name, rawNode)

    log(node)
    return node
}

/**
 * Creates a new [Extension]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newExtension(name: CharSequence, rawNode: Any? = null): Extension {
    val node = Extension()
    node.applyMetadata(this, name, rawNode)

    log(node)
    return node
}

/**
 * Creates a new [Import]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newImport(
    import: Name,
    style: ImportStyle,
    alias: Name? = null,
    rawNode: Any? = null,
): Import {
    val node = Import()
    node.applyMetadata(this, alias ?: import, rawNode, doNotPrependNamespace = true)
    node.import = import
    node.alias = alias
    node.style = style

    log(node)
    return node
}
