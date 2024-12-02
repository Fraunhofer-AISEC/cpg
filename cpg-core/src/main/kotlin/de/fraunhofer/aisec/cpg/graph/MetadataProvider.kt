/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.LOGGER
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.passes.inference.IsImplicitProvider
import de.fraunhofer.aisec.cpg.passes.inference.IsInferredProvider
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation

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
 * This internal function sets the code and location according to the [CodeAndLocationProvider].
 * This also performs some checks, e.g., if the config disabled setting the code.
 */
internal fun <AstNode> Node.setCodeAndLocation(
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
