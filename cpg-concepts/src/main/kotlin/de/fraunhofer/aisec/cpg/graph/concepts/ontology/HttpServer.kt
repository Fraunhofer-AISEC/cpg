package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

public open class HttpServer(
  public val httpRequestHandler: HttpRequestHandler?,
  underlyingNode: Node? = null,
) : Framework(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is HttpServer &&
              super.equals(other) &&
              other.httpRequestHandler == this.httpRequestHandler

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              httpRequestHandler,
          )
}
