package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class Integrity(
  underlyingNode: Node? = null,
) : SecurityFeature(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Integrity &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
