/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.LOGGER
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.inference.IsImplicitProvider
import de.fraunhofer.aisec.cpg.passes.inference.IsInferredProvider
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import org.slf4j.LoggerFactory

object NodeBuilder {
    internal val LOGGER = LoggerFactory.getLogger(NodeBuilder::class.java)

    fun log(node: Node) {
        LOGGER.trace("Creating {}", node)
    }
}

/**
 * This interfaces serves as base for different entities that provide some kind of meta-data for a
 * [Node], such as its language, code or location.
 */
interface MetadataProvider

/**
 * A simple interface that everything, that supplies a language, should implement. Examples include
 * each [Node], but also transformation steps, such as [Handler].
 */
interface LanguageProvider : MetadataProvider {
    val language: Language<*>?
}

/**
 * This interface denotes that the class is able to provide source code and location information for
 * a specific node and set it using the [setCodeAndLocation] function.
 */
interface CodeAndLocationProvider<in AstNode> : MetadataProvider {
    fun setCodeAndLocation(cpgNode: Node, astNode: AstNode)
}

/**
 * This interfaces serves as a base for entities that provide the current scope / name prefix. This
 * is reserved for future use.
 */
interface ScopeProvider : MetadataProvider {
    val scope: Scope?
}

/**
 * This interface denotes that the class is able to provide the current namespace. The
 * [applyMetadata] will use this information to set the parent of a [Name].
 */
interface NamespaceProvider : MetadataProvider {
    val namespace: Name?
}

/**
 * Applies various metadata on this [Node], based on the kind of provider in [provider]. This can
 * include:
 * - Setting [Node.code] and [Node.location], if a [CodeAndLocationProvider] is given
 * - Setting [Node.location], if a [LanguageProvider] is given
 * - Setting [Node.scope]. if a [ScopeProvider] is given
 * - Setting [Node.isInferred], if an [IsInferredProvider] is given
 *
 * Note, that one provider can implement multiple provider interfaces.
 */
fun Node.applyMetadata(
    provider: MetadataProvider?,
    name: CharSequence? = EMPTY_NAME,
    rawNode: Any? = null,
    localNameOnly: Boolean = false,
    defaultNamespace: Name? = null,
) {
    // We try to set the code and especially the location as soon as possible because the hashCode
    // implementation of the Node class relies on it. Otherwise, we could have a problem that the
    // location is not yet set, but the node is put into a hashmap. In this case the hashCode is
    // calculated based on an empty location and if we would later set the location, we would have a
    // mismatch. Each language frontend and also each handler implements CodeAndLocationProvider, so
    // calling a node builder from these should already set the location.
    if (provider is CodeAndLocationProvider<*> && rawNode != null) {
        @Suppress("UNCHECKED_CAST")
        (provider as CodeAndLocationProvider<Any>).setCodeAndLocation(this, rawNode)
    }

    if (provider is LanguageProvider) {
        this.language = provider.language
    }

    if (provider is IsInferredProvider) {
        this.isInferred = provider.isInferred
    }

    if (provider is IsImplicitProvider) {
        this.isImplicit = provider.isImplicit
    }

    if (provider is ScopeProvider) {
        this.scope = provider.scope
    } else {
        LOGGER.warn(
            "No scope provider was provided when creating the node {}. This might be an error",
            name
        )
    }

    if (provider is ContextProvider) {
        this.ctx = provider.ctx
    }

    if (this.ctx == null) {
        throw TranslationException(
            "Trying to create a node without a ContextProvider. This will fail."
        )
    }

    if (name != null) {
        val namespace =
            if (provider is NamespaceProvider) {
                provider.namespace ?: defaultNamespace
            } else {
                defaultNamespace
            }
        this.name = this.newName(name, localNameOnly, namespace)
    }
}

/**
 * Generates a [Name] object from the given [name]. If [localNameOnly] is set, only the localName is
 * used, otherwise the [namespace] is added to generate a fqn if the [name] is not a fqn anyway.
 */
