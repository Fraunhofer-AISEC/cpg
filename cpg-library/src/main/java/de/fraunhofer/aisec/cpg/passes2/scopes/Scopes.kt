/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes2.scopes

import de.fraunhofer.aisec.cpg.graph2.Block
import de.fraunhofer.aisec.cpg.graph2.Node

abstract class Scope<T : Node>(val node: T) {
    var children: MutableList<Scope<*>> = mutableListOf()
    var parent: Scope<*>? = null

    /**
     * Adds a child scope to the list of scope children. It also sets this scope as the parent of
     * the child scope.
     */
    fun addChild(child: Scope<*>) {
        this.children += child

        child.parent = this
    }
}

class BlockScope(node: Block) : Scope<Block>(node) {}
