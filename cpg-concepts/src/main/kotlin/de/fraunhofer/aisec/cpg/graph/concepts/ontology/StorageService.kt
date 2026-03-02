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
 * This entity represents a network-based service that can be used to access a particular storage backend. It has multiple subclasses, e.g., for databases or object stores. It has a list of storage resources associated to it.
 */
public open class StorageService(
  public val activityLogging: ActivityLogging?,
  public val storage: MutableList<Storage?>,
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

  override fun equals(other: Any?): Boolean = other is StorageService &&
              super.equals(other) &&
              other.activityLogging == this.activityLogging &&
              other.storage == this.storage

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              activityLogging,
              storage,
          )
}
