package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.MutableList

public open class MultiFactorAuthentiation(
  public val authenticities: MutableList<Authenticity?>,
  contextIsChecked: Boolean?,
  rotationInterval: Int?,
  underlyingNode: Node? = null,
) : Authenticity(contextIsChecked, rotationInterval, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is MultiFactorAuthentiation &&
              super.equals(other) &&
              other.authenticities == this.authenticities

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              authenticities,
          )
}
