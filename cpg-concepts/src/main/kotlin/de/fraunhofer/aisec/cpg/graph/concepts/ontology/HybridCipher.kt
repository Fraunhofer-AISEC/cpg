package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class HybridCipher(
  public val keyDerivationFunction: KeyDerivationFunction?,
  public val messageAuthenticationCode: MessageAuthenticationCode?,
  public val symmetricCipher: SymmetricCipher?,
  public val uses: AsymmetricCipher?,
  blockSize: Int?,
  cipherName: String?,
  keySize: Int?,
  padding: Padding?,
  underlyingNode: Node? = null,
) : Cipher(blockSize, cipherName, keySize, padding, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is HybridCipher &&
              super.equals(other) &&
              other.keyDerivationFunction == this.keyDerivationFunction &&
              other.messageAuthenticationCode == this.messageAuthenticationCode &&
              other.symmetricCipher == this.symmetricCipher &&
              other.uses == this.uses

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              keyDerivationFunction,
              messageAuthenticationCode,
              symmetricCipher,
              uses,
          )
}
