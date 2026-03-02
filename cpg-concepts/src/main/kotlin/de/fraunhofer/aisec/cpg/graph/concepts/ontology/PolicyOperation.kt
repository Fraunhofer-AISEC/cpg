package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation that is part of a policy. This can be used to define rules or checks that need to be performed to enforce the policy.
 */
public open class PolicyOperation(
  operatesOn: Policy,
  underlyingNode: Node? = null,
) : Operation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is PolicyOperation &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
