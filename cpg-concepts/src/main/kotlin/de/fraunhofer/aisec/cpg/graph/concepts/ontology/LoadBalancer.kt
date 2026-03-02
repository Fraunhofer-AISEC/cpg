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
 * A Load Balancer may have multiple access restriction features, e.g. a L3 firewall and a WAF
 */
public open class LoadBalancer(
  public val url: String?,
  public val accessRestriction: Boolean?,
  public val httpEndpoints: MutableList<HttpEndpoint?>,
  public val networkServices: MutableList<NetworkService?>,
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

  override fun equals(other: Any?): Boolean = other is LoadBalancer &&
              super.equals(other) &&
              other.url == this.url &&
              other.accessRestriction == this.accessRestriction &&
              other.httpEndpoints == this.httpEndpoints &&
              other.networkServices == this.networkServices

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              url,
              accessRestriction,
              httpEndpoints,
              networkServices,
          )
}
