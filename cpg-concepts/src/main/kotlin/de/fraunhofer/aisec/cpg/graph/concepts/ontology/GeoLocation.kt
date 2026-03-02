package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class GeoLocation(
  public val region: String?,
  underlyingNode: Node? = null,
) : Availability(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is GeoLocation &&
              super.equals(other) &&
              other.region == this.region

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              region,
          )
}
