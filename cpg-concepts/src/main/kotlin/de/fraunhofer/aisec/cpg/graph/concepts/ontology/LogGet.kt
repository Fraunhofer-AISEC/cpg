package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * A log get operation e.g. `logging.getLogger("...")`.
 */
public open class LogGet(
  operatesOn: Logging,
  underlyingNode: Node? = null,
) : LogOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LogGet &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
