package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an entry point that can be triggered remotely, such as a network endpoint.
 */
public open class RemoteEntryPoint(
  underlyingNode: Node? = null,
) : EntryPoint(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is RemoteEntryPoint &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
