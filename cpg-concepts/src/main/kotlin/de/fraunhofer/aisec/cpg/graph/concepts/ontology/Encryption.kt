package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * This represents an encryption.
 */
public open class Encryption(
  public val basedOn: Cipher?,
  public val secret: Secret?,
  underlyingNode: Node? = null,
) : Confidentiality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Encryption &&
              super.equals(other) &&
              other.basedOn == this.basedOn &&
              other.secret == this.secret

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              basedOn,
              secret,
          )
}
