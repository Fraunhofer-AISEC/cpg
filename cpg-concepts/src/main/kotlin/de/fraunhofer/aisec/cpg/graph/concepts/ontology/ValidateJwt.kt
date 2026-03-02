package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation to check the validity of a JWT token.
 */
public open class ValidateJwt(
  operatesOn: Authenticity,
  underlyingNode: Node? = null,
) : AuthenticationOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ValidateJwt &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
