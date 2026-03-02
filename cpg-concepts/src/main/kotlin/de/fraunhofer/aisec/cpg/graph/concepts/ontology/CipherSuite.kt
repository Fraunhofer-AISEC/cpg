package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList

public open class CipherSuite(
  public val authenticationMechanism: String?,
  public val keyExchangeAlgorithm: String?,
  public val macAlgorithm: String?,
  public val sessionCipher: String?,
  public val ciphers: MutableList<Cipher?>,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is CipherSuite &&
              super.equals(other) &&
              other.authenticationMechanism == this.authenticationMechanism &&
              other.keyExchangeAlgorithm == this.keyExchangeAlgorithm &&
              other.macAlgorithm == this.macAlgorithm &&
              other.sessionCipher == this.sessionCipher &&
              other.ciphers == this.ciphers

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              authenticationMechanism,
              keyExchangeAlgorithm,
              macAlgorithm,
              sessionCipher,
              ciphers,
          )
}
