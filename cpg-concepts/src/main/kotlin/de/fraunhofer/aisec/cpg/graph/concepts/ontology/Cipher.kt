package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

/**
 * Represents a cipher suite. E.g. `AES-XTS-plain64`.
 */
public open class Cipher(
  public val blockSize: Int?,
  public val cipherName: String?,
  public val keySize: Int?,
  public val padding: Padding?,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Cipher &&
              super.equals(other) &&
              other.blockSize == this.blockSize &&
              other.cipherName == this.cipherName &&
              other.keySize == this.keySize &&
              other.padding == this.padding

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              blockSize,
              cipherName,
              keySize,
              padding,
          )
}
