package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class MessageAuthenticationCode(
  public val type: String?,
  public val input: Input?,
  public val key: Key?,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is MessageAuthenticationCode &&
              super.equals(other) &&
              other.type == this.type &&
              other.input == this.input &&
              other.key == this.key

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              type,
              input,
              key,
          )
}
