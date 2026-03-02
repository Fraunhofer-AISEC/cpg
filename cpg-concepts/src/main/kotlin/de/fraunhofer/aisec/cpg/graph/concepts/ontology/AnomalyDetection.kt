package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Analyzes the activity of a NetworkService (which includes DatabaseServices).
 */
public open class AnomalyDetection(
  public val enabled: Boolean?,
  public val applicationLogging: ApplicationLogging?,
  underlyingNode: Node? = null,
) : Auditing(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is AnomalyDetection &&
              super.equals(other) &&
              other.enabled == this.enabled &&
              other.applicationLogging == this.applicationLogging

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              enabled,
              applicationLogging,
          )
}
