package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class Decryption(
  public val algorithm: String?,
  public val secret: Secret?,
  operatesOn: Cipher,
  underlyingNode: Node? = null,
) : CipherOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Decryption &&
              super.equals(other) &&
              other.algorithm == this.algorithm &&
              other.secret == this.secret

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              algorithm,
              secret,
          )
}
