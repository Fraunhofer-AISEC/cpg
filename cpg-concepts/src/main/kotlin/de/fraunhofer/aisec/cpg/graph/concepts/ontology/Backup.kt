package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.time.Duration

/**
 * RetentionPeriod in hours
 */
public open class Backup(
  public val enabled: Boolean?,
  public val interval: Duration?,
  public val retentionPeriod: Duration?,
  public val storage: Storage?,
  public val transportEncryption: Boolean?,
  underlyingNode: Node? = null,
) : Availability(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Backup &&
              super.equals(other) &&
              other.enabled == this.enabled &&
              other.interval == this.interval &&
              other.retentionPeriod == this.retentionPeriod &&
              other.storage == this.storage &&
              other.transportEncryption == this.transportEncryption

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              enabled,
              interval,
              retentionPeriod,
              storage,
              transportEncryption,
          )
}
