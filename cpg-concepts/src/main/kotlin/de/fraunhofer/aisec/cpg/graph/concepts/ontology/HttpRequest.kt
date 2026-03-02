package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class HttpRequest(
  public val call: String?,
  public val reqBody: String?,
  public val httpEndpoint: HttpEndpoint?,
  operatesOn: HttpClient,
  underlyingNode: Node? = null,
) : HttpClientOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is HttpRequest &&
              super.equals(other) &&
              other.call == this.call &&
              other.reqBody == this.reqBody &&
              other.httpEndpoint == this.httpEndpoint

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              call,
              reqBody,
              httpEndpoint,
          )
}
