package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Int
import kotlin.Short
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

/**
 * This service discloses security advisories, e.g. according to the CSAF standard. It has one or more feeds that contain the actual advisories as well as multiple (public) keys that are used to sign the advisory documents.
 */
public open class SecurityAdvisoryService(
  public val keies: MutableList<Key?>,
  public val securityAdvisoryFeeds: MutableList<SecurityAdvisoryFeed?>,
  ips: Array<String>?,
  ports: Array<Short>?,
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

  override fun equals(other: Any?): Boolean = other is SecurityAdvisoryService &&
              super.equals(other) &&
              other.keies == this.keies &&
              other.securityAdvisoryFeeds == this.securityAdvisoryFeeds

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              keies,
              securityAdvisoryFeeds,
          )
}
