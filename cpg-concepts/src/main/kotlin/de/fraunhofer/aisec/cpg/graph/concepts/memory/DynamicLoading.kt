/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.concepts.memory

import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.arch.OperatingSystemArchitecture
import de.fraunhofer.aisec.cpg.graph.concepts.flows.LibraryEntryPoint
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import java.util.Objects

/**
 * Represents an entity that loads a piece of code dynamically during runtime. Examples include a
 * class loader in Java, loading shared library code in C++. Interpreters, such as Python can also
 * load code dynamically during runtime.
 */
open class DynamicLoading(underlyingNode: Node? = null) :
    Concept(underlyingNode = underlyingNode), IsMemory

/** Represents an operation used by the [DynamicLoading] concept. */
abstract class DynamicLoadingOperation<T : Node>(
    underlyingNode: Node? = null,
    concept: Concept,
    /** Represents the entity that we load during runtime. */
    var what: T?,
    /**
     * If this operation is targeting a specific [OperatingSystemArchitecture], it can be specified
     * here.
     */
    var os: OperatingSystemArchitecture? = null,
) : MemoryOperation(underlyingNode = underlyingNode, concept = concept), IsMemory {
    override fun equals(other: Any?): Boolean {
        return other is DynamicLoadingOperation<*> &&
            super.equals(other) &&
            other.what == this.what &&
            other.os == this.os
    }

    override fun hashCode() = Objects.hash(super.hashCode(), what, os)
}

/**
 * Represents an operation that loads a shared library during runtime. A common example would be a
 * call to `dlopen` in C/C++.
 *
 * The [underlyingNode] is most likely a function call and [what] can point to a [Component]
 * representing the library.
 */
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
open class LoadLibrary(
    underlyingNode: Node? = null,
    concept: Concept,
    /** Represents the source code of library that we load in our graph. */
    what: Component?,
    /**
     * Some programming languages support an entry point that is called when a library is
     * dynamically loaded. Examples include `DllMain` in the Win32 API.
     */
    var entryPoints: List<LibraryEntryPoint> = emptyList(),
    /**
     * If this operation is targeting a specific [OperatingSystemArchitecture], it can be specified
     * here.
     */
    os: OperatingSystemArchitecture?,
) :
    DynamicLoadingOperation<Component>(
        underlyingNode = underlyingNode,
        concept = concept,
        what = what,
        os = os,
    ) {

    context(ContextProvider)
    /** Looks up symbol candidates for [symbol] in the [LoadLibrary.what]. */
    fun findSymbol(symbol: Symbol?): List<Declaration> {
        if (symbol == null) {
            return listOf()
        }

        return this.what?.translationUnits?.flatMap { it.scope?.lookupSymbol(symbol) ?: listOf() }
            ?: listOf()
    }

    override fun equals(other: Any?): Boolean {
        return other is LoadLibrary && super.equals(other) && other.entryPoints == this.entryPoints
    }

    override fun hashCode() = Objects.hash(super.hashCode(), entryPoints)
}

/**
 * Represents an operation that loads a symbol during runtime. A common example would be a call to
 * `dlsym` in C/C++.
 *
 * The [underlyingNode] is most likely a function call and [what] can point to a [Declaration]
 * representing the symbol (e.g., a [FunctionDeclaration]) that we load.
 *
 * If we are loading a symbol from an external library, [loader] can point to the [LoadLibrary]
 * operation that loaded the library.
 */
open class LoadSymbol<T : Declaration>(
    underlyingNode: Node? = null,
    concept: Concept,
    /** Represents the symbol's [Declaration] that we load in our graph. */
    what: T?,

    /**
     * If we are loading a symbol from an external library, this points to the [LoadLibrary]
     * operation that loaded the library.
     */
    var loader: LoadLibrary?,
    /**
     * If this operation is targeting a specific [OperatingSystemArchitecture], it can be specified
     * here.
     */
    os: OperatingSystemArchitecture?,
) :
    DynamicLoadingOperation<T>(
        underlyingNode = underlyingNode,
        concept = concept,
        what = what,
        os = os,
    ) {
    override fun equals(other: Any?): Boolean {
        return other is LoadSymbol<*> && super.equals(other) && other.loader == this.loader
    }

    override fun hashCode() = Objects.hash(super.hashCode(), loader)
}
