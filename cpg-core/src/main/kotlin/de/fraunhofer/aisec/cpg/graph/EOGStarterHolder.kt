/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.EOGStarterPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver

/**
 * This interface needs to be implemented by [Node]s in the graph, wo serve as an entry-point to the
 * EOG. This is primarily used in the [SymbolResolver] for resolution, so that we can follow the EOG
 * path from these nodes and resolve all symbols accordingly. But also other passes might be
 * interested in all EOG start nodes. They can use the [EOGStarterPass] to apply the pass on all
 * such nodes in the graph.
 *
 * In some cases, the [Node] that implements this interface will add itself, for example in a
 * [FunctionDeclaration], so that we can use all functions as an entry-point to symbol resolution.
 * In other cases, certain child nodes might be added to [eogStarters], for example to add all
 * top-level declarations in a [TranslationUnitDeclaration].
 *
 * The common denominator is that all the nodes contained in [eogStarters] **start** an EOG path,
 * i.e., they should have a valid [Node.nextEOG], but an empty [Node.prevEOG].
 */
interface EOGStarterHolder {
    val eogStarters: List<AstNode>
}
