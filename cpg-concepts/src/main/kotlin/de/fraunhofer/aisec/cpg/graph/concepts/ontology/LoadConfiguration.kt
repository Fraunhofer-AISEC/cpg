package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation to load a configuration from a source, such as a file.
 */
public open class LoadConfiguration(
  operatesOn: Configuration,
  underlyingNode: Node? = null,
) : ConfigurationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LoadConfiguration &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
