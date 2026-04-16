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
package de.fraunhofer.aisec.cpg.graph.edges.scopes

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Import as ImportNode
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSet
import de.fraunhofer.aisec.cpg.graph.edges.collections.MirroredEdgeCollection
import de.fraunhofer.aisec.cpg.graph.scopes.NamespaceScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import kotlin.reflect.KProperty

/**
 * The style of the import. This can be used to distinguish between different import modes, such as
 * importing a single symbol from a namespace, importing a whole namespace or importing all symbols
 * from a namespace.
 */
enum class ImportStyle {
    /**
     * Imports a single symbol from the target namespace. The current scope will contain a symbol
     * with the same name as the imported symbol.
     *
     * Note: Some languages support importing more than one symbol at a time. In this case, the list
     * is split into multiple [Import] nodes (and [Import] edges).
     */
    IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,

    /**
     * Imports the target namespace as a single symbol. The current scope will contain a symbol with
     * the same name as the imported namespace.
     */
    IMPORT_NAMESPACE,

    /**
     * Imports all symbols from the target namespace. The current scope will contain one new symbol
     * for each symbol in the namespace.
     *
     * This is also known as a "wildcard" import.
     */
    IMPORT_ALL_SYMBOLS_FROM_NAMESPACE,
}

/**
 * This edge represents the import of a [NamespaceScope] into another [Scope]. The [style] of import
 * (e.g., whether only a certain symbol or the whole namespace is imported) is determined by the
 * [declaration].
 */
class Import(start: Scope, end: NamespaceScope, var declaration: ImportNode? = null) :
    Edge<NamespaceScope>(start, end) {

    override var labels = setOf("SCOPE_IMPORT")

    val style: ImportStyle?
        get() = declaration?.style

    init {
        declaration?.import?.let { this.name = it.localName }
    }
}

/** A container to manage [Import] edges. */
class Imports(
    thisRef: Node,
    override var mirrorProperty: KProperty<MutableCollection<Import>>,
    outgoing: Boolean = true,
) :
    EdgeSet<NamespaceScope, Import>(
        thisRef = thisRef,
        init = { start, end -> Import(start as Scope, end) },
        outgoing = outgoing,
    ),
    MirroredEdgeCollection<NamespaceScope, Import>
