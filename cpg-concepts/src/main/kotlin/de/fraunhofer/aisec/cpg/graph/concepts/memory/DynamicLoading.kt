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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol

/**
 * Represents an entity that loads a piece of code dynamically during runtime. Examples include a
 * class loader in Java, loading shared library code in C++. Interpreters, such as Python can also
 * load code dynamically during runtime.
 */
class DynamicLoading(underlyingNode: Node) :
    Concept<DynamicLoadingOperation<*>>(underlyingNode = underlyingNode), IsMemory

/** Represents an operation used by the [DynamicLoading] concept. */
abstract class DynamicLoadingOperation<T : Node>(
    underlyingNode: Node,
    concept: Concept<DynamicLoadingOperation<T>>,
    /** Represents the entity that we load during runtime. */
    var what: T? = null,
) : MemoryOperation(underlyingNode = underlyingNode, concept = concept), IsMemory

/**
 * Represents an operation that loads a shared library during runtime. A common example would be a
 * call to `dlopen` in C/C++.
 *
 * The [underlyingNode] is most likely a function call and [what] can point to a [Component]
 * representing the library.
 */
class LoadLibrary(
    underlyingNode: Node,
    concept: Concept<DynamicLoadingOperation<Component>>,
    /** Represents the source code of library that we load in our graph. */
    what: Component? = null,
) :
    DynamicLoadingOperation<Component>(
        underlyingNode = underlyingNode,
        concept = concept,
        what = what,
    ) {

    /** Looks up symbol candidates for [symbol] in the [LoadLibrary.what]. */
    fun findSymbol(symbol: Symbol?): List<Declaration> {
        if (symbol == null) {
            return listOf()
        }

        return this.what?.translationUnits?.flatMap { it.scope?.lookupSymbol(symbol) ?: listOf() }
            ?: listOf()
    }
}

/**
 * Represents an operation that loads a function during runtime. A common example would be a call to
 * `dlsym` in C/C++.
 *
 * The [underlyingNode] is most likely a function call and [what] can point to a
 * [FunctionDeclaration] representing the function that we load.
 *
 * If we are loading a function from an external library, [loader] can point to the [LoadLibrary]
 * operation that loaded the library.
 */
class LoadFunction(
    underlyingNode: Node,
    concept: Concept<DynamicLoadingOperation<FunctionDeclaration>>,
    /** Represents the function that we load in our graph. */
    what: FunctionDeclaration? = null,

    /**
     * If we are loading a function from an external library, this points to the [LoadLibrary]
     * operation that loaded the library.
     */
    loader: LoadLibrary? = null,
) :
    DynamicLoadingOperation<FunctionDeclaration>(
        underlyingNode = underlyingNode,
        concept = concept,
        what = what,
    )

/**
 * Represents an operation that loads a record / class during runtime. A common example would be a
 * call to `Class.forName` in Java or approaches in Python to load classes dynamically.
 */
class LoadRecord(
    underlyingNode: Node,
    concept: Concept<DynamicLoadingOperation<RecordDeclaration>>,
    /** Represents the record that we load in our graph. */
    what: RecordDeclaration? = null,
) :
    DynamicLoadingOperation<RecordDeclaration>(
        underlyingNode = underlyingNode,
        concept = concept,
        what = what,
    )
