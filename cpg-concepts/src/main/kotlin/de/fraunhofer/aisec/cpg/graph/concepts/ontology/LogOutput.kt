package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

/**
 * A generic Logoutput.
 */
public open class LogOutput(
  public val call: String?,
  public val `value`: String?,
  operatesOn: Logging,
  underlyingNode: Node? = null,
) : Operation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is LogOutput &&
              super.equals(other) &&
              other.call == this.call &&
              other.value == this.value

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              call,
              value,
          )
}
