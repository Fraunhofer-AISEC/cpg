package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.Short
import kotlin.String
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

/**
 * A generic network service.
 */
public open class GenericNetworkService(
  ips: List<String>?,
  ports: List<Short>?,
  authenticity: Boolean?,
  computes: MutableList<Compute?>,
  serviceMetadataDocument: ServiceMetadataDocument?,
  transportEncryption: TransportEncryption?,
  internetAccessibleEndpoint: Boolean?,
  geoLocation: GeoLocation?,
  loggings: MutableList<Logging?>,
  redundancies: Redundancy?,
  usageStatistics: UsageStatistics?,
  creation_time: ZonedDateTime?,
  description: String?,
  resourceId: String?,
  labels: MutableMap<String, String>?,
  name: String?,
  raw: String?,
  parent: Resource?,
  underlyingNode: Node? = null,
) : NetworkService(ips, ports, authenticity, computes, serviceMetadataDocument, transportEncryption, internetAccessibleEndpoint, geoLocation, loggings, redundancies, usageStatistics, creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is GenericNetworkService &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
