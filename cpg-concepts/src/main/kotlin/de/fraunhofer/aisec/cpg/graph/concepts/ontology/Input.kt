package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * A certain Input for e.g. a function.
 */
public open class Input(
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Input &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
