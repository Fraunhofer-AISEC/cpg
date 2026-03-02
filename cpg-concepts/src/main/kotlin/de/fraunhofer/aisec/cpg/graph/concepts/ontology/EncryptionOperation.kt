package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class EncryptionOperation(
  public val algorithm: String?,
  public val secret: Secret?,
  operatesOn: Encryption,
  underlyingNode: Node? = null,
) : Operation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is EncryptionOperation &&
              super.equals(other) &&
              other.algorithm == this.algorithm &&
              other.secret == this.secret

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              algorithm,
              secret,
          )
}
