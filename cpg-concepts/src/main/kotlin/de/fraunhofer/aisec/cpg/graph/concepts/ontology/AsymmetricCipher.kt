package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class AsymmetricCipher(
  blockSize: Int?,
  cipherName: String?,
  keySize: Int?,
  padding: Padding?,
  underlyingNode: Node? = null,
) : Cipher(blockSize, cipherName, keySize, padding, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is AsymmetricCipher &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
