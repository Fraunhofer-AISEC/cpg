package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * A WAF is a L7 firewall that includes L3 capabilities
 */
public open class WebApplicationFirewall(
  public val enabled: Boolean?,
  underlyingNode: Node? = null,
) : Firewall(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is WebApplicationFirewall &&
              super.equals(other) &&
              other.enabled == this.enabled

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              enabled,
          )
}
