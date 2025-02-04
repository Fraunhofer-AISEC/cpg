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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node

open class MemoryAddress(override var name: Name, var isGlobal: Boolean = false) : Node() {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other != null && other::class != this::class) {
            return false
        }
        // TODO: What else do we need to compare?
        return other is MemoryAddress /*&& name == other.name*/ &&
            id == other.id &&
            isGlobal == other.isGlobal
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

/**
 * There is a value, but we cannot determine it while processing this node. We assume that this
 * value will definitely be set when we really execute the code. E.g., it's set outside the
 * function's context. This is used for a [ParameterDeclaration] and serves as some sort of stepping
 * stone.
 */
class ParameterMemoryValue(override var name: Name) : MemoryAddress(name) {
    // The ParameterMemoryValue is usually the Value of a parameter. Let's use this little helper to
    // get to the parameter's address
    var memoryAddress: Node? = null
}

/** We don't know the value. It might be set somewhere else or not. No idea. */
class UnknownMemoryValue(override var name: Name = Name("")) : MemoryAddress(name) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other != null && other::class != this::class) {
            return false
        }
        // TODO: What else do we need to compare?
        return other is MemoryAddress && name == other.name
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
