package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.MutableList

public open class ZoneRedundancy(
  geoLocations: MutableList<GeoLocation?>,
  underlyingNode: Node? = null,
) : Redundancy(geoLocations, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ZoneRedundancy &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
