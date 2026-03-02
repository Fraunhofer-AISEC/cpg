package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class AtRestEncryption(
  public val algorithm: String?,
  public val enabled: Boolean?,
  public val keyUrl: String?,
  basedOn: Cipher?,
  secret: Secret?,
  underlyingNode: Node? = null,
) : Encryption(basedOn, secret, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is AtRestEncryption &&
              super.equals(other) &&
              other.algorithm == this.algorithm &&
              other.enabled == this.enabled &&
              other.keyUrl == this.keyUrl

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              algorithm,
              enabled,
              keyUrl,
          )
}
