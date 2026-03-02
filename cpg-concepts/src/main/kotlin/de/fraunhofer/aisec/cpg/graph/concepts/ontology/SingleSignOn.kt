package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class SingleSignOn(
  public val enabled: Boolean?,
  contextIsChecked: Boolean?,
  rotationInterval: Int?,
  underlyingNode: Node? = null,
) : Authenticity(contextIsChecked, rotationInterval, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is SingleSignOn &&
              super.equals(other) &&
              other.enabled == this.enabled

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              enabled,
          )
}
