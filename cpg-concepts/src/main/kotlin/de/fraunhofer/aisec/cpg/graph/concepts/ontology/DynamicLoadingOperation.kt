package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation used by the [DynamicLoading] concept.
 */
public open class DynamicLoadingOperation(
  public val operatingSystemArchitecture: OperatingSystemArchitecture?,
  operatesOn: Memory,
  underlyingNode: Node? = null,
) : MemoryOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is DynamicLoadingOperation &&
              super.equals(other) &&
              other.operatingSystemArchitecture == this.operatingSystemArchitecture

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              operatingSystemArchitecture,
          )
}
