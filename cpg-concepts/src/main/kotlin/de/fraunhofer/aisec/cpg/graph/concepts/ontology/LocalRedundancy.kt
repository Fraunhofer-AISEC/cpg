package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.collections.MutableList

public open class LocalRedundancy(
  geoLocations: MutableList<GeoLocation?>,
  underlyingNode: Node? = null,
) : Redundancy(geoLocations, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LocalRedundancy &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
