package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class HashOperation(
  public val algorithm: String?,
  public val usesSalt: Boolean?,
  operatesOn: Confidentiality,
  underlyingNode: Node? = null,
) : CryptographicOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is HashOperation &&
              super.equals(other) &&
              other.algorithm == this.algorithm &&
              other.usesSalt == this.usesSalt

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              algorithm,
              usesSalt,
          )
}
