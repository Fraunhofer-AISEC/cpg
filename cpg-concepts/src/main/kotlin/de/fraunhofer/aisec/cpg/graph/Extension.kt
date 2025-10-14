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

import de.fraunhofer.aisec.cpg.graph.ast.AstNode
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import org.reflections.Reflections
import org.reflections.scanners.Scanners.SubTypes

/**
 * Retrieves a set of all [Concept] nodes associated with this [Node] and its AST children (if this
 * is an [ast.AstNode], see [ast.AstNode.nodes]).
 *
 * @return A set containing all [Concept] nodes found in the overlays of the [Node] and its eventual
 *   AST children.
 */
val Node.conceptNodes: Set<Concept>
    get() {
        return allOverlays<Concept>()
    }

/**
 * Retrieves a set of all [Operation] nodes associated with this [Node] and its AST children (if
 * this is an [ast.AstNode], see [ast.AstNode.nodes]).
 *
 * @return A set containing all [Operation] nodes found in the overlays of the [Node] and its
 *   eventual AST children.
 */
val Node.operationNodes: Set<Operation>
    get() {
        return allOverlays<Operation>()
    }

/**
 * Retrieves a set of all [T] overlay nodes associated with this [Node] and its AST children (if
 * this is an [ast.AstNode], see [ast.AstNode.nodes]).
 */
inline fun <reified T : OverlayNode> Node.allOverlays(): Set<T> {
    return (this as? AstNode)?.nodes?.flatMap { it.overlays }?.filterIsInstance<T>()?.toSet()
        ?: emptySet()
}

/** Returns a [Set] of all subclasses of the class [T]. */
inline fun <reified T : OverlayNode> listOverlayClasses(
    prefix: String = "de.fraunhofer.aisec"
): Set<Class<out T>> = Reflections(prefix, SubTypes).getSubTypesOf(T::class.java)
