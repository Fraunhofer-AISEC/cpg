package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents a JWT-based authentication, which extends the [TokenBasedAuth].
 */
public open class JwtAuthentication(
  enabled: Boolean?,
  enforced: Boolean?,
  token: Token?,
  contextIsChecked: Boolean?,
  rotationInterval: Int?,
  underlyingNode: Node? = null,
) : TokenBasedAuthentication(enabled, enforced, token, contextIsChecked, rotationInterval, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is JwtAuthentication &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
