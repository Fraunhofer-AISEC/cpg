package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public open class DatabaseOperation(
  public val calls: List<String>?,
  public val databaseService: DatabaseService?,
  operatesOn: DatabaseStorage,
  underlyingNode: Node? = null,
) : Operation(operatesOn, underlyingNode) {
  override fun equals(other: Any?): Boolean = other is DatabaseOperation &&
              super.equals(other) &&
              other.calls == this.calls &&
              other.databaseService == this.databaseService

  override fun hashCode(): Int = Objects.hash(
              super.hashCode(),
              calls,
              databaseService,
          )
}
