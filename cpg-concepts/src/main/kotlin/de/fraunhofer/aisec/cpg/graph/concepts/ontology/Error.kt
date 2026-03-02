package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class Error(
  public val message: String?,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is Error &&
              super.equals(other) &&
              other.message == this.message

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              message,
          )
}
