package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

public open class Secret(
  public val keySize: Int?,
  public val basedOn: Cipher?,
  enabled: Boolean?,
  expirationDate: ZonedDateTime?,
  isManaged: Boolean?,
  notBeforeDate: ZonedDateTime?,
  infrastructures: Infrastructure?,
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
) : Credential(enabled, expirationDate, isManaged, notBeforeDate, infrastructures, internetAccessibleEndpoint, geoLocation, loggings, redundancies, usageStatistics, creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is Secret &&
              super.equals(other) &&
              other.keySize == this.keySize &&
              other.basedOn == this.basedOn

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              keySize,
              basedOn,
          )
}
