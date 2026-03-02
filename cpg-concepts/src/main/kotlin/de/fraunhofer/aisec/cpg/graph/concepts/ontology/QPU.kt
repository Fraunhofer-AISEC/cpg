package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Float
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap

public open class QPU(
  public val oneQubitErrorRate: Float?,
  public val spamErrorRate: Float?,
  public val t1CoherenceTime: Float?,
  public val t2CoherenceTime: Float?,
  public val twoQubitErrorRate: Float?,
  public val universalGateSetEnabled: Boolean?,
  public val errorCorrectionEnabled: Boolean?,
  encryptionInUse: EncryptionInUse?,
  networkInterfaces: MutableList<NetworkInterface?>,
  remoteAttestation: RemoteAttestation?,
  resourceLogging: ResourceLogging?,
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
) : Compute(encryptionInUse, networkInterfaces, remoteAttestation, resourceLogging, internetAccessibleEndpoint, geoLocation, loggings, redundancies, usageStatistics, creation_time, description, resourceId, labels, name, raw, parent, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is QPU &&
              super.equals(other) &&
              other.oneQubitErrorRate == this.oneQubitErrorRate &&
              other.spamErrorRate == this.spamErrorRate &&
              other.t1CoherenceTime == this.t1CoherenceTime &&
              other.t2CoherenceTime == this.t2CoherenceTime &&
              other.twoQubitErrorRate == this.twoQubitErrorRate &&
              other.universalGateSetEnabled == this.universalGateSetEnabled &&
              other.errorCorrectionEnabled == this.errorCorrectionEnabled

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              oneQubitErrorRate,
              spamErrorRate,
              t1CoherenceTime,
              t2CoherenceTime,
              twoQubitErrorRate,
              universalGateSetEnabled,
              errorCorrectionEnabled,
          )
}
