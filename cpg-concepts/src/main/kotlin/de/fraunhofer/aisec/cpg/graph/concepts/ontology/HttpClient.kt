package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * HTTP Client.
 */
public open class HttpClient(
  public val isTLS: Boolean?,
  public val authenticity: Authenticity?,
  underlyingNode: Node? = null,
) : Http(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is HttpClient &&
              super.equals(other) &&
              other.isTLS == this.isTLS &&
              other.authenticity == this.authenticity

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              isTLS,
              authenticity,
          )
}
