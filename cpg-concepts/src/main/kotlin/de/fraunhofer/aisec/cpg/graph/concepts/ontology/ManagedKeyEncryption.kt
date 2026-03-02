package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class ManagedKeyEncryption(
  algorithm: String?,
  enabled: Boolean?,
  keyUrl: String?,
  basedOn: Cipher?,
  secret: Secret?,
  underlyingNode: Node? = null,
) : AtRestEncryption(algorithm, enabled, keyUrl, basedOn, secret, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ManagedKeyEncryption &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
