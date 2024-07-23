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
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.helpers.getCodeOfSubregion
import de.fraunhofer.aisec.cpg.passes.inference.IsImplicitProvider
import de.fraunhofer.aisec.cpg.passes.inference.IsInferredProvider
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.collections.ArrayDeque
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
 * a specific node.
 */
interface CodeAndLocationProvider<in AstNode> : MetadataProvider {
    /** Returns the raw code of the supplied [AstNode]. */
    fun codeOf(astNode: AstNode): String?

    /** Returns the [PhysicalLocation] of the supplied [AstNode]. */
    fun locationOf(astNode: AstNode): PhysicalLocation?
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

interface AstStackProvider : MetadataProvider {
    val astStack: ArrayDeque<Node>
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
    // We definitely need a context provider, because otherwise we cannot set the context and the
    // node cannot access necessary information about the current translation context it lives in.
    this.ctx =
        (provider as? ContextProvider)?.ctx
            ?: throw TranslationException(
                "Trying to create a node without a ContextProvider. This will fail."
            )

    // We try to set the code and especially the location as soon as possible because the hashCode
    // implementation of the Node class relies on it. Otherwise, we could have a problem that the
    // location is not yet set, but the node is put into a hashmap. In this case the hashCode is
    // calculated based on an empty location and if we would later set the location, we would have a
    // mismatch. Each language frontend and also each handler implements CodeAndLocationProvider, so
    // calling a node builder from these should already set the location.
    if (provider is CodeAndLocationProvider<*> && rawNode != null) {
        @Suppress("UNCHECKED_CAST")
        setCodeAndLocation(provider as CodeAndLocationProvider<Any>, rawNode)
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

    if (provider is AstStackProvider) {
        provider.astStack.lastOrNull().let { this.astParent = it }
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
    rawNode: Any? = null
): AnnotationMember {
    val node = AnnotationMember()
    node.applyMetadata(this, name, rawNode, true)

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

/**
 * Sometimes we need to explicitly (re)set the code and location of a node to another raw node than
 * originally used in the node builder. A common use-case for that is languages that contain
 * expression statements, which we simplify to simple expressions. But in these languages, the
 * expression often does not contain a semicolon at the end, where-as the statement does. In this
 * case we want to preserve the original code containing the semicolon and need to set the node's
 * code/location to the statement rather than the expression, after it comes back from the
 * expression handler.
 */
context(CodeAndLocationProvider<AstNode>)
fun <T : Node, AstNode> T.codeAndLocationFromOtherRawNode(rawNode: AstNode): T {
    setCodeAndLocation(this@CodeAndLocationProvider, rawNode)
    return this
}

/**
 * This function allows the setting of a node's code and location region as the code and location of
 * its children. Sometimes, when we translate a parent node in the language-specific AST with its
 * children into the CPG AST, we have to set a specific intermediate Node between, that has no
 * language-specific AST that can give it a proper code and location.
 *
 * While the location of the node is determined by the start and end of the child locations, the
 * code is extracted from the parent node to catch separators and auxiliary syntactic elements that
 * are between the child nodes.
 *
 * @param parentNode Used to extract the code for this node
 */
context(CodeAndLocationProvider<AstNode>)
fun <T : Node, AstNode> T.codeAndLocationFromChildren(parentNode: AstNode): T {
    var first: Node? = null
    var last: Node? = null

    // Search through all children to find the first and last node based on region startLine and
    // startColumn
    val worklist: MutableList<Node> = this.astChildren.toMutableList()
    while (worklist.isNotEmpty()) {
        val current = worklist.removeFirst()
        if (current.location == null || current.location?.region == Region()) {
            // If the node has no location we use the same search on his children again
            worklist.addAll(current.astChildren)
        } else {
            // Compare nodes by line and column in lexicographic order, i.e. column is compared if
            // lines are equal
            if (first == null) {
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
        this.location =
            PhysicalLocation(first.location?.artifactLocation?.uri ?: URI(""), newRegion)

        val parentCode = this@CodeAndLocationProvider.codeOf(parentNode)
        val parentRegion = this@CodeAndLocationProvider.locationOf(parentNode)?.region
        if (parentCode != null && parentRegion != null) {
            // If the parent has code and region the new region is used to extract the code
            this.code = getCodeOfSubregion(parentCode, parentRegion, newRegion)
        }
    }

    return this
}

/**
 * This internal function sets the code and location according to the [CodeAndLocationProvider].
 * This also performs some checks, e.g., if the config disabled setting the code.
 */
private fun <AstNode> Node.setCodeAndLocation(
    provider: CodeAndLocationProvider<AstNode>,
    rawNode: AstNode
) {
    if (this.ctx?.config?.codeInNodes == true) {
        // only set code, if it's not already set or empty
        val code = provider.codeOf(rawNode)
        if (code != null) {
            this.code = code
        } else {
            LOGGER.warn("Unexpected: No code for node {}", rawNode)
        }
    }
    this.location = provider.locationOf(rawNode)
}

context(AstStackProvider, ContextProvider)
fun <T : Node> T.withChildren(
    hasScope: Boolean = false,
    isGlobalScope: Boolean = false,
    init: (T) -> Unit
): T {
    val scopeManager =
        this@ContextProvider.ctx?.scopeManager
            ?: throw TranslationException(
                "Trying to create node children without a ContextProvider. This will fail."
            )

    (this@AstStackProvider).astStack.addLast(this@withChildren)

    if (isGlobalScope && this is TranslationUnitDeclaration) {
        scopeManager.resetToGlobal(this)
        init(this)
    } else if (hasScope) {
        scopeManager.enterScope(this)
        init(this)
        scopeManager.leaveScope(this)
    } else {
        init(this)
    }

    (this@AstStackProvider).astStack.removeLast()

    return this
}

/**
 * This function can be used to set the [Node.astParent] of this node to the current node on the
 * [AstStackProvider]'s stack. This is particularly useful if the node was created outside of the
 * [withChildren] lambda (for example, because it is used in multiple when-branches).
 *
 * Example:
 * ```kotlin
 * val binaryOperator = newBinaryOperator("|", rawNode = instr).withChildren {
 *   it.lhs = ptrDeref.withParent()
 *   it.rhs = value.withParent()
 * }
 * ```
 */
context(AstStackProvider)
fun <T : Node> T.withParent(): T {
    this.astParent = (this@AstStackProvider).astStack.lastOrNull()
    return this
}

context(ContextProvider)
fun <T : Declaration> T.declare(): T {
    val scopeManager =
        this@ContextProvider.ctx?.scopeManager
            ?: throw TranslationException(
                "Trying to create node children without a ContextProvider. This will fail."
            )
    scopeManager.addDeclaration(this)
    return this
}
