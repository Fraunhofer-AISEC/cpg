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
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.passes.inference.IsInferredProvider
import de.fraunhofer.aisec.cpg.passes.scopes.Scope
import org.slf4j.LoggerFactory

object NodeBuilder {
    private val LOGGER = LoggerFactory.getLogger(NodeBuilder::class.java)

    fun log(node: Node?) {
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
    val language: Language<out LanguageFrontend>?
}

/**
 * This interface denotes that the class is able to provide source code and location information for
 * a specific node and set it using the [setCodeAndLocation] function.
 */
interface CodeAndLocationProvider : MetadataProvider {
    fun <N, S> setCodeAndLocation(cpgNode: N, astNode: S?)
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
 * Note, that one provider can implement multiple provider interfaces. Additionally, if
 * [codeOverride] is specified, the supplied source code is used to override anything from the
 * provider.
 */
fun Node.applyMetadata(
    provider: MetadataProvider?,
    name: CharSequence? = EMPTY_NAME,
    rawNode: Any? = null,
    codeOverride: String? = null,
    localNameOnly: Boolean = false,
    defaultNamespace: Name? = null,
) {
    if (provider is CodeAndLocationProvider) {
        provider.setCodeAndLocation(this, rawNode)
    }

    if (provider is LanguageProvider) {
        this.language = provider.language
    }

    if (provider is IsInferredProvider) {
        this.isInferred = provider.isInferred
    }

    if (provider is ScopeProvider) {
        this.scope = provider.scope
    }

    if (name != null) {
        this.name = this.newName(name, localNameOnly, defaultNamespace)
    }

    if (codeOverride != null) {
        this.code = codeOverride
    }
}

fun LanguageProvider.newName(
    name: CharSequence,
    localNameOnly: Boolean = false,
    defaultNamespace: Name? = null,
): Name {
    val namespace =
        if (this is NamespaceProvider) {
            this.namespace ?: defaultNamespace
        } else {
            defaultNamespace
        }

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
fun MetadataProvider.newAnnotation(
    name: String?,
    code: String? = null,
    rawNode: Any? = null
): Annotation {
    val node = Annotation()
    node.applyMetadata(this, name, rawNode, code)

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
    name: String?,
    value: Expression?,
    code: String? = null,
    rawNode: Any? = null
): AnnotationMember {
    val node = AnnotationMember()
    node.applyMetadata(this, name, rawNode, code)

    node.value = value

    log(node)
    return node
}

/**
 * Provides a nice alias to [TypeParser.createFrom]. In the future, this should not be used anymore
 * since we are moving away from the [TypeParser] altogether.
 */
fun LanguageProvider.parseType(name: String) = TypeParser.createFrom(name, language)

/**
 * Provides a nice alias to [TypeParser.createFrom]. In the future, this should not be used anymore
 * since we are moving away from the [TypeParser] altogether.
 */
fun LanguageProvider.parseType(name: Name) = TypeParser.createFrom(name, language)

/** Returns a new [Name] based on the [localName] and the current namespace as parent. */
fun NamespaceProvider.fqn(localName: String): Name {
    return this.namespace.fqn(localName)
}
