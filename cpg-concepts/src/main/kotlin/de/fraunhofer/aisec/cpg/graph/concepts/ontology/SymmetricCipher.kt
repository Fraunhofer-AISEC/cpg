package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

/**
 * Represents a symmetric cipher.
 */
public open class SymmetricCipher(
  public val authTagSize: Int?,
  public val modus: String?,
  public val initializationVector: InitializationVector?,
  blockSize: Int?,
  cipherName: String?,
  keySize: Int?,
  padding: Padding?,
  underlyingNode: Node? = null,
) : Cipher(blockSize, cipherName, keySize, padding, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is SymmetricCipher &&
              super.equals(other) &&
              other.authTagSize == this.authTagSize &&
              other.modus == this.modus &&
              other.initializationVector == this.initializationVector

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              authTagSize,
              modus,
              initializationVector,
          )
}
