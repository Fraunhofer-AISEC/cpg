package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * A common abstract class for memory operations.
 */
public open class MemoryOperation(
  operatesOn: Memory,
  underlyingNode: Node? = null,
) : Operation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is MemoryOperation &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
