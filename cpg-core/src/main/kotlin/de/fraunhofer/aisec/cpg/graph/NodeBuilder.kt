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

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.LOGGER
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.getCodeOfSubregion
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import org.slf4j.LoggerFactory

object NodeBuilder {
    internal val LOGGER = LoggerFactory.getLogger(NodeBuilder::class.java)

    fun log(node: Node) {
        LOGGER.trace("Creating {}", node)
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

/**
 * A small helper function that can be used in building a [Node] with [Node.isImplicit] set to true.
 * In this case, no "rawNode" exists that can be used for the node builder. But, in order to
 * optionally supply [Node.code] and/or [Node.location] this function can be used.
 *
 * This also sets [Node.isImplicit] to true.
 */
fun <T : Node> T.implicit(code: String? = null, location: PhysicalLocation? = null): T {
    if (code != null) {
        this.code = code
    }
    if (location != null) {
        this.location = location
    }
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
fun <T : Node, AstNode> T.codeAndLocationFromOtherRawNode(rawNode: AstNode?): T {
    rawNode?.let { setCodeAndLocation(this@CodeAndLocationProvider, it) }
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
 * @param parentNode Used to extract the code for this node.
 * @param lineBreakSequence The char(s) used to describe a new line, usually either "\n" or "\r\n".
 *   This is needed because the location block spanning the children usually comprises more than one
 *   line.
 */
context(CodeAndLocationProvider<AstNode>)
fun <T : Node, AstNode> T.codeAndLocationFromChildren(
    parentNode: AstNode,
    lineBreakSequence: CharSequence = "\n"
): T {
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
            this.code = getCodeOfSubregion(parentCode, parentRegion, newRegion, lineBreakSequence)
        }
    }

    return this
}
