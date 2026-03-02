package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class ObjectStorageRequest(
  public val source: String?,
  public val objectStorage: ObjectStorage?,
  operatesOn: Storage,
  underlyingNode: Node? = null,
) : Operation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is ObjectStorageRequest &&
              super.equals(other) &&
              other.source == this.source &&
              other.objectStorage == this.objectStorage

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              source,
              objectStorage,
          )
}
