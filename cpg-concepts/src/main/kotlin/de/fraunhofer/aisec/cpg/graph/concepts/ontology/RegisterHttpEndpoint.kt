package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class RegisterHttpEndpoint(
  public val httpEndpoint: HttpEndpoint?,
  operatesOn: HttpRequestHandler,
  underlyingNode: Node? = null,
) : HttpRequestHandlerOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is RegisterHttpEndpoint &&
              super.equals(other) &&
              other.httpEndpoint == this.httpEndpoint

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              httpEndpoint,
          )
}
