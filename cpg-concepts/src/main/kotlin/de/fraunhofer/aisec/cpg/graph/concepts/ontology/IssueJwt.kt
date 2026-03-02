package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation to issue a new JWT token.
 */
public open class IssueJwt(
  operatesOn: Authenticity,
  underlyingNode: Node? = null,
) : AuthenticationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is IssueJwt &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
