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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects

/** The memory management mode of a memory concept. */
enum class MemoryManagementMode {
    /** The memory is unmanaged and must be manually allocated and de-allocated. */
    UNMANAGED,

    /**
     * The memory is managed by the runtime environment. This can be done using automatic memory
     * management, reference counting or garbage collection.
     */
    MANAGED_WITH_REFERENCE_COUNTING,

    /** The memory is managed by the runtime environment using garbage collection. */
    MANAGED_WITH_GARBAGE_COLLECTION,
}

/**
 * A generic concept to describe memory operations with a program. This includes allocation and
 * de-allocation of memory as well as copying memory regions.
 *
 * @param underlyingNode The underlying node in the graph that represents this memory concept.
 * @param mode The memory management mode of the memory concept.
 */
class Memory(underlyingNode: Node? = null, val mode: MemoryManagementMode) :
    Concept(underlyingNode = underlyingNode), IsMemory

/** A common interface for the "memory" sub-graph. */
interface IsMemory

/** A common abstract class for memory operations. */
abstract class MemoryOperation(underlyingNode: Node?, concept: Concept) :
    Operation(underlyingNode = underlyingNode, concept = concept), IsMemory

/**
 * Represents a memory allocation operation. This can be done using `malloc` in C or `new` in C++ or
 * by calling a constructor in managed languages.
 */
class Allocate(
    underlyingNode: Node? = null,
    concept: Concept,
    /** A reference to [what] is allocated, e.g., a variable. */
    var what: Node?,
) : MemoryOperation(underlyingNode = underlyingNode, concept = concept) {
    override fun equalWithoutUnderlying(other: OverlayNode): Boolean {
        return other is Allocate && super.equalWithoutUnderlying(other) && other.what == this.what
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), what)
    }
}

/**
 * Represents a memory de-allocation operation. This can be done using `free` in C or `delete` in
 * C++ or by calling a destructor in managed languages.
 */
class DeAllocate(
    underlyingNode: Node? = null,
    concept: Concept,
    /** A reference to [what] is de-allocated, e.g., a variable. */
    var what: Node?,
) : MemoryOperation(underlyingNode = underlyingNode, concept = concept) {
    override fun equalWithoutUnderlying(other: OverlayNode): Boolean {
        return other is Allocate && super.equalWithoutUnderlying(other) && other.what == this.what
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), what)
    }
}
