package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class KeyDerivationFunction(
  public val type: String?,
  public val input: Input?,
  underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
  override fun equals(other: Any?): Boolean = other is KeyDerivationFunction &&
              super.equals(other) &&
              other.type == this.type &&
              other.input == this.input

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              type,
              input,
          )
}