fun LanguageProvider.newName(
    name: CharSequence,
    localNameOnly: Boolean = false,
    namespace: Name? = null
): Name {
    val language = this.language

    // The name could already be a real "name" (of our Name class). In this case we can just set
    // the name (if it is qualified). This is preferred over passing an FQN as
    // CharSequence/String.
    return if (name is Name && name.isQualified()) {
        name
    } else if (language != null && name.contains(language.namespaceDelimiter)) {
        // Let's check, if this is an FQN as string / char sequence by any chance. Then we need
        // to parse the name. In the future, we might drop compatibility for this
        language.parseName(name)
    } else {
        // Otherwise, a local name is supplied. Some nodes only want a local name. In this case,
        // we create a new name with the supplied (local) name and set the parent to null.
        val parent =
            if (localNameOnly) {
                null
            } else {
                namespace
            }

        Name(name.toString(), parent, language?.namespaceDelimiter ?: ".")
    }
}

/**
 * Creates a new [Annotation]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newAnnotation(name: CharSequence?, rawNode: Any? = null): Annotation {
    val node = Annotation()
    node.applyMetadata(this, name, rawNode)

    log(node)
    return node
}

/**
 * Creates a new [AnnotationMember]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newAnnotationMember(
    name: CharSequence?,
    value: Expression?,
    rawNode: Any? = null
): AnnotationMember {
    val node = AnnotationMember()
    node.applyMetadata(this, name, rawNode, true)

    node.value = value

    log(node)
    return node
}

/** Returns a new [Name] based on the [localName] and the current namespace as parent. */
fun NamespaceProvider.fqn(localName: String): Name {
    return this.namespace.fqn(localName)
}

interface ContextProvider : MetadataProvider {
    val ctx: TranslationContext?
}

/**
 * This [MetadataProvider] makes sure that we can type our node builder functions correctly. For
 * language frontend and handlers, [T] should be set to the type of the raw node. For passes, [T]
 * should be set to [Nothing], since we do not have raw nodes there.
 *
 * Note: This does not work yet to 100 % satisfaction and is therefore not yet activated in the
 * builders.
 */
interface RawNodeTypeProvider<T> : MetadataProvider

/**
 * A small helper function that can be used in building a [Node] with [Node.isImplicit] set to true.
 * In this case, no "rawNode" exists that can be used for the node builder. But, in order to
 * optionally supply [Node.code] and/or [Node.location] this function can be used.
 *
 * This also sets [Node.isImplicit] to true.
 */
fun <T : Node> T.implicit(code: String? = null, location: PhysicalLocation? = null): T {
    this.code = code
    this.location = location
    this.isImplicit = true

    return this
}

fun <T : Node> T.codeAndLocationFrom(other: Node): T {
    this.code = other.code
    this.location = other.location

    return this
}

fun <T : Node, S> T.codeAndLocationFrom(frontend: LanguageFrontend<S, *>, rawNode: S): T {
    frontend.setCodeAndLocation(this, rawNode)

    return this
}

fun <T : Node, S> T.codeAndLocationFromChildren(
    frontend: LanguageFrontend<S, *>,
    parentNode: S
): T {
    var first: Node? = null
    var last: Node? = null

    // Search through all children to find the first and last node based on region startLine and
    // startColumn
    var worklist: MutableList<Node> = this.astChildren.toMutableList()
    while (worklist.isNotEmpty()) {
        var current = worklist.removeFirst()
        if (current.location?.region == null || current.location?.region == Region()) {
            // If the node has no location we use the same search on his children again
            worklist.addAll(current.astChildren)
        } else {
            // Compare nodes by line and column in lexicographic order, i.e. column is compared if
            // lines are equal
            if (first == null || last == null) {
                first = current
                last = current
            }
            first =
                minOf(
                    first,
                    current,
                    compareBy(
                        { it?.location?.region?.startLine },
                        { it?.location?.region?.startColumn }
                    )
                )
            last =
                maxOf(
                    last,
                    current,
                    compareBy(
                        { it?.location?.region?.endLine },
                        { it?.location?.region?.endColumn }
                    )
                )
        }
    }

    if (first != null && last != null) {
        // Starts and ends are combined to one region
        val newRegion =
            Region(
                startLine = first.location?.region?.startLine ?: -1,
                startColumn = first.location?.region?.startColumn ?: -1,
                endLine = last.location?.region?.endLine ?: -1,
                endColumn = last.location?.region?.endColumn ?: -1,
            )
        this.location?.region = newRegion

        val parentCode = frontend.codeOf(parentNode)
        val parentRegion = frontend.locationOf(parentNode)?.region
        if (parentCode != null && parentRegion != null) {
            // If the parent has code and region the new region is used to extract the code
            this.code = frontend.getCodeOfSubregion(parentCode, parentRegion, newRegion)
        }
    }

    return this
}
