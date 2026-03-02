package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.time.Duration

public open class BootLogging(
  enabled: Boolean?,
  monitoringEnabled: Boolean?,
  name: String?,
  retentionPeriod: Duration?,
  securityAlertsEnabled: Boolean?,
  loggingService: LoggingService?,
  underlyingNode: Node? = null,
) : Logging(enabled, monitoringEnabled, name, retentionPeriod, securityAlertsEnabled, loggingService, underlyingNode) {
  init {
    name?.let { this.name = Name(localName = it) }
  }

  override fun equals(other: Any?): Boolean = other is BootLogging &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
