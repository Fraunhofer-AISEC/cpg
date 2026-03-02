package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class ResourceType(
  underlyingNode: Node? = null,
) : Core(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ResourceType &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
