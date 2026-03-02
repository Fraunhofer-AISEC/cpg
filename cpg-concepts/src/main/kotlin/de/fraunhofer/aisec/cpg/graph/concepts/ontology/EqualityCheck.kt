package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation that checks whether two principals are equal.
 */
public open class EqualityCheck(
  public val leftPrincipal: Principal?,
  public val rightPrincipal: Principal?,
  operatesOn: Policy,
  underlyingNode: Node? = null,
) : PolicyOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is EqualityCheck &&
              super.equals(other) &&
              other.leftPrincipal == this.leftPrincipal &&
              other.rightPrincipal == this.rightPrincipal

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              leftPrincipal,
              rightPrincipal,
          )
}
