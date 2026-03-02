package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an abstract concept of request context. This usually holds the contextual information of a specific HTTP request.
 */
public open class HttpRequestContext(
  underlyingNode: Node? = null,
) : Http(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is HttpRequestContext &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
