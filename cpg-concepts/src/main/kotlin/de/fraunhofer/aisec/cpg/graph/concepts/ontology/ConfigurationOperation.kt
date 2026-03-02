package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class ConfigurationOperation(
  operatesOn: Configuration,
  underlyingNode: Node? = null,
) : Operation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ConfigurationOperation &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
