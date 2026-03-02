package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class Firewall(
  underlyingNode: Node? = null,
) : AccessRestriction(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Firewall &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
