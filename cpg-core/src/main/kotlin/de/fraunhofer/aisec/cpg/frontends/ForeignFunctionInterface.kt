/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * The CPG can hold nodes of different programming languages at the same time. There are several
 * frameworks which allow calling functions from one language to another one. Examples which we also
 * use in this repository are JNI or jep. However, the
 * [de.fraunhofer.aisec.cpg.passes.SymbolResolver] won't be able to resolve functions across such
 * interfaces for several reasons: First, it expects that the symbol and the declaration it refers
 * to are of the same language. Second, the interfaces may slightly change the name and for sure the
 * [Type]s (since each type has its own language).
 *
 * This class provides a way to specify such interfaces between two programming languages. It models
 * the way to resolve a symbol from the language [from] (e.g., caller) to the language [to] (e.g.,
 * callee, declaration).
 */
abstract class ForeignFunctionInterface<FROM : Language<*>, TO : Language<*>>(
    val from: FROM,
    val to: TO,
) {

    /**
     * Maps the [Symbol] [from] the [FROM] language to a [Symbol] of the [TO] language. This is
     * necessary to ensure that we can properly connect the graph nodes of the [FROM] language to
     * the graph nodes of the [TO] language.
     */
    abstract fun mapSymbol(from: Symbol): Symbol

    /**
     * Maps the [Type] [from] the [FROM] language to the [Type] of the [TO] language. This is
     * necessary to ensure that we can properly connect the graph nodes of the [FROM] language to
     * the graph nodes of the [TO] language.
     */
    abstract fun mapType(from: Type): Type
}
