package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation that checks whether a user or principal has access to a protected
 */
public open class CheckAccess(
  operatesOn: ProtectedAsset,
  underlyingNode: Node? = null,
) : ProtectedAssetOperation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is CheckAccess &&
              super.equals(other)

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
          )
}
