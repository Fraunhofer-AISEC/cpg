package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents a token-based authentication.
 */
public open class TokenBasedAuthentication(
  public val enabled: Boolean?,
  public val enforced: Boolean?,
  public val token: Token?,
  contextIsChecked: Boolean?,
  rotationInterval: Int?,
  underlyingNode: Node? = null,
) : Authenticity(contextIsChecked, rotationInterval, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is TokenBasedAuthentication &&
              super.equals(other) &&
              other.enabled == this.enabled &&
              other.enforced == this.enforced &&
              other.token == this.token

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              enabled,
              enforced,
              token,
          )
}
