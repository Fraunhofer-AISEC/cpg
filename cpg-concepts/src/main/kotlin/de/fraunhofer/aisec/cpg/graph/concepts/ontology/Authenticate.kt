package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an authentication operation.
 */
public open class Authenticate(
  public val credential: Credential?,
  operatesOn: Authenticity,
  underlyingNode: Node? = null,
) : AuthenticationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Authenticate &&
              super.equals(other) &&
              other.credential == this.credential

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              credential,
          )
}
