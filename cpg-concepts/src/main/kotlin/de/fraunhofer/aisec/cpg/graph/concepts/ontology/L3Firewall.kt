package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class L3Firewall(
  public val enabled: Boolean?,
  public val inbound: Boolean?,
  public val restrictedPorts: String?,
  underlyingNode: Node? = null,
) : Firewall(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is L3Firewall &&
              super.equals(other) &&
              other.enabled == this.enabled &&
              other.inbound == this.inbound &&
              other.restrictedPorts == this.restrictedPorts

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              enabled,
              inbound,
              restrictedPorts,
          )
}
