package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

/**
 * A padding for a cipher.
 */
public open class Padding(
  public val scheme: String?,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Padding &&
              super.equals(other) &&
              other.scheme == this.scheme

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              scheme,
          )
}
