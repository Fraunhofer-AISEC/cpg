package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.MutableList

public open class CryptographicHash(
  public val algorithm: String?,
  public val errors: MutableList<Error?>,
  underlyingNode: Node? = null,
) : Integrity(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is CryptographicHash &&
              super.equals(other) &&
              other.algorithm == this.algorithm &&
              other.errors == this.errors

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              algorithm,
              errors,
          )
}
