package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.MutableList

public open class Redundancy(
  public val geoLocations: MutableList<GeoLocation?>,
  underlyingNode: Node? = null,
) : Availability(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Redundancy &&
              super.equals(other) &&
              other.geoLocations == this.geoLocations

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              geoLocations,
          )
}
