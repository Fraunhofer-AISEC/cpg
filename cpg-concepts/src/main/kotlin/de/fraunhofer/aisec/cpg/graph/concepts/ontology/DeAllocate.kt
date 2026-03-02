package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents a memory de-allocation operation. This can be done using `free` in C or `delete` in C++ or by calling a destructor in managed languages.
 */
public open class DeAllocate(
  operatesOn: Memory,
  underlyingNode: Node? = null,
) : MemoryOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is DeAllocate &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
