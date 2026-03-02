package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an authorization operation based on JWT tokens.
 */
public open class AuthorizeJwt(
  operatesOn: Authenticity,
  underlyingNode: Node? = null,
) : AuthenticationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is AuthorizeJwt &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
