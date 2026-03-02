package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Rate limiting is used to control the frequency of incoming requests to prevent system overload
 */
public open class RateLimiting(
  public val enabled: Boolean?,
  public val maxRequests: Int?,
  public val timeWindowSeconds: Int?,
  underlyingNode: Node? = null,
) : AccessRestriction(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is RateLimiting &&
              super.equals(other) &&
              other.enabled == this.enabled &&
              other.maxRequests == this.maxRequests &&
              other.timeWindowSeconds == this.timeWindowSeconds

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              enabled,
              maxRequests,
              timeWindowSeconds,
          )
}
